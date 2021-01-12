package Client;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;

import java.io.IOException;
import java.util.Scanner;

public class WulffClient {

    //Server info og spaces
    String serverHost = "localhost"; //Lav denne om.
    RemoteSpace trades; //Kunne være et space på en server der indeholder alle "handler".
    RemoteSpace stocks; //Kunne være et space på en server, der indeholder info/priser/etc. om de forskellige stocks.
    //Klient info og spaces
    String name = "Alice"; //bare som test.
    SequentialSpace balance = new SequentialSpace(); //Skal dette være i samme space som stocks?
    SequentialSpace clientShareholdings = new SequentialSpace();
    RemoteSpace remoteStocks;

    public static String lock = "lock";

    public WulffClient() throws IOException {
        this.trades = new RemoteSpace("tcp://" + serverHost + ":9001/trades?keep");
        this.stocks = new RemoteSpace("tcp://" + serverHost + ":9001/trades?keep");
    }

    public static void main(String[] args) throws IOException {
        WulffClient wulffClient = new WulffClient();
        wulffClient.startConsoleApp();
    }

    void sellShares(String stockName, int quantity) throws InterruptedException {
        //Først finder vi vores beholdning af den specifikke type aktie.
        Object[] shares = clientShareholdings.queryp(new ActualField(stockName), new FormalField(Integer.class));
        if (shares == null)
            throw new IllegalArgumentException("Cannot sell shares: " + stockName + ". Cause: You don't own any shares of this stock");
        int owned = (Integer) shares[1];
        if (owned > quantity)
            throw new IllegalArgumentException("Cannot sell shares: " + stockName + ". Cause: Tried to sell more than you own.");


        //TODO: Overvej, om der her skal bruges en lock. Og skal den bruges sådan her, eller hvordan?
        //Min umiddelbare tanke er, at låsen her måske kan sikre, at man sælger til den rigtige pris.
        trades.get(new ActualField(lock));

        //Her henter vi den nuværende kurs på aktien.
        Object[] stock = stocks.queryp(new ActualField(stockName), new FormalField(Integer.class));
        if (stock == null) {
            trades.put(lock);
            throw new IllegalArgumentException("Could not find stock on the market.");
        }
        int currentPrice = (Integer) stock[1];

        //Her opdaterer vi klientens antal af aktier.
        shares = clientShareholdings.get(new ActualField(stockName), new FormalField(Integer.class));
        int currentQuantity = (Integer) shares[1];
        currentQuantity -= quantity;
        clientShareholdings.put(stockName, currentQuantity);

        //Her sender vi "salget" til serveren. Men hvordan skal det egentlig foregå?
        trades.put(name, "SELL", stock, quantity); //TODO: Skal dette være anderledes?
        System.out.println("Successfully sold " + quantity + " of shares: " + stockName);

        //Her opdaterer vi klientens balance.
        int currentBalance = (Integer) balance.get(new FormalField(Integer.class))[0];
        currentBalance += quantity * currentPrice;
        balance.put(currentBalance);
        System.out.println("Your balance is now: " + currentBalance);

        trades.put(lock);
    }


    ////////////////////////////////////////////////////////////
    //
    //      Console app herunder
    //
    ////////////////////////////////////////////////////////////

    //Scanner
    Scanner scanner = new Scanner(System.in);

    void startConsoleApp() {
        while (true) {
            System.out.println("Do you want to sell or buy?");
            String input = scanner.nextLine();
            if (input.equals("sell")) sellStockDialog();
        }
    }

    void sellStockDialog() {
        System.out.println("Which stock do you want to sell?");
        String stock = scanner.nextLine();
        System.out.println("How many?");
        int quantity = scanner.nextInt();
        try {
            sellShares(stock, quantity);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void queryRemoteStocks() {
            // SequentialSpace sequentialSpace = this.stocks.get(new ActualField("hej"));

        try {
            this.stocks.query(new ActualField("lock"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
