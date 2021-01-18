package BeastBank.yahooAPI;

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

    public Date getDate() { return date; }

    public double getClose() { return close; }

    public double getHigh() { return high; }

    public double getAdjclose() { return adjclose; }

    public int getVolume() { return volume; }

    public double getLow() { return low; }

    public double getOpen() { return open; }

    public void setAdjclose(double adjclose) { this.adjclose = adjclose; }

    public void setDate(Date date) { this.date = date; }

    public void setClose(double close) { this.close = close; }

    public void setHigh(double high) { this.high = high; }

    public void setLow(double low) { this.low = low; }

    public void setOpen(double open) { this.open = open; }

    public void setVolume(int volume) { this.volume = volume; }
}
