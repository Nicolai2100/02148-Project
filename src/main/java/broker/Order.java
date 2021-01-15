package broker;

import java.io.Serializable;
import java.util.UUID;

import static broker.Broker.buyOrderFlag;
import static broker.Broker.sellOrderFlag;


public class Order implements Serializable {

    private UUID id;
    protected String orderType; //BUT or SELL TODO: Skal måske være et enum, eller gøres på anden vis?
    private String orderedBy;
    private String stock; //Stock kan måske være en class for sig selv?
    private int quantity;
    private int minQuantity;

    //If the order is a SELL order, it should not sell for under this limit.
    //If the order is a BUY order, it should not buy for over this limit.
    private int limit;

    //If clientMatch is ANY, sell/buy to/from any other client.
    //If clientMatch is something else, only sell/buy to/from that client.
    private String clientMatch;
    public static final String anyFlag = "ANY";

    public Order(String orderedBy, String stock, int quantity) {
        this.orderedBy = orderedBy;
        this.stock = stock;
        this.quantity = quantity;
    }

    //TODO: Builder pattern?
    public Order(String orderType, String orderedBy, String stock, int quantity, int minQuantity) {
        this.orderType = orderType;
        this.orderedBy = orderedBy;
        this.stock = stock;
        this.quantity = quantity;
        this.minQuantity = minQuantity;
        this.limit = -1;
        this.clientMatch = anyFlag;
    }

    public Order(String orderType, String orderedBy, String stock, int quantity, int minQuantity, int limit) {
        this.orderType = orderType;
        this.orderedBy = orderedBy;
        this.stock = stock;
        this.quantity = quantity;
        this.minQuantity = minQuantity;
        this.limit = limit;
    }

    public Order(Object[] arr) {
        if (arr.length == 5) {
            orderedBy = (String) arr[0];
            orderType = (String) arr[1];
            stock = (String) arr[2];
            quantity = (Integer) arr[3];
            minQuantity = (Integer) arr[4];
        }
        if (arr.length == 6) {
            id = (UUID) arr[0];
            orderedBy = (String) arr[1];
            orderType = (String) arr[2];
            stock = (String) arr[3];
            quantity = (Integer) arr[4];
            minQuantity = (Integer) arr[5];
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMatchingOrderType() {
        return orderType.equals(sellOrderFlag) ? buyOrderFlag : sellOrderFlag;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public int getLimit() {
        return limit;
    }

    public String getClientMatch() {
        return clientMatch;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderType='" + orderType + '\'' +
                ", orderedBy='" + orderedBy + '\'' +
                ", stock='" + stock + '\'' +
                ", quantity=" + quantity +
                ", minQuantity=" + minQuantity +
                '}';
    }

    public boolean isOverOrUnderLimit(int price) {
        if (orderType.equals(sellOrderFlag)) {
            return price >= limit;
        } else {
            return price <= limit;
        }
    }
}
