package Client;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.Scanner;

import static model.Requests.*;
import static model.Channels.*;

public class LoginClient {
    static RemoteSpace serverClient = null;
    static RemoteSpace clientServer = null;
    static RemoteSpace userServer = null;
    static RemoteSpace serverUser = null;
    static String username = "Alice";
    static String password = "password";

    public static void main(String[] args) {
        boolean loggedIn = false;

        Scanner s = new Scanner(System.in);

        // connect to tuple space
        try {
            serverClient = new RemoteSpace("tcp://localhost:123/serverClient?keep");
            clientServer = new RemoteSpace("tcp://localhost:123/clientServer?keep");

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Established connection to " + serverClient.getUri() + " and " + clientServer.getUri());

        String message = "";
        System.out.println("Welcome to the Beast Bank!");

        do {
            if (!loggedIn) {  // Slet - for test
                loggedIn = logIn(s);

                do {
                    System.out.println("\n1: fetch account data \n2: buy stocks \n3: sell stocks \n0: log out");
                    message = s.nextLine();
                    try {
                        if (message.equalsIgnoreCase("1")) {
                            queryData();
                        } else if (message.equalsIgnoreCase("2")) { //todo NJL
                            System.out.println("to be implemented");
                        } else if (message.equalsIgnoreCase("0")) {
                            logOut();
                            break;
                        }

                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }

                } while (!message.equalsIgnoreCase("exit"));

                try {
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                System.exit(7);
            }
        } while (!loggedIn);
    }


    private static boolean logIn(Scanner s) {
        System.out.println("Enter credentials to continue");

        System.out.println("Enter username: ");
        //username = "Alice";
        username = s.nextLine();
        System.out.println("Enter password: ");
        password =  s.nextLine();
        //password = "password";

        try {
            System.out.println("...sending credentials");
            clientServer.put(username, LOGIN);
            clientServer.put(username, password);

            try {
                String serverUserName = String.format("tcp://localhost:123/server%s?keep", username);
                String userServerName = String.format("tcp://localhost:123/%sserver?keep", username);
                userServer = new RemoteSpace(userServerName);
                serverUser = new RemoteSpace(serverUserName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(serverUser.getUri());
            System.out.println(userServer.getUri());
            Thread.sleep(3000);
            Object[] serverResponse = serverUser.get(new FormalField(String.class));

            String responseStr = serverResponse[0].toString();
            System.out.println("Login - " + responseStr);

            if (responseStr.equals(OK)) {
                return true;
            } else {
                System.out.println("Error in credentials\nTry again");
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void logOut() throws InterruptedException {
        userServer.put(LOG_OUT);
        System.out.println("Logging out");
    }

    private static void queryData() throws InterruptedException {
        System.out.println("requesting data...");
        //todo use id
        userServer.put(QUERY_STOCKS);

        String responseStr = "";
        do {
            //Object[] response = serverClient.get(new FormalField(String.class));
            Object[] response = serverUser.get(new FormalField(String.class));
            responseStr = response[0].toString();

            if (responseStr.equals(MORE_DATA)) {
                response = serverUser.get(new FormalField(String.class), new FormalField(Integer.class));
                //response = serverClient.get(new FormalField(String.class), new FormalField(Integer.class));
                String stockName = response[0].toString();
                int stockPrice = Integer.parseInt(response[1].toString());
                System.out.println(stockName + " to price: " + stockPrice);

            } else if (responseStr.equals(NO_MORE_DATA)) {
                continue;
            }

        } while (responseStr.equals(MORE_DATA));

    }
}
