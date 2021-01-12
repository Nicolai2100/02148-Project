package Broker;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OrderPackage implements Serializable {

    private UUID id;
    private Set<MarketOrder> orders = new HashSet<>(); //TODO: Skal nok laves om, så den tager fx en supertype Order, som både kan være MarketOrder eller LimitOrder

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Set<MarketOrder> getOrders() {
        return orders;
    }

    public void setOrders(Set<MarketOrder> orders) {
        this.orders = orders;
    }
}
