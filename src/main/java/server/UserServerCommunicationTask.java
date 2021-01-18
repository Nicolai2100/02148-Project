package server;

import BeastProject.broker.Order;
import BeastProject.broker.OrderPackage;
import BeastProject.model.Stock;
import org.jspace.*;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static BeastProject.shared.Channels.*;
import static BeastProject.shared.Requests.*;

public class UserServerCommunicationTask implements Callable<String> {
    private final SequentialSpace userServer;
    private final SequentialSpace serverUser;
    private final String username;

    private RemoteSpace orderPackages;

    public UserServerCommunicationTask(SequentialSpace userServer,
                                       SequentialSpace serverUser,
                                       String username) {
        this.userServer = userServer;
        this.serverUser = serverUser;
        this.username = username;
        System.out.println("USCom: Starting private channel for: " + username);

        try {
            String brokerSpaceStr = String.format("tcp://%s:%d/%s?%s", BROKER_HOSTNAME, BROKER_PORT, ORDER_PACKAGES, CONNECTION_TYPE);
            orderPackages = new RemoteSpace(brokerSpaceStr);
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
                requestResolver(request);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "USCom: Handler for User Server comm stopped!";
    }

    public void requestResolver(String request) {
        System.out.println("USCom: User requested: " + request);
        try {
            switch (request) {
                case QUERY_STOCKS -> queryStocks();
                case BUY -> buyStock();
                case SELL -> sellStock();
                case LOG_OUT -> logOut();

                default -> System.out.println("USCom: ERROR IN SWITCH STMT");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void sellStock() throws InterruptedException {
        System.out.println("USCom: Processing order...");

        var responseObj = userServer.get(
                new FormalField(String.class),
                new FormalField(Integer.class),
                new FormalField(Double.class),
                new FormalField(Integer.class));

        String stockName = responseObj[0].toString();
        int amount = Integer.parseInt(responseObj[1].toString());
        double minPricePerStock = Double.parseDouble(responseObj[2].toString());
        minPricePerStock = minPricePerStock == 0.0 ? -1.0 : minPricePerStock;
        int minAmountReq = Integer.parseInt(responseObj[3].toString());

        try {
            var op = new OrderPackage();
            op.addOrder(new Order.OrderBuilder()
                    .sell().orderedBy(username)
                    .stock(stockName)
                    .quantity(amount)
                    .minQuantity(minAmountReq)
                    .limit((int) minPricePerStock)
                    .build());
            orderPackages.put(op);
            System.out.println("USCom: Order placed...");
            serverUser.put("Order placed");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buyStock() throws InterruptedException {
        var responseObj = userServer.get(
                new FormalField(String.class),
                new FormalField(Integer.class),
                new FormalField(Double.class),
                new FormalField(Integer.class));

        String stockName = responseObj[0].toString();
        int amount = Integer.parseInt(responseObj[1].toString());
        double minPricePerStock = Double.parseDouble(responseObj[2].toString());
        minPricePerStock = minPricePerStock == 0.0 ? -1.0 : minPricePerStock;
        int minAmountReq = Integer.parseInt(responseObj[3].toString());

        try {
            var op = new OrderPackage();
            op.addOrder(new Order
                    .OrderBuilder()
                    .buy()
                    .orderedBy(username)
                    .stock(stockName)
                    .quantity(amount)
                    .minQuantity(minAmountReq)
                    .limit((int) minPricePerStock)
                    .build());

            orderPackages.put(op);

            System.out.println("USCom: Order placed...");
            serverUser.put("Order placed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logOut() {
        System.out.printf("USCom: Logging %s out...\n", username);
        Server.logout(username);
    }

    public void queryStocks() throws InterruptedException {
        //Forward request to account BeastProject.service
        System.out.println("USCom: Sending request...");
        Server.serverAccountService.put(username, QUERY_STOCKS);

        Object[] accountServiceResponse = Server.accountServiceServer.get(
                new ActualField(username),
                new FormalField(String.class));
        String responseStr = accountServiceResponse[1].toString();

        if (responseStr.equals(OK)) {
            System.out.println("USCom: Fetching data...");
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

                    //Sending data to client
                    System.out.println("USCom: Sending data");
                    serverUser.put(MORE_DATA);
                    serverUser.put((Stock) dataResponse[1]);
                } else if (responseStr.equals(NO_MORE_DATA)) {
                    serverUser.put(NO_MORE_DATA);
                    break;
                }
            } while (responseStr.equals(MORE_DATA));

        } else if (responseStr.equals(KO)) {
            System.out.println("USCom: No such user in the system");
        }
    }
}
