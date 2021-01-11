package server;

import model.Stock;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static shared.Channels.*;
import static shared.Requests.*;
import static shared.StockNames.*;

public class UserServerCommunicationTask implements Callable<String> {
    private final SequentialSpace userServer;
    private final SequentialSpace serverUser;
    private final String username;

    private RemoteSpace marketOrders;

    public UserServerCommunicationTask(SequentialSpace userServer,
                                       SequentialSpace serverUser,
                                       String username) {
        this.userServer = userServer;
        this.serverUser = serverUser;
        this.username = username;
        System.out.println("USCom: Starting private channel for: " + username);

        try {
            String brokerSpaceStr = String.format("tcp://%s:%d/%s?%s", BROKER_HOSTNAME, BROKER_PORT, MARKET_ORDERS, CONNECTION_TYPE);
            marketOrders = new RemoteSpace(brokerSpaceStr);
            marketOrders.put("Hello broker");
            var thing = marketOrders.queryp(new FormalField(String.class));
            System.out.println(thing[0].toString());

            marketOrders.put(username, SELL, APPLE, 10);

            Object[] res = this.marketOrders.get(
                    new ActualField(username),
                    new FormalField(String.class),
                    new FormalField(String.class));
            System.out.println(res[0].toString() + res[1].toString() + res[2].toString());


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

    private void requestResolver(String request) {
        System.out.println("USCom: User requested: " + request);
        try {
            switch (request) {
                case QUERY_STOCKS -> queryStocks();
                case BUY_STOCK -> buyStock();
                case SELL_STOCK -> sellStock();
                case LOG_OUT -> logOut();
                default -> System.out.println("USCom: ERROR IN SWITCH STMT");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void sellStock() throws InterruptedException {
        System.out.println("USCom: Place order...");

        //todo - Wulff - implementer at ordren til broker bliver sendt her:

        try {
            marketOrders.put(username, SELL, APPLE, 10);

            Object[] res = this.marketOrders.get(
                    new ActualField(username),
                    new ActualField(MSG),
                    new FormalField(String.class));

            System.out.println(res[0].toString() + res[1].toString() + res[2].toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void buyStock() throws InterruptedException {
        //System.out.printf("%s: Placed order to %s %s shares of %s. %n", clientID, args[0], args[2], args[1]);

        System.out.println("USCom: Place order...");

        marketOrders.put(username, BUY, APPLE, 10);

        //("ALICE", "SELL", "AAPL", 10)

        Object[] res = marketOrders.get(
                new ActualField(username),
                new ActualField("MSG"),
                new FormalField(String.class)
        );
        System.out.println(res);
        System.out.println("Broker: ");
        System.out.println(res[0]);
        System.out.println(res[1]);
        System.out.println(res[2]);


        Thread.sleep(7000);
    }

    private void logOut() {
        System.out.printf("USCom: Logging %s out...\n", username);
        Server.logout(username);
    }

    private void queryStocks() throws InterruptedException {
        //Forward request to account service
        System.out.println("USCom: Sending request...");
        Server.serverAccountService.put(username, QUERY_STOCKS);

        Object[] accountServiceResponse = Server.accountServiceServer.get(
                new ActualField(username),
                new FormalField(String.class));
        String responseStr = accountServiceResponse[1].toString();

        if (responseStr.equals(OK)) {
            do {
                System.out.println("USCom: Fetching data...");

                accountServiceResponse = Server.accountServiceServer.get(
                        new ActualField(username),
                        new FormalField(String.class));
                responseStr = accountServiceResponse[1].toString();
                System.out.println(responseStr);

                if (responseStr.equals(MORE_DATA)) {
                    //Fetching data from service

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
                System.out.println();
            } while (responseStr.equals(MORE_DATA));

        } else if (responseStr.equals(KO)) {
            System.out.println("USCom: No such user in the system");
        }
    }
}
