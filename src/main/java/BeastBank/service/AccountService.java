package BeastBank.service;

import BeastBank.broker.Transaction;
import BeastBank.dao.FakeUserDataAccessService;
import BeastBank.model.Stock;
import BeastBank.model.User;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.*;

import static BeastBank.shared.Requests.*;
import static BeastBank.shared.Channels.*;


public class AccountService {
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

    private void requestHandler() throws Exception {
        while (true) {

            if (!connectedToServer) {
                // connect to tuple space
                try {
                    System.out.println(AccountService.class.getName() + ": Trying to establish connection to remote spaces...");
                    String serverService = String.format("tcp://%s:%d/%s?%s", SERVER_HOSTNAME, SERVER_PORT, SERVER_ACCOUNT_SERVICE, CONNECTION_TYPE);
                    String serviceServer = String.format("tcp://%s:%d/%s?%s", SERVER_HOSTNAME, SERVER_PORT, ACCOUNT_SERVICE_SERVER, CONNECTION_TYPE);
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

                        System.out.println(AccountService.class.getName() + ": " + requestStr + " for " + username + " received...");

                        //Does the system contain the user?
                        Optional<User> optionalUser = FakeUserDataAccessService.getInstance().selectUserByUsername(username);
                        if (optionalUser.isPresent()) {
                            System.out.println(AccountService.class.getName() + ": Account BeastProject.service: Credentials verified");
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

    public void requestDecider(String request, User user) throws Exception {
        switch (request) {
            case QUERY_STOCKS:
                queryAccountRequest(user);
                break;
            case TRANSACTION:
                transactionRequest(user);
                break;
            default: {
                System.out.println(AccountService.class.getName() + ": ERROR IN SWITCH STMT");
                throw new Exception(AccountService.class.getName() + ": NOT IMPLEMENTED!");
            }
        }
    }

    public void makeTransaction(String stockName, int amount, User seller, User buyer, double pricePerStock) {
        Stock stock = null;
        try {
            stock = seller.getAccount().withDrawStock(stockName, amount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (stock != null) {
            try {
                buyer.getAccount().insertStock(stock);

                double payment = buyer.getAccount().makePayment(pricePerStock, amount);
                FakeUserDataAccessService.getInstance().update(buyer.getId(), buyer);

                seller.getAccount().receivePayment(payment);
                FakeUserDataAccessService.getInstance().update(seller.getId(), seller);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void transactionRequest(User user) throws InterruptedException {
        //sender name, receiver name, stock name, price per stock, amount
        Transaction transaction = new Transaction(serverAccountService.get(
                new ActualField(user.getName()),
                new FormalField(String.class),
                new FormalField(String.class),
                //new FormalField(Double.class),
                new FormalField(Integer.class),
                new FormalField(Integer.class)
        ));

        User seller = null, buyer = null;
        try {
            seller = FakeUserDataAccessService.getInstance().selectUserByUsername(transaction.getSeller()).get();
            buyer = FakeUserDataAccessService.getInstance().selectUserByUsername(transaction.getBuyer()).get();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }

        makeTransaction(transaction.getStockName(),
                transaction.getQuantity(),
                seller,
                buyer,
                transaction.getPrice());
    }

    public void queryAccountRequest(User user) throws Exception {
        System.out.println(AccountService.class.getName() + ": Retrieving stocks for user: " + user.getName() + "...");
        ArrayList<Stock> stocks = returnListOfUserStocks(user);
        System.out.println(AccountService.class.getName() + ": Sending stocks to BeastBank.server...");

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
}
