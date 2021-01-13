package server;

import broker.Transaction;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.concurrent.Callable;

import static shared.Requests.MAKE_TRANSACTION;

public class TransactionTask implements Callable<String> {

    @Override
    public String call() throws Exception {
        System.out.println("started");
        while (true) {
            try {
                Transaction t = new Transaction(Server.brokerServer.get(
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(Integer.class),
                        new FormalField(Integer.class)
                ));
                System.out.println(t.getBuyer());
                System.out.println(t.getSeller());
                System.out.println(t.getStockName());
                System.out.println(t.getPrice());
                System.out.println(t.getQuantity());

                makeTransaction(t.getBuyer(), t.getSeller(), t.getStockName(), t.getQuantity(), t.getPrice());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // return TransactionTask.class.getName() + " ended.";
    }

    private void makeTransaction(String buyer, String seller, String stockName, int amount, double pricePerStock) throws InterruptedException {
        System.out.println("USCom: Starting transaction...");
        Server.serverAccountService.put(buyer, MAKE_TRANSACTION);

        //sender, receiver name, stockname, amount, price per stock
        var response = Server.accountServiceServer.get(
                new ActualField(buyer),
                new FormalField(String.class));

        System.out.println("USCom: Transaction status - " + response[1]);
    }
}
