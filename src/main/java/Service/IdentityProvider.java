package Service;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;

import static model.Requests.*;

public class IdentityProvider {

    public static void main(String[] args) {
        HashMap<String, String> namePasswordMap = new HashMap<>();
        namePasswordMap.put("user", "password");
        namePasswordMap.put("Alice", "password");
        namePasswordMap.put("Bob", "password");
        namePasswordMap.put("Charlie", "password");

        // connect to tuple space
        RemoteSpace serverIdProvider = null;
        RemoteSpace idProviderServer = null;
        try {
            serverIdProvider = new RemoteSpace("tcp://localhost:123/serverIdProvider?keep");
            idProviderServer = new RemoteSpace("tcp://localhost:123/idProviderServer?keep");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("Established connection to " + idProviderServer.getUri() + "\n and " + serverIdProvider.getUri());
        System.out.println("Waiting for requests...");

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
            }
        }
    }
}
