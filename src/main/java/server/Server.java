package server;

import org.jspace.*;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static shared.Requests.*;

//The main responsibility of the Server-class is to provide a SpaceRepository and channels
//which connect the client and the services
public class Server {
    static SpaceRepository repository = new SpaceRepository();

    static SequentialSpace clientServer;
    static SequentialSpace serverClient;
    static SequentialSpace serverIdProvider;
    static SequentialSpace idProviderServer;
    static SequentialSpace accountServiceServer;
    static SequentialSpace serverAccountService;

    static int numOfClientsConnected = 0;
    static ExecutorService executor;

    public void startServer() throws InterruptedException {
        repository = new SpaceRepository();

        // Create a local space for each channel
        clientServer = new QueueSpace();
        serverClient = new QueueSpace();
        serverIdProvider = new QueueSpace();
        idProviderServer = new QueueSpace();
        accountServiceServer = new QueueSpace();
        serverAccountService = new QueueSpace();

        //Add the spaces/channels to the repository
        repository.add("clientServer", clientServer);
        repository.add("serverClient", serverClient);
        repository.add("serverIdProvider", serverIdProvider);
        repository.add("idProviderServer", idProviderServer);
        repository.add("accountServiceServer", accountServiceServer);
        repository.add("serverAccountService", serverAccountService);

        // Open a gate
        repository.addGate("tcp://localhost:123/?keep");

        // Keep reading chat messages and printing them
        System.out.println("Running host on port 123");

        executor = Executors.newCachedThreadPool();

        //Main loop where requests are resolved
        while (2 + 2 < 5) {
            Object[] requestT = clientServer.get(new FormalField(String.class), new FormalField(String.class));
            String username = requestT[0].toString();
            String request = requestT[1].toString();
            System.out.println(username + " " + request);
            requestResolver(request, username);
        }
    }

    public void requestResolver(String request, String username) {
        System.out.println("Client requested: " + request);

        try {
            switch (request) {
                case LOGIN -> {
                    login(username);
                }
                default -> System.out.println("ERROR IN SWITCH STMT");
            }

        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean login(String username) throws InterruptedException {
        Object[] t = clientServer.get(new FormalField(String.class), new FormalField(String.class));
        String password = t[1].toString();

        Object[] response = null;
        try {
            System.out.println("Logging " + username + " in...");
            serverIdProvider.put(username, password);
            response = idProviderServer.get(new FormalField(String.class));

            // todo navnet på kanal kan gøres tilfældig
            //  eller være id i stedet for navn
            String userToServerName = username + "server";
            String serverToUserName = "server" + username;

            SequentialSpace userServer = new QueueSpace();
            SequentialSpace serverUser = new QueueSpace();

            try {
                repository.add(serverToUserName, serverUser);
                repository.add(userToServerName, userServer);

                System.out.println("Created private channels...");
                System.out.println(userToServerName);
                System.out.println(serverToUserName);
                numOfClientsConnected++;
                System.out.println("Number of clients connected: " + numOfClientsConnected);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            if (response[0].equals(OK)) {
                System.out.println(username + " logged in at " + LocalDateTime.now());

                executor.submit(new UserServerCommunication(userServer, serverUser, username));
            } else {
                System.out.println("Error in credentials");
                serverUser.put(KO);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void logout(String username) {
        repository.remove(username + "server");
        repository.remove("server" + username);
        numOfClientsConnected--;
        System.out.println("Number of clients connected: " + numOfClientsConnected);
    }
}
