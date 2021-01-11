package server;

import org.jspace.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static shared.Channels.*;
import static shared.Requests.*;

/**
 * The main responsibility of the Server-class is to provide a SpaceRepository
 * and channels which connect the client to the services
 **/
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
        repository.addGate("tcp://localhost:123/?" + CONNECTION_TYPE);

        // Keep reading chat messages and printing them
        System.out.println("Server: Started server on port 123");

        executor = Executors.newCachedThreadPool();

        //Main loop where client requests are resolved
        while (true) {
            Object[] requestT = clientServer.get(
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(String.class));
            String request = requestT[0].toString();
            String username = requestT[1].toString();
            String password = requestT[2].toString();
            System.out.println(username + " " + request);
            requestResolver(request, username, password);
        }
    }

    public void requestResolver(String request, String username, String password) {
        System.out.println("Client requested: " + request);

        try {
            switch (request) {
                case LOGIN -> {
                    login(username, password);
                }
                default -> System.out.println("ERROR IN SWITCH STMT");
            }

        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public void login(String username, String password) throws InterruptedException {
        executor.submit(new LoginTask(
                clientServer,
                serverClient,
                idProviderServer,
                serverIdProvider,
                username,
                password,
                executor));
    }

    public static void logout(String username) {
        repository.remove(username + "server");
        repository.remove("server" + username);
        numOfClientsConnected--;
        System.out.println("Number of clients connected: " + numOfClientsConnected);
    }
}
