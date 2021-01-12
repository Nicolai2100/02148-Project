package dao;

import model.Account;
import model.Stock;
import model.User;
import shared.SharedEncryption;

import java.util.*;

import static shared.StockNames.*;

public class FakeUserDataAccessService implements UserDao {
    private static List<User> DB;

    public static List<User> getDB() {
        return DB;
    }

    public static void setDB(List<User> DB) {
        FakeUserDataAccessService.DB = DB;
    }

    public FakeUserDataAccessService() {
        DB = new ArrayList<>();

        //todo udskift map med space

        String password = "password";

        Account aliceAccount = new Account(100);
        HashMap<String, Stock> aliceStockMap = new HashMap<>();
        aliceStockMap.put(TESLA, new Stock(TESLA, 20));
        aliceStockMap.put(APPLE, new Stock(APPLE, 31));

        aliceAccount.setStocks(aliceStockMap);
        User alice = new User("Alice", UUID.randomUUID(), SharedEncryption.encryptPassword(password), aliceAccount);
        DB.add(alice);

        Account bobAccount = new Account(100);
        HashMap<String, Stock> bobStockMap = new HashMap<>();
        bobStockMap.put(TESLA, new Stock(MICROSOFT, 20));
        bobStockMap.put(APPLE, new Stock(APPLE, 31));

        bobAccount.setStocks(bobStockMap);
        User bob = new User("Bob", UUID.randomUUID(), SharedEncryption.encryptPassword(password), bobAccount);

        DB.add(bob);

        Account charlieAccount = new Account(100);
        HashMap<String, Stock> charlieStockMap = new HashMap<>();
        charlieStockMap.put(TESLA, new Stock(GOOGLE, 20));
        charlieStockMap.put(APPLE, new Stock(MICROSOFT, 31));

        charlieAccount.setStocks(charlieStockMap);
        User charlie = new User("Charlie", UUID.randomUUID(), SharedEncryption.encryptPassword(password), charlieAccount);

        DB.add(charlie);
        
    }

    @Override
    public int insertUser(User user) {
        DB.add(user);
        return 1;
    }

    @Override
    public List<User> selectAllUsers() {
        return DB;
    }

    @Override
    public Optional<User> selectUserById(UUID id) {
        return DB.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();
    }

    public Optional<User> selectUserByUsername(String username) {
        return DB.stream()
                .filter(user -> user.getName().equals(username))
                .findFirst();
    }

    @Override
    public int delete(UUID id) {
        Optional<User> personMaybe = selectUserById(id);
        if (personMaybe.isEmpty())
            return 0;
        else
            DB.remove(personMaybe.get());
        return 1;
    }

    @Override
    public int update(UUID id, User user) {
        return selectUserById(id)
                .map(p -> {
                    int indexOfUserToUpdate = DB.indexOf(p);
                    if (indexOfUserToUpdate >= 0) {
                        DB.set(indexOfUserToUpdate, new User(user.getName(), id));
                        return 1;
                    }
                    return 0;
                }).orElse(0);
    }
}
