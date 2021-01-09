package Client;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WulffTestClient {

    String brokerHostname = "localhost";
    int brokerPort = 9001;
    RemoteSpace marketOrders;

    String clientID;

    ExecutorService executor = Executors.newCachedThreadPool();

    public WulffTestClient() throws IOException {
        marketOrders = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/marketOrders?keep");
    }

    public static void main(String[] args) throws Exception {
        new WulffTestClient().startClient();
    }

    void startClient() {
        executor.execute(consoleApplication);
    }

    Runnable consoleApplication = () -> {
        Scanner scanner = new Scanner(System.in);
        clientID = scanner.nextLine();
        executor.execute(new BrokerMessageListener());
        while (true) {
            String[] args = scanner.nextLine().toUpperCase().split(" ");
            System.out.printf("%s: Placed order to %s %s shares of %s. %n", clientID, args[0], args[2], args[1]);
            try {
                marketOrders.put(clientID, args[0], args[1], Integer.parseInt(args[2]));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    class BrokerMessageListener implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Object[] res = marketOrders.get(
                            new ActualField(clientID),
                            new ActualField("MSG"),
                            new FormalField(String.class)
                    );
                    System.out.println("Broker: " + res[2]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /*
    Runnable brokerMsgListener = () -> {
        while (true) {
            try {
                Object[] res = marketOrders.get(
                        new ActualField(clientID),
                        new ActualField("MSG"),
                        new FormalField(String.class)
                );
                System.out.println("Broker: " + res[2]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };*/
}
