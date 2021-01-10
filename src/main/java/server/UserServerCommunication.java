package server;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.concurrent.Callable;

import static shared.Requests.*;

public class UserServerCommunication implements Callable<String> {
    private final SequentialSpace userServer;
    private final SequentialSpace serverUser;
    private String username = "";

    public UserServerCommunication(SequentialSpace userServer,
                                   SequentialSpace serverUser,
                                   String username) {
        this.userServer = userServer;
        this.serverUser = serverUser;
        this.username = username;
        System.out.println("Starting private channel for: " + username);
    }

    //Listen for requests
    @Override
    public String call() throws Exception {
        try {
            //Start by telling user login successful
            serverUser.put(OK);

            while(true){
                Object[] requestT = userServer.get(new FormalField(String.class));
                System.out.println(requestT[0]);
                String request = requestT[0].toString();
                requestResolver(request);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Handler for User Server comm stopped!";
    }

    private void requestResolver(String request) {
        System.out.println("Client requested: " + request);
        try {
            switch (request) {
                case QUERY_STOCKS -> queryStocks();
                case LOG_OUT -> logOut();
                default -> System.out.println("ERROR IN SWITCH STMT");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void logOut() {
        System.out.printf("Logging %s out...\n", username);
        Server.logout(username);
    }

    private void queryStocks() throws InterruptedException {
        //Forward request to account service
        System.out.println("Sending request...");
        Server.serverAccountService.put(username, QUERY_STOCKS);

        Object[] accountServiceResponse = Server.accountServiceServer.get(new ActualField(username), new FormalField(String.class));
        String responseStr = accountServiceResponse[1].toString();

        System.out.println("Request: " + responseStr);

        if (responseStr.equals(OK)) {
            do {
                System.out.println("Fetching data...");

                accountServiceResponse = Server.accountServiceServer.get(new ActualField(username), new FormalField(String.class));
                responseStr = accountServiceResponse[1].toString();
                System.out.println(responseStr);

                if (responseStr.equals(MORE_DATA)) {
                    //Fetching data from service
                    Object[] dataResponse = Server.accountServiceServer.get(new ActualField(username), new FormalField(String.class), new FormalField(Integer.class));
                    String stockName = dataResponse[1].toString();
                    int stockPrice = Integer.parseInt(dataResponse[2].toString());

                    //Sending data to client
                    System.out.println("Sending data");
                    serverUser.put(MORE_DATA);
                    serverUser.put(stockName, stockPrice);
                } else if (responseStr.equals(NO_MORE_DATA)) {
                    serverUser.put(NO_MORE_DATA);
                    break;
                }
                System.out.println();
            } while (responseStr.equals(MORE_DATA));

        } else if (responseStr.equals(KO)) {
            //serverClient.put(KO);
            System.out.println("No such user in the system");
        }
    }
}
