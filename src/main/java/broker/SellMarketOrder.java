package broker;

public class SellMarketOrder extends MarketOrder {
    public SellMarketOrder(String orderedBy, String stock, int quantity) {
        super(orderedBy, stock, quantity);
    }

    public SellMarketOrder(Object[] arr) {
        super(arr);
        this.orderType = "SELL";
    }
}
