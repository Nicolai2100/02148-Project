package model;

import java.util.HashMap;
import java.util.Optional;

public class Account {
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

    public Stock withDrawStock(String stockName, int amount) {
        if (amount < 1) {
            String throwStr = String.format("Amount is too low!");
            throw new StockException(throwStr);
        }
        if (this.stockMap.containsKey(stockName)) {
            Stock stockToWithdraw = this.stockMap.remove(stockName);

            if (stockToWithdraw.getAmount() < amount) {
                String throwStr = String.format("Requested amount %d is greater than sender", amount);
                throw new StockException(throwStr);
            } else if (stockToWithdraw.getAmount() == amount) {
                return stockToWithdraw;
            } else // (stockToWithdraw.getAmount() > amount)
            {
                int numOfStock = stockToWithdraw.getAmount();
                int numOfStockToWithDraw = numOfStock - amount;
                int numOfStockToKeep = numOfStock - numOfStockToWithDraw;

                stockToWithdraw.setAmount(numOfStockToKeep);
                this.stockMap.put(stockName, stockToWithdraw);

                stockToWithdraw.setAmount(numOfStockToWithDraw);
                return stockToWithdraw;
            }
        }
        String throwStr = String.format("Sender does not have requested stock...");
        throw new StockException(throwStr);
    }

    public void insertStock(Stock stock) {
        if (validStock(stock))
            if (this.stockMap.containsKey(stock.getName())) {
                Stock stockToUpdate = this.stockMap.remove(stock.getName());
                stockToUpdate.setAmount(stockToUpdate.getAmount() + stock.getAmount());
                this.stockMap.put(stock.getName(), stockToUpdate);
            } else {
                this.stockMap.put(stock.getName(), stock);
            }
    }

    private boolean validStock(Stock stock) {
        if (stock.getAmount() < 1)
            throw new StockException("Amount too low!");
        if (stock.getName().length() < 1)
            throw new StockException("Name too short!");

        return true;
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
