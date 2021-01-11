package model;

public class Stock {
    private String name;
    private double price;
    private double boughtAtValue;

    public Stock(String name, double boughtAtValue) {
        this.name = name;
        this.boughtAtValue = boughtAtValue;
    }

    public Stock(String name, int boughtAtValue) {
        this.name = name;
        this.boughtAtValue = boughtAtValue;
    }

    public Stock(Object[] arr) {
        name = (String) arr[0];
        if (arr[1] instanceof Integer)
            boughtAtValue = (Integer) arr[1];
        else
            boughtAtValue = (Double) arr[1];

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
