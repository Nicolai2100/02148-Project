package model;

import java.util.ArrayList;
import java.util.HashMap;

public class Account<T> {
    //todo Kredit score - skal udregnes automatisk.
    private double balance;
    private double credit;

    private ArrayList<Stock> stockList = new ArrayList<>();
    private HashMap<String, Stock> stockMap = new HashMap<>();

    public Account(int balance, T stocks) {
        this.balance = balance;
        this.credit = 0.0;
        if (stocks instanceof ArrayList)
            this.stockList = (ArrayList<Stock>) stocks;
        else if (stocks instanceof HashMap)
            this.stockMap = (HashMap<String, Stock>) stocks;
    }

    public Account(double balance) {
        this.balance = balance;
        this.credit = 0.0;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getCredit() {
        return credit;
    }

    public void setCredit(double credit) {
        this.credit = credit;
    }

    //todo - NJL - Not the smartest?
    public T getStocks(T obj) {
        //if (stockMap.isEmpty())
        //    return (T) stockList;
        //else return (T) stockMap;
        if (obj instanceof ArrayList)
            return (T) this.stockList;
        else
            return (T) this.stockMap;
    }

    public void setStocks(T stocks) {
        if (stocks instanceof ArrayList)
            this.stockList = (ArrayList<Stock>) stocks;
        else if (stocks instanceof HashMap)
            this.stockMap = (HashMap<String, Stock>) stocks;
    }
}
