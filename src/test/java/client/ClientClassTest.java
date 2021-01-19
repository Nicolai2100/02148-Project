package client;

import BeastBank.bank.ClientClass;
import BeastBank.broker.Broker;
import org.junit.jupiter.api.Test;
import BeastBank.service.AccountServiceMain;
import BeastBank.service.IdentityProvider;
import BeastBank.bank.Program;

import static BeastBank.shared.StockNames.TESLA;

class ClientClassTest {

    void setUp() {

        Runnable r4 = () -> {
            try {
                Broker.main(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread4 = new Thread(r4);
        thread4.start();

        Runnable r1 = () -> Program.main(null);
        Thread thread1 = new Thread(r1);
        thread1.start();

        Runnable r2 = () -> IdentityProvider.main(null);
        Thread thread2 = new Thread(r2);
        thread2.start();

        Runnable r3 = () -> AccountServiceMain.main(null);
        Thread thread3 = new Thread(r3);
        thread3.start();
    }

    @Test
    void linRealUser() {
//        public <T> T checkInvalidResponse(T type, T lowerLimit, T upperLimit, T userInput, String errorMsg) {

        ClientClass c = new ClientClass();
        c.checkInvalidResponse(-2.0, 1.0, -1.0, "error" );
    }

    @Test
    void loginRealUser() {
        String[] args = {"Alice", "password"};
        new ClientClass().startClient(args);
    }

    @Test
    void queryMarket() {
        String[] args = {"Alice", "password", "2"};
        new ClientClass().startClient(args);
    }

    @Test
    void sellStock() {
        String[] args = {"Alice", "password", "4", TESLA, "1", "0.0", "1"};
        new ClientClass().startClient(args);
    }

    @Test
    void buyStock() {
        String[] args = {"Bob", "password", "3", TESLA, "1", "0.0", "1"};
        new ClientClass().startClient(args);
    }

    @Test
    void buyStockAndQuery() {
        String[] args = {"Bob", "password", "1", "3", TESLA, "1", "0.0", "1", "1"};
        new ClientClass().startClient(args);
    }

    @Test
    void sellAndBuyStock() throws InterruptedException {
        String[] args = {"Alice", "password", "4", TESLA, "1", "22.2", "1", "3", TESLA, "1", "22.2", "1"};
        new ClientClass().startClient(args);
    }

    @Test
    void queryStocks() {
        String[] args = {"Alice", "password", "1"};
        new ClientClass().startClient(args);
    }


    @Test
    void loginRealUserWrongPassword() {
        String[] args = {"Alice", "passwor"};
        new ClientClass().startClient(args);
    }

    @Test
    void loginWrongUsernameAndPassword() {
        String[] args = {"Ali", "passwor"};
        new ClientClass().startClient(args);
    }
}