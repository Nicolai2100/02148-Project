package client;

import model.Stock;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import static shared.Channels.*;
import static shared.Channels.CONNECTION_TYPE;
import static shared.Requests.*;
import static shared.Requests.MORE_DATA;

public class NJLClientClass {

    RemoteSpace serverClient = null;
    RemoteSpace clientServer = null;
    RemoteSpace userServer = null;
    RemoteSpace serverUser = null;
    String username;
    String password;
    boolean runningWithArgs = false;
    ArrayList<String> argList = new ArrayList<>();

    public void startClient(String[] args) {
        boolean loggedIn = false;

        if (args.length > 1) {
            runningWithArgs = true;
            username = args[0];
            password = args[1];

            for (int i = 0; i < args.length; i++) {
                if (i > 1) {
                    argList.add(args[i]);
                    System.out.printf("Client arg number: %s is \"%s\"", i, args[i]);
                    System.out.println();
                }
            }
        }

        // connect to tuple space
        try {
            String serverService = String.format("tcp://localhost:123/%s?" + CONNECTION_TYPE, SERVER_CLIENT);
            String serviceServer = String.format("tcp://localhost:123/%s?" + CONNECTION_TYPE, CLIENT_SERVER);
            serverClient = new RemoteSpace(serverService);
            clientServer = new RemoteSpace(serviceServer);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("Welcome to the Beast Bank!");
        Scanner s = new Scanner(System.in);

        do {
            if (!loggedIn) {  // Slet - for test
                loggedIn = logIn(s);

                if (loggedIn) {
                    if (runningWithArgs)
                        try {
                            testRequestLoop();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    requestLoop(s);
                    s.close();
                    System.exit(7);
                }
            }
        }
        while (!loggedIn);
    }

    private void testRequestLoop() throws InterruptedException {
        String message;
        while (argList.size() > 0) {
            System.out.println("\n1: Fetch account data \n2: Buy stocks \n3: Sell stocks \n0: Log out");
            System.out.println("Processing request " + argList.get(0));
            message = argList.remove(0);
            sendRequest(message, new Scanner(System.in));
        }
        System.out.println("Test client finished");
        System.exit(2);
    }

    private void requestLoop(Scanner s) {
        String message;
        do {
            System.out.println("\n1: Fetch account data \n2: Buy stocks \n3: Sell stocks \n0: Log out");
            message = s.nextLine();
            try {
                sendRequest(message, s);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        } while (!message.equalsIgnoreCase("exit"));
    }

    private void sendRequest(String message, Scanner scanner) throws InterruptedException {
        if (message.equalsIgnoreCase("1")) {
            queryData();
        } else if (message.equalsIgnoreCase("2")) {
            queryMarket();
        } else if (message.equalsIgnoreCase("3")) {
            buyStock();
        } else if (message.equalsIgnoreCase("4")) {
            sellStock(scanner);
        } else if (message.equalsIgnoreCase("0")) {
            logOut();
            startClient(new String[]{""});
        }
    }

    private void queryMarket() throws InterruptedException {
        userServer.put(QUERY_MARKETORDERS);
    }

    private void sellStock(Scanner scanner) throws InterruptedException {

        /*System.out.println("Write name of stock to sell:");
        String stockName = scanner.nextLine();
        System.out.println("Write number of stocks to sell:");
        int amount = scanner.nextInt();
        userServer.put(SELL_STOCK);
        userServer.put(stockName, amount);*/

        System.out.println("Write name of stock to sell:");
        String stockName = argList.remove(0);
        System.out.println("Write number of stocks to sell:");
        int amount = Integer.parseInt(argList.remove(0));
        userServer.put(SELL_STOCK);
        userServer.put(stockName, amount);

        var thing = serverUser.get(new FormalField(String.class));
        System.out.println(thing[0]);
    }

    private void buyStock() throws InterruptedException {
        userServer.put(BUY_STOCK);
    }

    private void queryData() throws InterruptedException {
        System.out.println("Requesting data...");
        //todo use id
        userServer.put(QUERY_STOCKS);

        String responseStr = "";
        Object[] response;

        response = serverUser.get(new FormalField(Double.class));
        System.out.println("Balance: " + response[0].toString());
        do {
            response = serverUser.get(new FormalField(String.class));
            responseStr = response[0].toString();

            if (responseStr.equals(MORE_DATA)) {
                response = serverUser.get(new FormalField(Stock.class));
                System.out.println(response[0].toString());

            } else if (responseStr.equals(NO_MORE_DATA)) {
                continue;
            }
        } while (responseStr.equals(MORE_DATA));
    }

    private boolean logIn(Scanner s) {
        System.out.println("Enter credentials to continue");

        if (!runningWithArgs) {
            System.out.println("Enter username: ");
            username = s.nextLine().trim();
            System.out.println("Enter password: ");
            password = s.nextLine().trim();
        } else {
            System.out.println("Username: " + username);
            System.out.println("Password: " + password);
        }

        try {
            System.out.println("...sending credentials");
            clientServer.put(LOGIN, username, password);

            try {
                String serverUserStr = String.format("tcp://localhost:123/server%s?%s", username, CONNECTION_TYPE);
                String userServerStr = String.format("tcp://localhost:123/%sserver?%s", username, CONNECTION_TYPE);
                userServer = new RemoteSpace(userServerStr);
                serverUser = new RemoteSpace(serverUserStr);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Thread.sleep(1000); //Skal bruges for ellers går det for hurtigt ved test virker det ikke... ?

            Object[] serverResponse = serverUser.get(new FormalField(String.class));

            String responseStr = KO;
            try {
                responseStr = serverResponse[0].toString();
                System.out.println("Login - " + responseStr);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

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

    private void logOut() throws InterruptedException {
        userServer.put(LOG_OUT);
        System.out.println("Logging out...");
    }
}


