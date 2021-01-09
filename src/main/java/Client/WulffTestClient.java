package Client;

import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

public class WulffTestClient {

    String brokerHostname = "localhost";
    int brokerPort = 9001;

    String clientID;

    RemoteSpace marketOrders;

    public WulffTestClient() throws IOException {
        marketOrders = new RemoteSpace("tcp://" + brokerHostname + ":" + brokerPort + "/trades?keep");
    }

    public static void main(String[] args) {

    }

    void start() throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        clientID = scanner.nextLine();
        while(true) {
            String[] args = scanner.nextLine().toUpperCase().split(" ");
            System.out.printf("%s: Placed order to %s %s shares of %s.", clientID, args[0], args[2], args[1]);
            marketOrders.put(clientID, args[0], args[1], Integer.parseInt(args[2]));
        }
    }
}
