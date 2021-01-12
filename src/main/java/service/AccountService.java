package service;

import model.Account;
import model.Stock;
import model.User;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static shared.Requests.*;
import static shared.StockNames.*;
import static shared.Channels.*;


public class AccountService {
    //static boolean serviceRunning = true;
    boolean connectedToServer = false;
    RemoteSpace serverAccountService = null;
    RemoteSpace accountServiceServer = null;
    HashMap<String, HashMap> accountsMap;

    public void startService(String[] args) {
        accountsMap = instantiateTestData();

        try {
            requestHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestHandler() throws Exception {
        while (true) {

            if (!connectedToServer) {
                // connect to tuple space
                try {
                    System.out.println(AccountService.class.getName() + ": Trying to establish connection to remote spaces...");
                    String serverService = String.format("tcp://localhost:123/%s?%s", SERVER_ACCOUNT_SERVICE, CONNECTION_TYPE);
                    String serviceServer = String.format("tcp://localhost:123/%s?%s", ACCOUNT_SERVICE_SERVER, CONNECTION_TYPE);
                    serverAccountService = new RemoteSpace(serverService);
                    accountServiceServer = new RemoteSpace(serviceServer);
                    connectedToServer = true;

                    System.out.printf(AccountService.class.getName() + ": Established connection to remote spaces:\n%s and \n%s at " + LocalDateTime.now(),
                            serverAccountService.getUri(),
                            accountServiceServer.getUri());
                    System.out.println(AccountService.class.getName() + ":\n\nWaiting for requests...");

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    connectedToServer = false;
                }

            } else if (connectedToServer) {

                while (connectedToServer) {
                    Object[] request;
                    try {
                        //Which user account should be accessed? And what is requested?
                        request = serverAccountService.get(new FormalField(String.class), new FormalField(String.class));
                        System.out.println(request);
                        String username = request[0].toString();
                        String requestStr = request[1].toString();

                        System.out.println(AccountService.class.getName() + ": " + requestStr + " for " + username + " received...");

                        //Does the system contain the user?
                        if (accountsMap.containsKey(username)) {
                            System.out.println(AccountService.class.getName() + ": Account service: Credentials verified");
                            accountServiceServer.put(username, OK);
                            requestDecider(requestStr, username, accountsMap);

                        } else {
                            System.out.println(AccountService.class.getName() + ": No such user exists");
                            accountServiceServer.put(username, KO);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        connectedToServer = false;
                    }
                }
            }
        }
    }

    public void requestDecider(String request, String username, HashMap<String, HashMap> accounts) throws Exception {
        switch (request) {
            case QUERY_STOCKS -> {
                queryUserStocks(accounts, username);
            }
            case DELETE_STOCKS -> System.out.println(AccountService.class.getName() + ": to be implemented!");
            case INSERT_STOCKS -> System.out.println(AccountService.class.getName() + ": to be implemented!");
            default -> {
                System.out.println(AccountService.class.getName() + ": ERROR IN SWITCH STMT");
                throw new Exception(AccountService.class.getName() + ": NOT IMPLEMENTED!");
            }
        }
    }

    public void queryUserStocks(HashMap<String, HashMap> accounts, String username) throws Exception {
        System.out.println(AccountService.class.getName() + ": Retrieving stocks for user: " + username + "...");
        ArrayList<Stock> stocks = returnListOfUserStocks(accounts, username);
        System.out.println(AccountService.class.getName() + ": Sending stocks to server...");
        for (Stock stock : stocks) {
            accountServiceServer.put(username, MORE_DATA);
            accountServiceServer.put(username, stock);
        }
        accountServiceServer.put(username, NO_MORE_DATA);
    }

    public ArrayList<Stock> returnListOfUserStocks(HashMap<String, HashMap> accounts, String username) throws InterruptedException {
        ArrayList<Stock> stocks = new ArrayList<>();
        Map<String, Stock> map = accounts.get(username);

        for (Map.Entry<String, Stock> entry : map.entrySet()) {
            stocks.add(entry.getValue());
        }
        return stocks;
    }

    public void insertStocks() {
        System.out.println("TO BE IMPLEMENTED");
    }

    public void removeStocks() {
        System.out.println("TO BE IMPLEMENTED");
    }

    static HashMap instantiateTestData() {
        //todo udskift map med space
        HashMap<String, HashMap> accountsMap = new HashMap<>();

        User alice = new User("Alice", UUID.randomUUID());
        User bob = new User("Bob", UUID.randomUUID());
        User charlie = new User("Charlie", UUID.randomUUID());

        Account aliceAccount = new Account(100);

        HashMap<String, Stock> aliceStockMap1 = new HashMap<>();
        HashMap<String, Stock> bobStockMap2 = new HashMap<>();
        HashMap<String, Stock> charlieStockMap3 = new HashMap<>();

        aliceStockMap1.put(TESLA, new Stock(TESLA, 20));
        aliceStockMap1.put(APPLE, new Stock(APPLE, 31));
        accountsMap.put(alice.getName(), aliceStockMap1);

        bobStockMap2.put(MICROSOFT, new Stock(MICROSOFT, 20));
        accountsMap.put(bob.getName(), bobStockMap2);

        charlieStockMap3.put(GOOGLE, new Stock(GOOGLE, 31));
        accountsMap.put(charlie.getName(), charlieStockMap3);

        return accountsMap;
    }
}
