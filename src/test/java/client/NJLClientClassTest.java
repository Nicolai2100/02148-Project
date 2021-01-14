package client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.AccountServiceMain;
import service.IdentityProvider;
import broker.Broker;
import bank.Program;

class NJLClientClassTest {

    @BeforeEach
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
    void queryMarket() {
        String[] args = {"Alice", "password", "2"};
        new NJLClientClass().startClient(args);
    }

    @Test
    void queryStocks() {
        String[] args = {"Alice", "password", "1"};
        new NJLClientClass().startClient(args);
    }

    @Test
    void buyStock() {
        String[] args = {"Alice", "password", "3"};
        new NJLClientClass().startClient(args);
    }

    @Test
    void sellStock() {
        String[] args = {"Alice", "password", "4", "Tesla", "2"};
        new NJLClientClass().startClient(args);
    }

    @Test
    void loginRealUser() {
        String[] args = {"Alice", "password"};
        new NJLClientClass().startClient(args);
    }

    @Test
    void loginRealUserWrongPassword() {
        String[] args = {"Alice", "passwor"};
        new NJLClientClass().startClient(args);
    }

    @Test
    void loginWrongUsernameAndPassword() {
        String[] args = {"Ali", "passwor"};
        new NJLClientClass().startClient(args);
    }
}