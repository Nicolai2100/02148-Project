package BeastBank;

import BeastBank.bank.ClientClass;

public class Client {
    public static void main(String[] args) {
        /**
         * Toggle remoteServer true/false for connecting to localhost or remote server...
         */
        boolean remoteServer = false;

        String[] extendedArgs = new String[args.length + 1];
        extendedArgs[0] = Boolean.toString(remoteServer);

        for (int i = 1; i < extendedArgs.length; i++) {
            extendedArgs[i] = args[i - 1];
        }
        new ClientClass().startClient(extendedArgs);
    }
}
