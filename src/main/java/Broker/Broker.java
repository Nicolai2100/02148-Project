package Broker;

import model.StockInfo;
import org.jspace.*;

import java.util.*;
import java.util.concurrent.*;

public class Broker {

    //Brokerens hostname og port
    String hostName = "localhost";
    int port = 9001;

    SequentialSpace stocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace newOrderPackages = new SequentialSpace();
    SequentialSpace marketOrders = new SequentialSpace();
    SequentialSpace marketOrdersInProcess = new RandomSpace(); //TODO: Denne kan i princippet slås sammen med marketOrders, og det bør stadig fungere.
    SequentialSpace limitOrders = new SequentialSpace(); //TODO: Bør både market og limit orders være i samme space?
    SequentialSpace transactions = new SequentialSpace();

    SpaceRepository tradeRepo = new SpaceRepository();

    public static final String sellOrderFlag = "SELL";
    public static final String buyOrderFlag = "BUY";
    static final String msgFlag = "MSG";
    static final String totalFlag = "TOTAL";
    static final String lock = "lock";
    static final String waitingForNewTotal = "WAITING_FOR_NEW_TOTAL";
    static final String newTotal = "NEW_TOTAL";

    ExecutorService executor = Executors.newCachedThreadPool();
    static final int standardTimeout = 10; //TODO: Consider what this should be, or make it possible to set it per order.
    static final TimeUnit timeoutUnit = TimeUnit.HOURS; //TODO: Just for now, for testing...
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
        marketOrdersInProcess.put(totalFlag, "AAPL", sellOrderFlag, 0); //TODO: Hvordan kan vi gøre dette et andet sted?
        marketOrdersInProcess.put(totalFlag, "AAPL", buyOrderFlag, 0); //TODO: Også denne?
        marketOrdersInProcess.put(lock);
        //executor.submit(new NewPackagesHandler());
        executor.submit(new NewTestOrderHandler());
    }

    class NewTestOrderHandler implements Callable<String> {

        @Override
        public String call() throws Exception {
            while(serviceRunning) {
                try {
                    Order order = new Order(marketOrders.get(
                            new FormalField(String.class), //Name of the client who made the order
                            new FormalField(String.class), //Type of order, eg. SELL or BUY
                            new FormalField(String.class), //Name of the stock
                            new FormalField(Integer.class), //Quantity
                            new FormalField(Integer.class)));
                            //new FormalField(Boolean.class))); //All or nothing
                    order.setId(UUID.randomUUID()); //TODO: Skal dette gøres anderledes?
                    marketOrdersInProcess.put(
                            order.getId(),
                            order.getOrderedBy(),
                            order.getOrderType(),
                            order.getStock(),
                            order.getQuantity(),
                            order.getMinQuantity()
                            //order.isAllOrNothing()
                    );
                    executor.submit(new Test2(order));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return "Handler for handling market sale orders stopped!";
        }
    }

    class Test2 implements Runnable {
        Order order;

        public Test2(Order order) {
            this.order = order;
        }

        @Override
        public void run() {
            Set<Order> res;
            try {
                Set<Order> start = new HashSet<>();
                start.add(order);
                res = executor.submit(new TryFindSetOfMatches(start, order.getQuantity(), order.getMatchingOrderType(), order.getStock())).get();
                System.out.println("Her kommer resultatet:");
                for (Order o : res) {
                    System.out.println(o.toString());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    class NewPackagesHandler implements Callable<String> {
        @Override
        public String call() throws Exception {
            while(serviceRunning) {
                try {
                    OrderPackage orderPkg = (OrderPackage) newOrderPackages.get(new FormalField(OrderPackage.class))[0];

                    //Give the new package a UUID
                    orderPkg.setPackageID(UUID.randomUUID());

                    //Give each order in the package a UUID;
                    for (Order order : orderPkg.getOrders())
                        order.setId(UUID.randomUUID());

                    //Submit the package to be processed by a new task
                    executor.submit(new ProcessOrderPkgTask(orderPkg));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return "Handler for handling market sale orders stopped!";
        }
    }

    class ProcessOrderPkgTask implements Callable<String> {

        private OrderPackage orderPkg;

        public ProcessOrderPkgTask(OrderPackage orderPkg) {
            this.orderPkg = orderPkg;
        }

        @Override
        public String call() throws Exception {
            for (Order order : orderPkg.getOrders()) {

            }
            return null;
        }
    }

    class QueryAllPkgOrdersTask implements Callable<Boolean> {

        private OrderPackage orderPkg;

        public QueryAllPkgOrdersTask(OrderPackage orderPkg) {
            this.orderPkg = orderPkg;
        }

        @Override
        public Boolean call() throws Exception {

            return null;
        }
    }

    class ProcessOrderTask implements Callable<String> {

        private Order order;
        TemplateField[] totalSharesTemplateFields;
        TemplateField[] thisOrderTemplateFields;
        TemplateField[] matchingTemplateFields;

        public ProcessOrderTask(Order order) {
            this.order = order;

            totalSharesTemplateFields = new TemplateField[]{
                    new ActualField(totalFlag), //Indicates we want to get the total quantity
                    new ActualField(order.getOrderType()), //Indicates if it's the total for sale ("SELL") or purchase ("BUY")
                    new ActualField(order.getStock()), //The name of the stock
                    new FormalField(Integer.class) //The quantity
            };
            thisOrderTemplateFields = new TemplateField[]{
                    new ActualField(order.getId()),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(Integer.class)
            };
            matchingTemplateFields = new TemplateField[]{
                    new FormalField(UUID.class),
                    new FormalField(String.class),
                    new ActualField(order.getMatchingOrderType()),
                    new ActualField(order.getStock()),
                    new FormalField(Integer.class)
            };
        }

        Callable<Order> queryMatchTask = () -> new Order(marketOrdersInProcess.query(matchingTemplateFields));

        Callable<List<Order>> queryMatchesTask = () -> {
            List<Object[]> allMatches = marketOrdersInProcess.queryAll(matchingTemplateFields);
            List<Order> matches = new ArrayList<>();
            int qTotal = 0;
            for (Object match : allMatches) {
                if (qTotal >= order.getQuantity())
                    break;
                matches.add((Order) match);
                qTotal += ((Order) match).getQuantity();
            }
            if (qTotal < (order).getQuantity())
                throw new Exception("Not enough");
            return matches;
        };

        Callable<Boolean> waitForConditionalTotalQuantity = () -> {
            while (true) {
                //We put a tuple that signals that the task is waiting for the total value to change.
                marketOrdersInProcess.put(order.getId(), order.getOrderType(), order.getStock(), waitingForNewTotal);
                //We wait for a tuple that signals that the total value has changed.
                marketOrdersInProcess.get(
                        new ActualField(order.getId()),
                        new ActualField(order.getOrderType()),
                        new ActualField(order.getStock()),
                        new ActualField(newTotal)
                );
                //We retrieve the first signal tuplet, so that the task is not signaling anymore.
                marketOrdersInProcess.get(
                        new ActualField(order.getId()),
                        new ActualField(order.getOrderType()),
                        new ActualField(order.getStock()),
                        new ActualField(waitingForNewTotal));
                //We check the condition. If it's true, we break the loop.
                int currentTotal = (Integer) marketOrdersInProcess.query(totalSharesTemplateFields)[3];
                if (currentTotal >= order.getQuantity())
                    break;
            }
            return null;
        };

        @Override
        public String call() throws InterruptedException {
            try {
                //First we update the total amount of shares of the stock that are currently up for sale/purchase
                //This also notifies each task that are blocking/waiting for the value to change.
                updateTotalQuantity(order, order.getQuantity());

                //Here we start to wait, until the total number of shares that up for sale/purchase
                //exceeds the amount that this order wants to buy/sell.
                executor.submit(waitForConditionalTotalQuantity).get(standardTimeout, timeoutUnit); //TODO: Consider what the timout should be.

                marketOrdersInProcess.get(new ActualField(lock));

                //Here we check that this order has not been processed/fulfilled by another orders completion.
                Object[] thisOrderRes = marketOrdersInProcess.queryp(thisOrderTemplateFields);
                if (thisOrderRes == null) {
                    //If null, it should mean that this particular order has aldready been processed.
                    //In that case, just put the lock back, and let the task finish.
                    marketOrdersInProcess.put(lock);
                    return null; //TODO: Should we do something else here?
                }


                Order matchOrder = new Order(marketOrdersInProcess.get(matchingTemplateFields));
                marketOrdersInProcess.get(thisOrderTemplateFields);

                marketOrdersInProcess.put(lock);

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
                    if (order.getOrderType().equals(sellOrderFlag))
                        System.out.printf("%s sold less shares than he/her wanted. Placing new sale order of %d shares of %s.%n", order.getOrderedBy(), order.getQuantity() - min, order.getStock());
                    if (order.getOrderType().equals(buyOrderFlag))
                        System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.%n", order.getOrderedBy(), order.getQuantity() - min, order.getStock());

                    marketOrders.put(
                            order.getOrderedBy(),
                            sellOrderFlag,
                            order.getStock(),
                            order.getQuantity() - min);
                } else if (min < matchOrder.getQuantity()) {
                    if (matchOrder.getOrderType().equals(sellOrderFlag))
                        System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.%n", matchOrder.getOrderedBy(), matchOrder.getQuantity() - min, matchOrder.getStock());
                    if (matchOrder.getOrderType().equals(buyOrderFlag))
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

    private void updateTotalQuantity(Order order, int change) throws InterruptedException {
        //Here we want to get the total number of shares of the specific stock currently for either sale or purchase.
        //This information is relevant later.
        int totalShares = (Integer) marketOrdersInProcess.get(
                new ActualField(totalFlag), //Indicates we want to get the total quantity
                new ActualField(order.getOrderType()), //Indicates if it's the total for sale ("SELL") or purchase ("BUY")
                new ActualField(order.getStock()), //The name of the stock
                new FormalField(Integer.class) //The quantity
        )[2];
        //Then we want to update it by adding the quantity of this new order.
        totalShares += order.getQuantity();
        marketOrdersInProcess.put(
                totalFlag,
                order.getOrderType(),
                order.getStock(),
                totalShares
        );
        //Then we need to signal each task waiting for an update on the total, that it has been updated.
        List<Object[]> listeners = marketOrdersInProcess.queryAll(
                new FormalField(UUID.class),
                new FormalField(String.class),
                new FormalField(String.class),
                new ActualField(waitingForNewTotal)
        );
        for (Object[] l : listeners) {
            marketOrdersInProcess.put(l[0], l[1], l[2], newTotal);
        }
        //Done with updating total and notifying waiting threads.
    }


    /**
     * This should recursively try to find orders and add them to a set of orders,
     * such that the final set contains orders, that "solves the puzzle".
     */
    class TryFindSetOfMatches implements Callable<Set<Order>> {

        Set<Order> orders;
        int q;
        String matchType; //BUY
        String otherType; //SELL
        String stock;

        //tomt set, 10, BUY, aapl
        public TryFindSetOfMatches(Set<Order> orders, int q, String matchType, String stock) {
            this.orders = orders;
            this.q = q;
            this.matchType = matchType;
            this.otherType = matchType.equals(sellOrderFlag) ? buyOrderFlag : sellOrderFlag;
            this.stock = stock;
        }

        private boolean containsOrder(Order order) {
            for (Order o : orders) {
                if (o.getId().equals(order.getId())) return true;
            }
            return false;
        }

        @Override
        public Set<Order> call() throws Exception {
            TemplateField[] matchFields = new TemplateField[]{
                    new FormalField(UUID.class),
                    new FormalField(String.class),
                    new ActualField(matchType),
                    new ActualField(stock),
                    new FormalField(Integer.class),
                    new FormalField(Integer.class)
                    //new FormalField(Boolean.class)
            };

            Order match = null;
            do {
                //This is busy waiting. not good. Observe instead...
                match = new Order(marketOrdersInProcess.query(matchFields)); //space might need to be random.
            } while (containsOrder(match));

            orders.add(match);
            if (match.getQuantity() == q) {
                return orders;
            } else if (match.getQuantity() < q) {
                return executor.submit(new TryFindSetOfMatches(orders, q - match.getQuantity(), matchType, stock)).get();
            } else if (match.getMinQuantity() > q) {
                return executor.submit(new TryFindSetOfMatches(orders, match.getQuantity() - q, otherType, stock)).get();
            } else {
                return orders;
            }
        }
    }

/*
    class FindMatchingBuyOrderHandler2 implements Callable<String> {

        private MarketOrder order;
        String matchingOrderType;
        TemplateField[] thisOrderTemplateFields;
        TemplateField[] matchingTemplateFields;
        private List<MarketOrder> matchOrderQueries = new ArrayList<>();
        private List<MarketOrder> matchOrdersFinal = new ArrayList<>();

        public FindMatchingBuyOrderHandler2(MarketOrder order) {
            this.order = order;
            matchingOrderType = order.getOrderType().equals(sellOrderFlag) ? buyOrderFlag : sellOrderFlag;
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

        Runnable findMatchOrdersTask = () -> {
            while (sumStocksOfMatchOrders(matchOrderQueries) < order.getQuantity()) {
                try {
                    MarketOrder matchQuery = new MarketOrder(marketOrdersInProcess.get(matchingTemplateFields));
                    if (!containsMatchOrder(matchQuery)) matchOrderQueries.add(matchQuery);
                    putMarketOrder(matchQuery, marketOrdersInProcess);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable findMatchOrdersTask2 = () -> {
            while (sumStocksOfMatchOrders(matchOrderQueries) < order.getQuantity()) {
                List<Object[]> resList = marketOrdersInProcess.queryAll(matchingTemplateFields);
                matchOrderQueries = new ArrayList<>();
                for (Object[] res : resList) {
                    if (sumStocksOfMatchOrders(matchOrderQueries) < order.getQuantity())
                        matchOrderQueries.add(new MarketOrder(res));
                }
                if (sumStocksOfMatchOrders(matchOrderQueries) < order.getQuantity()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        private int sumStocksOfMatchOrders(List<MarketOrder> orders) {
            int sum = 0;
            for (MarketOrder order : orders) {
                sum += order.getQuantity();
            }
            return sum;
        }

        private boolean containsMatchOrder(MarketOrder order) {
            for (MarketOrder o : matchOrderQueries) {
                if (order.getId().equals(o.getId())) return true;
            }
            return false;
        }

        @Override
        public String call() throws InterruptedException {
            try {
                //MarketOrder matchOrderQuery = executor.submit(queryMatchTask).get(standardTimeout, timeoutUnit);

                executor.submit(findMatchOrdersTask2).get(standardTimeout, timeoutUnit);

                marketOrdersInProcess.get(new ActualField(lock));

                Object[] thisOrderRes = marketOrdersInProcess.queryp(thisOrderTemplateFields);
                if (thisOrderRes == null) {
                    //If null, it should mean that this particular order has aldready been processed.
                    //In that case, just put the lock back, and let the task finish.
                    //TODO: Eller hvad, skal der gøres noget andet?
                    marketOrdersInProcess.put(lock);
                    return null; //TODO: Skal der gøres mere her?
                }

                //MarketOrder matchOrder = new MarketOrder(marketOrdersInProcess.get(matchingTemplateFields));

                for (MarketOrder order : matchOrderQueries) {

                }

                marketOrdersInProcess.get(thisOrderTemplateFields);

                marketOrdersInProcess.put(lock);

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
                    if (order.getOrderType().equals(sellOrderFlag))
                        System.out.printf("%s sold less shares than he/her wanted. Placing new sale order of %d shares of %s.%n", order.getOrderedBy(), order.getQuantity() - min, order.getStock());
                    if (order.getOrderType().equals(buyOrderFlag))
                        System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.%n", order.getOrderedBy(), order.getQuantity() - min, order.getStock());

                    marketOrders.put(
                            order.getOrderedBy(),
                            sellOrderFlag,
                            order.getStock(),
                            order.getQuantity() - min);
                } else if (min < matchOrder.getQuantity()) {
                    if (matchOrder.getOrderType().equals(sellOrderFlag))
                        System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.%n", matchOrder.getOrderedBy(), matchOrder.getQuantity() - min, matchOrder.getStock());
                    if (matchOrder.getOrderType().equals(buyOrderFlag))
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
*/
    void putMarketOrder(Order order, Space space) {
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

    /*
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
                    order.setId(UUID.randomUUID()); //TODO: Skal dette gøres anderledes?
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
    */
}