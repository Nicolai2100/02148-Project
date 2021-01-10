package Client;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.Scanner;

import static model.Requests.*;

public class LoginClient {
    static RemoteSpace serverClient = null;
    static RemoteSpace clientServer = null;
    static RemoteSpace userServer = null;
    static RemoteSpace serverUser = null;
    static String username = "Alice";
    static String password = "password";

    public static void main(String[] args) {
        boolean loggedIn = false;

        // parse arguments
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

        if (!loggedIn) {  // Slet - for test

            do {
                System.out.println("Enter credentials to continue");

                System.out.println("Enter username: ");
                username = "Alice";
                //username = s.nextLine();
                System.out.println("Enter password: ");
                //password =  s.nextLine();
                password = "password";

                try {
                    System.out.println("...sending credentials");
                    clientServer.put(username, LOGIN);
                    clientServer.put(username, password);

                    //Object[] response = serverClient.get(new ActualField(username), new FormalField(String.class));

                    try{
                        String serUser = String.format("tcp://localhost:123/server%s?keep", username);
                        String userSer = String.format("tcp://localhost:123/%sserver?keep", username);
                        userServer = new RemoteSpace(userSer);
                        serverUser = new RemoteSpace(serUser);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    System.out.println(serverUser.getUri());
                    System.out.println(userServer.getUri());
                    Object[] serverResponse = serverUser.get(new FormalField(String.class));

                    String responseStr = serverResponse[0].toString();
                    System.out.println("Login - " + responseStr);

                    if (responseStr.equals(OK)) {
                        loggedIn = true;
                    }
                    else {
                        System.out.println("Error in credentials\nTry again");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (!loggedIn);
        } // Slet - for test

        do {
            System.out.println("\n 1: fetch account data \n2: buy stocks \n3: sell stocks \n0: log out");
            message = s.nextLine();
            try {
                if (message.equalsIgnoreCase("1")) {
                   queryData();
                } else if (message.equalsIgnoreCase("2")) { //todo NJL
                    System.out.println("to be implemented");
                } else if (message.equalsIgnoreCase("0")) {
                    System.out.println("Logging out"); //todo NJL
                    break;
                }

            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }

        } while (!message.equalsIgnoreCase("exit"));

        try {
            clientServer.put("Bye");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.exit(7);
    }

    private static void queryData() throws InterruptedException {
        System.out.println("requesting data...");
        //Smartere måde at gøre det på?
        clientServer.put(QUERY_STOCKS);
        //todo use id
        clientServer.put(QUERY_STOCKS, username);

        String responseStr = "";
        do {
            Object[] response = serverClient.get(new FormalField(String.class));
            responseStr = response[0].toString();

            if (responseStr.equals(MORE_DATA)) {
                response = serverClient.get(new FormalField(String.class), new FormalField(Integer.class));
                String stockName = response[0].toString();
                int stockPrice = Integer.parseInt(response[1].toString());
                System.out.println(stockName + " to price: " + stockPrice);

            } else if (responseStr.equals(NO_MORE_DATA)) {
                continue;
            }

        } while (responseStr.equals(MORE_DATA));

    }
}
