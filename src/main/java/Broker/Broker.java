package Broker;

import model.StockInfo;
import org.jspace.*;

public class Broker {

    //Brokerens hostname og port
    String hostName = "localhost";
    int port = 9001;

    SequentialSpace stocks = new SequentialSpace(); //Skal indeholde info og kurser på de forskellige aktier på markedet.
    SequentialSpace marketOrders = new SequentialSpace();
    SequentialSpace limitOrders = new SequentialSpace(); //TODO: Bør både market og limit orders være i samme space?
    SequentialSpace transactions = new SequentialSpace();

    SpaceRepository tradeRepo = new SpaceRepository();

    static final String sellOrderFlag = "SELL";
    static final String buyOrderFlag = "BUY";
    static final String allFlag = "ALL";
    static final String mostFlag = "MOST";

    boolean serviceRunning;

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
        new MarketSaleOrderHandler().start();
        new TransactionsHandler().start();
    }

    /**
     * The responsibility of this class is to constantly handle new orders to sell shares of a stock.
     * This is done by starting a new thread that tries to find a matching buyer of the shares.
     */
    class MarketSaleOrderHandler extends Thread {
        @Override
        public void run() {
            while(serviceRunning) {
                try {
                    SellMarketOrder sellOrder = new SellMarketOrder(marketOrders.get(
                            new FormalField(String.class), //Name of the client who made the order
                            new ActualField(sellOrderFlag), //Type of order, eg. SELL or BUY
                            new FormalField(String.class), //Name of the stock
                            new FormalField(Integer.class))); //Quantity
                    new FindMatchingBuyOrderHandler(sellOrder).start(); //TODO: Tråden bør enten gemmes et sted og holdes øje med, eller vi bør måske bruge en slags Thread Pool.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * The responsibility of this class is to take a sale order as an argument, and then try
     * to find a matching buyer of the shares.
     */
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
                        new ActualField(buyOrderFlag),
                        new ActualField(sellOrder.getStock()),
                        new FormalField(Integer.class)));

                //Scenarios:
                //1. The seller wants to sell more than the buyer. The buyer get to buy all the shares he/she wants. The seller makes a new order.
                //2. The buyer wants to buy more than the seller. The seller sells everything. The buyer makes a new order.
                //3. Seller and buyer wants to buy/sell the same amount.

                //We choose the smallest of the two quantities to find the max number of shares
                //that the two clients may trade.
                int min = Math.min(sellOrder.getQuantity(), buyOrder.getQuantity());

                //We find the current stock price
                StockInfo stockInfo = new StockInfo(stocks.get(new ActualField(sellOrder.getStock()), new FormalField(Integer.class)));

                //Here we send a message (to the bank?) to complete the transaction.
                transactions.put(sellOrder.getOrderedBy(), buyOrder.getOrderedBy(), stockInfo.getName(), stockInfo.getPrice(), min);
                System.out.printf("%s sold %d shares of %s to %s.", sellOrder.getOrderedBy(), min, sellOrder.getStock(), buyOrder.getOrderedBy());

                if (min < sellOrder.getQuantity()) {
                    System.out.printf("%s sold less shares than he/her wanted. Placing new sale order of %d shares of %s.", sellOrder.getOrderedBy(), sellOrder.getQuantity() - min, sellOrder.getStock());
                    marketOrders.put(new MarketOrder(
                            sellOrder.getOrderedBy(),
                            sellOrderFlag,
                            sellOrder.getStock(),
                            sellOrder.getQuantity() - min)
                            .toArray());
                } else if (min < buyOrder.getQuantity()) {
                    System.out.printf("%s bought less shares than he/her wanted. Placing new buy order of %d shares of %s.", buyOrder.getOrderedBy(), buyOrder.getQuantity() - min, buyOrder.getStock());
                    marketOrders.put(new MarketOrder(
                            buyOrder.getOrderedBy(),
                            buyOrderFlag,
                            buyOrder.getStock(),
                            buyOrder.getQuantity() - min)
                            .toArray());
                }
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

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}