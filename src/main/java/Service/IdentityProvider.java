package Service;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;

import static shared.Channels.*;
import static shared.Requests.*;

public class IdentityProvider {
    static boolean connectedToServer = false;
    static RemoteSpace serverIdProvider = null;
    static RemoteSpace idProviderServer = null;

    public static void main(String[] args) {
        HashMap<String, String> namePasswordMap = new HashMap<>();
        namePasswordMap.put("user", "password");
        namePasswordMap.put("Alice", "password");
        namePasswordMap.put("Bob", "password");
        namePasswordMap.put("Charlie", "password");

        while (true) {

            if (!connectedToServer) {
                // connect to tuple space
                try {
                    System.out.println(IdentityProvider.class.getName() + ": Trying to establish connection to remote spaces...");

                    String serverService = String.format("tcp://localhost:123/%s?%s", SERVER_ID_PROVIDER, CONNECTION_TYPE);
                    String serviceServer = String.format("tcp://localhost:123/%s?%s", ID_PROVIDER_SERVER, CONNECTION_TYPE);
                    serverIdProvider = new RemoteSpace(serverService);
                    idProviderServer = new RemoteSpace(serviceServer);

                    connectedToServer = true;
                    System.out.printf(IdentityProvider.class.getName() + ": Established connection to remote spaces:\n%s and \n%s at " + LocalDateTime.now(),
                            serverIdProvider.getUri(),
                            idProviderServer.getUri());
                    System.out.println(IdentityProvider.class.getName() + ":\n\nWaiting for requests...");

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    connectedToServer = false;
                }

            } else if (connectedToServer) {

                while (true) {
                    Object[] credentials;
                    try {
                        credentials = serverIdProvider.get(new FormalField(String.class), new FormalField(String.class));

                        String username = credentials[0].toString();
                        String password = credentials[1].toString();
                        System.out.println(IdentityProvider.class.getName() + ": Credentials received: " + username + " " + password);

                        if (namePasswordMap.containsKey(username)) {
                            if (namePasswordMap.get(username).equals(password)) {
                                System.out.println(IdentityProvider.class.getName() + ": Credentials verified at: " + LocalDateTime.now());
                                idProviderServer.put(OK);
                            } else {
                                System.out.println(IdentityProvider.class.getName() + ": User submitted wrong password!");
                                idProviderServer.put(KO);
                            }
                        } else {
                            System.out.println(IdentityProvider.class.getName() + ": No such user exists");
                            idProviderServer.put(KO);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        connectedToServer = false;
                    }
                }
            }
        }
    }
}
