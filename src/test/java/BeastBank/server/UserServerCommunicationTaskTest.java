package BeastBank.server;

import BeastBank.bank.Program;
import BeastBank.bank.UserServerCommunicationTask;
import BeastBank.broker.Broker;
import org.jspace.SequentialSpace;
import org.junit.Before;
import org.junit.Test;

import BeastBank.service.AccountServiceMain;
import BeastBank.service.IdentityProvider;

import static BeastBank.shared.Requests.*;

public class UserServerCommunicationTaskTest {


    @Before
    public  void setUp() throws InterruptedException {
        Runnable r4 = () -> {
            try {
                Broker.main(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread4 = new Thread(r4);
        thread4.start();

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
    public void call() throws InterruptedException {
        setUp();

        UserServerCommunicationTask uscom = new UserServerCommunicationTask(
                new SequentialSpace(),
                new SequentialSpace(),
                "Alice");
        Thread.sleep(2000);
        uscom.requestResolver(TRANSACTION);


    }
}