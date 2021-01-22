package BeastBank.broker;

import org.jspace.SequentialSpace;

import java.util.List;

public class Broker_eksempel {


/*

    public static void main(String[] args) {
        SequentialSpace p0, p1, p2, p3, p4, p5, p6, p7, p8;

        //Client sends an order package:
        p0.put(pkg);

        //Receive new order package:
        while(true) {
            pkg = p0.get(pkg);
            p1.put(pkg);

            List<UUID> packagesToNotify;
            OrderPackage[] waitingPackages = p5.queryAll(pkg);
            for (OrderPackage pkg : waitingPackages)
                if (some conditions here) packagesToNotify.add(pkg.getID());

            if (packagesToNotify.size != 0) {
                p6.put(packagesToNotify, "notDoneYet");
            } else {
                p6.put(packagesToNotify, "done");
            }
        }

        //Get next package and lock:
        while(true) {
            pkg = p1.get(pkg);
            p2.get("lock");
            p3.put(pkg);
        }

        //Try to find enough matches:
        while(true) {
            pkg = p3.get(pkg);
            //Business logic that tries to find enough matches
            //for the orders in the package.
            //Puts a boolean with the result along with the package
            //in the next space, p4.
            p4.put(pkg, enoughMatchesFound);
        }

        //Remove orders and signal bank to make transactions
        while(true) {
            //Only gets a package from p4 if the boolean in the tuple is true.
            p4.get(pkg, true);
            //Signal bank to make transactions here.
            p2.put("lock");
        }

        //Signal waiting for notification to go back in queue
        while(true) {
            //Only gets a package from p4 if the boolean in the tuple is false.
            pkg = p4.get(pkg, false)[0];
            p5.put(pkg, pkg.getID());
            p2.put("lock");
        }

        //Notify waiting package
        while(true) {
            List<UUID> ids = p6.get(packagesToNotify, "notDoneYet")[0];
            OrderPackage idOfpkgToNotify = ids.remove(0);
            p7.put(idOfpkgToNotify);
            if (packagesToNotify.size() == 0)
                p6.put()
        }

        //No waiting packages to notify
        while(true) {
            p6.get(packagesToNotify, "done");
        }

        //Put package back in queue
        while (true) {
            p7.get(idOfpkgToNotify);
            pkg = p5.get(pkg, idOfpkgToNotify)[0];
            p1.put(pkg);
        }
    }
*/

}
