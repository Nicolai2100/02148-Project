package BeastProject.model;

public class Stock implements java.io.Serializable {
    private String name;
    private double price;
    private double boughtAtValue;
    private int amount;

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Stock(String name, double boughtAtValue, int amount) {
        this.name = name;
        this.boughtAtValue = boughtAtValue;
        this.amount = amount;
    }

    public Stock(String name, int boughtAtValue, int amount) {
        this.name = name;
        this.boughtAtValue = boughtAtValue;
        this.amount = amount;
    }

    public double getBoughtAtValue() {
        return boughtAtValue;
    }

    public void setBoughtAtValue(double boughtAtValue) {
        this.boughtAtValue = boughtAtValue;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "name='" + name + '\'' +
                ", price=" + price +
                ", boughtAtValue=" + boughtAtValue +
                ", amount=" + amount +
                '}';
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
