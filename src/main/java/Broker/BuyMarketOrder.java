package Broker;

public class BuyMarketOrder extends MarketOrder {
    public BuyMarketOrder(String orderedBy, String stock, int quantity) {
        super(orderedBy, stock, quantity);
    }

    public BuyMarketOrder(Object[] arr) {
        super(arr);
    }
}
