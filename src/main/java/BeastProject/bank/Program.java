package BeastProject.bank;

public class Program {

    public static void main(String[] args) {
        try {
            new Server().startServer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
