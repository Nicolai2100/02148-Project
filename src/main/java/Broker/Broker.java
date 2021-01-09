package Broker;

import model.StockInfo;
import org.jspace.*;

import java.util.concurrent.*;

public class Broker {

    //Brokerens hostname og port
    String hostName = "localhost";
    int port = 9001;

    SequentialSpace stocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace marketOrders = new SequentialSpace();
    SequentialSpace marketOrdersInProcess = new SequentialSpace();
    SequentialSpace limitOrders = new SequentialSpace(); //TODO: Bør både market og limit orders være i samme space?
    SequentialSpace transactions = new SequentialSpace();

    SpaceRepository tradeRepo = new SpaceRepository();

    static final String sellOrderFlag = "SELL";
    static final String buyOrderFlag = "BUY";
    static final String msgFlag = "MSG";

    ExecutorService executor = Executors.newCachedThreadPool();
    static final int standardTimeout = 5; //TODO: Consider what this should be, or make it possible to set it per order.
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

    private void startService() throws InterruptedException {
        serviceRunning = true;
        stocks.put("AAPL", 110);
        executor.submit(new Broker.Broker.MarketOrderHandler());
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
                    marketOrdersInProcess.put(
                            order.getOrderedBy(),
                            order.getOrderType(),
                            order.getStock(),
                            order.getQuantity()
                    );
                    Future<String> future = executor.submit(new FindMatchingBuyOrderHandler(order));
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

        public FindMatchingBuyOrderHandler(MarketOrder order) {
            this.order = order;
            matchingOrderType = order.getOrderType().equals(sellOrderFlag) ? buyOrderFlag : sellOrderFlag;
        }

        Callable<BuyMarketOrder> findMatchTask = () -> new BuyMarketOrder(marketOrdersInProcess.get(
                new FormalField(String.class),
                new ActualField(matchingOrderType),
                new ActualField(order.getStock()),
                new FormalField(Integer.class)));

        @Override
        public String call() throws InterruptedException {
            try {
                MarketOrder matchOrder = executor.submit(findMatchTask).get(standardTimeout, timeoutUnit);

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
                System.out.printf("%s sold %d shares of %s to %s.%n", order.getOrderedBy(), min, order.getStock(), matchOrder.getOrderedBy());

                if (min < order.getQuantity()) {
                    System.out.printf("%s sold less shares than he/her wanted. Placing new sale order of %d shares of %s.%n", order.getOrderedBy(), order.getQuantity() - min, order.getStock());
                    marketOrders.put(
                            order.getOrderedBy(),
                            sellOrderFlag,
                            order.getStock(),
                            order.getQuantity() - min);
                } else if (min < matchOrder.getQuantity()) {
                    System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.%n", matchOrder.getOrderedBy(), matchOrder.getQuantity() - min, matchOrder.getStock());
                    marketOrders.put(
                            matchOrder.getOrderedBy(),
                            buyOrderFlag,
                            matchOrder.getStock(),
                            matchOrder.getQuantity() - min);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                marketOrders.put(order.getOrderedBy(), msgFlag, "Sale order failed due to timeout.");
            }
            return null;
        }
    }

    //TODO: Denne handler skal nok hellere være i "banken" eller hvor det nu ellers er, at transaktioner skal udføres.. I hvert fald der hvor "transaction spacet" er.
    class TransactionsHandler extends Thread {
        @Override
        public void run() {
            while(serviceRunning) {
                try {
                    Transaction t = new Transaction(transactions.get(
                            new FormalField(String.class),
                            new FormalField(String.class),
                            new FormalField(String.class),
                            new FormalField(Integer.class),
                            new FormalField(Integer.class)
                    ));
                    //TODO: Udfør transaktionen..

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}