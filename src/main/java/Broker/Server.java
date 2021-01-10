package Broker;

import org.jspace.*;

import java.time.LocalDateTime;

import static model.Requests.*;

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

    public static void main(String[] args) throws InterruptedException {
        repository = new SpaceRepository();

        // Create a local space for each channel
        clientServer = new QueueSpace();
        serverClient = new QueueSpace();
        serverIdProvider = new QueueSpace();
        idProviderServer = new QueueSpace();
        accountServiceServer = new QueueSpace();
        serverAccountService = new QueueSpace();

        // Add the spaces/channels to the repository
        //todo - gør dette til en lobby og opret så nye client channels for hver ny klient der logger ind
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

        //Main loop where requests are resolved
        while (2 + 2 < 5) {
            Object[] requestT = clientServer.get(new FormalField(String.class), new FormalField(String.class));
            String username = requestT[0].toString();
            String request = requestT[1].toString();

            requestResolver(request, username);
        }
    }

    static void requestResolver(String request, String username) {
        System.out.println("Client requested: " + request);

        try {
            switch (request) {
                case LOGIN -> {
                    login(username);

                }
                case QUERY_STOCKS -> queryStocks();
                default -> System.out.println("ERROR IN SWITCH STMT");
            }

        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }


    static void queryStocks() throws InterruptedException {
        //Who is asking and what is the request
        Object[] request = clientServer.get(new FormalField(String.class), new FormalField((String.class)));
        String query = request[0].toString();
        String username = request[1].toString();

        System.out.println(query + username);

        //Forward request to account service
        System.out.println("Sending request...");
        serverAccountService.put(username, query);

        Object[] accountServiceResponse = accountServiceServer.get(new ActualField(username), new FormalField(String.class));
        String responseStr = accountServiceResponse[1].toString();

        System.out.println("Request: " + responseStr);

        if (responseStr.equals(OK)) {
            do {
                System.out.println("Fetching data...");

                accountServiceResponse = accountServiceServer.get(new ActualField(username), new FormalField(String.class));
                responseStr = accountServiceResponse[1].toString();
                System.out.println(responseStr);

                if (responseStr.equals(MORE_DATA)) {
                    //Fetching data from service
                    Object[] dataResponse = accountServiceServer.get(new ActualField(username), new FormalField(String.class), new FormalField(Integer.class));
                    String stockName = dataResponse[1].toString();
                    int stockPrice = Integer.parseInt(dataResponse[2].toString());
                    //Sending data to client
                    System.out.println("Sending data");
                    serverClient.put(MORE_DATA);
                    serverClient.put(stockName, stockPrice);
                } else if (responseStr.equals(NO_MORE_DATA)) {
                    serverClient.put(NO_MORE_DATA);
                    break;
                }
                System.out.println();
            } while (responseStr.equals(MORE_DATA));

        } else if (responseStr.equals(KO)) {
            serverClient.put(KO);
            System.out.println("No such user in the system");
        }
    }

    static boolean login(String username) throws InterruptedException {
        Object[] t = clientServer.get(new FormalField(String.class), new FormalField(String.class));
        //String username = t[0].toString();
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

            Space userServer = new QueueSpace();
            Space serverUser = new QueueSpace();

            try{
                repository.add(serverToUserName, serverUser);
                repository.add(userToServerName, userServer);
                System.out.println("Created private channels...");
                System.out.println(userToServerName);
                System.out.println(serverToUserName);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }

            // todo navnet på kanal kan gøres tilfældig
            //serverUser.put(username, serverToUserName);
            //userServer.put(username, userToServerName);

            if (response[0].equals(OK)) {
                System.out.println(username + " logged in at " + LocalDateTime.now());
                serverUser.put(OK);

            } else {
                System.out.println("Error in credentials");
                serverUser.put(KO);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}
