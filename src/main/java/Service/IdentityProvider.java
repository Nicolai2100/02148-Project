package Service;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.HashMap;

/*server.(username(credentials),password(credentials)) -> identityProvider.credentials(string,string) ;
identityProvider.check(credentials) -> server.response(string) ;*/
public class IdentityProvider {

    public static void main(String[] args) {
        HashMap<String, String> namePasswordMap = new HashMap<>();
        namePasswordMap.put("user", "password");

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
                        System.out.println("Credentials verified");
                        idProviderServer.put("ok");
                    } else {
                        System.out.println("User submitted wrong password!");
                        idProviderServer.put("ko");
                    }
                } else {
                    System.out.println("No such user exists");
                    idProviderServer.put("ko");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
