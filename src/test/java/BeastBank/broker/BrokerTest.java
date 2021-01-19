package BeastBank.broker;

import BeastBank.bank.ClientInputException;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import BeastBank.shared.Channels;
import BeastBank.shared.Requests;
import org.junit.Test;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import BeastBank.broker.Broker;
import BeastBank.service.AccountServiceMain;
import BeastBank.service.IdentityProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

class BrokerTest {

    RemoteSpace orders;
    RemoteSpace orderPkgs;
    RemoteSpace transactions;
    RemoteSpace stocks;
    String brokerHostname = Channels.BROKER_HOSTNAME;
    int brokerPort = Channels.BROKER_PORT;
    ExecutorService executor;
    int timeout = 4;
    TimeUnit timoutUnit = TimeUnit.SECONDS;

    static final String alice = "ALICE";
    static final String bob = "BOB";
    static final String charlie = "CHARLIE";
    static final String apple = "AAPL";
    static final String tesla = "TESLA";
    static final String vestas = "VESTAS";

    Callable<Object[]> getDoneTask = () -> {
        Object[] res = transactions.get(new FormalField(List.class));
        return res;
    };

    @Before
    public void setup() throws InterruptedException, IOException {
        Broker.main(new String[]{});
        orders = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/" + Requests.ORDERS + "?keep");
        orderPkgs = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/" + Requests.ORDER_PACKAGES + "?keep");
        transactions = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/" + Requests.TRANSACTIONS + "?keep");
        stocks = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/" + Requests.STOCKS + "?keep");
        executor = Executors.newFixedThreadPool(1);
    }

    @After
    public void finish() throws InterruptedException {
        //orders.get(new ActualField("DONE!"));
    }

    private void printRes(List transactions) {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("An order package was completed by other orders.");
            return;
        }
        System.out.println("An order package was completed with these transactions: ");
        for (Object t : transactions) {
            System.out.println(t.toString());
        }
    }

    @Test
    public void test1() throws Exception {
        OrderPackage alicepkg = new OrderPackage();
        alicepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(alice).stock(apple).quantity(10).build());
        alicepkg.addOrder(new Order.OrderBuilder().buy().orderedBy(alice).stock(tesla).quantity(5).build());

        OrderPackage bobpkg = new OrderPackage();
        bobpkg.addOrder(new Order.OrderBuilder().buy().orderedBy(bob).stock(apple).quantity(10).build());

        OrderPackage charliepkg = new OrderPackage();
        charliepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(charlie).stock(tesla).quantity(5).build());

        orderPkgs.put(alicepkg);
        orderPkgs.put(charliepkg);
        orderPkgs.put(bobpkg);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res2 = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res3 = (ArrayList) executor.submit(getDoneTask).get()[0];
        printRes(res);
        printRes(res2);
        printRes(res3);
    }

    @Test
    public void test2() throws Exception {
        OrderPackage alicepkg = new OrderPackage();
        alicepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(alice).stock(apple).quantity(10).build());
        alicepkg.addOrder(new Order.OrderBuilder().buy().orderedBy(alice).stock(tesla).quantity(5).build());

        OrderPackage bobpkg = new OrderPackage();
        bobpkg.addOrder(new Order.OrderBuilder().buy().orderedBy(bob).stock(apple).quantity(10).build());
        bobpkg.addOrder(new Order.OrderBuilder().sell().orderedBy(bob).stock(vestas).quantity(8).minQuantity(5).build());

        OrderPackage charliepkg = new OrderPackage();
        charliepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(charlie).stock(tesla).quantity(5).build());
        charliepkg.addOrder(new Order.OrderBuilder().buy().orderedBy(charlie).stock(vestas).quantity(5).build());

        orderPkgs.put(alicepkg);
        orderPkgs.put(charliepkg);
        orderPkgs.put(bobpkg);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res2 = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res3 = (ArrayList) executor.submit(getDoneTask).get()[0];
        printRes(res);
        printRes(res2);
        printRes(res3);
    }

    @Test
    public void test3() throws Exception {
        OrderPackage alicepkg = new OrderPackage();
        alicepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(alice).stock(apple).quantity(10).build());
        alicepkg.addOrder(new Order.OrderBuilder().buy().orderedBy(alice).stock(tesla).quantity(5).build());

        OrderPackage bobpkg = new OrderPackage();
        bobpkg.addOrder(new Order.OrderBuilder().buy().orderedBy(bob).stock(apple).quantity(10).build());
        bobpkg.addOrder(new Order.OrderBuilder().sell().orderedBy(bob).stock(vestas).quantity(8).minQuantity(5).build());
        bobpkg.addOrder(new Order.OrderBuilder().buy().orderedBy(bob).stock("DTU").quantity(10).build());

        OrderPackage charliepkg = new OrderPackage();
        charliepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(charlie).stock(tesla).quantity(5).build());
        charliepkg.addOrder(new Order.OrderBuilder().buy().orderedBy(charlie).stock(vestas).quantity(5).build());

        orderPkgs.put(alicepkg);
        orderPkgs.put(charliepkg);
        orderPkgs.put(bobpkg);
        //Bør give et resultat

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res2 = (ArrayList) executor.submit(getDoneTask).get()[0];
        printRes(res);
        printRes(res2);

        throw new ClientInputException("UDKOMMENTERET!");

        /* assertThrows(TimeoutException.class, () -> {
            ArrayList res3 = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[0];
        });*/
    }

    @Test
    public void test4() throws Exception {
        OrderPackage alicepkg = new OrderPackage();
        alicepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(alice).quantity(10).stock(apple).clientMatch(bob).build());

        OrderPackage bobpkg = new OrderPackage();
        bobpkg.addOrder(new Order.OrderBuilder().buy().orderedBy(bob).quantity(10).stock(apple).clientMatch(alice).build());

        orderPkgs.put(alicepkg);
        orderPkgs.put(bobpkg);

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get()[0];
        printRes(res);
    }

    @Test
    public void test5() throws Exception {
        OrderPackage alicepkg = new OrderPackage();
        alicepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(alice).quantity(10).stock(apple).clientMatch(bob).build());

        OrderPackage bobpkg = new OrderPackage();
        bobpkg.addOrder(new Order.OrderBuilder().buy().orderedBy(bob).quantity(10).stock(apple).clientMatch(charlie).build());

        orderPkgs.put(alicepkg);
        orderPkgs.put(bobpkg);

        throw new ClientInputException("UDKOMMENTERET!");

      /*  assertThrows(TimeoutException.class, () -> {
            ArrayList res = (ArrayList) executor.submit(getDoneTask).get(timeout, timoutUnit)[0];
        });*/
    }

    @Test
    public void test6() throws Exception {
        OrderPackage alicepkg = new OrderPackage();
        alicepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(alice).quantity(10).stock(apple).clientMatch(bob).build());

        OrderPackage bobpkg = new OrderPackage();
        bobpkg.addOrder(new Order.OrderBuilder().buy().orderedBy(bob).quantity(10).stock(apple).build());

        orderPkgs.put(alicepkg);
        orderPkgs.put(bobpkg);

        ArrayList res = (ArrayList) executor.submit(getDoneTask).get()[0];
        printRes(res);
    }

    @Test
    public void test7() throws Exception {
        OrderPackage alicepkg = new OrderPackage();
        alicepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(alice).quantity(10).stock(apple).limit(150).build());

        OrderPackage bobpkg = new OrderPackage();
        bobpkg.addOrder(new Order.OrderBuilder().buy().orderedBy(bob).quantity(10).stock(apple).build());

        orderPkgs.put(alicepkg);
        orderPkgs.put(bobpkg);

        throw new ClientInputException("UDKOMMENTERET!");

       /* assertThrows(TimeoutException.class, () -> {
            ArrayList res1 = (ArrayList) executor.submit(getDoneTask).get(4, TimeUnit.SECONDS)[0];
            ArrayList res2 = (ArrayList) executor.submit(getDoneTask).get(4, TimeUnit.SECONDS)[0];
        });*/
    }

    @Test
    public void test8() throws Exception {
        OrderPackage alicepkg = new OrderPackage();
        alicepkg.addOrder(new Order.OrderBuilder().sell().orderedBy(alice).quantity(10).stock(tesla).limit(150).build());

        OrderPackage bobpkg = new OrderPackage();
        bobpkg.addOrder(new Order.OrderBuilder().buy().orderedBy(bob).quantity(10).stock(tesla).build());

        orderPkgs.put(alicepkg);
        orderPkgs.put(bobpkg);

        Thread.sleep(2000);
        stocks.get(new ActualField(tesla), new FormalField(Integer.class));
        stocks.put(tesla, 160);

        ArrayList res1 = (ArrayList) executor.submit(getDoneTask).get()[0];
        ArrayList res2 = (ArrayList) executor.submit(getDoneTask).get()[0];
        printRes(res1);
        printRes(res2);
    }
}