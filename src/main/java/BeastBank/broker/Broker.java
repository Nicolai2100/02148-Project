package BeastBank.broker;

import BeastBank.yahooAPI.StockStream;
import org.jspace.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static BeastBank.shared.Channels.*;
import static BeastBank.shared.Requests.*;

public class Broker {

    //Brokerens hostname og port
    String hostName = BROKER_HOSTNAME;
    int port = BROKER_PORT;

    SequentialSpace cachedStocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace remoteStocks = new SequentialSpace();
    SequentialSpace orderPackageQueue = new SequentialSpace();
    SequentialSpace waitingPackages = new SequentialSpace();
    SequentialSpace orders = new SequentialSpace();
    SequentialSpace transactions = new SequentialSpace();

    RemoteSpace serverBroker;
    RemoteSpace brokerServer;

    SpaceRepository tradeRepo = new SpaceRepository();

    public static final String sellOrderFlag = SELL;
    public static final String buyOrderFlag = BUY;
    static final String lock = "lock";
    static final String waiting = "WAITING";

    ExecutorService executor = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    boolean serviceRunning;

    public static StockStream stockStream;
    private static RandomSpace stockPrices;

    public Broker() {
        tradeRepo.add(ORDERS, orders);
        tradeRepo.add(ORDER_PACKAGES, orderPackageQueue);
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

    public static void main(String[] args) throws InterruptedException {
        Broker broker = new Broker();
        stockStream.startStream();
        broker.startService();
    }

    public void startService() throws InterruptedException {
        serviceRunning = true;
        cachedStocks.put("AAPL", 100); //Just for testing
        cachedStocks.put("TESLA", 100);
        cachedStocks.put("VESTAS", 100);
        cachedStocks.put("DTU", 100);
        orders.put(lock);
        executor.submit(new NewOrderPkgHandler());
        scheduledExecutorService.scheduleAtFixedRate(new StockRateListener(), 0, 500, TimeUnit.MILLISECONDS);
    }


    class NewOrderPkgHandler implements Runnable {
        @Override
        public void run() {
            while (serviceRunning) {
                try {
                    OrderPackage orderPkg = (OrderPackage) orderPackageQueue.get(new FormalField(OrderPackage.class))[0];
                    if (orderPkg.getPackageID() == null) {
                        addNewPackage(orderPkg);
                    }

                    if (Calendar.getInstance().after(orderPkg.getTimeOfExpiration()))
                        continue;

                    orders.get(new ActualField(lock));
                    if (findMatchesForPackage(orderPkg)) {
                        for (Order order : orderPkg.getOrders())
                            getOrdersFromSpace(order, orders);
                        List<Transaction> finalTransactions = generateTransactions(orderPkg.getOrders());
                        transactions.put(finalTransactions); //TODO: Kun for testing
                        for (Transaction transaction : finalTransactions)
                            startTransaction(transaction);
                    } else {
                        signalWaiting(orderPkg);
                    }
                    orders.put(lock);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void addNewPackage(OrderPackage orderPkg) throws InterruptedException {
        orderPkg.setPackageID(UUID.randomUUID());
        Calendar expirationTime = Calendar.getInstance();
        expirationTime.add(Calendar.DATE, 1);
        orderPkg.setTimeOfExpiration(expirationTime);
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
            wakeUpWaitingPackages(order);
        }
    }

    class StockRateListener implements Runnable {
        @Override
        public void run() {
            updateRates();
        }
    }


    private void signalWaiting(OrderPackage orderPkg) throws InterruptedException {
        waitingPackages.put(orderPkg.getPackageID(), orderPkg);
        for (Order order : orderPkg.getOrders()) {
            waitingPackages.put(orderPkg.getPackageID(), order.getMatchingOrderType(), order.getStock(), order.getLimit(), waiting);
        }
    }

    private void wakeUpWaitingPackages(Order order) throws InterruptedException {
        List<Object[]> signals = waitingPackages.getAll(
                new FormalField(UUID.class),
                new ActualField(order.orderType),
                new ActualField(order.getStock()),
                new FormalField(Integer.class),
                new ActualField(waiting)
        );
        for (Object[] signal : signals) {
            Object[] pkgTuple = waitingPackages.getp(
                    new ActualField(signal[0]),
                    new FormalField(OrderPackage.class)
            );
            if (pkgTuple == null) continue;
            orderPackageQueue.put(pkgTuple[1]);
        }
    }


    private void wakeUpLimitOrders(String stock) throws InterruptedException {
        List<Object[]> signals = waitingPackages.getAll(
                new FormalField(UUID.class),
                new FormalField(String.class),
                new ActualField(stock),
                new FormalField(Integer.class),
                new ActualField(waiting)
        );

        for (Object[] signal : signals) {
            Object[] pkgTuple = waitingPackages.getp(
                    new ActualField(signal[0]),
                    new FormalField(OrderPackage.class)
            );
            if (pkgTuple == null) continue;
            orderPackageQueue.put(pkgTuple[1]);
        }
    }

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
        System.out.println("Broker: Starting transaction...");
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
