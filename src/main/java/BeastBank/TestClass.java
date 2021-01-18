package BeastBank;

import BeastBank.bank.Program;
import BeastBank.broker.Broker;
import BeastBank.service.AccountServiceMain;
import BeastBank.service.IdentityProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;

import BeastBank.shared.Channels;


public class TestClass {

    public static void main(String[] args) {

        try {
            String azureVMName = "Hoster";
            String hostName = InetAddress.getLocalHost().getHostName();
            String hostAddress = hostName.equals(azureVMName) ? "52.146.147.182" : "localhost";
            Channels.SERVER_HOSTNAME = hostName;
            System.out.printf("Listening on %s:%d\n", hostAddress, Channels.SERVER_PORT);


        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


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
}
