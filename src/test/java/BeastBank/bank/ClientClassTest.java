package BeastBank.bank;

import BeastBank.Client;
import BeastBank.broker.Broker;
import BeastBank.service.AccountServiceMain;
import BeastBank.service.IdentityProvider;
import org.junit.Test;

import static BeastBank.shared.StockNames.APPLE;
import static BeastBank.shared.StockNames.TESLA;

public class ClientClassTest {

    //@Before
    public void setUp() {
        Runnable r4 = () -> Broker.main(null);
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
    public void someMethodTest() {
        ClientClass c = new ClientClass();
        c.validateResponse(-2, 1, -1, "error");
    }

    @Test
    public void loginRealUser() {
        String[] args = {"Alice", "password"};
        Client.main(args);
    }

    @Test
    public void queryStocks() {
        String[] args = {"Alice", "password", "1"};
        Client.main(args);
    }

    @Test
    public void sellAndQueryStock() {
        String[] args = {"Alice", "password", "4", TESLA, "1", "0", "1", "4", APPLE, "1", "0.0", "1", "2"};
        Client.main(args);
    }

    @Test
    public void queryMarket() {
        String[] args = {"Alice", "password", "2"};
        Client.main(args);
    }

    @Test
    public void sellStock() {
        String[] args = {"Alice", "password", "4", TESLA, "1", "0", "1", ""};
        Client.main(args);
    }

    @Test
    public void buyStock() {
        String[] args = {"Bob", "password", "3", TESLA, "1", "0", "1", "Alice"};
        Client.main(args);
    }

    @Test
    public void buyAPPLEStock() {
        String[] args = {"Bob", "password", "3", APPLE, "1", "0", "1"};
        Client.main(args);
    }

    @Test
    public void buyStockAndQuery() {
        String[] args = {"Bob", "password", "1", "3", TESLA, "1", "0", "1", "1"};
        Client.main(args);
    }

    @Test
    public void sellAndBuyStock() throws InterruptedException {
        String[] args = {"Alice", "password", "4", TESLA, "1", "22", "1", "3", TESLA, "1", "22.2", "1"};
        Client.main(args);
    }


    @Test
    public void loginRealUserWrongPassword() {
        String[] args = {"Alice", "passwor"};
        Client.main(args);
    }

    @Test
    public void loginWrongUsernameAndPassword() {
        String[] args = {"Ali", "passwor"};
        Client.main(args);
    }
}