
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileManagement {

    public static void receiveFile(InputStream inputStream, OutputStream outputStream, long fileLength) throws IOException {
        int readFromFile = 0;
        int bytesRead;
        byte[] readBuffer = new byte[64];
        while((readFromFile < fileLength)){
            bytesRead = inputStream.read(readBuffer);
            readFromFile += bytesRead;
            outputStream.write(readBuffer, 0, bytesRead);
        }
    }

    public static void sendFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        int readCount;
        byte[] buffer = new byte[64];
        //read from the file and send it in the socket
        while ((readCount = inputStream.read(buffer)) > 0){
            outputStream.write(buffer, 0, readCount);
        }
    }
}
