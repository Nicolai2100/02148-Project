package Broker;

import java.io.Serializable;
import java.util.*;

public class OrderPackage implements Serializable {

    private UUID clientID;
    private UUID packageID;
    private List<Order> orders = new ArrayList<>(); //TODO: Skal nok laves om, så den tager fx en supertype Order, som både kan være MarketOrder eller LimitOrder
    private List<Order> matchOrders = new ArrayList<>();

    public UUID getClientID() {
        return clientID;
    }

    public UUID getPackageID() {
        return packageID;
    }

    public void setPackageID(UUID packageID) {
        this.packageID = packageID;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public List<Order> getMatchOrders() {
        return matchOrders;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }
}
