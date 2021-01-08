import model.StockInfo;
import org.jspace.FormalField;
import org.jspace.QueueSpace;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import java.util.ArrayList;

import static model.Requests.*;

public class TestClass {

    static SequentialSpace clientServer;
    static SequentialSpace serverClient;
    static SequentialSpace serverIdProvider;
    static SequentialSpace idProviderServer;
    static SequentialSpace accountServiceServer;
    static SequentialSpace serverAccountService;

    public static void main(String[] args) throws InterruptedException {
        SpaceRepository repository = new SpaceRepository();

        // Create a local space for each channel
        clientServer = new QueueSpace();
        serverClient = new QueueSpace();

        serverIdProvider = new QueueSpace();
        idProviderServer = new QueueSpace();

        accountServiceServer = new QueueSpace();
        serverAccountService = new QueueSpace();

        // Add the spaces/channels to the repository
        repository.add("clientServer", clientServer);
        repository.add("serverClient", serverClient);

        repository.add("serverIdProvider", serverIdProvider);
        repository.add("idProviderServer", idProviderServer);

        repository.add("accountServiceServer", accountServiceServer);
        repository.add("serverAccountService", serverAccountService);

        // Open a gate
        repository.addGate("tcp://localhost:123/?keep");

        // Keep reading chat messages and printing them
        System.out.println("Running host on port 123");

        System.out.println("putting data");
        serverAccountService.put(QUERY_STOCKS, "Alice");
        System.out.println("waiting for response");

        Object[] response = accountServiceServer.get(new FormalField((String.class)));
        String responseStr = response[0].toString();

        if (responseStr.equals(OK)) {
            ArrayList<StockInfo> stocks = new ArrayList<>();
            do {
                System.out.println("Fetching data...");

                Object[] t = accountServiceServer.get(new FormalField(String.class), new FormalField(String.class));
                responseStr = t[0].toString();
               String stockName = t[1].toString();
               int stockPrice = Integer.getInteger(t[2].toString());

                if (responseStr.equals(NO_MORE_DATA)) {
                    System.out.println(NO_MORE_DATA);
                    break;
                }
                stocks.add(new StockInfo(t[1].toString(), Integer.parseInt(t[2].toString())));
                System.out.println();
            } while (responseStr.equals(MORE_DATA));

        } else
            System.out.println("error");
    }
}
