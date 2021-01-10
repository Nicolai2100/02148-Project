package Service;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;

import static model.Requests.*;

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
                    System.out.println("Trying to establish connection to remote spaces...");
                    serverIdProvider = new RemoteSpace("tcp://localhost:123/serverIdProvider?keep");
                    idProviderServer = new RemoteSpace("tcp://localhost:123/idProviderServer?keep");

                    connectedToServer = true;
                    System.out.printf("Established connection to remote spaces:\n%s and \n%s at " + LocalDateTime.now(),
                            serverIdProvider.getUri(),
                            idProviderServer.getUri());
                    System.out.println("\n\nWaiting for requests...");

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    connectedToServer = false;
                }

            } else if (connectedToServer) {

                while (1 + 1 == 2) {
                    Object[] credentials;
                    try {
                        credentials = serverIdProvider.get(new FormalField(String.class), new FormalField(String.class));

                        String username = credentials[0].toString();
                        String password = credentials[1].toString();
                        System.out.println("Credentials received: " + username + " " + password);

                        if (namePasswordMap.containsKey(username)) {
                            if (namePasswordMap.get(username).equals(password)) {
                                System.out.println("Credentials verified at: " + LocalDateTime.now());
                                idProviderServer.put(OK);
                            } else {
                                System.out.println("User submitted wrong password!");
                                idProviderServer.put(KO);
                            }
                        } else {
                            System.out.println("No such user exists");
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
