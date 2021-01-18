package BeastBank.service;

import BeastBank.dao.FakeUserDataAccessService;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import BeastBank.shared.SharedEncryption;

import java.io.IOException;
import java.time.LocalDateTime;

import static BeastBank.shared.Channels.*;
import static BeastBank.shared.Requests.*;

public class IdentityProvider {
    static boolean connectedToServer = false;
    static RemoteSpace serverIdProvider = null;
    static RemoteSpace idProviderServer = null;

    public static void main(String[] args) {

        while (true) {
            if (!connectedToServer) {
                // connect to tuple space
                try {
                    System.out.println(IdentityProvider.class.getName() + ": Trying to establish connection to remote spaces...");

                    String serverService = String.format("tcp://%s:%d/%s?%s", SERVER_HOSTNAME, SERVER_PORT, SERVER_ID_PROVIDER, CONNECTION_TYPE);
                    String serviceServer = String.format("tcp://%s:%d/%s?%s", SERVER_HOSTNAME, SERVER_PORT, ID_PROVIDER_SERVER, CONNECTION_TYPE);
                    serverIdProvider = new RemoteSpace(serverService);
                    idProviderServer = new RemoteSpace(serviceServer);

                    connectedToServer = true;
                    System.out.println(IdentityProvider.class.getName() + ": Waiting for requests...");

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

                        var optionalUser = FakeUserDataAccessService.getInstance().selectUserByUsername(username);

                        if (optionalUser.isPresent()) {
                            if (SharedEncryption.validatePassword(optionalUser.get().getPassword(), password)) {
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

            //todo - NJL - tokens?
            /*  // validate credentials
            String hashed = fetchedUser.getPassword();
            if (BCrypt.checkpw(password, hashed)) {
            String token = JWTHandler.provider.generateToken(fetchedUser);
            ctx.header("Authorization", new JWTResponse(token).jwt);
            // the golden line. All hail this statement
            ctx.header("Access-Control-Expose-Headers", "Authorization");
            ctx.status(HttpStatus.OK_200);
            ctx.result("Success - User login was successful");
            ctx.json(fetchedUser);




            fetchedUser = getOrCreateRootUser(username);
            String token = JWTHandler.provider.generateToken(fetchedUser);
            ctx.header("Authorization", new JWTResponse(token).jwt);
            ctx.header("Access-Control-Expose-Headers", "Authorization");
            ctx.status(HttpStatus.OK_200);
            ctx.result("Success - User login with root was successful");
            ctx.json(fetchedUser);
            ctx.contentType(ContentType.JSON);*/
