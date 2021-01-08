package Client;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        // parse arguments
        Scanner s = new Scanner(System.in);

        String username = "user";
        String password = "password";

        if (args.length > 0)
            username = args[0];

        RemoteSpace serverClient = null;
        RemoteSpace clientServer = null;
        // connect to tuple space
        try {
            serverClient = new RemoteSpace("tcp://localhost:123/serverClient?keep");
            clientServer = new RemoteSpace("tcp://localhost:123/clientServer?keep");

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Established connection to " + serverClient + " and " + clientServer);

        String message = "";
        do {
            System.out.println("Press 1 to login");
            System.out.println("Press 2 to get more data");

            message = s.nextLine();
            try {
                if (message.equalsIgnoreCase("1")) {
                    System.out.println("...sending credentials");
                    clientServer.put(username, password);

                    //Object[] data = serverClient.get(new ActualField("data"), new FormalField(String.class));
                    Object[] data = serverClient.get(new FormalField(String.class), new FormalField(String.class));
                    System.out.println(data[0]);

                } else if (message.equalsIgnoreCase("2")) {
                    System.out.println("fetcing data");
                    //Object[] data = serverClient.get(new ActualField("data"), new FormalField(String.class));
                    Object[] data = serverClient.get(new FormalField(String.class), new FormalField(String.class));
                    System.out.println(data[0]);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!message.equalsIgnoreCase("exit"));

        try{
            clientServer.put("Bye");
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        System.exit(7);
    }
}

/*// User code
channel(user,server).put(username,password) ;
response := channel(server,user).get(string)

// Server code
credentials := channel(user,server).get(string,string) ;
channel(server,user).put(check(credentials))
*/

/*user.(username,password) -> server.credentials(string,string) ;
server.(username(credentials),password(credentials)) -> identityProvider.credentials(string,string) ;
identityProvider.check(credentials) -> server.response(string) ;
if (response==ok)@server then
    while notEnoughData()@user do {
        user.("getData") -> server.t("getData")
        server.(generateData()) -> user.data(string))
    }
else
    server.("bye") -> user.response("bye")
*/
