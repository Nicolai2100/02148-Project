package Broker;

import com.google.gson.internal.LinkedTreeMap;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class Broker2Test {

    RemoteSpace orders;
    RemoteSpace orderPkgs;
    String brokerHostname = "localhost";
    int brokerPort = 9001;
    ExecutorService executor;
    int timeout = 10; //4
    TimeUnit timoutUnit = TimeUnit.SECONDS;

    Callable<Object[]> getDoneTask = () -> {
        Object[] res = orders.get(new ActualField("DONE!"), new FormalField(List.class));
        return res;
    };

    Callable<Object[]> getDoneTask2 = () -> {
        Object[] res = orderPkgs.get(new ActualField("DONE!"), new FormalField(List.class));
        return res;
    };

    @BeforeEach
    void setup() throws InterruptedException, IOException {
        Broker2.main(new String[]{});
        orders = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/orders?keep");
        orderPkgs = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/orderPackages?keep");
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
    void test1() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 5);
        orders.put("BOB", "BUY", "AAPL", 10, 5);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 1);
        printRes(res);
    }

    @Test
    void test2() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 10, 10);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 1);
        printRes(res);
    }

    @Test
    void test3() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 10, 5);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 1);
        printRes(res);
    }

    @Test
    void test4() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 5);
        orders.put("BOB", "BUY", "AAPL", 10, 10);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 1);
        printRes(res);
    }

    @Test
    void test5() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 5, 5);
        orders.put("BOB", "BUY", "AAPL", 10, 10);
        //Bør IKKE give et resultat

        assertThrows(TimeoutException.class, () -> {
            ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        });
    }

    @Test
    void test6() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 5, 5);
        //Bør IKKE give et resultat

        assertThrows(TimeoutException.class, () -> {
            ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        });
    }

    @Test
    void test7() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 5);
        orders.put("BOB", "BUY", "AAPL", 5, 5);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 1);
        printRes(res);
    }

    @Test
    void test8() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 5, 5);
        orders.put("CHARLIE", "BUY", "AAPL", 5, 5);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 2);
        printRes(res);
    }

    @Test
    void test9() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 5, 5);
        orders.put("CHARLIE", "BUY", "AAPL", 10, 5);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 2);
        printRes(res);
    }

    @Test
    void test10() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 5, 5);
        orders.put("CHARLIE", "BUY", "AAPL", 10, 10);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 1);
        printRes(res);
    }

    @Test
    void test11() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 5);
        orders.put("BOB", "BUY", "AAPL", 5, 5);
        orders.put("CHARLIE", "BUY", "TESLA", 10, 10);
        orders.put("BOB", "SELL", "TESLA", 20, 10);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 1);
        printRes(res);
        res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 1);
        printRes(res);
    }

    @Test
    void test12() throws InterruptedException, TimeoutException, ExecutionException {
        orders.put("ALICE", "SELL", "AAPL", 10, 9);
        orders.put("BOB", "BUY", "AAPL", 1, 1);
        orders.put("CHARLIE", "BUY", "AAPL", 2, 1);
        orders.put("DANIEL", "BUY", "AAPL", 2, 2);
        orders.put("ELLEN", "BUY", "AAPL", 2, 2);
        orders.put("FRANK", "BUY", "AAPL", 1, 2);
        orders.put("GERT", "BUY", "AAPL", 3, 1);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[1];
        assertEquals(res.size(), 6);
        printRes(res);
    }

    @Test
    void test13() throws InterruptedException, TimeoutException, ExecutionException {
        OrderPackage alice = new OrderPackage();
        alice.getOrders().add(new Order("SELL", "ALICE", "AAPL", 10, 10));
        alice.getOrders().add(new Order("BUY", "ALICE", "TESLA", 5, 5));

        OrderPackage bob = new OrderPackage();
        bob.getOrders().add(new Order("BUY", "BOB", "AAPL", 10, 10));

        OrderPackage charlie = new OrderPackage();
        charlie.getOrders().add(new Order("SELL", "CHARLIE", "TESLA", 5, 5));

        orderPkgs.put(alice);
        orderPkgs.put(charlie);
        orderPkgs.put(bob);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask2).get()[1];
        ArrayList res2 = (ArrayList) executor.submit(getDoneTask2).get()[1];
        ArrayList res3 = (ArrayList) executor.submit(getDoneTask2).get()[1];
        //assertEquals(res.size(), 2);
        printRes(res);
        printRes(res2);
        printRes(res3);
    }


}