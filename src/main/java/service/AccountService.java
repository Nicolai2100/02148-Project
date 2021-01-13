package service;

import dao.FakeUserDataAccessService;
import model.Stock;
import model.User;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.*;

import static shared.Requests.*;
import static shared.Channels.*;
import static shared.StockNames.*;


public class AccountService {
    //static boolean serviceRunning = true;
    boolean connectedToServer = false;
    RemoteSpace serverAccountService = null;
    RemoteSpace accountServiceServer = null;

    public AccountService() {
    }

    public void startService(String[] args) {
        try {
            requestHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //todo NJL transactions() {


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

                    System.out.println(AccountService.class.getName() + ": Waiting for requests...");

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

                        String username = request[0].toString();
                        String requestStr = request[1].toString();

                        System.out.println(requestStr);

                        System.out.println(AccountService.class.getName() + ": " + requestStr + " for " + username + " received...");

                        //Does the system contain the user?
                        Optional<User> optionalUser = FakeUserDataAccessService.getInstance().selectUserByUsername(username);
                        if (optionalUser.isPresent()) {
                            System.out.println(AccountService.class.getName() + ": Account service: Credentials verified");
                            accountServiceServer.put(username, OK);
                            requestDecider(requestStr, optionalUser.get());

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

    //The buyer has to send the money
    public void makeTransaction(String stockName, int amount, User sender, User receiver, double pricePerStock) {
        Stock stock = null;

        try {
            stock = sender.getAccount().withDrawStock(stockName, amount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (stock != null) {
            try {
                receiver.getAccount().insertStock(stock);
                sender.getAccount().receivePayment(pricePerStock * amount);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void transactionRequest(User user) throws InterruptedException {
        //sender, receiver name, stockname, amount, price per stock
        var response = serverAccountService.get(
                new ActualField(user.getName()),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(Integer.class)
                , new FormalField(Double.class));

        System.out.println(response[0].toString() + response[1].toString() + response[2].toString());

        makeTransaction(TESLA, 1, user,
                FakeUserDataAccessService.getInstance().selectUserByUsername("Bob").get(), 22.2);

    }

    public void requestDecider(String request, User user) throws Exception {
        switch (request) {
            case QUERY_STOCKS -> {
                queryAccountRequest(user);
            }
            case MAKE_TRANSACTION -> transactionRequest(user);

            case DELETE_STOCKS -> System.out.println(AccountService.class.getName() + ": to be implemented!");
            case INSERT_STOCKS -> System.out.println(AccountService.class.getName() + ": to be implemented!");
            default -> {
                System.out.println(AccountService.class.getName() + ": ERROR IN SWITCH STMT");
                throw new Exception(AccountService.class.getName() + ": NOT IMPLEMENTED!");
            }
        }
    }

    public void queryAccountRequest(User user) throws Exception {
        System.out.println(AccountService.class.getName() + ": Retrieving stocks for user: " + user.getName() + "...");
        ArrayList<Stock> stocks = returnListOfUserStocks(user);
        System.out.println(AccountService.class.getName() + ": Sending stocks to server...");

        accountServiceServer.put(user.getName(), user.getAccount().getBalance());

        for (Stock stock : stocks) {
            accountServiceServer.put(user.getName(), MORE_DATA);
            accountServiceServer.put(user.getName(), stock);
        }
        accountServiceServer.put(user.getName(), NO_MORE_DATA);
    }

    public ArrayList<Stock> returnListOfUserStocks(User user) throws InterruptedException {
        ArrayList<Stock> stocks = new ArrayList<>();
        Map<String, Stock> map = user.getAccount().getStocks();

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

}
