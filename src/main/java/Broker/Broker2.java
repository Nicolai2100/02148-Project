package Broker;

import org.jspace.*;

import java.util.*;
import java.util.concurrent.*;

public class Broker2 {

    //Brokerens hostname og port
    String hostName = "localhost";
    int port = 9001;

    SequentialSpace stocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace newOrderPackages = new SequentialSpace();
    SequentialSpace orders = new SequentialSpace();
    SequentialSpace transactions = new SequentialSpace();

    SpaceRepository tradeRepo = new SpaceRepository();

    public static final String sellOrderFlag = "SELL";
    public static final String buyOrderFlag = "BUY";
    static final String msgFlag = "MSG";
    static final String totalFlag = "TOTAL";
    static final String lock = "lock";
    static final String waiting = "WAITING";
    static final String notifyChange = "CHANGE";

    ExecutorService executor = Executors.newCachedThreadPool();
    static final int standardTimeout = 10; //TODO: Consider what this should be, or make it possible to set it per order.
    static final TimeUnit timeoutUnit = TimeUnit.HOURS; //TODO: Just for now, for testing...
    boolean serviceRunning;

    public Broker2() {
        tradeRepo.add("orders", orders);
        tradeRepo.add("orderPackages", newOrderPackages);
        tradeRepo.add("transactions", transactions);
        tradeRepo.addGate("tcp://" + hostName + ":" + port + "/?keep");
    }

    public static void main(String[] args) throws InterruptedException {
        Broker2 broker = new Broker2();
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
            while(serviceRunning) {
                try {
                    OrderPackage orderPkg = (OrderPackage) newOrderPackages.get(new FormalField(OrderPackage.class))[0];
                    executor.submit(new ProcessPackageTask(orderPkg));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

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
                transactions.put(finalTransactions); //TODO: Kun for test
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void waitForChange(Order order, Space space) throws InterruptedException {
        space.put(order.getId(), order.getMatchingOrderType(), order.getStock(), waiting);
        space.get(
                new ActualField(order.getId()),
                new ActualField(order.getMatchingOrderType()),
                new ActualField(order.getStock()),
                new ActualField(notifyChange));
    }

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
                List<Object[]> res = space.queryAll(matchTemplate);
                for (Object[] e : res) {
                    Order match = new Order(e);
                    //Break if the sender of both orders are the same client.
                    if (match.getOrderedBy().equals(order.getOrderedBy())) break;
                    if (!containsOrder(
                            matchingOrders, match) &&
                            !containsOrder(orderPkg.getMatchOrders(), match)
                            && (totalQfound + match.getMinQuantity() <= order.getQuantity())
                    ) {
                        matchingOrders.add(match);
                        orderPkg.getMatchOrders().add(match);
                        totalQfound += match.getQuantity();
                    }
                    if (totalQfound >= order.getMinQuantity()) break;
                }
                if (totalQfound >= order.getMinQuantity()) {
                    break;
                } else {
                    if (!checkIfThisExists())
                        break;
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

        public List<Transaction> lockTransactions(Space space) throws InterruptedException {

            Object[] thisOrder = space.getp(thisTemplate);
            if (thisOrder == null) {
                //space.put(lock);
                return new ArrayList<>();
                //This means that this order has probably already been processed by another orders task.
            }

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
            notifyListeners(orders);
            return generateTransactions(matchingOrders);
        }

        private List<Transaction> generateTransactions(List<Order> matches) {
            List<Transaction> transactions = new ArrayList<>();
            int remainingQ = order.getQuantity();
            for (Order match : matches) {
                int transactionQ = 0;
                while ((transactionQ <= remainingQ) && (transactionQ <= match.getQuantity())) transactionQ++;
                transactionQ -= 1;
                if (order.getOrderType().equals(sellOrderFlag)) {
                    transactions.add(new Transaction(order.getOrderedBy(), match.getOrderedBy(), order.getStock(), 100, transactionQ));
                } else {
                    transactions.add(new Transaction(match.getOrderedBy(), order.getOrderedBy(), order.getStock(), 100, transactionQ));
                }
                remainingQ -= transactionQ;
            }
            return transactions;
        }

        private boolean checkIfThisExists() throws InterruptedException {
            orders.get(new ActualField(lock));
            boolean b = !(orders.queryp(thisTemplate) == null);
            orders.put(lock);
            return b;
        }
    }
}