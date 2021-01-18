package client;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import BeastProject.shared.Channels;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WulffTestClient {

    String brokerHostname = Channels.BROKER_HOSTNAME;
    int brokerPort = Channels.BROKER_PORT;
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
            try {
            String[] args = scanner.nextLine().toUpperCase().split(" ");
            if (args.length == 1 && args[0].equals("CLEAR")){
                marketOrders.put("CLEAR");
            }
            if (args.length == 3) {
                System.out.printf("%s: Placed order to %s %s shares of %s. %n", clientID, args[0], args[2], args[1]);
                marketOrders.put(clientID, args[0], args[1], Integer.parseInt(args[2]), 0);
            }
            if (args.length == 4) {
                System.out.printf("%s: Placed order to %s minimum %s, maximum %s shares of %s. %n", clientID, args[0], args[3], args[2], args[1]);
                marketOrders.put(clientID, args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            }
            if (args.length == 5) {
                System.out.printf("%s: Placed order to %s minimum %s, maximum %s shares of %s. %n", args[0], args[1], args[4], args[3], args[2]);
                marketOrders.put(args[0], args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            }
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
    };
}
