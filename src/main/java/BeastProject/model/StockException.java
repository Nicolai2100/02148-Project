package BeastProject.model;

public class StockException
        extends RuntimeException {
    public StockException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public StockException(String errorMessage) {
        super(errorMessage);
    }
}