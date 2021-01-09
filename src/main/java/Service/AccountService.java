package Service;

import model.StockInfo;
import model.User;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static model.Requests.*;
import static model.StockNames.*;

public class AccountService {
    static boolean ServerRunning = true;
    static RemoteSpace serverAccountService = null;
    static RemoteSpace accountServiceServer = null;

    public static void main(String[] args) {
        HashMap<String, HashMap> accountsMap = instantiateTestData();

        // connect to tuple space
        try {
            serverAccountService = new RemoteSpace("tcp://localhost:123/serverAccountService?keep");
            accountServiceServer = new RemoteSpace("tcp://localhost:123/accountServiceServer?keep");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("Established connection to " + serverAccountService.getUri() + "\n and " + accountServiceServer.getUri());
        System.out.println("Waiting for requests...");

        while (AccountService.ServerRunning) {
            Object[] request;
            try {
                //Which user account should be accessed? And what is requested?
                request = serverAccountService.get(new FormalField(String.class), new FormalField(String.class));
                System.out.println(request);
                String requestStr = request[0].toString();
                String username = request[1].toString();

                System.out.println(requestStr + " for " + username + " received: ");

                if (accountsMap.containsKey(username)) {
                    System.out.println("Credentials verified");
                    accountServiceServer.put(OK);
                    requestDecider(requestStr, username, accountsMap);

                } else {
                    System.out.println("No such user exists");
                    accountServiceServer.put("ko");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void requestDecider(String request, String username, HashMap<String, HashMap> accounts) {
        /*
        switch (request) {
            case QUERY_STOCKS -> {
                System.out.println("Retrieving account stocks...");
                ArrayList<StockInfo> stocks = queryUserStocks(accounts, username);
                try {

                    for (StockInfo stock : stocks) {
                        accountServiceServer.put(MORE_DATA, stock.getName(), stock.getPrice());
                    }
                    accountServiceServer.put(NO_MORE_DATA);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            case DELETE_STOCKS -> System.out.println("to be implemented!");
            case INSERT_STOCKS -> System.out.println("to be implemented!");
            default -> System.out.println("ERROR IN SWITCH STMT");
        }
         */
    }

    public static ArrayList<StockInfo> queryUserStocks(HashMap<String, HashMap> accounts, String username) {
        ArrayList<StockInfo> stockInfos = new ArrayList<>();
        Map<String, StockInfo> map = accounts.get(username);
        for (Map.Entry<String, StockInfo> entry : map.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
            stockInfos.add(entry.getValue());
        }
        return stockInfos;
    }

    public void insertStocks() {
    }


    public void removeStocks() {
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
