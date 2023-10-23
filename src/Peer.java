import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class Peer {
    int unchokingInterval;
    int optimisticUnchokingInterval;
    String fileName;
    int fileSize;
    int pieceSize;
    int peerID;
    String hostName;
    int portNumber;
    boolean hasFile;
    int kNeighbors;

    boolean isChoked = false;
    boolean isInterested = false;
    Client client;
    // PeerID's of preferredNeighbors
    private ArrayList<Integer> preferredNeighbors;
    private ArrayList<Integer> interestedNeighbors;


    private ArrayList<Integer> getPreferredNeighbors(int k, ArrayList<Integer> interested) {
        ArrayList<Integer> prefNeighbors = new ArrayList<>(kNeighbors);

        return prefNeighbors;
    }
    public Peer(int id, LinkedHashMap<String, String> commonInfo, LinkedHashMap<Integer, String[]> peerInfo, String File ) {

        // Reading in all Common.cfg Info

        peerID = id;
        unchokingInterval = Integer.parseInt(commonInfo.get("UnchokingInterval"));
        optimisticUnchokingInterval = Integer.parseInt(commonInfo.get("OptimisticUnchokingInterval"));
        fileName = commonInfo.get("FileName");
        fileSize = Integer.parseInt(commonInfo.get("FileSize"));
        pieceSize = Integer.parseInt(commonInfo.get("PieceSize"));
        kNeighbors = Integer.parseInt(commonInfo.get("NumberOfPreferredNeighbors"));
        // Reading in all PeerInfo.cfg Info

        hostName = peerInfo.get(id)[0];
        portNumber = Integer.parseInt(peerInfo.get(id)[1]);
        hasFile = Boolean.parseBoolean(peerInfo.get(id)[2]);

        // Initializing client
        client = new Client(this);
        client.run();
    }

}
