package Broker;

public class MarketOrder {

    protected String orderType; //BUT or SELL TODO: Skal måske være et enum, eller gøres på anden vis?
    private String orderedBy;
    private String stock; //Stock kan måske være en class for sig selv?
    private int quantity; //TODO: Vi kan eventuelt starte med, at man kun kan købe én aktie ad gangen, for et gøre det enklere..

    final static String allFlag = "ALL";
    final static String mostFlag = "MOST";

    public MarketOrder(String orderedBy, String stock, int quantity) {
        this.orderedBy = orderedBy;
        this.stock = stock;
        this.quantity = quantity;
    }

    public MarketOrder(String orderedBy, String orderType, String stock, int quantity) {
        this.orderedBy = orderedBy;
        this.orderType = orderType;
        this.stock = stock;
        this.quantity = quantity;
    }

    public MarketOrder(Object[] arr) {
        orderedBy = (String) arr[0];
        orderType = (String) arr[1];
        stock = (String) arr[2];
        quantity = (Integer) arr[3];
    }

    public String getOrderedBy() {
        return orderedBy;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getStock() {
        return stock;
    }

    public int getQuantity() {
        return quantity;
    }

    public Object[] toArray() {
        return new Object[]{orderedBy, orderType, stock, quantity};
    }

}
