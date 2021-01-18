package server;

import org.jspace.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static BeastProject.shared.Channels.*;
import static BeastProject.shared.Requests.*;

/**
 * The main responsibility of the Server-class is to provide a SpaceRepository
 * and channels which connect the client to the services
 **/
public class Server {
    static SpaceRepository repository = new SpaceRepository();

    static SequentialSpace clientServer;
    static RandomSpace serverClient;
    static SequentialSpace serverIdProvider;
    static SequentialSpace idProviderServer;
    static SequentialSpace accountServiceServer;
    static SequentialSpace serverAccountService;

    static SequentialSpace serverBroker;
    static SequentialSpace brokerServer;

    static int numOfClientsConnected = 0;
    static ExecutorService executor;

    public void startServer() throws InterruptedException {
        repository = new SpaceRepository();

        // Create a local space for each channel
        clientServer = new QueueSpace();
        serverClient = new RandomSpace();
        serverIdProvider = new QueueSpace();
        idProviderServer = new QueueSpace();
        accountServiceServer = new QueueSpace();
        serverAccountService = new QueueSpace();

        serverBroker = new QueueSpace();
        brokerServer = new QueueSpace();

        //Add the spaces/channels to the repository
        repository.add(CLIENT_SERVER, clientServer);
        repository.add(SERVER_CLIENT, serverClient);
        repository.add(SERVER_ID_PROVIDER, serverIdProvider);
        repository.add(ID_PROVIDER_SERVER, idProviderServer);
        repository.add(ACCOUNT_SERVICE_SERVER, accountServiceServer);
        repository.add(SERVER_ACCOUNT_SERVICE, serverAccountService);

        repository.add(SERVER_BROKER, serverBroker);
        repository.add(BROKER_SERVER, brokerServer);

        // Open a gate
        String uri = String.format("tcp://%s:%d/?%s", SERVER_HOSTNAME, SERVER_PORT, CONNECTION_TYPE);
        repository.addGate(uri);

        // Keep reading chat messages and printing them
        System.out.println("Server: Started server on: " + uri);

        executor = Executors.newCachedThreadPool();

        //Start TransactionTask
        executor.submit(new TransactionTask());

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
