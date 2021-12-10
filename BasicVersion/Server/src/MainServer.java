import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class MainServer {


    public static void main(String[] args) throws IOException {
        ServerSocket ss = null;
        Socket socket = null;
        boolean optimizePerformance = true;

        try {
            ss = new ServerSocket(3333);
            ExecutorService executorService = Executors.newFixedThreadPool(8);
            HashMap<String, String> hashMapPasswordHash = null;
            if(optimizePerformance)
                hashMapPasswordHash = createHashMapFromMostCommonPassword();

            while (true){
                System.out.println("Waiting connection");
                socket = ss.accept();
                System.out.println("Connection from: " + socket);
                ClientHandlerExecutor clientHandler = new ClientHandlerExecutor(socket, hashMapPasswordHash);

                executorService.submit(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ss != null) ss.close();
            if (socket != null) socket.close();
        }


    }

    private static HashMap<String, String> createHashMapFromMostCommonPassword() {
        System.out.println("==> Start computing hashMap for most common used password...");
        HashMap<String, String> hashMap = new HashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get("10k-most-common_filered.txt"))) {
            stream.forEach(password -> {
                try {
                    hashMap.put(new String(CryptoUtils.hashSHA1(password), StandardCharsets.UTF_8), password);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }
        System.out.println("==> End computing hashMap for most common used password");
        size(hashMap);
        return hashMap;
    }

    public static void size(Map map) {
        try{
            System.out.println("Index Size: " + map.size());
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            ObjectOutputStream oos=new ObjectOutputStream(baos);
            oos.writeObject(map);
            oos.close();
            System.out.println("Data Size: " + baos.size());
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
