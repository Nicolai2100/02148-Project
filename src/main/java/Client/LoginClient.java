package Client;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.Scanner;

import static model.Requests.*;

public class LoginClient {

    public static void main(String[] args) {
        boolean loggedIn = false;

        // parse arguments
        Scanner s = new Scanner(System.in);

        RemoteSpace serverClient = null;
        RemoteSpace clientServer = null;
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
                String username = s.nextLine();
                System.out.println("Enter password: ");
                String password =  s.nextLine();

                try {
                    System.out.println("...sending credentials");
                    clientServer.put(LOGIN);
                    clientServer.put(username, password);

                    Object[] response = serverClient.get(new FormalField(String.class));
                    String responseStr = response[0].toString();
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
            System.out.println("Type to \n 1: fetch account data \n2: buy stocks \n3: sell stocks \n0: log out");
            message = s.nextLine();
            try {

                if (message.equalsIgnoreCase("1")) {
                    System.out.println("fetcing data");
                    //Object[] data = serverClient.get(new FormalField(String.class));
                    clientServer.put(QUERY_STOCKS);

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
}
