
public class Request {

    private byte[] hashPwd;
    private int pwdLength;
    private long fileLength;

    public Request (byte[] hashPwd, int pwdLength, long fileLength){
        this.hashPwd = hashPwd;
        this.pwdLength= pwdLength;
        this.fileLength = fileLength;
    }

    public byte[] getHashPassword() {
        return hashPwd;
    }


    public int getLengthPwd() {
        return pwdLength;
    }

    public long getLengthFile() {
        return fileLength;
    }
}
