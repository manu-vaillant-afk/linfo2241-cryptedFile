import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MainClient {

    /**
     * This function hashes a string with the SHA-1 algorithm
     * @param data The string to hash
     * @return An array of 20 bytes which is the hash of the string
     */
    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return md.digest(data.getBytes());
    }

    /**
     * This function is used by a client to send the information needed by the server to process the file
     * @param out Socket stream connected to the server where the data are written
     * @param hashPwd SHA-1 hash of the password used to derive the key of the encryption
     * @param pwdLength Length of the clear password
     * @param fileLength Length of the encrypted file
     */
    public static void sendRequest(DataOutputStream out, byte[] hashPwd, int pwdLength,
                                   long fileLength) throws IOException {
        out.write(hashPwd,0, 20);
        out.writeInt(pwdLength);
        out.writeLong(fileLength);
    }

    public static void main(String[] args) {
        try{
            String[] commonPasswords = createArrayFromMostCommonPassword();

            String password = "hardcore";

            SecretKey keyGenerated = CryptoUtils.getKeyFromPassword(password);

            File inputFile = new File("src/test_file.pdf");
            File encryptedFile = new File("test_file-encrypted-client.pdf");
            File decryptedClient = new File("test_file-decrypted-client.pdf");

            // This is an example to help you create your request
            CryptoUtils.encryptFile(keyGenerated, inputFile, encryptedFile);
            System.out.println("Encrypted file length: " + encryptedFile.length());


            // Creating socket to connect to server (in this example it runs on the localhost on port 3333)
            Socket socket = new Socket("localhost", 3333);

            // For any I/O operations, a stream is needed where the data are read from or written to. Depending on
            // where the data must be sent to or received from, different kind of stream are used.
            OutputStream outSocket = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outSocket);
            InputStream inFile = new FileInputStream(encryptedFile);
            DataInputStream inSocket = new DataInputStream(socket.getInputStream());


            // SEND THE PROCESSING INFORMATION AND FILE
            byte[] hashPwd = hashSHA1(password);
            int pwdLength = password.length();
            long fileLength = encryptedFile.length();
            sendRequest(out, hashPwd, pwdLength, fileLength);
            out.flush();

            FileManagement.sendFile(inFile, out);

            // GET THE RESPONSE FROM THE SERVER
            OutputStream outFile = new FileOutputStream(decryptedClient);
            long fileLengthServer = inSocket.readLong();
            System.out.println("Length from the server: "+ fileLengthServer);
            FileManagement.receiveFile(inSocket, outFile, fileLengthServer);

            out.close();
            outSocket.close();
            outFile.close();
            inFile.close();
            inSocket.close();
            socket.close();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                NoSuchPaddingException | IllegalBlockSizeException | IOException | BadPaddingException |
                InvalidKeyException e) {
            e.printStackTrace();
        }

    }

    //Create string array with most common passwords
    private static String[] createArrayFromMostCommonPassword() {
        System.out.println("==> Start computing array for most common used password...");
        String[] res = null;
        try (Stream<String> stream = Files.lines(Paths.get("10k-most-common_filered.txt"))) {
            List<String> list = stream.collect(Collectors.toList());

            res = new String[(int) list.size()];
            int cpt=0;
            for(String password : list){
                res[cpt] = password;
                cpt++;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        System.out.println("==> End computing array for most common used password");
        return res;
    }
}
