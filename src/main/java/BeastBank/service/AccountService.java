package BeastBank.service;

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
    private boolean connectedToServer = false;
    private RemoteSpace serverAccountService = null;
    private RemoteSpace accountServiceServer = null;
    private String serviceStr = AccountService.class.getName() + ": ";

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
                    System.out.println(serviceStr + "Trying to establish connection to remote spaces...");
                    String serverService = String.format("tcp://%s:%d/%s?%s", SERVER_HOSTNAME, SERVER_PORT, SERVER_ACCOUNT_SERVICE, CONNECTION_TYPE);
                    String serviceServer = String.format("tcp://%s:%d/%s?%s", SERVER_HOSTNAME, SERVER_PORT, ACCOUNT_SERVICE_SERVER, CONNECTION_TYPE);
                    serverAccountService = new RemoteSpace(serverService);
                    accountServiceServer = new RemoteSpace(serviceServer);
                    connectedToServer = true;

                    System.out.println(serviceStr + "Waiting for requests...");

                } catch (IOException e) {
                    System.out.println(serviceStr + e.getMessage());
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

                        System.out.println(serviceStr + requestStr + " for " + username + " received...");

                        requestDecider(requestStr, username);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        connectedToServer = false;
                    }
                }
            }
        }
    }

    public void requestDecider(String request, String username) throws Exception {
        switch (request) {
            case QUERY_STOCKS:
                queryAccountRequest(username);
                break;
            case TRANSACTION:
                transactionRequest(username);
                break;
            default: {
                System.out.println(serviceStr + "ERROR IN SWITCH STMT");
                throw new Exception(serviceStr + "NOT IMPLEMENTED!");
            }
        }
    }

    public boolean makeTransaction(String stockName, int amount, User seller, User buyer, double pricePerStock) {
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

                System.out.println(serviceStr + "Transaction finished...");
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void transactionRequest(String username) throws InterruptedException {
        Optional<User> optionalUser = selectOptionalUser(username);

        var t = serverAccountService.get(
                new ActualField(username), //seller
                new FormalField(String.class), //buyer
                new FormalField(String.class),
                new FormalField(Integer.class),
                new FormalField(Integer.class));

        String sellerName = t[0].toString();
        String buyerName = t[1].toString();
        String stockName = t[2].toString();
        int price = Integer.parseInt(t[3].toString());
        int amount = Integer.parseInt(t[4].toString());

        User seller = null, buyer = null;
        try {
            seller = FakeUserDataAccessService.getInstance().selectUserByUsername(sellerName).get();
            buyer = FakeUserDataAccessService.getInstance().selectUserByUsername(buyerName).get();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }

        boolean transactionOk = makeTransaction(
                stockName,
                amount,
                seller,
                buyer,
                price);

        if (transactionOk) accountServiceServer.put(sellerName, buyerName, OK);
        else accountServiceServer.put(sellerName, buyerName, KO);
    }

    public void queryAccountRequest(String username) throws Exception {
        Optional<User> optionalUser = selectOptionalUser(username);
        if (optionalUser.isEmpty()) {
            accountServiceServer.put(username, KO);
        } else {
            accountServiceServer.put(username, OK);
            User user = optionalUser.get();

            System.out.println(serviceStr + "Retrieving stocks for user: " + user.getName() + "...");
            ArrayList<Stock> stocks = returnListOfUserStocks(user);
            System.out.println(serviceStr + "Sending stocks to BeastBank.server...");

            accountServiceServer.put(user.getName(), user.getAccount().getBalance());

            for (Stock stock : stocks) {
                accountServiceServer.put(user.getName(), MORE_DATA);
                accountServiceServer.put(user.getName(), stock);
            }
            accountServiceServer.put(user.getName(), NO_MORE_DATA);
        }
    }

    public ArrayList<Stock> returnListOfUserStocks(User user) {
        ArrayList<Stock> stocks = new ArrayList<>();
        Map<String, Stock> map = user.getAccount().getStocks();

        for (Map.Entry<String, Stock> entry : map.entrySet()) {
            stocks.add(entry.getValue());
        }
        return stocks;
    }

    private Optional<User> selectOptionalUser(String username) {
        //Does the system contain the user?
        Optional<User> optionalUser = FakeUserDataAccessService.getInstance().selectUserByUsername(username);
        if (optionalUser.isPresent()) {
            System.out.println(serviceStr + "User exist, continuing to process request...");
        } else {
            System.out.println(serviceStr + "No such user exists...");
        }
        return optionalUser;
    }
}
