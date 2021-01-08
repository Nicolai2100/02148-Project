package Broker;

import model.StockInfo;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import java.util.ArrayList;
import java.util.List;

public class Broker {

    String hostName = "localhost";
    int port = 9001;
    boolean serviceRunning;

    SequentialSpace stocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace marketOrders = new SequentialSpace();
    SequentialSpace limitOrders = new SequentialSpace(); //TODO: Bør både market og limit orders være i samme space?
    SequentialSpace transactions = new SequentialSpace();

    SpaceRepository tradeRepo = new SpaceRepository();

    static String sellOrderString = "SELL";
    static String buyOrderString = "BUY";

    public Broker() {
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
                    new FindMatchingOrderHandler(sellOrder).start(); //TODO: Tråden bør enten gemmes et sted og holdes øje med, eller vi bør måske bruge en slags Thread Pool.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class FindMatchingOrderHandler extends Thread {

        private SellMarketOrder sellOrder;

        public FindMatchingOrderHandler(SellMarketOrder sellOrder) {
            this.sellOrder = sellOrder;
        }

        @Override
        public void run() {
            try {
                BuyMarketOrder buyOrder = new BuyMarketOrder(marketOrders.get(
                        new FormalField(String.class),
                        new ActualField(buyOrderString),
                        new ActualField(sellOrder.getStock()),
                        new ActualField(1)));

                //Her finder vi den nuværende pris på aktien
                StockInfo stockInfo = new StockInfo(stocks.get(new ActualField(sellOrder.getStock()), new FormalField(Integer.class)));

                //Her opretter vi en transaktion, der skal udføres – eventuel af en anden tråd?
                transactions.put(sellOrder.getOrderedBy(), buyOrder.getOrderedBy(), stockInfo.getName(), stockInfo.getPrice(), sellOrder.getQuantity());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}