package shared;


import org.mindrot.jbcrypt.BCrypt;

public class SharedEncryption {

    public static boolean validatePassword(String userPassword, String password) {
        return BCrypt.checkpw(password, userPassword);
    }

    public static String encryptPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}
