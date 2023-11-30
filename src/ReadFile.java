import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class ReadFile {
    public static LinkedHashMap<String, String> readCommonInfo(String path)
    {

        LinkedHashMap<String, String> commonInfo = new LinkedHashMap<String, String>();

        try {
            File commonFile = new File(path);
            Scanner reader = new Scanner(commonFile);

            while(reader.hasNextLine())
            {
                String data = reader.nextLine();
                String[] dataArr = data.split(" ");

                commonInfo.put(dataArr[0], dataArr[1]);
            }
        }
        catch (FileNotFoundException e)
        {
            System.out.println("File not found");
        }

        return commonInfo;
    }

    public static LinkedHashMap<Integer, String[]> readPeerInfo(String path)
    {
        LinkedHashMap<Integer, String[]> peerInfo = new LinkedHashMap<Integer, String[]>();

        try {
            File peerFile = new File(path);
            Scanner reader = new Scanner(peerFile);

            while(reader.hasNextLine())
            {
                String data = reader.nextLine();
                String[] dataArr = data.split(" ");

                Integer peerID = Integer.parseInt(dataArr[0]);
                String[] peerData = {dataArr[1], dataArr[2], dataArr[3]};

                peerInfo.put(peerID, peerData);
            }
        }
        catch (FileNotFoundException e)
        {
            System.out.println("File not found");
        }

        return peerInfo;
    }
}
