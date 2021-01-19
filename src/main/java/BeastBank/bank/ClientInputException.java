package BeastBank.bank;

public class ClientInputException extends RuntimeException {

    public ClientInputException(String errorMessage) {
        super(errorMessage);
    }
}
