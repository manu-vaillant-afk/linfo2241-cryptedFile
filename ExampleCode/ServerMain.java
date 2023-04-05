import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class ServerMain {

    /**
     * @param in Stream from which to read the request
     * @return Request with information required by the server to process encrypted file
     */
    public static Request readRequest(DataInputStream in) throws IOException {
        byte [] hashPwd = new byte[20];
        int count = in.read(hashPwd,0, 20);
        if (count < 0){
            throw new IOException("Server could not read from the stream");
        }
        int pwdLength = in.readInt();
        long fileLength = in.readLong();

        return new Request(hashPwd, pwdLength, fileLength);
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        File decryptedFile = new File("test_file-decrypted-server.pdf");
        File networkFile = new File("temp-server.pdf");

        ServerSocket ss = new ServerSocket(3333);
        System.out.println("Waiting connection");
        Socket socket = ss.accept();
        System.out.println("Connection from: " + socket);

        // Stream to read request from socket
        InputStream inputStream = socket.getInputStream();
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        // Stream to write response to socket
        DataOutputStream outSocket = new DataOutputStream(socket.getOutputStream());


        // Stream to write the file to decrypt
        OutputStream outFile = new FileOutputStream(networkFile);

        Request request = readRequest(dataInputStream);
        long fileLength = request.getLengthFile();

        FileManagement.receiveFile(inputStream, outFile, fileLength);
        /*
        int readFromFile = 0;
        int bytesRead = 0;
        byte[] readBuffer = new byte[64];

        System.out.println("[Server] File length: "+ fileLength);
        while((readFromFile < fileLength)){
            bytesRead = inputStream.read(readBuffer);
            readFromFile += bytesRead;
            outFile.write(readBuffer, 0, bytesRead);
        }*/

        System.out.println("File length: " + networkFile.length());

        // HERE THE PASSWORD IS HARDCODED, YOU MUST REPLACE THAT WITH THE BRUTEFORCE PROCESS
        String password = "test";
        SecretKey serverKey = CryptoUtils.getKeyFromPassword(password);

        CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);

        // Send the decryptedFile
        InputStream inDecrypted = new FileInputStream(decryptedFile);
        outSocket.writeLong(decryptedFile.length());
        outSocket.flush();
        FileManagement.sendFile(inDecrypted, outSocket);
        /*
        int readCount;
        byte[] buffer = new byte[64];
        //read from the file and send it in the socket
        while ((readCount = inDecrypted.read(buffer)) > 0){
            outSocket.write(buffer, 0, readCount);
        }*/

        dataInputStream.close();
        inputStream.close();
        inDecrypted.close();
        outFile.close();
        socket.close();

    }
}
