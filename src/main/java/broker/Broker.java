package broker;

import org.jspace.*;
import returntypes.StockInfo;

import java.util.UUID;
import java.util.concurrent.*;

import static shared.Channels.*;
import static shared.Requests.*;


public class Broker {

    //Brokerens hostname og port
    String hostName = BROKER_HOSTNAME;
    int port = BROKER_PORT;

    SequentialSpace stocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace marketOrders = new SequentialSpace();
    SequentialSpace marketOrdersInProcess = new SequentialSpace(); //TODO: Denne kan i princippet slås sammen med marketOrders, og det bør stadig fungere.
    SequentialSpace limitOrders = new SequentialSpace(); //TODO: Bør både market og limit orders være i samme space?
    SequentialSpace transactions = new SequentialSpace();

    SpaceRepository tradeRepo = new SpaceRepository();

    ExecutorService executor = Executors.newCachedThreadPool();
    static final int standardTimeout = 1; //TODO: Consider what this should be, or make it possible to set it per order.
    static final TimeUnit timeoutUnit = TimeUnit.SECONDS;
    boolean serviceRunning;

    public Broker() {
        tradeRepo.add("marketOrders", marketOrders);
        tradeRepo.addGate("tcp://" + hostName + ":" + port + "/?keep");
    }

    public static void main(String[] args) throws InterruptedException {
        Broker broker = new Broker();
        broker.startService();
    }

    public void startService() throws InterruptedException {
        serviceRunning = true;
        stocks.put("AAPL", 110);
        marketOrdersInProcess.put(LOCK);
        executor.submit(new MarketOrderHandler());
    }

    /**
     * The responsibility of this class is to constantly handle new orders to sell shares of a stock.
     * This is done by starting a new thread that tries to find a matching buyer of the shares.
     */
    class MarketOrderHandler implements Callable<String> {
        @Override
        public String call() throws Exception {
            while(serviceRunning) {
                try {
                    MarketOrder order = new MarketOrder(marketOrders.get(
                            new FormalField(String.class), //Name of the client who made the order
                            new FormalField(String.class), //Type of order, eg. SELL or BUY
                            new FormalField(String.class), //Name of the stock
                            new FormalField(Integer.class))); //Quantity
                    order.setId(UUID.randomUUID().toString()); //TODO: Skal dette gøres anderledes?

                    System.out.println("Broker: ");
                    System.out.println(order.getOrderedBy());
                    System.out.println(order.getOrderType());
                    System.out.println(order.getStatus());

                    marketOrders.put("Hello " + order.getOrderedBy() + " from broker");


                    marketOrdersInProcess.put(
                            order.getId(),
                            order.getOrderedBy(),
                            order.getOrderType(),
                            order.getStock(),
                            order.getQuantity()
                    );
                    executor.submit(new FindMatchingBuyOrderHandler(order));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return "Handler for handling market sale orders stopped!";
        }
    }

    /**
     * The responsibility of this class is to take a sale order as an argument, and then try
     * to find a matching buyer of the shares.
     */
    class FindMatchingBuyOrderHandler implements Callable<String> {

        private MarketOrder order;
        String matchingOrderType;
        TemplateField[] thisOrderTemplateFields;
        TemplateField[] matchingTemplateFields;

        public FindMatchingBuyOrderHandler(MarketOrder order) {
            this.order = order;
            matchingOrderType = order.getOrderType().equals(SELL) ? BUY : SELL;
            thisOrderTemplateFields = new TemplateField[]{
                    new ActualField(order.getId()),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(Integer.class)
            };
            matchingTemplateFields = new TemplateField[]{
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new ActualField(matchingOrderType),
                    new ActualField(order.getStock()),
                    new FormalField(Integer.class)
            };
        }

        Callable<MarketOrder> queryMatchTask = () -> new MarketOrder(marketOrdersInProcess.query(matchingTemplateFields));

        @Override
        public String call() throws InterruptedException {
            try {
                MarketOrder matchOrderQuery = executor.submit(queryMatchTask).get(standardTimeout, timeoutUnit);

                marketOrdersInProcess.get(new ActualField(LOCK));

                //Object[] matchingGetRes = marketOrdersInProcess.queryp(matchingTemplateFields);
                Object[] thisOrderRes = marketOrdersInProcess.queryp(thisOrderTemplateFields);
                if (thisOrderRes == null) {      //matchingGetRes == null ||
                    //If null, it should mean that this particular order has aldready been processed.
                    //In that case, just put the lock back, and let the task finish.
                    //TODO: Eller hvad, skal der gøres noget andet?
                    marketOrdersInProcess.put(LOCK);
                    return null; //TODO: Skal der gøres mere her?
                }

                MarketOrder matchOrder = new MarketOrder(marketOrdersInProcess.get(matchingTemplateFields));
                marketOrdersInProcess.get(thisOrderTemplateFields);

                marketOrdersInProcess.put(LOCK);

                //Scenarios:
                //1. The seller wants to sell more than the buyer. The buyer get to buy all the shares he/she wants. The seller makes a new order.
                //2. The buyer wants to buy more than the seller. The seller sells everything. The buyer makes a new order.
                //3. Seller and buyer wants to buy/sell the same amount.

                //We choose the smallest of the two quantities to find the max number of shares
                //that the two clients may trade.
                int min = Math.min(order.getQuantity(), matchOrder.getQuantity());

                //We find the current stock price
                Object[] res = stocks.queryp(new ActualField(order.getStock()), new FormalField(Integer.class));
                if (res == null) return null; //TODO: Bør der gives besked til klienten om, at der er sket en fejl – eller sørger vi for dette et andet sted?
                StockInfo stockInfo = new StockInfo(res);

                //Here we send a message (to the bank?) to complete the transaction.
                transactions.put(order.getOrderedBy(), matchOrder.getOrderedBy(), stockInfo.getName(), stockInfo.getPrice(), min);
                //TODO: Get OK or NOT OK from bank the confirm/not confirm the transaction. Do something with the response.

                System.out.printf("%s sold %d shares of %s to %s.%n", order.getOrderedBy(), min, order.getStock(), matchOrder.getOrderedBy());

                if (min < order.getQuantity()) {
                    if (order.getOrderType().equals(SELL))
                        System.out.printf("%s sold less shares than he/her wanted. Placing new sale order of %d shares of %s.%n", order.getOrderedBy(), order.getQuantity() - min, order.getStock());
                    if (order.getOrderType().equals(BUY))
                        System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.%n", order.getOrderedBy(), order.getQuantity() - min, order.getStock());

                    marketOrders.put(
                            order.getOrderedBy(),
                            SELL,
                            order.getStock(),
                            order.getQuantity() - min);
                } else if (min < matchOrder.getQuantity()) {
                    if (matchOrder.getOrderType().equals(SELL))
                        System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.%n", matchOrder.getOrderedBy(), matchOrder.getQuantity() - min, matchOrder.getStock());
                    if (matchOrder.getOrderType().equals(BUY))
                        System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.%n", matchOrder.getOrderedBy(), matchOrder.getQuantity() - min, matchOrder.getStock());
                    marketOrders.put(
                            matchOrder.getOrderedBy(),
                            BUY,
                            matchOrder.getStock(),
                            matchOrder.getQuantity() - min);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                marketOrders.put(order.getOrderedBy(), MSG, "Sale order failed due to timeout.");
            }
            return null;
        }
    }

    void putMarketOrder(MarketOrder order, Space space) {
        try {
            space.put(
                    order.getId(),
                    order.getOrderedBy(),
                    order.getOrderType(),
                    order.getStock(),
                    order.getQuantity()
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}