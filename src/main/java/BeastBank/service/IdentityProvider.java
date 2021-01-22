package BeastBank.service;

import BeastBank.dao.FakeUserDataAccessService;
import org.jspace.FormalField;
import org.jspace.QueueSpace;
import BeastBank.shared.SharedEncryption;
import org.jspace.SpaceRepository;

import java.time.LocalDateTime;

import static BeastBank.shared.Channels.*;
import static BeastBank.shared.Requests.*;

public class IdentityProvider {

    private final static String serviceStr = IdentityProvider.class.getName() + ": ";

    public static void main(String[] args) {
        //Create remote tuple spaces
        SpaceRepository repository = new SpaceRepository();

        QueueSpace serverIdProvider = new QueueSpace();
        QueueSpace idProviderServer = new QueueSpace();

        repository.add(SERVER_ID_PROVIDER, serverIdProvider);
        repository.add(ID_PROVIDER_SERVER, idProviderServer);

        //Open a gate
        String uri = String.format("tcp://%s:%d/?%s", ID_PROVIDER_HOSTNAME, ID_PROVIDER_PORT, CONNECTION_TYPE);
        repository.addGate(uri);

        System.out.println(serviceStr + "Started listening on: " + uri);

        while (true) {
            Object[] credentials;
            try {
                credentials = serverIdProvider.get(new FormalField(String.class), new FormalField(String.class));

                String username = credentials[0].toString();
                String password = credentials[1].toString();

                System.out.println(serviceStr + "Credentials received: " + username + " " + password);

                boolean userValid = validateCredentials(username, password);

                if (userValid) {
                    idProviderServer.put(username, OK);
                } else {
                    idProviderServer.put(username, KO);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static boolean validateCredentials(String username, String password) {
        var optionalUser = FakeUserDataAccessService.getInstance().selectUserByUsername(username);

        if (optionalUser.isPresent()) {
            if (SharedEncryption.validatePassword(optionalUser.get().getPassword(), password)) {
                System.out.println(serviceStr + "Credentials verified at: " + LocalDateTime.now());
                return true;
            } else {
                System.out.println(serviceStr + "User submitted wrong password!");
            }
        } else {
            System.out.println(serviceStr + "No such user exists");
        }
        return false;
    }
}
