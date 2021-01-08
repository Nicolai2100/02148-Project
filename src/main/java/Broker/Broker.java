package Broker;

import model.StockInfo;
import org.jspace.*;

import java.io.IOException;

public class Broker {

    //Brokerens hostname og port
    String hostName = "localhost";
    int port = 9001;

    SequentialSpace stocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace marketOrders = new SequentialSpace();
    SequentialSpace limitOrders = new SequentialSpace(); //TODO: Bør både market og limit orders være i samme space?
    SequentialSpace transactions = new SequentialSpace();

    SpaceRepository tradeRepo = new SpaceRepository();

    RemoteSpace transactionsInBank = new RemoteSpace("blabla");

    static final String sellOrderString = "SELL";
    static final String buyOrderString = "BUY";

    boolean serviceRunning;

    public Broker() throws IOException {
        tradeRepo.add("tradeRequests", marketOrders);
        tradeRepo.addGate("tcp://" + hostName + ":" + port + "/?keep");
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.startService();
    }

    private void startService() {
        serviceRunning = true;
        new MarketOrderHandler().start();
        new TransactionsHandler().start();
    }

    //Denne tråds ansvar er at konstant tage imod ordre om at sælge bestemte aktier,
    // for derefter at finde en køber til disse.
    //TODO: For nemheds skyld tillader den indtil videre kun salg/køb af ÈN aktie ad gangen. Hvad skal vi gøre for at sælge/købe flere ad gangen?
    class MarketOrderHandler extends Thread {
        @Override
        public void run() {
            while(serviceRunning) {
                try {
                    //Eksempel på tuple: ("Alice", "SELL", "AAPL", 2). ID/Navn, Order type, stock name, quantity
                    SellMarketOrder sellOrder = new SellMarketOrder(marketOrders.get(
                            new FormalField(String.class),
                            new ActualField(sellOrderString),
                            new FormalField(String.class),
                            new ActualField(1))); //Dette gør, at vi pt. kun ser på salg/køb af én aktie..
                    new Broker.Broker.FindMatchingBuyOrderHandler(sellOrder).start(); //TODO: Tråden bør enten gemmes et sted og holdes øje med, eller vi bør måske bruge en slags Thread Pool.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class FindMatchingBuyOrderHandler extends Thread {

        private SellMarketOrder sellOrder;

        public FindMatchingBuyOrderHandler(SellMarketOrder sellOrder) {
            this.sellOrder = sellOrder;
        }

        @Override
        public void run() {
            try {
                BuyMarketOrder buyOrder = new BuyMarketOrder(marketOrders.get(
                        new FormalField(String.class),
                        new ActualField(buyOrderString),
                        new ActualField(sellOrder.getStock()),
                        new ActualField(1))); //TODO: Igen tager vi kun én aktie ad gangen, indtil videre..

                //Her finder vi den nuværende pris på aktien
                StockInfo stockInfo = new StockInfo(stocks.get(new ActualField(sellOrder.getStock()), new FormalField(Integer.class)));

                //Her opretter vi en transaktion, der skal udføres – eventuel af en anden tråd?
                //TODO: "transaction spacet" bør nok ligge i en anden klasse – "banken". Så her skal det nok være et remote space, som der puttes i.
                transactions.put(sellOrder.getOrderedBy(), buyOrder.getOrderedBy(), stockInfo.getName(), stockInfo.getPrice(), sellOrder.getQuantity());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //TODO: Denne handler skal nok hellere være i "banken" eller hvor det nu ellers er, at transaktioner skal udføres.. I hvert fald der hvor "transaction spacet" er.
    class TransactionsHandler extends Thread {
        @Override
        public void run() {
            while(serviceRunning) {
                try {
                    Transaction t = new Transaction(transactions.get(
                            new FormalField(String.class),
                            new FormalField(String.class),
                            new FormalField(String.class),
                            new FormalField(Integer.class),
                            new FormalField(Integer.class)
                    ));
                    //TODO: Udfør transaktionen..

                    transactions.put("Alice", "Bob", "AAPL", 110, 5); //Så skal et space kun til transactions.

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}