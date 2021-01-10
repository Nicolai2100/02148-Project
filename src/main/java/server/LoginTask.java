package server;

import org.jspace.FormalField;
import org.jspace.QueueSpace;
import org.jspace.SequentialSpace;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static shared.Requests.*;

/**
 * This class is only used for processing logins
 */
public class LoginTask implements Callable<String> {
    private final SequentialSpace clientServer;
    private final SequentialSpace serverClient;
    private final SequentialSpace serverIdProvider;
    private final SequentialSpace idProviderServer;
    private final String username;
    private final String password;

    private ExecutorService executor;

    public LoginTask(SequentialSpace clientServer,
                     SequentialSpace serverClient,
                     SequentialSpace idProviderServer,
                     SequentialSpace serverIdProvider,
                     String username,
                     String password,
                     ExecutorService executor) {
        this.clientServer = clientServer;
        this.serverClient = serverClient;
        this.idProviderServer = idProviderServer;
        this.serverIdProvider = serverIdProvider;
        this.username = username;
        this.password = password;
        this.executor = executor;
    }

    @Override
    public String call() throws Exception {

        System.out.println("Started login thread...");

        login(username);

        return "Finished login procedure for " + username;
    }

    public void login(String username) throws InterruptedException {
        try {
            System.out.println("Logging " + username + " in...");
            serverIdProvider.put(username, password);
            Object[] response = idProviderServer.get(new FormalField(String.class));

            // todo navnet på kanal kan gøres tilfældig
            //  eller være id i stedet for navn
            String userToServerName = username + "server";
            String serverToUserName = "server" + username;

            SequentialSpace userServer = new QueueSpace();
            SequentialSpace serverUser = new QueueSpace();
            Server.repository.add(serverToUserName, serverUser);
            Server.repository.add(userToServerName, userServer);

            try {
                System.out.println("Created private channels...");
                System.out.println(userToServerName);
                System.out.println(serverToUserName);
                Server.numOfClientsConnected++;
                System.out.println("Number of clients connected: " + Server.numOfClientsConnected);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            if (response[0].equals(OK)) {
                System.out.println(username + " logged in at " + LocalDateTime.now());

                executor.submit(new UserServerCommunicationTask(userServer, serverUser, username));
            } else {
                System.out.println("Error in credentials");

                //todo - hjælp hvorfor virker det ikke her?
                //serverUser.put(KO);
                serverClient.put(username, KO);
                Server.logout(username);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
