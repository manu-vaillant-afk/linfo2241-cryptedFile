import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ClientHandlerExecutor implements Runnable {

    private Socket socket;

    public ClientHandlerExecutor(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        File decryptedFile = new File("test_file-decrypted-server-"+threadName+".pdf");
        File networkFile = new File("temp-server-"+threadName+".pdf");

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
            BruteForce bf = new BruteForce('a', 'z', request.getLengthPwd());
            int i = 0;
            while (!fileDecrypted) {
                String password = bf.next();
                //System.out.println("Try password " + password + "... : attempt nr:" + i);
                i++;
                byte[] hashBf = hashSHA1(password);
                if (Arrays.equals(hashBf, hashClient)) {
                    SecretKey serverKey = CryptoUtils.getKeyFromPassword(password);
                    CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);
                    fileDecrypted = true;
                    System.out.println("["+threadName+"] ===> find : " + password);
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
     * This function hashes a string with the SHA-1 algorithm
     *
     * @param data The string to hash
     * @return An array of 20 bytes which is the hash of the string
     */
    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return md.digest(data.getBytes());
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
