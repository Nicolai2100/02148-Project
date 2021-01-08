package model;

public class StockInfo {

    private String name;
    private int price;

    public StockInfo(Object[] arr) {
        name = (String) arr[0];
        price = (Integer) arr[1];
    }

    public StockInfo(String name, int price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
