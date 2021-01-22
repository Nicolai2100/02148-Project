package BeastBank.bank;

import BeastBank.model.Stock;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import static BeastBank.shared.Channels.*;
import static BeastBank.shared.Channels.CONNECTION_TYPE;
import static BeastBank.shared.Requests.*;
import static BeastBank.shared.Requests.MORE_DATA;

public class ClientClass {

    private RemoteSpace serverClient = null;
    private RemoteSpace clientServer = null;
    private RemoteSpace userServer = null;
    private RemoteSpace serverUser = null;

    private RemoteSpace serverClientMsgs = null;

    private ArrayList<String> argList = new ArrayList<>();
    private boolean runningWithArgs = false;

    private String username;
    private String password;
    private final Scanner scanner = new Scanner(System.in);
    private String HOSTNAME = "127.0.0.1";

    public void startClient(String[] args) {
        if (args[0].equalsIgnoreCase("true")) {
            HOSTNAME = REMOTE_SERVER_HOSTNAME;
        }

        //The args is intended only to be used for test purposes...
        if (args.length > 2) {
            username = args[1];
            password = args[2];
            runningWithArgs = true;

            for (int i = 3; i < args.length; i++) {
                argList.add(args[i]);
                System.out.printf("Client arg number: %s is \"%s\"\n", i, args[i]);
            }
        }

        //Connect to tuple space
        boolean notConnectedServer = true;
        while (notConnectedServer)
            try {
                System.out.println("trying to connect to: " + HOSTNAME + ":" + SERVER_PORT);

                String serverService = String.format("tcp://%s:%d/%s?%s", HOSTNAME, SERVER_PORT, SERVER_CLIENT, CONNECTION_TYPE);
                serverClient = new RemoteSpace(serverService);

                String serviceServer = String.format("tcp://%s:%d/%s?%s", HOSTNAME, SERVER_PORT, CLIENT_SERVER, CONNECTION_TYPE);
                clientServer = new RemoteSpace(serviceServer);

                String serverClientMsgsStr = String.format("tcp://%s:%d/%s?%s", HOSTNAME, SERVER_PORT, SERVER_CLIENT_MSG, CONNECTION_TYPE);
                serverClientMsgs = new RemoteSpace(serverClientMsgsStr);

                System.out.println("Connecting to " + serverClient.getUri());
                System.out.println("Connecting to " + clientServer.getUri());
                notConnectedServer = false;

            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

        System.out.println("Welcome to the Beast Bank!");

        do {
            if (runningWithArgs) {
                testRequestLoop();
            } else {
                getCredentials();
                if (logIn(username, password)) {
                    requestLoop();
                    System.exit(7);
                }
            }
        }
        while (true);
    }

    private void requestLoop() {
        String request = "";

        while (!request.equalsIgnoreCase("exit")) {

            String msgOption = "";
            boolean hasMsg = checkMsgFromBank();
            if (hasMsg) {
                msgOption = "\n6: Read messages from bank";
            }
            String options = String.format
                    ("\n1: Fetch account data \n2: See current market orders \n3: Buy stocks \n4: Sell stocks %s \n0: Log out \nexit: Shut down", msgOption);
            System.out.println(options);

            request = scanner.next();
            try {
                sendRequest(request, hasMsg);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
        try {
            logOut();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    private void sendRequest(String message, boolean hasMsg) throws InterruptedException {
        if (message.equalsIgnoreCase("1")) {
            queryData();
        } else if (message.equalsIgnoreCase("2")) {
            queryMarket();
        } else if (message.equalsIgnoreCase("3")) {
            buyStock();
        } else if (message.equalsIgnoreCase("4")) {
            sellStock();
        } else if (hasMsg && message.equalsIgnoreCase("6")) {
            readMsgs();
        } else if (message.equalsIgnoreCase("0")) {
            logOut();
            startClient(new String[]{""});
        }
    }

    private boolean checkMsgFromBank() {
        try {
            var response = serverClientMsgs.queryAll(
                    new ActualField(username),
                    new FormalField(String.class));

            if (response.size() > 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void readMsgs() throws InterruptedException {
        var response = serverClientMsgs.getAll(
                new ActualField(username),
                new FormalField(String.class));

        for (Object[] msg : response) {
            System.out.println("Message: " + msg[1]);
        }
    }

    private void queryMarket() throws InterruptedException {
        System.out.println("Requesting data...");
        userServer.put(QUERY_MARKET_ORDERS);

        String responseStr;
        Object[] response;

        do {
            response = serverUser.get(new FormalField(String.class));
            responseStr = response[0].toString();

            if (responseStr.equals(NO_MORE_DATA)) {
                System.out.println("No more orders...");
            } else if (responseStr.equals(MORE_DATA)) {

                response = serverUser.get(
                        new FormalField(Integer.class),
                        new FormalField(Integer.class),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(Integer.class),
                        new FormalField(Integer.class),
                        new FormalField(String.class)
                );

                System.out.printf("Order no. %s of %s:\n%s - %s, quantity: %s, price: %s, by: %s\n",
                        response[0], response[1], response[2], response[3], response[4], response[5], response[6]);
            }
        } while (responseStr.equals(MORE_DATA));
    }

    private void buyStock() throws InterruptedException {
        stockTrade(BUY);
    }

    private void sellStock() throws InterruptedException {
        stockTrade(SELL);
    }

    private void stockTrade(String trade) throws InterruptedException {
        String minOrMax = trade.equals(SELL) ? "minimum" : "maximum";

        if (!runningWithArgs) {
            try {

                System.out.printf("Enter name of stock to %s:\n", trade.toLowerCase());
                String stockName = checkInputForExitToAbort();

                System.out.printf("Enter number of stocks to %s:\n", trade.toLowerCase());
                int amount = scanner.nextInt();

                int minAmountReq = 1;
                if (amount > 1) {
                    System.out.printf("Enter minimum number of stocks to %s:\n", trade.toLowerCase());
                    minAmountReq = scanner.nextInt();
                }

                int minPricePerStock;
                System.out.printf("Enter %s price per stock \n(Enter \"0\" for current market price) :\n", minOrMax);
                minPricePerStock = scanner.nextInt();

                String targetCustomer = "";
                System.out.println("If you only want to trade with one certain user");
                System.out.println("(Enter that users name or press \"0\" for any user)");
                targetCustomer = scanner.next();

                String priceStr = minPricePerStock == 0 ? "market price" : Integer.toString(minPricePerStock);

                String sellOrBuy = trade.equals(SELL) ? "sell" : "buy";
                if (targetCustomer.length() > 1) {
                    String fromToStr = trade.equals(SELL) ? "to" : "from";
                    System.out.printf("Are you sure you want to %s %s %s for %s each, %s %s? \n", sellOrBuy, amount, stockName, priceStr, fromToStr, targetCustomer);
                } else {
                    System.out.printf("Are you sure you want to %s %s %s for %s each? \n", sellOrBuy, amount, stockName, priceStr);
                }

                System.out.println("1 - Yes\n0 - No");
                if (checkInputForExitToAbort().equalsIgnoreCase("1")) {
                    userServer.put(trade);
                    userServer.put(stockName, amount, minPricePerStock, minAmountReq, targetCustomer);
                } else return;

            } catch (ClientInputException e) {
                System.out.println(e.getMessage());
                return;
            }

        } else {
            String stockName = argList.remove(0);
            int amount = Integer.parseInt(argList.remove(0));
            int pricePerStock = Integer.parseInt(argList.remove(0));
            int minAmountReq = Integer.parseInt(argList.remove(0));
            String targetCustomer = (argList.remove(0));

            userServer.put(trade);
            userServer.put(stockName, amount, pricePerStock, minAmountReq, targetCustomer);
        }
        var response = serverUser.get(new FormalField(String.class));
        System.out.println(response[0] + "...");
        Thread.sleep(1000);
    }

    private void queryData() throws InterruptedException {
        System.out.println("Requesting data...");

        userServer.put(QUERY_STOCKS);

        String responseStr;
        Object[] response;

        response = serverUser.get(new FormalField(Double.class));
        System.out.println("Balance: " + response[0].toString());
        do {
            response = serverUser.get(new FormalField(String.class));
            responseStr = response[0].toString();

            if (responseStr.equals(MORE_DATA)) {
                response = serverUser.get(new FormalField(Stock.class));
                System.out.println(response[0].toString());

            }
        } while (responseStr.equals(MORE_DATA));
    }

    private void getCredentials() {
        System.out.println("Enter credentials to continue");
        System.out.println("Enter username: ");
        username = checkExitToLogOut(scanner.next().trim());
        System.out.println("Enter password: ");
        password = checkExitToLogOut(scanner.next().trim());
    }

    private String checkInputForExitToAbort() {
        String msg = scanner.next();
        if (msg.equalsIgnoreCase("exit")) {
            requestLoop();
        }
        return msg;
    }

    public <T> T validateResponse(T lowerLimit, T upperLimit, T userInput, String errorMsg) {
        String typen = "";
        if (userInput instanceof String) {
            typen = "string";
        } else if (userInput instanceof Integer) {
            typen = "integer";
        } else if (userInput instanceof Double) {
            typen = "double";
        }

        if (typen.equals("integer")) {
            if (lowerLimit != null && upperLimit != null) {
                if ((Integer) userInput >= (Integer) lowerLimit && (Integer) userInput <= (Integer) upperLimit) {
                    return userInput;
                }
            }
        }
        if (typen.equals("double")) {
            if (lowerLimit != null && upperLimit != null) {
                if ((Double) userInput >= (Double) lowerLimit && (Double) userInput <= (Double) upperLimit) {
                    return userInput;
                }
            }
        }
        if (typen.equals("string")) {
            if (((String) userInput).length() > 2) {
                return userInput;
            }
        }
        throw new ClientInputException(errorMsg);
    }

    private String checkExitToLogOut(String msg) {
        if (msg.equalsIgnoreCase("exit")) {
            try {
                shutDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return msg;
    }

    private void shutDown() throws InterruptedException {
        logOut();
        System.out.println("Bye");
        System.exit(2);
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
            String serverUserStr = String.format("tcp://%s:%s/server%s?%s", HOSTNAME, SERVER_PORT, username, CONNECTION_TYPE);
            String userServerStr = String.format("tcp://%s:%s/%sserver?%s", HOSTNAME, SERVER_PORT, username, CONNECTION_TYPE);
            userServer = new RemoteSpace(userServerStr);
            serverUser = new RemoteSpace(serverUserStr);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logOut() throws InterruptedException {
        try {
            userServer.put(LOG_OUT);
            System.out.println("Logging out...");
        } catch (NullPointerException e) {
            //System.out.println("User not logged in.");
        }
    }

    private void testRequestLoop() {
        logIn(username, password);
        connectToPrivateChannel(username);
        boolean hasMsg = false;
        try {
            String message;
            while (argList.size() > 0 || hasMsg) {
                if (!hasMsg) {
                    System.out.println("Processing request " + argList.get(0));
                    message = argList.remove(0);
                    sendRequest(message, true);
                    hasMsg = checkMsgFromBank();

                } else if (hasMsg) {
                    sendRequest("6", true);
                    hasMsg = false;
                }
            }

            System.out.println("Test client finished");
            logOut();
            System.exit(2);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
