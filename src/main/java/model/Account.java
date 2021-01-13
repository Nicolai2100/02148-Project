package model;

import java.util.HashMap;

public class Account  {
    //todo Kredit score - skal udregnes automatisk.
    private double balance;
    private double credit;

    private HashMap<String, Stock> stockMap = new HashMap<>();

    public Account(int balance, HashMap stocks) {
        this.balance = balance;
        this.credit = 0.0;
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

    public HashMap<String, Stock> getStocks() {
        return this.stockMap;
    }

    public void setStocks(HashMap<String, Stock> stockMap) {
        this.stockMap = stockMap;
    }
}
