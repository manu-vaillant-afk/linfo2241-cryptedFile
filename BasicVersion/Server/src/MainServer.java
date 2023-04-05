import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainServer {

    public static void main(String[] args) throws IOException {
        ServerSocket ss = null;
        Socket socket = null;

        try {
            ss = new ServerSocket(3333);
            ExecutorService executorService = Executors.newFixedThreadPool(8);

            while (true){
                System.out.println("Waiting connection");
                socket = ss.accept();
                System.out.println("Connection from: " + socket);
                ClientHandlerExecutor clientHandler = new ClientHandlerExecutor(socket);

                executorService.submit(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ss != null) ss.close();
            if (socket != null) socket.close();
        }


    }
}
