package BeastBank.service;

import BeastBank.bank.Program;
import BeastBank.dao.FakeUserDataAccessService;
import BeastBank.model.User;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static BeastBank.shared.StockNames.TESLA;

public class AccountServiceTest {

    @Before
    public void setUp() throws InterruptedException {
    /*    Runnable r4 = () -> {
            try {
                Broker.main(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread4 = new Thread(r4);
        thread4.start();*/


        /*Runnable r3 = () -> AccountServiceMain.main(null);
        Thread accountService = new Thread(r3);
        accountService.start();*/

        Runnable r1 = () -> Program.main(null);
        Thread program = new Thread(r1);
        program.start();
    }

    @Test
    public void makeTransaction() {
        AccountService accountService = new AccountService();

        User alice = FakeUserDataAccessService.getInstance().selectUserByUsername("Alice").get();
        User bob = FakeUserDataAccessService.getInstance().selectUserByUsername("Bob").get();

        assertEquals(alice.getAccount().getStocks().get(TESLA).getAmount(), 2);
        //He have 1 to begin with
        assertEquals(bob.getAccount().getStocks().get(TESLA).getAmount(), 1);

        accountService.makeTransaction(TESLA, 1, alice, bob, 22.2);

        assertEquals(alice.getAccount().getStocks().get(TESLA).getAmount(), 1);
        assertEquals(bob.getAccount().getStocks().get(TESLA).getAmount(), 2);
    }

    @Test
    public void makeWrongTransaction() {
        AccountService accountService = new AccountService();

        User alice = FakeUserDataAccessService.getInstance().selectUserByUsername("Alice").get();
        User bob = FakeUserDataAccessService.getInstance().selectUserByUsername("Bob").get();

        assertEquals(alice.getAccount().getStocks().get(TESLA).getAmount(), 2);
        //He have 1 to begin with
        assertEquals(bob.getAccount().getStocks().get(TESLA).getAmount(), 1);

        accountService.makeTransaction(TESLA, -1, alice, bob, 22.2);

        assertEquals(alice.getAccount().getStocks().get(TESLA).getAmount(), 1);
        assertEquals(bob.getAccount().getStocks().get(TESLA).getAmount(), 2);
    }
}