import java.util.LinkedHashMap;

public class peerProcess {
    static String commonPath = "./project_config_file_large/Common.cfg";
    static String peerInfoPath = "./project_config_file_large/PeerInfo.cfg";
    public static void main(String[] args){
        final Object lock = new Object();

        LinkedHashMap<String, String> commonInfo = ReadFile.readCommonInfo(commonPath);
        LinkedHashMap<Integer, String[]> peerInfo = ReadFile.readPeerInfo(peerInfoPath);

        int peerID = Integer.parseInt(args[0]);
        Peer peer = new Peer(peerID, commonInfo, peerInfo);

        // For choking and optimistic choking interval
        PeerThread pThread = new PeerThread(peer);
        pThread.start();
        // Initializing client
        Client client = new Client(peer, peerInfo);
        client.start();
        // Initialize server to listen
        Server server = new Server(peer);
    }

}
