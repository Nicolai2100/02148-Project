package broker;

public class MarketOrder {

    private String id;
    protected String orderType; //BUT or SELL TODO: Skal måske være et enum, eller gøres på anden vis?
    private String orderedBy;
    private String stock; //Stock kan måske være en class for sig selv?
    private int quantity; //TODO: Vi kan eventuelt starte med, at man kun kan købe én aktie ad gangen, for et gøre det enklere..
    private String status;

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
        if (arr.length == 4) {
            orderedBy = (String) arr[0];
            orderType = (String) arr[1];
            stock = (String) arr[2];
            quantity = (Integer) arr[3];
        }
        if (arr.length == 5) {
            id = (String) arr[0];
            orderedBy = (String) arr[1];
            orderType = (String) arr[2];
            stock = (String) arr[3];
            quantity = (Integer) arr[4];
        }

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
