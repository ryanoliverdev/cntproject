import java.util.*;

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
    // PeerID's of preferredNeighbors along with download rates (maybe can get rid of these after sorting)
    private ArrayList<int[]> preferredNeighbors;
    private ArrayList<Integer> interestedNeighbors;

    // Private Functions

    // Message Bodies
    private void sendHandshakeMessage(){
        String message = "P2PFILESHARINGPROJ0000000000" + peerID;
        client.sendMessage(message);
    }
    private void sendChokeMessage(){

    }
    private void sendUnChokeMessage(){

    }
    private void sendInterestMessage(){

    }
    private void sendUnInterestMessage(){

    }
    private void sendHasFileMessage(){

    }
    private void sendBitFieldMessage(){

    }
    private void sendRequestPiecesMessage(){

    }
    private void sendPiecesMessage(){

    }

    // Message Types
    private void chokePeer(int srcPeerID){

    }
    private void unChokePeer(int srcPeerID){

    }
    private void setInterestPeer(int srcPeerID){

    }
    private void unSetInterestPeer(int srcPeerID){

    }
    private void setHasFilePeer(int srcPeerID){

    }
    private void sendBitfield(int srcPeerID){

    }
    private void requestPieces(int srcPeerID){

    }
    private void sendPieces(int srcPeerID){

    }
    private int getDownloadRate(int peerID){
        // needs implementation
        return 0;
    }
    private ArrayList<int[]> getPreferredNeighbors(int k, ArrayList<Integer> interested) {
        int n = interested.size();
        ArrayList<int[]> prefNeighbors = new ArrayList<>(kNeighbors);
        for (int i = 0; i < n; i++)
        {
            // pass tuple with download rate and id to be sorted later
            int currNeighbor = interested.get(i);
            int rate = getDownloadRate(currNeighbor);
            int [] tuple = {currNeighbor, rate};
            prefNeighbors.add(tuple);
        }
        Collections.sort(prefNeighbors, Comparator.comparing(a -> a[1]));
        // get the top k
        List<int[]> lastKElements = prefNeighbors.subList(n - k, n);
        ArrayList<int[]> result = new ArrayList<>(lastKElements);
        return result;
    }

    // Public Functions
    public void requestPreferredNeighbors(int k, ArrayList<Integer> neighbors){
        if (!preferredNeighbors.isEmpty()){
            preferredNeighbors = getPreferredNeighbors(k, neighbors);
        }
        else
        {
            // get k random neighbors
        }
    }
    public void interpretPeerMessage(int srcPeerID, byte[] message){
        // need to make messages byte arrays
        // placeholders
        int option = 0;
        switch(option) {
            case 0 -> chokePeer(srcPeerID);
            case 1 -> unChokePeer(srcPeerID);
            case 2 -> setInterestPeer(srcPeerID);
            case 3 -> unSetInterestPeer(srcPeerID);
            case 4 -> setHasFilePeer(srcPeerID);
            case 5 -> sendBitfield(srcPeerID);
            case 6 -> requestPieces(srcPeerID);
            case 7 -> sendPieces(srcPeerID);
        }
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
