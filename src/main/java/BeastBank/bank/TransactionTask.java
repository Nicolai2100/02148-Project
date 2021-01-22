package BeastBank.bank;

import BeastBank.broker.Transaction;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.concurrent.Callable;

import static BeastBank.shared.Requests.OK;
import static BeastBank.shared.Requests.TRANSACTION;

public class TransactionTask implements Callable<String> {
    private final String serverStr = TransactionTask.class.getName() + ": ";

    @Override
    public String call() {
        while (true) {
            try {
                Transaction t = new Transaction(Server.brokerServer.get(
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(Integer.class),
                        new FormalField(Integer.class)));

                System.out.println(serverStr + t.getBuyer() + " buying from " + t.getSeller());

                makeTransaction(t.getBuyer(), t.getSeller(), t.getStockName(), t.getQuantity(), t.getPrice());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void makeTransaction(String buyer, String seller, String stockName, int amount, int pricePerStock) throws InterruptedException {
        Server.serverAccountService.put(seller, TRANSACTION);
        Server.serverAccountService.put(seller, buyer, stockName, pricePerStock, amount);

        var response = Server.accountServiceServer.get(new ActualField(seller), new ActualField(buyer), new FormalField(String.class));

        if (response[2].toString().equals(OK)) {

            System.out.println(serverStr + "Transaction status - " + response[2]);

            String msgToSeller = String.format("Hello %s. We are happy to inform you that %s stocks was sold successfully in the amount of %d.",
                    seller, stockName, amount);

            String msgToBuyer = String.format("Hello %s. We are happy to inform you that %s stocks was bought successfully in the amount of %d.",
                    buyer, stockName, amount);

            Server.serverClientMessages.put(seller, msgToSeller);
            Server.serverClientMessages.put(buyer, msgToBuyer);
        } else {
            System.out.println(serverStr + "Transaction status - failed...");
        }
    }
}
