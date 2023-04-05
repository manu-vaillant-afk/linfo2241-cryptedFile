import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ClientHandlerExecutor implements Runnable {

    private Socket socket;
    private HashMap<String, String> hashMapPasswordHash;

    public ClientHandlerExecutor(Socket socket, HashMap<String, String> hashMapPasswordHash){
        this.socket = socket;
        this.hashMapPasswordHash = hashMapPasswordHash;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        File decryptedFile = new File("temp/test_file-decrypted-server-"+threadName+".pdf");
        File networkFile = new File("temp/temp-server-"+threadName+".pdf");
        try{
            // Stream to read request from socket
            InputStream inputStream = null;
            inputStream = socket.getInputStream();

            DataInputStream dataInputStream = new DataInputStream(inputStream);
            // Stream to write response to socket
            DataOutputStream outSocket = null;
            outSocket = new DataOutputStream(socket.getOutputStream());

            // Stream to write the file to decrypt
            OutputStream outFile = new FileOutputStream(networkFile);

            Request request = readRequest(dataInputStream);
            long fileLength = request.getLengthFile();
            byte[] hashClient = request.getHashPassword();

            FileManagement.receiveFile(inputStream, outFile, fileLength);

            System.out.println("["+threadName+"] File length: " + networkFile.length());

            // HERE THE PASSWORD IS HARDCODED, YOU MUST REPLACE THAT WITH THE BRUTEFORCE PROCESS
            boolean fileDecrypted = false;

            // If optimization is enabled, we first search if the password is in the hashMap
            String passwordFromHashMap = "";
            boolean findPassword = false;
            if(hashMapPasswordHash != null){
                passwordFromHashMap = hashMapPasswordHash.get(new String(hashClient, StandardCharsets.UTF_8));
                if(passwordFromHashMap != null) {
                    findPassword = true;
                    System.out.println(("["+threadName+"] ===> Find password with '10k-most-common_filered.txt' file : "+passwordFromHashMap));
                }
            }

            //If optimization is enabled and we've found the password in the hashMap
            if (findPassword){
                SecretKey serverKey = CryptoUtils.getKeyFromPassword(passwordFromHashMap);
                CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);
            }
            else{
                BruteForce bf = new BruteForce('a', 'z', request.getLengthPwd());
                int i = 0;
                while (!fileDecrypted) {
                    String password = bf.next();
                    //System.out.println("Try password " + password + "... : attempt nr:" + i);
                    i++;
                    byte[] hashBf = CryptoUtils.hashSHA1(password);
                    if (Arrays.equals(hashBf, hashClient)) {
                        SecretKey serverKey = CryptoUtils.getKeyFromPassword(password);
                        CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);
                        fileDecrypted = true;
                        System.out.println("["+threadName+"] ===> Find password with BF : " + password);
                    }
                }
            }

            // Send the decryptedFile
            InputStream inDecrypted = new FileInputStream(decryptedFile);
            outSocket.writeLong(decryptedFile.length());
            outSocket.flush();
            FileManagement.sendFile(inDecrypted, outSocket);

            dataInputStream.close();
            inputStream.close();
            inDecrypted.close();
            outFile.close();

            //Delete both files
            boolean decryptedDelete = decryptedFile.delete();
            boolean networkDelete = networkFile.delete();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | BadPaddingException
                | InvalidKeyException | NoSuchPaddingException | IOException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param in Stream from which to read the request
     * @return Request with information required by the server to process encrypted file
     */
    public static Request readRequest(DataInputStream in) throws IOException {
        byte[] hashPwd = new byte[20];
        int count = in.read(hashPwd, 0, 20);
        if (count < 0) {
            throw new IOException("Server could not read from the stream");
        }
        int pwdLength = in.readInt();
        long fileLength = in.readLong();

        return new Request(hashPwd, pwdLength, fileLength);
    }
}
