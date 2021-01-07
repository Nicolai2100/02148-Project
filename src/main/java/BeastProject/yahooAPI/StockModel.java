package BeastProject.yahooAPI;

import java.util.Date;

public class StockModel {
    private Date date;
    private int volume;
    private double high;
    private double low;
    private double adjclose;
    private double close;
    private double open;

    public StockModel(Date date, int volume, double high, double low, double adjclose, double close, double open) {
        this.date = date;
        this.volume = volume;
        this.high = high;
        this.low = low;
        this.adjclose = adjclose;
        this.close = close;
        this.open = open;
    }
}
