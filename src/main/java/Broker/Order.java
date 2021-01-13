package Broker;

import java.io.Serializable;
import java.util.UUID;

import static Broker.Broker.buyOrderFlag;
import static Broker.Broker.sellOrderFlag;

public class Order implements Serializable {

    private UUID id;
    protected String orderType; //BUT or SELL TODO: Skal måske være et enum, eller gøres på anden vis?
    private String orderedBy;
    private String stock; //Stock kan måske være en class for sig selv?
    private int quantity;
    private int minQuantity;

    final static String allFlag = "ALL";
    final static String mostFlag = "MOST";

    public Order(String orderedBy, String stock, int quantity) {
        this.orderedBy = orderedBy;
        this.stock = stock;
        this.quantity = quantity;
    }

    public Order(String orderType, String orderedBy, String stock, int quantity, int minQuantity) {
        this.orderType = orderType;
        this.orderedBy = orderedBy;
        this.stock = stock;
        this.quantity = quantity;
        this.minQuantity = minQuantity;
    }

    public Order(Object[] arr) {
        if (arr.length == 5) {
            orderedBy = (String) arr[0];
            orderType = (String) arr[1];
            stock = (String) arr[2];
            quantity = (Integer) arr[3];
            //allOrNothing = (Boolean) arr[4];
            minQuantity = (Integer) arr[4];
        }
        if (arr.length == 6) {
            id = (UUID) arr[0];
            orderedBy = (String) arr[1];
            orderType = (String) arr[2];
            stock = (String) arr[3];
            quantity = (Integer) arr[4];
            //allOrNothing = (Boolean) arr[5];
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
}
