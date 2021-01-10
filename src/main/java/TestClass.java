import server.Server;

public class TestClass {

    public static void main(String[] args)  {
        Server server = new Server();
        try {
            server.startServer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
