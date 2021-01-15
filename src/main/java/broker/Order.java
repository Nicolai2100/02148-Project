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

    //TODO: Builder pattern?
    private Order(String orderType, String orderedBy, String stock, int quantity, int minQuantity, int limit, String clientMatch) {
        this.orderType = orderType;
        this.orderedBy = orderedBy;
        this.stock = stock;
        this.quantity = quantity;
        this.minQuantity = minQuantity;
        this.limit = limit;
        this.clientMatch = clientMatch;
    }

    public Order(Object[] arr) {
        if (arr.length == 7) {
            orderedBy = (String) arr[0];
            orderType = (String) arr[1];
            stock = (String) arr[2];
            quantity = (Integer) arr[3];
            minQuantity = (Integer) arr[4];
            limit = (Integer) arr[5];
            clientMatch = (String) arr[6];
        }
        if (arr.length == 8) {
            id = (UUID) arr[0];
            orderedBy = (String) arr[1];
            orderType = (String) arr[2];
            stock = (String) arr[3];
            quantity = (Integer) arr[4];
            minQuantity = (Integer) arr[5];
            limit = (Integer) arr[6];
            clientMatch = (String) arr[7];
        }

    }

    public static class OrderBuilder {
        private String orderType;
        private String orderedBy;
        private String stock;
        private int quantity = 1; //TODO: For now, we set 1 as the default.
        private int minQuantity = -1; //If still -1 when build, we set it equal to the quantity.
        private int limit = -1; //We use the value -1 to indicate, that there is no limit (a "market order");
        private String clientMatch = anyFlag; //The default is that we want to buy/sell from/to anyone.

        public OrderBuilder orderType(String orderType) {
            if (!orderType.equals(sellOrderFlag) && !orderType.equals(buyOrderFlag)) throw new IllegalArgumentException();
            this.orderType = orderType;
            return this;
        }

        public OrderBuilder orderedBy(String orderedBy) {
            if (orderedBy == null) throw new IllegalArgumentException();
            this.orderedBy = orderedBy;
            return this;
        }

        public OrderBuilder stock(String stock) {
            if (stock == null) throw new IllegalArgumentException();
            this.stock = stock;
            return this;
        }

        public OrderBuilder quantity(int quantity) {
            if (quantity < 1) throw new IllegalArgumentException();
            this.quantity = quantity;
            return this;
        }

        public OrderBuilder minQuantity(int minQuantity) {
            if (quantity < 1) throw new IllegalArgumentException();
            this.minQuantity = minQuantity;
            return this;
        }

        public OrderBuilder limit(int limit) {
            if (quantity < -1) throw new IllegalArgumentException();
            this.limit = limit;
            return this;
        }

        public OrderBuilder clientMatch(String clientMatch) {
            if (clientMatch == null) throw new IllegalArgumentException();
            this.clientMatch = clientMatch;
            return this;
        }

        public OrderBuilder sell() {
            this.orderType = sellOrderFlag;
            return this;
        }

        public OrderBuilder buy() {
            this.orderType = buyOrderFlag;
            return this;
        }

        public Order build() throws Exception {
            if (orderType == null)
                throw new Exception("Tried to build an order without providing an order type.");
            if (orderedBy == null)
                throw new Exception("Tried to build an order without providing an ID for who ordered it.");
            if (stock == null)
                throw new Exception("Tried to build an order without providing a name for the stock.");

            //If the minQuantity has not been set, set it to the default (equal to quantity).
            if (minQuantity == -1) {
                minQuantity = quantity;
            }
            //If somehow the minimum quantity is set to higher than the quantity, set it equal to the quantity.
            if (minQuantity > quantity)
                minQuantity = quantity;

            return new Order(orderType, orderedBy, stock, quantity, minQuantity, limit, clientMatch);
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
        if (limit == -1) return true; //means that it is a market order, in which case we don't care for the limit.
        if (orderType.equals(sellOrderFlag)) {
            return price >= limit;
        } else {
            return price <= limit;
        }
    }
}
