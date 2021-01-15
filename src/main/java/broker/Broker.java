package broker;

import org.jspace.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static shared.Channels.*;
import static shared.Requests.*;

public class Broker {

    //ns hostname og port
    String hostName = BROKER_HOSTNAME;
    int port = BROKER_PORT;

    SequentialSpace stocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace newOrderPackages = new SequentialSpace();
    SequentialSpace orders = new SequentialSpace();
    SequentialSpace transactions = new SequentialSpace();

    RemoteSpace serverBroker;
    RemoteSpace brokerServer;

    SpaceRepository tradeRepo = new SpaceRepository();

    public static final String sellOrderFlag = SELL;
    public static final String buyOrderFlag = BUY;
    static final String msgFlag = "MSG";
    static final String totalFlag = "TOTAL";
    static final String lock = "lock";
    static final String waiting = "WAITING";
    static final String notifyChange = "CHANGE";

    ExecutorService executor = Executors.newCachedThreadPool();
    static final int standardTimeout = 10; //TODO: Consider what this should be, or make it possible to set it per order.
    static final TimeUnit timeoutUnit = TimeUnit.HOURS; //TODO: Just for now, for testing...
    boolean serviceRunning;

    public Broker() {
        tradeRepo.add(ORDERS, orders);
        tradeRepo.add(ORDER_PACKAGES, newOrderPackages);
        tradeRepo.add("transactions", transactions);
        tradeRepo.addGate("tcp://" + hostName + ":" + port + "/?keep");

        boolean connectedToBankServer = false;

        while (!connectedToBankServer) {
            // connect to bank server
            try {
                String serverService = String.format("tcp://localhost:123/%s?%s", SERVER_BROKER, CONNECTION_TYPE);
                String serviceServer = String.format("tcp://localhost:123/%s?%s", BROKER_SERVER, CONNECTION_TYPE);
                serverBroker = new RemoteSpace(serverService);
                brokerServer = new RemoteSpace(serviceServer);
                connectedToBankServer = true;
                System.out.println("Broker: Connection to bank server up...");

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

    }

    public static void main(String[] args) throws InterruptedException {
        Broker broker = new Broker();
        broker.startService();
    }

    private void startService() throws InterruptedException {
        serviceRunning = true;
        stocks.put("AAPL", 110);
        orders.put(lock);
        executor.submit(new NewOrderPkgHandler());
    }

    class NewOrderPkgHandler implements Runnable {
        @Override
        public void run() {
            while (serviceRunning) {
                try {
                    OrderPackage orderPkg = (OrderPackage) newOrderPackages.get(new FormalField(OrderPackage.class))[0];
                    executor.submit(new ProcessPackageTask(orderPkg));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * A task that processes a package of orders by starting new tasks for each order that it contains.
     * Each of these tasks returns a list of matching orders. When they all complete, this task
     * tries to secure them all, and finally send a list of transactions to the bank.
     */
    class ProcessPackageTask implements Runnable {

        OrderPackage orderPkg;

        public ProcessPackageTask(OrderPackage orderPkg) {
            this.orderPkg = orderPkg;
        }

        @Override
        public void run() {
            List<ProcessOrderTask> tasks = new ArrayList<>();
            List<List<Order>> finalOrders = new ArrayList<>();
            List<Transaction> finalTransactions = new ArrayList<>();

            try {
                //First we give each order of the package a unique ID
                //Then we put the order in the orders space.
                for (Order order : orderPkg.getOrders()) {
                    order.setId(UUID.randomUUID());
                    orders.put(
                            order.getId(),
                            order.getOrderedBy(),
                            order.getOrderType(),
                            order.getStock(),
                            order.getQuantity(),
                            order.getMinQuantity()
                    );
                    //We notifiy any listeners, that the space has been changed.
                    notifyListeners(orders);
                    //We instantiate a new task to find matching orders of the order, and add it to a list of tasks.
                    tasks.add(new ProcessOrderTask(orderPkg, order));
                }

                //We invoke all the tasks at once.
                List<Future<List<Order>>> futures = executor.invokeAll(tasks);

                //We try to retrieve a result for each of the tasks
                //TODO: Should we use a timeout on the .get() ?
                for (Future<List<Order>> future : futures) {
                    finalOrders.add(future.get());
                }

                //Now we are ready the lock all the orders, remove them and create the transactions.
                //We grab the lock.
                orders.get(new ActualField(lock));

                //We call lockTransaction() on each task, which removes the orders, and returns transactions.
                //We add the transactions to a list.
                for (ProcessOrderTask task : tasks) {
                    finalTransactions.addAll(task.lockTransactions(orders));
                }
                //We put the lock back.
                orders.put(lock);

                //We put the final transactions in the transactions space.
                //transactions.put(finalTransactions); //TODO: Kun for test

                for (Transaction transact : finalTransactions) {
                    startTransaction(transact);
                }

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Lets an order signal that it waits for a change in space.
     * The method then blocks until the corresponding signal of change has been received.
     *
     * @param order The order that wants to signal that it is waiting.
     * @param space The space where the signal should be put in.
     * @throws InterruptedException
     */
    private void waitForChange(Order order, Space space) throws InterruptedException {
        space.put(order.getId(), order.getMatchingOrderType(), order.getStock(), waiting);
        space.get(
                new ActualField(order.getId()),
                new ActualField(order.getMatchingOrderType()),
                new ActualField(order.getStock()),
                new ActualField(notifyChange));
    }

    /**
     * Signals that a change has occured in the space. Does so by first retrieving
     * all current waiting signals, and for each of these puts a corresponding signal
     * back.
     *
     * @param space
     * @throws InterruptedException
     */
    private void notifyListeners(Space space) throws InterruptedException {
        List<Object[]> listeners = space.getAll(
                new FormalField(UUID.class),

                new FormalField(String.class),
                new FormalField(String.class),
                new ActualField(waiting)
        );
        for (Object[] l : listeners) {
            space.put(l[0], l[1], l[2], notifyChange);
        }
    }

    class ProcessOrderTask implements Callable<List<Order>> {

        OrderPackage orderPkg;
        Order order;
        List<Order> matchingOrders = new ArrayList<>();
        int totalQfound = 0;
        TemplateField[] thisTemplate;
        TemplateField[] matchTemplate;

        public ProcessOrderTask(OrderPackage orderPkg, Order order) {
            this.orderPkg = orderPkg;
            this.order = order;
            thisTemplate = new TemplateField[]{
                    new ActualField(order.getId()),
                    new ActualField(order.getOrderedBy()),
                    new ActualField(order.getOrderType()),
                    new ActualField(order.getStock()),
                    new FormalField(Integer.class),
                    new FormalField(Integer.class)
            };
            System.out.println("Broker: Received order: " + order);

            matchTemplate = new TemplateField[]{
                    new FormalField(UUID.class),
                    new FormalField(String.class),
                    new ActualField(order.getMatchingOrderType()),
                    new ActualField(order.getStock()),
                    new FormalField(Integer.class),
                    new FormalField(Integer.class)
            };
        }

        @Override
        public List<Order> call() {
            try {
                findMatchingOrders(orders);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return matchingOrders;
        }

        private void findMatchingOrders(Space space) throws InterruptedException {
            while (true) {
                //First we query all orders that match the matching template.
                List<Object[]> res = space.queryAll(matchTemplate);
                //Then we loop over them.
                for (Object[] e : res) {
                    Order match = new Order(e); //TODO - this needs a constructor
                    //Break if the sender of both orders are the same client.
                    if (match.getOrderedBy().equals(order.getOrderedBy())) break;

                    //We only want to add the order to the final matches, if:
                    //  1. It is not already added to the matches of this order.
                    //  2. It is not already added to the matches of another order in the same order package.
                    //  3. The current total amount of match quantities plus the minimum quantity of the new match does not exceed this orders max quantity.
                    if (!containsOrder(
                            matchingOrders, match) &&
                            !containsOrder(orderPkg.getMatchOrders(), match)
                            && (totalQfound + match.getMinQuantity() <= order.getQuantity())
                    ) {
                        matchingOrders.add(match);
                        orderPkg.getMatchOrders().add(match);
                        totalQfound += match.getQuantity();
                    }
                    //If the total quantity found is greater or equal to the minimum quantity of this order, break.
                    if (totalQfound >= order.getMinQuantity()) break;
                }
                if (totalQfound >= order.getMinQuantity()) {
                    break;
                } else {
                    if (!checkIfThisExists()) //TODO: Not sure if this is necessary
                        break;
                    //if not enough matching orders were found, wait until a change has happened in the space.
                    //This is to avoid "busy waiting".
                    waitForChange(order, space);
                }
            }
        }

        private boolean containsOrder(List<Order> list, Order order) {
            for (Order e : list) {
                if (e.getId() == order.getId()) return true;
            }
            return false;
        }

        /**
         * TODO: Should be renamed, and maybe also refactorized.
         * Removes the order and all the final matching orders from the space.
         * Then it calls generateTransactions() to generate a list of transactions, which
         * it then returns.
         *
         * @param space the space to remove tuples from.
         * @return A list of transactions.
         * @throws InterruptedException
         */
        public List<Transaction> lockTransactions(Space space) throws InterruptedException {

            Object[] thisOrder = space.getp(thisTemplate);
            if (thisOrder == null) {
                //This means that this order has probably already been processed by another orders task.
                return new ArrayList<>();
            }

            //We remove each of the matching orders from the space.
            for (Order o : matchingOrders) {
                space.get(
                        new ActualField(o.getId()),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new ActualField(o.getStock()),
                        new FormalField(Integer.class),
                        new FormalField(Integer.class)
                );

            }
            //We notify listeners that a change has happened.
            notifyListeners(orders);

            //We return a list of transactions.
            return generateTransactions(matchingOrders);
        }

        /**
         * Generates a list of transactions from this orders matching orders.
         *
         * @param matches
         * @return list of transactions.
         */
        private List<Transaction> generateTransactions(List<Order> matches) {
            List<Transaction> transactions = new ArrayList<>();

            //remainingQ is the remaining amount of shares that this order wants to trade. Starts at the max quantity.
            int remainingQ = order.getQuantity();

            for (Order match : matches) {
                //Find the max numbers of shares that this order and the match may trade.
                //TODO: This is a silly way of doing it. Figure out the math..
                int transactionQ = 0;
                while ((transactionQ <= remainingQ) && (transactionQ <= match.getQuantity())) transactionQ++;
                transactionQ -= 1;

                //Put a transaction in the list.
                if (order.getOrderType().equals(sellOrderFlag)) {
                    transactions.add(new Transaction(order.getOrderedBy(), match.getOrderedBy(), order.getStock(), 100, transactionQ));
                } else {
                    transactions.add(new Transaction(match.getOrderedBy(), order.getOrderedBy(), order.getStock(), 100, transactionQ));
                }
                //Update the remaining quantity.
                remainingQ -= transactionQ;
            }
            return transactions;
        }

        /**
         * @return True if this order still exists in the space, which means it hasn't been processed by another order yet.
         * False false if it no longer exists. This means it has been processed by another order.
         * @throws InterruptedException
         */
        private boolean checkIfThisExists() throws InterruptedException {
            orders.get(new ActualField(lock));
            boolean b = !(orders.queryp(thisTemplate) == null);
            orders.put(lock);
            return b;
        }
    }

    public void startTransaction(Transaction transaction) {
        System.out.println("Broker: Starting transaction...");
        try {
            brokerServer.put(transaction.getSeller(),
                    transaction.getBuyer(),
                    transaction.getStockName(),
                    transaction.getPrice(),
                    transaction.getQuantity());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}