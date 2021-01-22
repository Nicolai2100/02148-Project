package BeastBank.bank;

import org.jspace.*;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

import static BeastBank.bank.Server.repository;
import static BeastBank.shared.Requests.*;

/**
 * This class is used for processing logins
 */
public class LoginTask implements Callable<String> {
    private final RemoteSpace serverIdProvider;
    private final RemoteSpace idProviderServer;
    private final String username;
    private final String password;
    private final String loginStr = LoginTask.class.getName() + ": ";

    public LoginTask(RemoteSpace idProviderServer,
                     RemoteSpace serverIdProvider,
                     String username,
                     String password) {
        this.idProviderServer = idProviderServer;
        this.serverIdProvider = serverIdProvider;
        this.username = username;
        this.password = password;
    }

    @Override
    public String call() {
        System.out.println(loginStr + "Started login thread...");
        login(username);
        return loginStr + "Finished login procedure for " + username;
    }

    public void login(String username) {
        try {
            System.out.println(loginStr + "Logging " + username + " in...");
            serverIdProvider.put(username, password);
            Object[] response = idProviderServer.get(new ActualField(username), new FormalField(String.class));
            boolean credentialsVerified = response[1].equals(OK);

            boolean userAlreadyLoggedIn = false;

            if (repository.get(username + "server") != null) {
                userAlreadyLoggedIn = true;
            }

            String userToServerName = username + "server";
            String serverToUserName = "server" + username;

            if (userAlreadyLoggedIn) {
                repository.remove(userToServerName);
                repository.remove(serverToUserName);
            }
            Space userServer = new QueueSpace();
            Space serverUser = new QueueSpace();
            repository.add(serverToUserName, serverUser);
            repository.add(userToServerName, userServer);

            System.out.printf(loginStr + "Created private channels for %s...\n", username);

            if (!userAlreadyLoggedIn) Server.numOfClientsConnected++;
            System.out.println(loginStr + "Number of clients connected: " + Server.numOfClientsConnected);

            if (credentialsVerified) {
                System.out.println(loginStr + username + " logged in at " + LocalDateTime.now());
                Server.executor.submit(new UserServerCommunicationTask(userServer, serverUser, username));
            } else {
                System.out.println(loginStr + "Error in credentials");
                serverUser.put(KO);
                Server.logout(username);
            }
        } catch (
                InterruptedException e) {
            e.printStackTrace();
        }
    }
}
