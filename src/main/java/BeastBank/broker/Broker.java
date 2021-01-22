package BeastBank.broker;

import BeastBank.yahooAPI.StockStream;
import org.jspace.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static BeastBank.shared.Channels.*;
import static BeastBank.shared.Requests.*;
import static BeastBank.shared.StockNames.*;

public class Broker {

    //Hostname and port of the broker
    String hostName = BROKER_HOSTNAME;
    int port = BROKER_PORT;

    SequentialSpace cachedStocks = new SequentialSpace();
    SequentialSpace remoteStocks = new SequentialSpace();
    SequentialSpace orders = new SequentialSpace();
    SequentialSpace transactions = new SequentialSpace();

    SequentialSpace p0 = new SequentialSpace();
    SequentialSpace p1 = new SequentialSpace();
    SequentialSpace p2 = new SequentialSpace();
    SequentialSpace p3 = new SequentialSpace();
    SequentialSpace p4 = new SequentialSpace();

    RemoteSpace serverBroker;
    RemoteSpace brokerServer;

    SpaceRepository tradeRepo = new SpaceRepository();
    private String brokerStr = Broker.class.getName() + ": ";

    public static final String sellOrderFlag = SELL;
    public static final String buyOrderFlag = BUY;
    static final String lock = "lock";

    ExecutorService executor = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    boolean serviceRunning;

    public static StockStream stockStream;
    private static RandomSpace stockPrices;

    public Broker() {
        tradeRepo.add(ORDERS, orders);
        tradeRepo.add(ORDER_PACKAGES, p0); //orderPackageQueue
        tradeRepo.add(TRANSACTIONS, transactions);
        tradeRepo.add(STOCKS, cachedStocks); //TODO: Skal nok fjernes igen, pt. kun for testing.
        tradeRepo.add("remoteStocks", remoteStocks); //TODO: Skal nok fjernes igen, pt. kun for testing.
        tradeRepo.addGate("tcp://" + hostName + ":" + port + "/?keep");
        this.stockStream = new StockStream();
        boolean connectedToBankServer = false;

        while (!connectedToBankServer) {
            // connect to BeastProject.bank BeastBank.server
            try {
                String serverService = String.format("tcp://%s:%d/%s?%s", SERVER_HOSTNAME, SERVER_PORT, SERVER_BROKER, CONNECTION_TYPE);
                String serviceServer = String.format("tcp://%s:%d/%s?%s", SERVER_HOSTNAME, SERVER_PORT, BROKER_SERVER, CONNECTION_TYPE);
                serverBroker = new RemoteSpace(serverService);
                brokerServer = new RemoteSpace(serviceServer);
                connectedToBankServer = true;
                System.out.println("Broker: Connection to BeastProject.bank BeastBank.server up...");

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        stockStream.startStream();

        try {
            broker.startService();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the broker service by submitting each runnable to the executor.
     * @throws InterruptedException
     */
    public void startService() throws InterruptedException {
        serviceRunning = true;
        //TODO: These stock prices are currently just for having something to test.
        cachedStocks.put(APPLE, 100);
        cachedStocks.put(TESLA, 100);
        cachedStocks.put(VESTAS, 100);
        cachedStocks.put(DTU, 100);
        p4.put(lock);

        //We start each thread that constitute the transitions of our petri-net
        executor.submit(new GetLockAndStartProcessing());
        executor.submit(new NotifyPackageToGoBackInQueue());
        executor.submit(new DiscardDueToExpiration());
        executor.submit(new TryToFindMatches());
        executor.submit(new RemoveOrdersAndSignalBank());
        executor.submit(new SignalWaitingForNotification());

        //TODO: This could be done better. Ideally we should listen for notifications when certain stock prices change.
        //Instead, for now, we just check the prices very half second.
        scheduledExecutorService.scheduleAtFixedRate(new StockRateListener(), 0, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * This runnable is responsible for getting the lock from p4 and an order package from p0
     * Then proceeds to check if the order package is brand new (doesn't have an ID yet), and if so,
     * it assigns it an ID and adds all its orders to the order space.
     * Furthermore, if the package is new, it gets all signals from waiting packages in P3 and
     * constructs a stack of packages that needs to be notified.
     * This stack may then be passed on to P1, if it is not empty.
     */
    class GetLockAndStartProcessing implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    p4.get(new ActualField(lock));
                    OrderPackage orderPkg = (OrderPackage) p0.get(new FormalField(OrderPackage.class))[0];
                    Stack<OrderPackage> packagesToNotify = new Stack<>();

                    //Here we check, if the order package is brand new and hasn't been processed before.
                    if (orderPkg.getPackageID() == null) {
                        System.out.printf("%s Received new order: %s \n ", brokerStr,orderPkg.getOrders().get(0).toString());
                        //We give it a unique ID
                        orderPkg.setPackageID(UUID.randomUUID());

                        //We set the expiration time
                        Calendar expirationTime = Calendar.getInstance();
                        expirationTime.add(Calendar.DATE, 1);
                        orderPkg.setTimeOfExpiration(expirationTime);

                        //We add all its orders to the order space.
                        for (Order order : orderPkg.getOrders()) {
                            order.setId(UUID.randomUUID());
                            orders.put(
                                    order.getId(),
                                    order.getOrderedBy(),
                                    order.getOrderType(),
                                    order.getStock(),
                                    order.getQuantity(),
                                    order.getMinQuantity(),
                                    order.getLimit(),
                                    order.getClientMatch()
                            );
                        }

                        //We get all signals from P3 and construct the stack of packages that needs to be notified.
                        for (Order order : orderPkg.getOrders()) {
                            List<Object[]> signals = p3.getAll(
                                    new FormalField(UUID.class),
                                    new ActualField(order.orderType),
                                    new ActualField(order.getStock()),
                                    new FormalField(Integer.class),
                                    new FormalField(OrderPackage.class)
                            );
                            for (Object[] signal : signals) {
                                packagesToNotify.add((OrderPackage) signal[4]);
                            }
                        }
                    }

                    //Here we put a tuple in P1. If no packages need to be notified,
                    //we add the signal "PROCEED".
                    if (packagesToNotify.isEmpty()) {
                        p1.put(PROCEED, orderPkg);
                    } else {
                        p1.put(orderPkg, packagesToNotify);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *This runnable is responsible for notifying waiting packages and putting them back in P0,
     * so that they may be processed again.
     * It does so by getting a tuple with an order package and a stack containing packages to notify from P1.
     * It then "pops" the first element from the stack and gets the matching order from P3 and puts it in P0.
     * Afterwards it puts a new tuple back in P1. If the stack is empty, it puts a tuple with a signal that
     * makes it go through another transition.
     */
    class NotifyPackageToGoBackInQueue implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Object[] res = p1.get(new FormalField(OrderPackage.class), new FormalField(Stack.class));
                    OrderPackage pkg = (OrderPackage) res[0];
                    Stack packagesToNotify = (Stack) res[1];

                    //If the stack is empty, put a tuple in p1 that signals that it may
                    //proceed through another transition.
                    //If it is not empty, pop an element (a package) from the stack,
                    //and then get the stack from P3 and put it in P0. Also put a tuple back in P1
                    //with that contains the stack.
                    if (packagesToNotify.isEmpty()) {
                        p1.put(PROCEED, pkg);
                    } else {
                        OrderPackage pkgToNotify = (OrderPackage) packagesToNotify.pop();
                        Object[] pkgres = p3.getp(new ActualField(pkgToNotify));
                        if (pkgres != null) {
                            OrderPackage waitingPkg = (OrderPackage) pkgres[0];
                            if (Calendar.getInstance().after(waitingPkg.getTimeOfExpiration())) {
                                 p0.put(EXPIRED, waitingPkg);
                            } else {
                                p0.put(waitingPkg);
                            }
                        }
                        p1.put(pkg, packagesToNotify);
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * This runnable gets order packages from P0 that are expired.
     * TODO: This should perhaps also get expired packages from other places, cause what
     * if a package is waiting in P3 and expires? Then it should just be removed.
     * But for now, we only remove them from P0 to conform to our petri-net.
     */
    class DiscardDueToExpiration implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    p0.get(new ActualField(EXPIRED), new FormalField(OrderPackage.class));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This runnable is responsible for getting an order package from P1 and then
     * call the methods that tries to determine if the package can find matches for
     * all its orders and "complete".
     * Afterwards, it puts a tuple in P2 that contains either the signal SUCCESS or FAILURE
     * to indicate the result.
     */
    class TryToFindMatches implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    OrderPackage orderPkg = (OrderPackage) p1.get(new ActualField(PROCEED), new FormalField(OrderPackage.class))[1];
                    if (findMatchesForPackage(orderPkg)) {
                        p2.put(SUCCESS, orderPkg);
                    } else {
                        p2.put(FAILURE, orderPkg);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This runnable gets order packages from P2 that succeeded in finding matches
     * for all its orders. Then it proceeds to remove all the involved orders from
     * the order space, and then generates a list of transactions that needs to be made.
     * It then sends these transactions to the bank server, which will perform the transactions.
     * Finally it releases the lock by putting it back in P4.
     */
    class RemoveOrdersAndSignalBank implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    OrderPackage orderPkg = (OrderPackage) p2.get(new ActualField(SUCCESS), new FormalField(OrderPackage.class))[1];
                    for (Order order : orderPkg.getOrders())
                        getOrdersFromSpace(order, orders);

                    List<Transaction> finalTransactions = generateTransactions(orderPkg.getOrders());
                    transactions.put(finalTransactions); //TODO: Kun for testing
                    for (Transaction transaction : finalTransactions)
                        startTransaction(transaction);

                    p4.put(lock);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This runnable takes order packages from P2 that failed to ind enough matches.
     * It then puts them in P3 and also puts a number of signals in P3, that signal
     * what kind of new orders it wants to be notified about.
     * Finally, it releases the lock by putting it back in P4.
     */
    class SignalWaitingForNotification implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    OrderPackage orderPkg = (OrderPackage) p2.get(new ActualField(FAILURE), new FormalField(OrderPackage.class))[1];
                    p3.put(orderPkg);
                    for (Order order : orderPkg.getOrders()) {
                        p3.put(orderPkg.getPackageID(), order.getMatchingOrderType(), order.getStock(), order.getLimit(), orderPkg);
                    }
                    p4.put(lock);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class StockRateListener implements Runnable {
        @Override
        public void run() {
            updateRates();
        }
    }

    /**
     * Signals each waiting order in P3 that a change in a certain stock's price has
     * changed, and the ones that were notifed back in P0.
     * @param stock
     * @throws InterruptedException
     */
    private void wakeUpLimitOrders(String stock) throws InterruptedException {
        List<Object[]> signals = p3.getAll(
                new FormalField(UUID.class),
                new FormalField(String.class),
                new ActualField(stock),
                new FormalField(Integer.class),
                new FormalField(OrderPackage.class)
        );

        for (Object[] signal : signals) {
            Object[] res = p3.getp(new ActualField(signal[4]));
            if (res == null) continue;
            p0.put(res[0]);
        }
    }

    /**
     * This method takes an order package and for each order calls a method to search
     * for matches for that order. Each of these method calls need to return true ind order
     * for this method to return true.
     * @param orderPkg
     * @return
     * @throws InterruptedException
     */
    private boolean findMatchesForPackage(OrderPackage orderPkg) throws InterruptedException {
        orderPkg.getMatchOrders().clear();
        boolean foundMatchesForAll = true;
        for (Order order : orderPkg.getOrders()) {
            if (!findMatchesForOrder(orders, orderPkg, order)) {
                foundMatchesForAll = false;
                break;
            }
        }
        return foundMatchesForAll;
    }

    /**
     * This method performs the most important business logic.
     * It takes a single order and the order package it belongs to, and then tries to find
     * enough matches to complete that particular order.
     * @param space
     * @param orderPkg
     * @param order
     * @return true if it suceeds in finding enough matches. False if not.
     * @throws InterruptedException
     */
    private boolean findMatchesForOrder(Space space, OrderPackage orderPkg, Order order) throws InterruptedException {

        //Means that it has been processed by another order.
        if (!checkIfOrderExists(order))
            return true;

        order.getMatches().clear();
        int totalQfound = 0;
        TemplateField[] matchTemplate = new TemplateField[]{
                new FormalField(UUID.class),
                new FormalField(String.class),
                new ActualField(order.getMatchingOrderType()),
                new ActualField(order.getStock()),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(String.class)
        };

        //First, we query all orders that match the matching template.
        List<Object[]> res = space.queryAll(matchTemplate);
        //Then we loop over them.
        for (Object[] e : res) {
            Order match = new Order(e);

            //First, we check if this is a limit order, and if the current price is over or under the limit.
            //If not, we break and wait for changes.
            if (order.getLimit() != -1 && !order.isOverOrUnderLimit(getCurrentPrice(order.getStock())))
                break;

            //If the sender of both orders are the same client, continue.
            if (match.getOrderedBy().equals(order.getOrderedBy()))
                continue;

            //If this order wants to sell/buy to/from a specific client, and the match doesn't match that client, continue.
            if (!order.getClientMatch().equals(Order.anyFlag)
                    && !order.getClientMatch().equals(match.getOrderedBy())) {
                continue;
            }

            //If the match wants to sell/buy to/from a specific client, and this doesn't match that client, continue.
            if (!match.getClientMatch().equals(Order.anyFlag)
                    && !match.getClientMatch().equals(order.getOrderedBy())) {
                continue;
            }

            //If the match is a limit order, and the current price doesn't pass that limit, continue.
            if (match.getLimit() != -1 && !match.isOverOrUnderLimit(getCurrentPrice(order.getStock()))) {
                continue;
            }

            //If the match has already been added any order in the order package, continue.
            if (containsOrder(orderPkg.getMatchOrders(), match))
                continue;

            //If the total quantity found plus the minimum quantity of the match exceeds this orders max quantity, continue.
            if (totalQfound + match.getMinQuantity() > order.getQuantity())
                continue;

            //Finally, all is good, and we add the match to the final list of matching orders.
            order.getMatches().add(match);
            orderPkg.getMatchOrders().add(match);
            totalQfound += match.getQuantity();

            //If the total quantity found is greater or equal to the minimum quantity of this order, break.
            if (totalQfound >= order.getMinQuantity()) break;
        }
        return totalQfound >= order.getMinQuantity();
    }

    private boolean containsOrder(List<Order> list, Order order) {
        for (Order e : list) {
            if (e.getId() == order.getId()) return true;
        }
        return false;
    }

    /**
     * Removes the order and all the final matching orders from the space.
     * Then it calls generateTransactions() to generate a list of transactions, which
     * it then returns.
     * @param space the space to remove tuples from.
     * @return A list of transactions.
     * @throws InterruptedException
     */
    public void getOrdersFromSpace(Order order, Space space) throws InterruptedException {

        Object[] thisOrder = space.getp(
                new ActualField(order.getId()),
                new ActualField(order.getOrderedBy()),
                new ActualField(order.getOrderType()),
                new ActualField(order.getStock()),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(String.class)
        );
        if (thisOrder == null) {
            //This means that this order has probably already been processed by another orders task.
            return;
        }

        //We remove each of the matching orders from the space.
        for (Order o : order.getMatches()) {
            space.get(
                    new ActualField(o.getId()),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new ActualField(o.getStock()),
                    new FormalField(Integer.class),
                    new FormalField(Integer.class),
                    new FormalField(Integer.class),
                    new FormalField(String.class)
            );
        }
    }

    /**
     * Generates a list of transactions from this orders matching orders.
     * @param orders
     * @return list of transactions.
     */
    private List<Transaction> generateTransactions(List<Order> orders) throws InterruptedException {
        List<Transaction> transactions = new ArrayList<>();
        for (Order order : orders) {
            //remainingQ is the remaining amount of shares that this order wants to trade. Starts at the max quantity.
            int remainingQ = order.getQuantity();

            for (Order match : order.getMatches()) {
                //Find the max numbers of shares that this order and the match may trade.
                //TODO: This is a silly way of doing it. Figure out the math..
                int transactionQ = 0;
                while ((transactionQ <= remainingQ) && (transactionQ <= match.getQuantity())) transactionQ++;
                transactionQ -= 1;

                //Put a transaction in the list.
                if (order.getOrderType().equals(sellOrderFlag)) {
                    transactions.add(new Transaction(order.getOrderedBy(), match.getOrderedBy(), order.getStock(), getCurrentPrice(order.getStock()), transactionQ));
                } else {
                    transactions.add(new Transaction(match.getOrderedBy(), order.getOrderedBy(), order.getStock(), getCurrentPrice(order.getStock()), transactionQ));
                }
                //Update the remaining quantity.
                remainingQ -= transactionQ;
            }
        }
        return transactions;
    }

    /**
     * @return True if this order still exists in the space, which means it hasn't been processed by another order yet.
     * False false if it no longer exists. This means it has been processed by another order.
     * @throws InterruptedException
     */
    private boolean checkIfOrderExists(Order order) throws InterruptedException {
        boolean b = !(orders.queryp(
                new ActualField(order.getId()),
                new ActualField(order.getOrderedBy()),
                new ActualField(order.getOrderType()),
                new ActualField(order.getStock()),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(String.class)
        ) == null);
        return b;
    }

    private int getCurrentPrice(String stock) throws InterruptedException {
        int price = (Integer) cachedStocks.query(new ActualField(stock), new FormalField(Integer.class))[1];
        return price;
    }

    private void updateRates() {
        try {
            List<Object[]> stocks = remoteStocks.queryAll(
                    new FormalField(String.class),
                    new FormalField(Integer.class)
            );
            for (Object[] stock : stocks) {
                Object[] cachedStock = cachedStocks.getp(new ActualField(stock[0]), new FormalField(Integer.class));
                if (cachedStock == null) {
                    cachedStocks.put(stock[0], stock[1]);
                } else {
                    if (cachedStock[1] != stock[1])
                        wakeUpLimitOrders((String) cachedStock[0]);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startTransaction(Transaction transaction) {
        System.out.println(brokerStr + "Starting transaction...");
        try {
            brokerServer.put(
                    transaction.getSeller(),
                    transaction.getBuyer(),
                    transaction.getStockName(),
                    transaction.getPrice(),
                    transaction.getQuantity());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public double findLatestStockPrice(String stock) throws InterruptedException {
        Object[] response = stockPrices.getp(new ActualField(stock), new FormalField(Double.class));
        if (response == null) {
            stockPrices.put(stock, 10);
            return 10;
        } else {
            if (Math.random() > 0.5) {
                stockPrices.put(response[0], (double) response[1] + 0.01);
                return ((double) response[1] + 0.01);
            } else {
                stockPrices.put(response[0], (double) response[1] - 0.01);
                return ((double) response[1] - 0.01);
            }
        }
    }
}
