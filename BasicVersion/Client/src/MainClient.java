import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private static String[] commonPasswords;
    private static HashMap<Integer, String> networkFilesDifficultyMap;
    private static String networkFilesFolder;
    private static int nbThread;
    private static int generatedPasswordSize;
    private static Random random;
    private static double commonPasswordRate;
    private static String outputMeasurementsFile;
    private static Writer fileWriter;
    private static double lambda;

    //Configure difficulties for the client
    private static void configureClient() {
        nbThread = 50;
        networkFilesFolder = networkFilesDifficultyMap.get(0);
        generatedPasswordSize = 5;
        commonPasswordRate = (double)1/3;
        outputMeasurementsFile = "measurements.csv";
        lambda = (double)1/20;
    }

    //Delete all pdf files in temp/ directory
    public static void cleanTempDirectory(){
        File tempDirectory = new File("temp");
        final File[] files = tempDirectory.listFiles();
        assert files != null;
        for (File f : files)
            f.delete();
    }

    //Synchronized method to write elapsed time of request to file 'outputMeasurementsFile'
    public static synchronized void writeResult(long elapsedTime){
        try {
            fileWriter.write(elapsedTime+System.lineSeparator());
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Create string array with most common passwords
    private static String[] createArrayFromMostCommonPassword() {
        System.out.println("==> Start computing array for most common used password...");
        String[] res = null;
        try (Stream<String> stream = Files.lines(Paths.get("10k-most-common_filered.txt"))) {
            List<String> list = stream.collect(Collectors.toList());

            res = new String[list.size()];
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

    /**
     * Get random file path based on network files difficulty
     * @return random network file name
     */
    public static String getNetworkFileName(){
        int randomFileNumber = random.nextInt(5 - 1 + 1)+1;
        return networkFilesFolder+"/file-"+randomFileNumber+".bin";
    }

    /**
     * Get random password to crypt file
     * Based on 'commonPasswordRate', this fuction will return a common password from the file '10k-most-common_filered.txt'
     * @return random password
     */
    public static String getRandomPassword(){
        if (Math.random() < commonPasswordRate){
            String password = "";
            //Loop until we get a password which length == generatedPasswordSize
            while (password.length() != generatedPasswordSize){
                int index = random.nextInt(commonPasswords.length - 0 + 1);
                password = commonPasswords[index];
            }
            return password;
        }

        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'

        return random.ints(leftLimit, rightLimit + 1)
                .limit(generatedPasswordSize)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    //Get random time (seconds) following and exponential distribution
    public static double exponentialNbSeconds() {
        Random rand = new Random();
        return Math.log(1 - rand.nextDouble())/(- lambda);
    }

    //Main client
    public static void main(String[] args) throws IOException {
        cleanTempDirectory();

        commonPasswords = createArrayFromMostCommonPassword();
        networkFilesDifficultyMap = new HashMap<>();
        networkFilesDifficultyMap.put(0, "Files-20KB");
        networkFilesDifficultyMap.put(1, "Files-50KB");
        networkFilesDifficultyMap.put(2, "Files-100KB");
        networkFilesDifficultyMap.put(3, "Files-5MB");
        networkFilesDifficultyMap.put(4, "Files-50MB");
        configureClient();

        fileWriter = new FileWriter(outputMeasurementsFile, false); //overwrites file

        random = new Random();
        ExecutorService executorService = Executors.newFixedThreadPool(nbThread);
        for(int i=0; i<nbThread; i++){
            ClientSendRequestExecutor clientSendRequestExecutor = new ClientSendRequestExecutor();
            executorService.submit(clientSendRequestExecutor);
        }
    }



}
