import java.io.FileInputStream;
import java.security.MessageDigest;


public class SHA1Sum
{
    static String getChecksum(String fileName)
    {
        StringBuffer buffer = new StringBuffer();
        try {

            String filePath = fileName;

            MessageDigest polynomial = MessageDigest.getInstance("SHA1");

            FileInputStream inputStream = new FileInputStream(filePath);

            byte[] pack = new byte[1024];

            int readCounter = 0;

            while ((readCounter = inputStream.read(pack)) != -1)
            {
                polynomial.update(pack, 0, readCounter);
            }

            inputStream.close();

            byte[] sum = polynomial.digest();

            // save as hex

            for (int i = 0; i < sum.length; i++)
            {
                buffer.append(Integer.toString((sum[i] & 0xff) + 0x100, 16).substring(1));
            }

        } catch (Exception e)
        {
            System.out.println("Cannot generate a checksum" + e.getMessage());
        }

        return buffer.toString();
    }
}
