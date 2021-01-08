package Broker;

import org.jspace.*;

import java.time.LocalDateTime;

import static model.Requests.*;

//The main responsibility of the Server-class is to provide a SpaceRepository and channels
//which connect the client and the services
public class Server {
    static SequentialSpace clientServer;
    static SequentialSpace serverClient;
    static SequentialSpace serverIdProvider;
    static SequentialSpace idProviderServer;

    public static void main(String[] args) throws InterruptedException {
        SpaceRepository repository = new SpaceRepository();

        // Create a local space for each channel
        clientServer = new QueueSpace();
        serverClient = new QueueSpace();
        serverIdProvider = new QueueSpace();
        idProviderServer = new QueueSpace();

        // Add the spaces/channels to the repository
        repository.add("clientServer", clientServer);
        repository.add("serverClient", serverClient);
        repository.add("serverIdProvider", serverIdProvider);
        repository.add("idProviderServer", idProviderServer);

        // Open a gate
        repository.addGate("tcp://localhost:123/?keep");

        // Keep reading chat messages and printing them
        System.out.println("Running host on port 123");

        while (2 + 2 < 5) {
            Object[] requestT = clientServer.get(new FormalField(String.class));
            String request = requestT[0].toString();

            requestResolver(request);

        }
    }

    static void requestResolver(String request) {
        System.out.println("Client requested: " + request);
        try {

            switch (request) {
                case LOGIN -> {
                    Object[] t = clientServer.get(new FormalField(String.class), new FormalField(String.class));
                    String username = t[0].toString();
                    String password = t[1].toString();
                    login(username, password);

                }
                case QUERY_STOCKS -> System.out.println("to be implemented");
                default -> System.out.println("ERROR IN SWITCH STMT");
            }

        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    static boolean login(String user, String password) {
        Object[] response = null;
        try {
            System.out.println("Logging " + user + " in...");
            serverIdProvider.put(user, password);
            response = idProviderServer.get(new FormalField(String.class));

            if (response[0].equals(OK)) {
                System.out.println(user + " logged in at " + LocalDateTime.now());
                serverClient.put(OK);
            } else {
                System.out.println("Error in credentials");
                serverClient.put(KO);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}
