package Broker;

import model.StockInfo;
import org.jspace.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.*;

public class Broker {

    //Brokerens hostname og port
    String hostName = "localhost";
    int port = 9001;

    SequentialSpace stocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace marketOrders = new SequentialSpace();
    SequentialSpace limitOrders = new SequentialSpace(); //TODO: Bør både market og limit orders være i samme space?
    SequentialSpace transactions = new SequentialSpace();
    SpaceRepository tradeRepo = new SpaceRepository();
    ArrayList<SpaceRepository> remoteStocks = new ArrayList<SpaceRepository>();
    static final String sellOrderFlag = "SELL";
    static final String buyOrderFlag = "BUY";
    static final String msgFlag = "MSG";

    ExecutorService executor = Executors.newCachedThreadPool();
    static final int standardTimeout = 5; //TODO: Consider what this should be, or make it possible to set it per order.
    static final TimeUnit timeoutUnit = TimeUnit.SECONDS;
    boolean serviceRunning;

    public Broker() {
        tradeRepo.add("marketOrders", marketOrders);
        tradeRepo.add("AAPL", new SequentialSpace());
        tradeRepo.addGate("tcp://" + hostName + ":" + port + "/?keep");
        remoteStocks.add(new SpaceRepository());
        remoteStocks.get(0).add("AAPL", new SequentialSpace());
        SpaceRepository object = remoteStocks.get(0);
    }

    public static void main(String[] args) throws InterruptedException {
        Broker broker = new Broker();
        broker.startService();
    }
    SequentialSpace findstock (String stock) {
        SequentialSpace sequentialSpace = (SequentialSpace) tradeRepo.get(stock);
        if (sequentialSpace == null) {
            sequentialSpace = new SequentialSpace();
            try {
                sequentialSpace.put("lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tradeRepo.add(stock, sequentialSpace);
            return sequentialSpace;
        } else {
            return sequentialSpace;
        }
    }

    boolean buyOrder (String stock, int amount, String buyer) {
        SequentialSpace sequentialSpace = findstock(stock);
        try {
            sequentialSpace.get(new ActualField("lock"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
            // Seller tuple: seller, amount, atomic
            try {
                LinkedList<Object[]> objects = sequentialSpace.queryAll(new FormalField(String.class),
                        new FormalField(Integer.class),
                        new FormalField(Boolean.class));
                ArrayList<Integer, String> sellerlist = new ArrayList<>();
                int foundstocks = 0;
                for (Object[] object : objects) {
                    if (foundstocks + (int) object[1] < amount) {
                        foundstocks += (int) object[1];
                        sellerlist.add((String) object[0]);
                    } else if (foundstocks + (int) object[1] > amount && (boolean) object[2]) {
                        sequentialSpace.get(new ActualField(object[0]),
                                new FormalField(Integer.class),
                                new FormalField(Boolean.class));
                        sequentialSpace.put(object[0], foundstocks + (int) object[1] - amount, object[2]);

                    } else if (foundstocks + (int) object[1] == amount) {
                        // do what
                        sellerlist.add((String) object[0]);
                        for (String seller : sellerlist) {
                            sequentialSpace.get(new ActualField(seller),
                                    new FormalField(Integer.class),
                                    new FormalField(Boolean.class));
                        }
                        return true;
                    }
                }
                // Clean up the now sold ones

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        return false;
    }

    void sellOrder (String stock, String seller, int amount, boolean sellSome) {
        SequentialSpace sequentialSpace = findstock(stock);
        try {
            sequentialSpace.put(seller, amount, sellSome);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startService() throws InterruptedException {
        serviceRunning = true;
        stocks.put("AAPL", 110);
        executor.submit(new MarketSaleOrderHandler());
    }


    /**
     * The responsibility of this class is to constantly handle new orders to sell shares of a stock.
     * This is done by starting a new thread that tries to find a matching buyer of the shares.
     */
    class MarketSaleOrderHandler implements Callable<String> {
        @Override
        public String call() throws Exception {
            while(serviceRunning) {
                try {
                    SellMarketOrder sellOrder = new SellMarketOrder(marketOrders.get(
                            new FormalField(String.class), //Name of the client who made the order
                            new ActualField(sellOrderFlag), //Type of order, eg. SELL or BUY
                            new FormalField(String.class), //Name of the stock
                            new FormalField(Integer.class))); //Quantity
                    Future<String> future = executor.submit(new FindMatchingBuyOrderHandler(sellOrder));
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

        private SellMarketOrder sellOrder;

        public FindMatchingBuyOrderHandler(SellMarketOrder sellOrder) {
            this.sellOrder = sellOrder;
        }

        Callable<BuyMarketOrder> findBuyerTask = () -> new BuyMarketOrder(marketOrders.get(
                new FormalField(String.class),
                new ActualField(buyOrderFlag),
                new ActualField(sellOrder.getStock()),
                new FormalField(Integer.class)));

        @Override
        public String call() throws InterruptedException {
            try {
                BuyMarketOrder buyOrder = executor.submit(findBuyerTask).get(standardTimeout, timeoutUnit);

                //Scenarios:
                //1. The seller wants to sell more than the buyer. The buyer get to buy all the shares he/she wants. The seller makes a new order.
                //2. The buyer wants to buy more than the seller. The seller sells everything. The buyer makes a new order.
                //3. Seller and buyer wants to buy/sell the same amount.

                //We choose the smallest of the two quantities to find the max number of shares
                //that the two clients may trade.
                int min = Math.min(sellOrder.getQuantity(), buyOrder.getQuantity());

                //We find the current stock price
                Object[] res = stocks.queryp(new ActualField(sellOrder.getStock()), new FormalField(Integer.class));
                if (res == null) return null; //TODO: Bør der gives besked til klienten om, at der er sket en fejl – eller sørger vi for dette et andet sted?
                StockInfo stockInfo = new StockInfo(res);

                //Here we send a message (to the bank?) to complete the transaction.
                transactions.put(sellOrder.getOrderedBy(), buyOrder.getOrderedBy(), stockInfo.getName(), stockInfo.getPrice(), min);
                System.out.printf("%s sold %d shares of %s to %s.%n", sellOrder.getOrderedBy(), min, sellOrder.getStock(), buyOrder.getOrderedBy());

                if (min < sellOrder.getQuantity()) {
                    System.out.printf("%s sold less shares than he/her wanted. Placing new sale order of %d shares of %s.%n", sellOrder.getOrderedBy(), sellOrder.getQuantity() - min, sellOrder.getStock());
                    marketOrders.put(
                            sellOrder.getOrderedBy(),
                            sellOrderFlag,
                            sellOrder.getStock(),
                            sellOrder.getQuantity() - min);
                } else if (min < buyOrder.getQuantity()) {
                    System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.%n", buyOrder.getOrderedBy(), buyOrder.getQuantity() - min, buyOrder.getStock());
                    marketOrders.put(
                            buyOrder.getOrderedBy(),
                            buyOrderFlag,
                            buyOrder.getStock(),
                            buyOrder.getQuantity() - min);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                marketOrders.put(sellOrder.getOrderedBy(), msgFlag, "Sale order failed due to timeout.");
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
    public class model  {
            String name;
            int amount;
            public model (String name, int amount) {
                this.name = name;
                this.amount = amount;
        }
    }
}