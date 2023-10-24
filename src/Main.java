import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Set;

public class Main {

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
    public static void main(String[] args) {

        String commonPath = "project_config_file_small/Common.cfg";//"src/project_config_file_small/Common.cfg";
        String peerInfoPath = "project_config_file_small/PeerInfo.cfg";//"src/project_config_file_small/PeerInfo.cfg";

        // We must use LinkedHashMaps since the directions specify that "You need to
        // start the peer processes in the order specified in the file PeerInfo.cfg on the machine specified in the file."
        // The LinkedHashMap keeps the order of the keys in the order they were inserted.

        LinkedHashMap<String, String> commonInfo;
        LinkedHashMap<Integer, String[]> peerInfo;

        commonInfo = readCommonInfo(commonPath);
        peerInfo = readPeerInfo(peerInfoPath);

        Integer[] peerIDs = peerInfo.keySet().toArray(new Integer[peerInfo.size()]);

        // Testing setting up a peer
        // Need to implement Server and Client classes within peer class
        Peer Peer1 = new Peer(peerIDs[0], commonInfo, peerInfo, "src/project_config_file_small/1001/thefile");
        Peer Peer2 = new Peer(peerIDs[1], commonInfo, peerInfo, "src/project_config_file_small/1002/thefile");

        

    }
}