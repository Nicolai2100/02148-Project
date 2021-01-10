package Service;

import model.StockInfo;
import model.User;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static model.Requests.*;
import static model.StockNames.*;
import static model.Channels.*;


public class AccountService {
    //static boolean serviceRunning = true;
    static boolean connectedToServer = false;
    static RemoteSpace serverAccountService = null;
    static RemoteSpace accountServiceServer = null;
    static HashMap<String, HashMap> accountsMap;

    public static void main(String[] args) {
        //todo få servicen til at blive robust overfor server nedbrud
        while (true) {

            try {
                mainLoop();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    mainLoop();
                } catch (Exception exception) {
                    exception.printStackTrace();

                }
            }
        }
    }

    private static void mainLoop() throws Exception {
        accountsMap = instantiateTestData();

        while (true) {

            if (!connectedToServer) {
                // connect to tuple space
                try {
                    System.out.println("Trying to establish connection to remote spaces...");
                    serverAccountService = new RemoteSpace("tcp://localhost:123/serverAccountService?keep");
                    accountServiceServer = new RemoteSpace("tcp://localhost:123/accountServiceServer?keep");
                    connectedToServer = true;

                    System.out.printf("Established connection to remote spaces:\n%s and \n%s at " + LocalDateTime.now(),
                            serverAccountService.getUri(),
                            accountServiceServer.getUri());
                    System.out.println("\n\nWaiting for requests...");

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    connectedToServer = false;
                }

            } else if (connectedToServer) {

                while (AccountService.connectedToServer) {
                    Object[] request;
                    try {
                        //Which user account should be accessed? And what is requested?
                        request = serverAccountService.get(new FormalField(String.class), new FormalField(String.class));
                        System.out.println(request);
                        String username = request[0].toString();
                        String requestStr = request[1].toString();

                        System.out.println(requestStr + " for " + username + " received...");

                        //Does the system contain the user?
                        if (accountsMap.containsKey(username)) {
                            System.out.println("Credentials verified");
                            accountServiceServer.put(username, OK);
                            requestDecider(requestStr, username, accountsMap);

                        } else {
                            System.out.println("No such user exists");
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

    public static void requestDecider(String request, String username, HashMap<String, HashMap> accounts) throws Exception {
        switch (request) {
            case QUERY_STOCKS -> {
                queryUserStocks(accounts, username);
            }
            case DELETE_STOCKS -> System.out.println("to be implemented!");
            case INSERT_STOCKS -> System.out.println("to be implemented!");
            default -> {
                System.out.println("ERROR IN SWITCH STMT");
                throw new Exception("NOT IMPLEMENTED!");
            }
        }
    }

    public static void queryUserStocks(HashMap<String, HashMap> accounts, String username) throws Exception {
        System.out.println("Retrieving stocks for user: " + username + "...");
        ArrayList<StockInfo> stocks = returnListOfUserStocks(accounts, username);
        System.out.println("Sending stocks to server...");
        for (StockInfo stock : stocks) {
            accountServiceServer.put(username, MORE_DATA);
            accountServiceServer.put(username, stock.getName(), stock.getPrice());
        }
        accountServiceServer.put(username, NO_MORE_DATA);

    }

    public static ArrayList<StockInfo> returnListOfUserStocks(HashMap<String, HashMap> accounts, String username) throws InterruptedException {
        ArrayList<StockInfo> stockInfos = new ArrayList<>();
        Map<String, StockInfo> map = accounts.get(username);

        for (Map.Entry<String, StockInfo> entry : map.entrySet()) {
            stockInfos.add(entry.getValue());
        }
        return stockInfos;
    }

    public void insertStocks() {
        System.out.println("TO BE IMPLEMENTED");
    }

    public void removeStocks() {
        System.out.println("TO BE IMPLEMENTED");
    }

    static HashMap instantiateTestData() {
        HashMap<String, HashMap> accountsMap = new HashMap<>();

        User alice = new User("Alice", UUID.randomUUID());
        User bob = new User("Bob", UUID.randomUUID());
        User charlie = new User("Charlie", UUID.randomUUID());

        HashMap<String, StockInfo> aliceStockMap1 = new HashMap<>();
        HashMap<String, StockInfo> bobStockMap2 = new HashMap<>();
        HashMap<String, StockInfo> charlieStockMap3 = new HashMap<>();

        aliceStockMap1.put(TESLA, new StockInfo(TESLA, 20));
        aliceStockMap1.put(APPLE, new StockInfo(APPLE, 31));
        accountsMap.put(alice.getName(), aliceStockMap1);

        bobStockMap2.put(MICROSOFT, new StockInfo(MICROSOFT, 20));
        accountsMap.put(bob.getName(), bobStockMap2);

        charlieStockMap3.put(GOOGLE, new StockInfo(GOOGLE, 31));
        accountsMap.put(charlie.getName(), charlieStockMap3);

        return accountsMap;
    }
}
