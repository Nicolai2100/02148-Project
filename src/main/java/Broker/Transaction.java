package Broker;

import java.util.Date;

public class Transaction {
    private String seller;
    private String buyer;
    private String stockName;
    private int price;
    private int quantity;
    private Date date;

    public Transaction(String seller, String buyer, String stockName, int price, int quantity) {
        this.seller = seller;
        this.buyer = buyer;
        this.stockName = stockName;
        this.price = price;
        this.quantity = quantity;
        date = new Date();
    }

    public Transaction(Object[] arr) {
        seller = (String) arr[0];
        buyer = (String) arr[1];
        stockName = (String) arr[2];
        price = (Integer) arr[3];
        quantity = (Integer) arr[4];
        date = new Date();
    }

    public String getSeller() {
        return seller;
    }

    public String getBuyer() {
        return buyer;
    }

    public String getStockName() {
        return stockName;
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public Date getDate() {
        return date;
    }
}
