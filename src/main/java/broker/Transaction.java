package broker;

import java.util.Date;

public class Transaction {
    private String seller;
    private String buyer;
    private String stockName;
    private double price;
    private int quantity;
    private Date date;

    @Override
    public String toString() {
        return "Transaction{" +
                "seller='" + seller + '\'' +
                ", buyer='" + buyer + '\'' +
                ", stockName='" + stockName + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", date=" + date +
                '}';
    }

    public Transaction(Object[] arr) {
        seller = (String) arr[0];
        buyer = (String) arr[1];
        stockName = (String) arr[2];
        price = (Double) arr[3];
        quantity = (Integer) arr[4];
        date = new Date();
    }

    public Transaction(String seller, String buyer, String stockName, int pricePerStock, int quantity) {
        this.seller = seller;
        this.buyer = buyer;
        this.stockName = stockName;
        this.price = pricePerStock;
        this.quantity = quantity;
        this.date = new Date();
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

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public Date getDate() {
        return date;
    }
}
