package BeastBank.bank;

import BeastBank.broker.Order;
import BeastBank.broker.OrderPackage;
import BeastBank.model.Stock;
import org.jspace.*;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;

import static BeastBank.shared.Channels.*;
import static BeastBank.shared.Requests.*;

public class UserServerCommunicationTask implements Callable<String> {
    private final Space userServer;
    private final Space serverUser;
    private final String username;

    private RemoteSpace orderPackages;
    private RemoteSpace orders;
    private String serverStr = UserServerCommunicationTask.class.getName() + ": ";

    public UserServerCommunicationTask(Space userServer,
                                       Space serverUser,
                                       String username) {
        this.userServer = userServer;
        this.serverUser = serverUser;
        this.username = username;
        System.out.println(serverStr + "Starting private channel for: " + username);

        try {
            String brokerSpaceStr = String.format("tcp://%s:%d/%s?%s", BROKER_HOSTNAME, BROKER_PORT, ORDER_PACKAGES, CONNECTION_TYPE);
            orderPackages = new RemoteSpace(brokerSpaceStr);

            String brokerSpaceOrdersStr = String.format("tcp://%s:%d/%s?%s", BROKER_HOSTNAME, BROKER_PORT, ORDERS, CONNECTION_TYPE);
            orders = new RemoteSpace(brokerSpaceOrdersStr);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String call() throws Exception {
        try {
            //Start by sending "login OK" to user via private channel
            serverUser.put(OK);

            //Listen for requests
            while (true) {
                Object[] requestT = userServer.get(new FormalField(String.class));
                String request = requestT[0].toString();
                resolveRequest(request);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return serverStr + "Handler for User Server comm stopped!";
    }

    public void resolveRequest(String request) {
        System.out.println(serverStr + "User requested: " + request);
        try {
            switch (request) {
                case QUERY_STOCKS:
                    queryStocks();

                case QUERY_MARKET_ORDERS:
                    queryMarket();

                    break;
                case BUY:
                    buyStock();
                    break;

                case SELL:
                    sellStock();
                    break;

                case LOG_OUT:
                    logOut();
                    break;

                default:
                    System.out.println(serverStr +  "USCom: ERROR IN SWITCH STMT");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void queryMarket() throws InterruptedException {
        var response = orders.queryAll(
                new FormalField(UUID.class),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(String.class));

        int numOfOrders = response.size();

        if (numOfOrders < 1) {
            serverUser.put(NO_MORE_DATA);
        } else {
            for (var order : response) {
                int numOfOrder = response.indexOf(order) + 1;
                System.out.println(UserServerCommunicationTask.class.getName() + ": USCom: Sending data");
                serverUser.put(MORE_DATA);
                serverUser.put(numOfOrder, numOfOrders, order[2], order[3], order[4], order[5], order[1]);
            }
            serverUser.put(NO_MORE_DATA);
        }
    }

    private OrderPackage getOrderFromClient(String orderType) throws Exception {
        var responseObj = userServer.get(
                new FormalField(String.class),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(Integer.class),
                new FormalField(String.class));

        String stockName = responseObj[0].toString();
        int amount = Integer.parseInt(responseObj[1].toString());
        int minPricePerStock = Integer.parseInt(responseObj[2].toString());
        minPricePerStock = minPricePerStock == 0 ? -1 : minPricePerStock;
        int minAmountReq = Integer.parseInt(responseObj[3].toString());
        String targetUser = responseObj[4].toString();

        var op = new OrderPackage();
        if (orderType.equalsIgnoreCase(SELL)) {

            if (targetUser.length() > 1) {
                op.addOrder(new Order.OrderBuilder()
                        .sell()
                        .orderedBy(username)
                        .stock(stockName)
                        .quantity(amount)
                        .minQuantity(minAmountReq)
                        .limit(minPricePerStock)
                        .clientMatch(targetUser)
                        .build());
            } else {
                op.addOrder(new Order.OrderBuilder()
                        .sell()
                        .orderedBy(username)
                        .stock(stockName)
                        .quantity(amount)
                        .minQuantity(minAmountReq)
                        .limit(minPricePerStock)
                        .build());
            }

        } else {
            if (targetUser.length() > 1) {
                op.addOrder(new Order.OrderBuilder()
                        .buy()
                        .orderedBy(username)
                        .stock(stockName)
                        .quantity(amount)
                        .minQuantity(minAmountReq)
                        .limit(minPricePerStock)
                        .clientMatch(targetUser)
                        .build());
            } else {
                op.addOrder(new Order.OrderBuilder()
                        .buy()
                        .orderedBy(username)
                        .stock(stockName)
                        .quantity(amount)
                        .minQuantity(minAmountReq)
                        .limit(minPricePerStock)
                        .build());
            }
        }
        return op;
    }

    private void sellStock() {
        System.out.println(serverStr + "Processing order...");

        try {
            OrderPackage op = getOrderFromClient(SELL);
            orderPackages.put(op);
            System.out.println(serverStr + "Order placed...");
            serverUser.put("Order placed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buyStock() {
        System.out.println(serverStr + "Processing order...");

        try {
            OrderPackage op = getOrderFromClient(BUY);
            orderPackages.put(op);
            System.out.println(serverStr + "Order placed...");
            serverUser.put("Order placed");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logOut() {
        System.out.printf(serverStr + "Logging %s out...\n", username);
        Server.logout(username);
    }

    public void queryStocks() throws InterruptedException {
        //Forward request to account BeastProject.service
        System.out.println(serverStr + "Sending request...");
        Server.serverAccountService.put(username, QUERY_STOCKS);

        Object[] accountServiceResponse = Server.accountServiceServer.get(
                new ActualField(username),
                new FormalField(String.class));
        String responseStr = accountServiceResponse[1].toString();

        if (responseStr.equals(OK)) {
            System.out.println(serverStr + "Fetching data...");
            accountServiceResponse = Server.accountServiceServer.get(
                    new ActualField(username),
                    new FormalField(Double.class));

            serverUser.put(accountServiceResponse[1]);

            do {
                accountServiceResponse = Server.accountServiceServer.get(
                        new ActualField(username),
                        new FormalField(String.class));
                responseStr = accountServiceResponse[1].toString();

                if (responseStr.equals(MORE_DATA)) {
                    //Fetching data from BeastProject.service

                    Object[] dataResponse = Server.accountServiceServer.get(
                            new ActualField(username),
                            new FormalField(Stock.class));

                    ArrayList<Stock> stocks = new ArrayList<>();
                    stocks.add((Stock) dataResponse[1]);

                    //Sending data to BeastBank.client
                    System.out.println(serverStr + "Sending data");
                    serverUser.put(MORE_DATA);
                    serverUser.put((Stock) dataResponse[1]);
                } else if (responseStr.equals(NO_MORE_DATA)) {
                    serverUser.put(NO_MORE_DATA);
                    break;
                }
            } while (responseStr.equals(MORE_DATA));

        } else if (responseStr.equals(KO)) {
            System.out.println(serverStr + "No such user in the system");
        }
    }
}
