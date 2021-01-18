package server;

import BeastBank.bank.TransactionTask;
import BeastBank.broker.Transaction;
import BeastBank.bank.Program;
import BeastBank.broker.Broker;
import org.junit.Before;
import org.junit.Test;
import BeastBank.service.AccountServiceMain;
import BeastBank.service.IdentityProvider;

import static BeastBank.shared.StockNames.TESLA;

public class TransactionTaskTest {

    @Before
    public void setUp() throws Exception {

        Runnable r1 = () -> Program.main(null);
        Thread thread1 = new Thread(r1);
        thread1.start();

        Runnable r2 = () -> IdentityProvider.main(null);
        Thread thread2 = new Thread(r2);
        thread2.start();

        Runnable r3 = () -> AccountServiceMain.main(null);
        Thread thread3 = new Thread(r3);
        thread3.start();
    }

    @Test
    public void call() throws Exception {
        // setUp();
        Runnable r4 = () -> {
            try {
                Broker.main(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread4 = new Thread(r4);
        thread4.start();

        Transaction transaction = new Transaction("Alice", "Bob", TESLA, 20, 1);

        Broker broker = new Broker();
        broker.startTransaction(transaction);

        TransactionTask transactionTask = new TransactionTask();
        transactionTask.call();

    }
}