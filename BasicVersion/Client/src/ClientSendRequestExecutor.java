import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class ClientSendRequestExecutor implements Runnable {
    @Override
    public void run() {
        try{
            String threadName = Thread.currentThread().getName();

            String password = MainClient.getRandomPassword();

            SecretKey keyGenerated = CryptoUtils.getKeyFromPassword(password);

            File inputFile = new File(MainClient.getNetworkFileName());
            File encryptedFile = new File("temp/test_file-encrypted-client"+threadName+".pdf");
            File decryptedClient = new File("temp/test_file-decrypted-client"+threadName+".pdf");

            // This is an example to help you create your request
            CryptoUtils.encryptFile(keyGenerated, inputFile, encryptedFile);
            System.out.println("["+threadName+"] Encrypted file length: " + encryptedFile.length() +" with password "+password);


            // Creating socket to connect to server (in this example it runs on the localhost on port 3333)
            Socket socket = new Socket("localhost", 3333);

            // For any I/O operations, a stream is needed where the data are read from or written to. Depending on
            // where the data must be sent to or received from, different kind of stream are used.
            OutputStream outSocket = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outSocket);
            InputStream inFile = new FileInputStream(encryptedFile);
            DataInputStream inSocket = new DataInputStream(socket.getInputStream());


            // SEND THE PROCESSING INFORMATION AND FILE
            byte[] hashPwd = MainClient.hashSHA1(password);
            int pwdLength = password.length();
            long fileLength = encryptedFile.length();
            long startRequest = System.currentTimeMillis();
            MainClient.sendRequest(out, hashPwd, pwdLength, fileLength);
            out.flush();

            FileManagement.sendFile(inFile, out);

            // GET THE RESPONSE FROM THE SERVER
            OutputStream outFile = new FileOutputStream(decryptedClient);
            long fileLengthServer = inSocket.readLong();
            System.out.println("["+threadName+"] Get response :Length from the server: "+ fileLengthServer);
            FileManagement.receiveFile(inSocket, outFile, fileLengthServer);
            long receiveResponse = System.currentTimeMillis();
            long elapsedTime = receiveResponse - startRequest;
            MainClient.writeResult(elapsedTime);

            out.close();
            outSocket.close();
            outFile.close();
            inFile.close();
            inSocket.close();
            socket.close();

            encryptedFile.delete();
            decryptedClient.delete();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                NoSuchPaddingException | IllegalBlockSizeException | IOException | BadPaddingException |
                InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}
