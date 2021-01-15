package Broker;

import broker.Broker;
import broker.Order;
import broker.OrderPackage;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class BrokerTest {

    RemoteSpace orders;
    RemoteSpace orderPkgs;
    RemoteSpace transactions;
    String brokerHostname = "localhost";
    int brokerPort = 9001;
    ExecutorService executor;
    int timeout = 4;
    TimeUnit timoutUnit = TimeUnit.SECONDS;

    Callable<Object[]> getDoneTask = () -> {
        Object[] res = transactions.get(new FormalField(List.class));
        return res;
    };

    @BeforeEach
    void setup() throws InterruptedException, IOException {
        Broker.main(new String[]{});
        orders = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/orders?keep");
        orderPkgs = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/orderPackages?keep");
        transactions = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/transactions?keep");
        executor = Executors.newFixedThreadPool(1);
    }

    @AfterEach
    void finish() throws InterruptedException {
        //orders.get(new ActualField("DONE!"));
    }

    private void printRes(List transactions) {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("An order package was completed by other orders.");
            return;
        }
        System.out.println("An order package was completed with these transactions:  ");
        for (Object t : transactions) {
            System.out.println(t.toString());
        }
    }

    @Test
    void test1() throws InterruptedException, ExecutionException {
        OrderPackage alice = new OrderPackage();
        alice.addOrder(new Order("SELL", "ALICE", "AAPL", 10, 10, 0));
        alice.addOrder(new Order("BUY", "ALICE", "TESLA", 5, 5, 0));

        OrderPackage bob = new OrderPackage();
        bob.addOrder(new Order("BUY", "BOB", "AAPL", 10, 10, 0));

        OrderPackage charlie = new OrderPackage();
        charlie.addOrder(new Order("SELL", "CHARLIE", "TESLA", 5, 5, 0));

        orderPkgs.put(alice);
        orderPkgs.put(charlie);
        orderPkgs.put(bob);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res2 = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res3 = (ArrayList) executor.submit(getDoneTask).get()[0];
        //assertEquals(res.size(), 2);
        printRes(res);
        printRes(res2);
        printRes(res3);
    }

    @RepeatedTest(20)
    void test2() throws InterruptedException, ExecutionException {
        OrderPackage alice = new OrderPackage();
        alice.addOrder(new Order("SELL", "ALICE", "AAPL", 10, 10, 0));
        alice.addOrder(new Order("BUY", "ALICE", "TESLA", 5, 5, 0));

        OrderPackage bob = new OrderPackage();
        bob.addOrder(new Order("BUY", "BOB", "AAPL", 10, 10, 0));
        bob.addOrder(new Order("SELL", "BOB", "VESTAS", 8, 5, 0));

        OrderPackage charlie = new OrderPackage();
        charlie.addOrder(new Order("SELL", "CHARLIE", "TESLA", 5, 5, 0));
        charlie.addOrder(new Order("BUY", "CHARLIE", "VESTAS", 5, 5, 0));

        orderPkgs.put(bob);
        Thread.sleep(100);
        orderPkgs.put(charlie);
        Thread.sleep(100);
        orderPkgs.put(alice);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res2 = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res3 = (ArrayList) executor.submit(getDoneTask).get()[0];
        printRes(res);
        printRes(res2);
        printRes(res3);
    }

    @Test
    void test3() throws InterruptedException, ExecutionException {
        OrderPackage alice = new OrderPackage();
        alice.addOrder(new Order("SELL", "ALICE", "AAPL", 10, 10, 0));
        alice.addOrder(new Order("BUY", "ALICE", "TESLA", 5, 5, 0));

        OrderPackage bob = new OrderPackage();
        bob.addOrder(new Order("BUY", "BOB", "AAPL", 10, 10, 0));
        bob.addOrder(new Order("SELL", "BOB", "VESTAS", 8, 5, 0));
        bob.addOrder(new Order("BUY", "BOB", "DTU", 10, 10, 0));

        OrderPackage charlie = new OrderPackage();
        charlie.addOrder(new Order("SELL", "CHARLIE", "TESLA", 5, 5, 0));
        charlie.addOrder(new Order("BUY", "CHARLIE", "VESTAS", 5, 5, 0));

        orderPkgs.put(alice);
        orderPkgs.put(charlie);
        orderPkgs.put(bob);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res2 = (ArrayList) executor.submit(getDoneTask).get()[0];
        printRes(res);
        printRes(res2);

        assertThrows(TimeoutException.class, () -> {
            ArrayList res3 = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[0];
        });
    }
}