import org.jspace.FormalField;
import org.jspace.QueueSpace;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;


import static model.Requests.*;

public class TestClass {

    static SequentialSpace clientServer;
    static SequentialSpace serverClient;
    static SequentialSpace accountServiceServer;
    static SequentialSpace serverAccountService;

    public static void main(String[] args) throws InterruptedException {
        SpaceRepository repository = new SpaceRepository();

        // Create a local space for each channel
        clientServer = new QueueSpace();
        serverClient = new QueueSpace();

        accountServiceServer = new QueueSpace();
        serverAccountService = new QueueSpace();

        // Add the spaces/channels to the repository
        repository.add("clientServer", clientServer);
        repository.add("serverClient", serverClient);

        repository.add("accountServiceServer", accountServiceServer);
        repository.add("serverAccountService", serverAccountService);

        // Open a gate
        repository.addGate("tcp://localhost:123/?keep");

        // Keep reading chat messages and printing them
        System.out.println("Running host on port 123");

        System.out.println("Sending request");
        serverAccountService.put(QUERY_STOCKS, "Alice");

        Object[] response = accountServiceServer.get(new FormalField((String.class)));
        String responseStr = response[0].toString();

        if (responseStr.equals(OK)) {
            do {
                System.out.println("Fetching data...");

                response = accountServiceServer.get(new FormalField(String.class));
                responseStr = response[0].toString();

                if (responseStr.equals(MORE_DATA)) {
                    //Fetching data from service
                    Object[] dataResponse = accountServiceServer.get(new FormalField(String.class), new FormalField(Integer.class));
                    String stockName = dataResponse[0].toString();
                    int stockPrice = Integer.parseInt(dataResponse[1].toString());
                    //Sending data to client
                    System.out.println("Sending data");
                    serverClient.put(MORE_DATA);
                    serverClient.put(stockName, stockPrice);
                } else if (responseStr.equals(NO_MORE_DATA)) {
                    System.out.println(NO_MORE_DATA);
                    serverClient.put(NO_MORE_DATA);
                    break;
                }
                System.out.println();
            } while (responseStr.equals(MORE_DATA));

        } else if (responseStr.equals(KO)) {
            serverClient.put(KO);
            System.out.println("No such user in the system");
        }
    }
}
