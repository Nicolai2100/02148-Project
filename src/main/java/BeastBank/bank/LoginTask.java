package BeastBank.bank;

import org.jspace.FormalField;
import org.jspace.QueueSpace;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static BeastBank.shared.Requests.*;

/**
 * This class is used for processing logins
 */
public class LoginTask implements Callable<String> {
    private final RemoteSpace serverIdProvider;
    private final RemoteSpace idProviderServer;
    private final String username;
    private final String password;

    private final ExecutorService executor;

    public LoginTask(RemoteSpace idProviderServer,
                     RemoteSpace serverIdProvider,
                     String username,
                     String password,
                     ExecutorService executor) {
        this.idProviderServer = idProviderServer;
        this.serverIdProvider = serverIdProvider;
        this.username = username;
        this.password = password;
        this.executor = executor;
    }

    @Override
    public String call() {

        System.out.println("Started login thread...");
        login(username);
        return "Finished login procedure for " + username;
    }

    public void login(String username) {
        try {
            System.out.println("Logging " + username + " in...");
            serverIdProvider.put(username, password);
            Object[] response = idProviderServer.get(new FormalField(String.class));

            String userToServerName = username + "server";
            String serverToUserName = "server" + username;

            SequentialSpace userServer = new QueueSpace();
            SequentialSpace serverUser = new QueueSpace();
            Server.repository.add(serverToUserName, serverUser);
            Server.repository.add(userToServerName, userServer);

            try {
                System.out.printf("Created private channels for %s...", username);
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
                serverUser.put(KO);
                Server.logout(username);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
