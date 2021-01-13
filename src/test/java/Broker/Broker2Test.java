package Broker;

import org.jspace.ActualField;
import org.jspace.RemoteSpace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class Broker2Test {

    RemoteSpace orders;
    String brokerHostname = "localhost";
    int brokerPort = 9001;
    //ExecutorService executor = Executors.newCachedThreadPool();

    @BeforeEach
    void setup() throws InterruptedException, IOException {
        Broker2.main(new String[]{});
        orders = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/orders?keep");
    }

    @AfterEach
    void finish() throws InterruptedException {
        orders.get(new ActualField("DONE!"));
    }

    @Test
    void test1() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 10, 5);
        orders.put("BOB", "BUY", "AAPL", 10, 5);
        //Bør give et resultat
    }

    @Test
    void test2() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 10, 10);
        //Bør give et resultat
    }

    @Test
    void test3() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 10, 5);
        //Bør give et resultat
    }

    @Test
    void test4() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 10, 5);
        orders.put("BOB", "BUY", "AAPL", 10, 10);
        //Bør give et resultat
    }

    @Test
    void test5() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 5, 5);
        orders.put("BOB", "BUY", "AAPL", 10, 10);
        //Bør IKKE give et resultat
    }

    @Test
    void test6() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 5, 5);
        //Bør IKKE give et resultat
    }

    @Test
    void test7() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 10, 5);
        orders.put("BOB", "BUY", "AAPL", 5, 5);
        //Bør give et resultat
    }

    @Test
    void test8() throws InterruptedException {
        orders.put("ALICE", "SELL", "AAPL", 10, 10);
        orders.put("BOB", "BUY", "AAPL", 3, 3);
        orders.put("CHARLIE", "BUY", "AAPL", 5, 5);
        orders.put("DANIEL", "BUY", "AAPL", 2, 2);
        //Bør give et resultat
    }

    @Test
    void test9() throws InterruptedException {

    }

    @Test
    void test10() throws InterruptedException {

    }

    @Test
    void test11() throws InterruptedException {

    }

    @Test
    void test12() throws InterruptedException {

    }

}