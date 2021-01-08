package Broker;

public abstract class MarketOrder {

    private String orderedBy;
    private String stock; //Stock kan måske være en class for sig selv?
    private int quantity; //TODO: Vi kan eventuelt starte med, at man kun kan købe én aktie ad gangen, for et gøre det enklere..

    public MarketOrder(String orderedBy, String stock, int quantity) {
        this.orderedBy = orderedBy;
        this.stock = stock;
        this.quantity = quantity;
    }

    public MarketOrder(Object[] arr) {
        orderedBy = (String) arr[0];
        stock = (String) arr[2];
        quantity = (Integer) arr[3];
    }

    public String getOrderedBy() {
        return orderedBy;
    }

    public void setOrderedBy(String orderedBy) {
        this.orderedBy = orderedBy;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
