package Broker;

import org.jspace.*;

/*user.(username,password) -> server.credentials(string,string) ;
        server.(username(credentials),password(credentials)) -> identityProvider.credentials(string,string) ;
        identityProvider.check(credentials) -> server.response(string) ;
        if (response==ok)@server then
        while notEnoughData()@user do {
        user.("getData") -> server.t("getData")
        server.(generateData()) -> user.data(string))
        }
        else
        server.("bye") -> user.response("bye")*/
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
            Object[] t = clientServer.get(new FormalField(String.class), new FormalField(String.class));
            String username = t[0].toString();
            String password = t[1].toString();

            System.out.println("Message received: " + t[0] + " " + t[1]);

            if (login(username, password)) {
                System.out.println("Credentials \"ok\"");
                serverClient.put("ok");
            } else {
                System.out.println("Error in credentials");
                serverClient.put("ko");
            }
        }
    }

    static boolean login(String user, String password) {
        Object[] response = null;
        try {
            System.out.println("Logging " + user + " in...");
            serverIdProvider.put(user, password);
            response = idProviderServer.get(new FormalField(String.class));
            if (response[0].equals("ok")) {
                System.out.println(user + " logged in");
                serverClient.put("credentials", "ok");
                return true;

            } else {
                System.out.println("error");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}
