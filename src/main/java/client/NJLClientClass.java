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
        //For test...
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

        //Connect to tuple space
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
            if (runningWithArgs) {
                testRequestLoop();
            } else {
                getCredentials(s);
                if (logIn(username, password)) {
                    requestLoop(s);
                    s.close();
                    System.exit(7);
                }
            }
        }
        while (true);
    }

    private void testRequestLoop() {
        logIn(username, password);
        connectToPrivateChannel(username);

        try {
            String message;
            while (argList.size() > 0) {
                System.out.println("Processing request " + argList.get(0));
                message = argList.remove(0);
                sendRequest(message, new Scanner(System.in));
            }
            System.out.println("Test client finished");
            System.exit(2);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void requestLoop(Scanner s) {
        String message;
        do {
            System.out.println("\n1: Fetch account data \n2: See current market orders \n3: Buy stocks \n4: Sell stocks \n0: Log out");
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
            buyStock(scanner);
        } else if (message.equalsIgnoreCase("4")) {
            sellStock(scanner);
        } else if (message.equalsIgnoreCase("0")) {
            logOut();
            startClient(new String[]{""});
        }
    }

    private void queryMarket() throws InterruptedException {
        userServer.put(QUERY_MARKET_ORDERS);
    }

    private void buyStock(Scanner scanner) throws InterruptedException {
        stockTrade(scanner, BUY);
    }

    private void sellStock(Scanner scanner) throws InterruptedException {
        stockTrade(scanner, SELL);
    }

    private void stockTrade(Scanner scanner, String trade) throws InterruptedException {
        if (!runningWithArgs) {
            System.out.printf("Write name of stock to %s:\n", trade.toLowerCase());
            String stockName = scanner.nextLine();
            if (stockName.length() < 2 || stockName.equalsIgnoreCase("exit")) {
                return;
            }
            System.out.printf("Write number of stocks to %s:\n", trade.toLowerCase());
            int amount = scanner.nextInt();
            if (amount < 1 || stockName.equalsIgnoreCase("exit")) {
                return;
            }

            int minAmountReq = 1;

            if (amount > 1) {
                System.out.printf("Write minimum number of stocks to %s:\n", trade.toLowerCase());
                minAmountReq = scanner.nextInt();
                if (minAmountReq < 1 || stockName.equalsIgnoreCase("exit")) {
                    return;
                }
            }
            double minPricePerStock = 0.0;
            System.out.println("Write minimum price per stock:");
            minPricePerStock = scanner.nextDouble();
            if (stockName.equalsIgnoreCase("exit")) {
                return;
            }
            userServer.put(trade);
            userServer.put(stockName, amount, minPricePerStock, minAmountReq);

        } else {
            String stockName = argList.remove(0);
            int amount = Integer.parseInt(argList.remove(0));
            double pricePerStock = Double.parseDouble(argList.remove(0));
            int minAmountReq = Integer.parseInt(argList.remove(0));

            userServer.put(trade);
            userServer.put(stockName, amount, pricePerStock, minAmountReq);
        }

        var response = serverUser.get(new FormalField(String.class));
        System.out.println(response[0] + "...");
    }

    private void queryData() throws InterruptedException {
        System.out.println("Requesting data...");

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

    private void getCredentials(Scanner s) {
        System.out.println("Enter credentials to continue");

        System.out.println("Enter username: ");
        username = s.nextLine().trim();
        System.out.println("Enter password: ");
        password = s.nextLine().trim();
    }

    private boolean logIn(String username, String password) {

        try {
            System.out.println("...sending credentials");
            clientServer.put(LOGIN, username, password);

            connectToPrivateChannel(username);

            Thread.sleep(1000);

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

    private void connectToPrivateChannel(String username) {
        try {
            String serverUserStr = String.format("tcp://localhost:123/server%s?%s", username, CONNECTION_TYPE);
            String userServerStr = String.format("tcp://localhost:123/%sserver?%s", username, CONNECTION_TYPE);
            userServer = new RemoteSpace(userServerStr);
            serverUser = new RemoteSpace(serverUserStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logOut() throws InterruptedException {
        userServer.put(LOG_OUT);
        System.out.println("Logging out...");
    }
}
