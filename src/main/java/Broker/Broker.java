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

    }

    //Denne tråds ansvar er at konstant tage imod ordre om at sælge bestemte aktier,
    // for derefter at finde en køber til disse.
    //TODO: For nemheds skyld tillader den indtil videre kun salg/køb af ÈN aktie ad gangen. Hvad skal vi gøre for at sælge/købe flere ad gangen?
    Thread marketOrderHandler = new Thread(new Runnable() {
        public void run() {
            while(true) {
                try {
                    //Eksempel på tuple: ("Alice", "SELL", "AAPL", 2). ID/Navn, Order type, stock name, quantity
                    SellMarketOrder sellOrder = new SellMarketOrder(marketOrders.get(
                            new FormalField(String.class),
                            new ActualField(sellOrderString),
                            new FormalField(String.class),
                            new ActualField(1))); //Dette gør, at vi pt. kun ser på salg/køb af én aktie..
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
    });















    //Both should be sorted by price
    List<SellMarketOrder> sellMarketOrders = new ArrayList<SellMarketOrder>();
    List<BuyMarketOrder> BuyMarketOrders = new ArrayList<BuyMarketOrder>();

    private void startMarketOrderHandler() {
        Thread OrderHandler = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Object[] res = marketOrders.get(new FormalField(String.class), new FormalField(String.class), new FormalField(String.class), new FormalField(Integer.class));
                        String orderedBy = (String) res[0];
                        String orderType = (String) res[1];
                        String stockName = (String) res[2];
                        int quantity = (Integer) res[3];
                        if (orderType.equals(sellOrderString)) {
                            sellMarketOrders.add(new SellMarketOrder(orderedBy, stockName, quantity));
                        } else {
                            BuyMarketOrders.add(new BuyMarketOrder(orderedBy, stockName, quantity));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

}