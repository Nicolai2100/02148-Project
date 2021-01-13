package Broker;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OrderPackage implements Serializable {

    private UUID clientID;
    private UUID packageID;
    private Set<Order> orders = new HashSet<>(); //TODO: Skal nok laves om, så den tager fx en supertype Order, som både kan være MarketOrder eller LimitOrder
    private Set<Order> matchOrders = new HashSet<>();

    public UUID getClientID() {
        return clientID;
    }

    public UUID getPackageID() {
        return packageID;
    }

    public void setPackageID(UUID packageID) {
        this.packageID = packageID;
    }

    public Set<Order> getOrders() {
        return orders;
    }

    public void setOrders(Set<Order> orders) {
        this.orders = orders;
    }

    public Set<Order> getMatchOrders() {
        return matchOrders;
    }
}
