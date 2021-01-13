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
        //executor.submit(new NewOrderHandler());
        executor.submit(new NewOrderPkgHandler());

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    notifyListeners(orders);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 5, TimeUnit.SECONDS);
    }
/*
    class NewOrderHandler implements Callable<String> {

        @Override
        public String call() throws Exception {
            while(serviceRunning) {
                try {
                    Order order = new Order(orders.get(
                            new FormalField(String.class), //Name of the client who made the order
                            new FormalField(String.class), //Type of order, eg. SELL or BUY
                            new FormalField(String.class), //Name of the stock
                            new FormalField(Integer.class), //Quantity
                            new FormalField(Integer.class)));
                    order.setId(UUID.randomUUID()); //TODO: Skal dette gøres anderledes?
                    System.out.println("Received order:");
                    System.out.println(order.toString());
                    orders.put(
                            order.getId(),
                            order.getOrderedBy(),
                            order.getOrderType(),
                            order.getStock(),
                            order.getQuantity(),
                            order.getMinQuantity()
                    );
                    notifyListeners(orders);
                    executor.submit(new ProcessOrderTask(order));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return "Handler for handling market sale orders stopped!";
        }
    }
*/
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
            List<List<Transaction>> transactions = new ArrayList<>();

            try {
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
                    notifyListeners(orders);
                    tasks.add(new ProcessOrderTask(orderPkg, order));
                }
                List<Future<List<Transaction>>> futures = executor.invokeAll(tasks);
                for (Future<List<Transaction>> future : futures) {
                    transactions.add(future.get());
                }

                List<Transaction> result = new ArrayList<>();
                for (List<Transaction> l : transactions) {
                    result.addAll(l);
                }
                newOrderPackages.put("DONE!", result); //TODO: Kun for test
            } catch (InterruptedException | ExecutionException e) {
                //e.printStackTrace();
                try {
                    List<Transaction> result = new ArrayList<>();
                    newOrderPackages.put("DONE!", result); //TODO: Kun for test;
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }

            //transactions.add(executor.submit(new ProcessOrderTask(orderPkg, order)).get(standardTimeout, timeoutUnit));
            //space.put("DONE!", matchingOrders); //TODO: Kun for test
           // newOrderPackages.put("DONE!", transactions); //TODO: Kun for test
        }
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

    class ProcessOrderTask implements Callable<List<Transaction>> {

        OrderPackage orderPkg;
        Order order;
        List<Order> matchingOrders = new ArrayList<>();
        List<Transaction> transactions = new ArrayList<>();
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

        private void waitForChange(Space space) throws InterruptedException {
            space.put(order.getId(), order.getMatchingOrderType(), order.getStock(), waiting);
            space.get(
                    new ActualField(order.getId()),
                    new ActualField(order.getMatchingOrderType()),
                    new ActualField(order.getStock()),
                    new ActualField(notifyChange));
        }

        private boolean containsOrder(List<Order> list, Order order) {
            for (Order e : list) {
                if (e.getId() == order.getId()) return true;
            }
            return false;
        }

        private void findMatchingOrders(Space space) throws InterruptedException {
            while (true) {
                List<Object[]> res = space.queryAll(matchTemplate);
                for (Object[] e : res) {
                    Order match = new Order(e);
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
                    waitForChange(space);
                }
            }
        }

        private void lockTransactions(Space space) throws InterruptedException {
            space.get(new ActualField(lock));

            Object[] thisOrder = space.getp(thisTemplate);
            if (thisOrder == null) {
                space.put(lock);
                return;
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

            generateTransactions(matchingOrders);

            space.put(lock);
            notifyListeners(orders);
        }

        private void generateTransactions(List<Order> matches) {
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
        }

        private boolean checkIfThisExists() throws InterruptedException {
            orders.get(new ActualField(lock));
            boolean b = !(orders.queryp(thisTemplate) == null);
            orders.put(lock);
            return b;
        }

        @Override
        public List<Transaction> call() {
            try {
                findMatchingOrders(orders);
                if (!checkIfThisExists())
                    return transactions;
                lockTransactions(orders);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return transactions;
        }
    }
}