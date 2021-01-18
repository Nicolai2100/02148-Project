package BeastBank.broker;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.PileSpace;
import org.jspace.SpaceRepository;

public class LockService {

    /**
     * The lock BeastProject.service is designed to keep efficency tip top. It creates locks for each stock that each locks a small
     * part of the buy/sell orders in the stock. It also keeps track of which number the next entry into a stock should be,
     * and helps the BeastProject.broker with providing numbers for new keys if they are needed.
     */

    static final int maxLockShares = 10;
    SpaceRepository nextEntry = new SpaceRepository();

    /**
     * A little demonstration of how to use this.
     * @param args
     */
    public static void main (String[] args) {
        LockService lockService = new LockService();
        for (int i = 0; i < 22; i++) {
            // Insertorder should be called on this whenever you insert an order into the market space.
            newLock lock = lockService.insertOrder("AAPL");
            System.out.println("This num is to be appended to the inserted order: " + lock.num + " and this bool is whather or not " +
                    "a new key should be inserted: " + lock.newKeyNum);
        }
        for (int i = 0; i < 5; i++) {
            // Delete order takes the int which comes back from a get request on a order, to know which "subsection" needs to have new things added.
            // Call this whenever an order is taken out.
            lockService.deleteOrder("AAPL", 1);
            System.out.println("Delete order puts deleted  numbers into a stack, such that the system knows which subsections are incomplete");
        }
        newLock lock = lockService.insertOrder("AAPL");
        System.out.println("Added into subsection: " + lock.num + " instead of 2 because earlier entrys had been removed");
    }

    /**
     * Insert order is called whenever a order is insert into the trade repository. It takes the stock and then it generate
     * the int needed to enumerate what lock this stock belongs to.
     * @param stock
     * @return newLock returns the number of the next entry, which is the same number to be added as the key if the boolean is true.
     */
    public newLock insertOrder(String stock) {
        Object[] object = null;
        // This checks if this is the very first time a certain stock is queried, if so it adds the next set of numbers.
        if (nextEntry.get(stock) == null) {
            PileSpace pileSpace = new PileSpace();
            try {
                pileSpace.put("nextNumber", 1);
                for (int i = 0; i < maxLockShares - 1; i++) {
                    pileSpace.put(0);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nextEntry.add(stock, pileSpace);
            return new newLock(0, true);
        }
        try {
            // If there has been put in more orders than has been taken out, we put in the numbers for the next key.
            if (nextEntry.get(stock).queryp(new FormalField(Integer.class)) == null) {
                Object[] nextNum = nextEntry.get(stock).get(new ActualField("nextNumber"), new FormalField(Integer.class));
                nextEntry.get(stock).put("nextNumber", (int) nextNum[1]+1);
                // We put in numbers to the stack, such that we know where to put the next tuples.
                for (int i = 0; i < maxLockShares - 1; i++) {
                    nextEntry.get(stock).put(nextNum[1]);
                }
                return new newLock((int) nextNum[1],true);
            }
            object = nextEntry.get(stock).get(new FormalField(Integer.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // It will hopefully be different from null because we query the field to find out if it's there.
        return new newLock((int) object[0], false);
    }

    public void deleteOrder(String stock, int num) {
        try {
            nextEntry.get(stock).put(num);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    // i needed to return two datatypes, so i had to create an object. 
    public class newLock {
        int num;
        boolean newKeyNum;
        public newLock (int num, boolean newKeyNum) {
            this.num = num;
            this.newKeyNum = newKeyNum;
        }
    }
}
