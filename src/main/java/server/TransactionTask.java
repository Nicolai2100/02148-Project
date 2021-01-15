package server;

import Broker.Transaction;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.concurrent.Callable;

import static shared.Requests.TRANSACTION;

public class TransactionTask implements Callable<String> {

    @Override
    public String call() {
        while (true) {
            try {
                Transaction t = new Transaction(Server.brokerServer.get(
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(Double.class),
                        new FormalField(Integer.class)));

                System.out.println("TransactionTask: " + t.getBuyer()  + " buys from " + t.getSeller());
                makeTransaction(t.getBuyer(), t.getSeller(), t.getStockName(), t.getQuantity(), t.getPrice());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void makeTransaction(String buyer, String seller, String stockName, int amount, double pricePerStock) throws InterruptedException {

        Server.serverAccountService.put(seller, TRANSACTION);
        Server.serverAccountService.put(seller, buyer, stockName, pricePerStock, amount);

        var response = Server.accountServiceServer.get(new ActualField(seller), new FormalField(String.class));
        System.out.println("TransactionTask: Transaction status - " + response[1]);
    }
}
