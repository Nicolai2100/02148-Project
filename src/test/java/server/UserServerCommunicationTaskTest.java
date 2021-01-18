package server;

import BeastProject.bank.Program;
import BeastProject.broker.Broker;
import org.jspace.SequentialSpace;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import BeastProject.service.AccountServiceMain;
import BeastProject.service.IdentityProvider;

import static BeastProject.shared.Requests.*;

public class UserServerCommunicationTaskTest {


    @BeforeEach
    void setUp() throws InterruptedException {
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