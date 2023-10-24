import java.util.*;
import java.nio.ByteBuffer;

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
    Server server;

    // PeerID's of preferredNeighbors along with download rates (maybe can get rid of these after sorting)
    private ArrayList<int[]> preferredNeighbors;
    private ArrayList<Integer> interestedNeighbors;

    // Private Functions

    // Message Bodies
    private void /*byte[] */ sendHandshakeMessage(){
        //String message = "P2PFILESHARINGPROJ0000000000" + peerID;
        byte[] handshakeMessage = new byte[32]; //32 byte handshake message: 18, 10, 4
        byte[] peerIDbytes = ByteBuffer.allocate(4).putInt(peerID).array();
        byte[] header = "P2PFILESHARINGPROJ".getBytes();

        //put header into handshake message array
        System.arraycopy(header, 0, handshakeMessage, 0, header.length);
        for(int i = 18; i < 28; i++)//start in array at index 18(after header)
        {
            handshakeMessage[i] = 0; //put in 0 bits for 10 bytes
        }
        System.arraycopy(peerIDbytes, 0, handshakeMessage, 28, 4);
        
        //below line for debugging, remove ltr
        client.sendMessage("P2PFILESHARINGPROJ0000000000\"" + peerID);
    }
    private void sendChokeMessage()
    {
        int messageType = 0; // "choke" message type

        // Create a byte array to store the message
        byte[] chokeMessage = new byte[5]; // 4 bytes for length, 1 byte for message type

        // Calculate the message length (1 byte for the type, no payload)
        int messageLength = 1;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, chokeMessage, 0, 4);

        // Set the message type
        chokeMessage[4] = (byte) messageType;

        // Simulate sending the "choke" message to the peer
        
    }
    private void sendUnChokeMessage()
    {
        int messageType = 1; // "unchoke" message type

        // Create a byte array to store the message
        byte[] unchokeMessage = new byte[5]; // 4 bytes for length, 1 byte for message type

        // Calculate the message length (1 byte for the type, no payload)
        int messageLength = 1;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, unchokeMessage, 0, 4);

        // Set the message type
        unchokeMessage[4] = (byte) messageType;
        
        // Send message to neighbor peers
    }
    private void sendInterestMessage()
    {
        int messageType = 2; // "interest" message type

        // Create a byte array to store the message
        byte[] interestMessage = new byte[5]; // 4 bytes for length, 1 byte for message type

        // Calculate the message length (1 byte for the type, no payload)
        int messageLength = 1;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, interestMessage, 0, 4);

        // Set the message type
        interestMessage[4] = (byte) messageType;

        // Send peer message
    }
    private void sendUnInterestMessage()
    {
        int messageType = 2; // "uninterest" message type

        // Create a byte array to store the message
        byte[] uninterestMessage = new byte[5]; // 4 bytes for length, 1 byte for message type

        // Calculate the message length (1 byte for the type, no payload)
        int messageLength = 1;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, uninterestMessage, 0, 4);

        // Set the message type
        uninterestMessage[4] = (byte) messageType;

        // Send peer message
    }
    private void sendHasFileMessage()
    {
        
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

        // Reading in all PeerInfo.cfg Info

        hostName = peerInfo.get(id)[0];
        portNumber = Integer.parseInt(peerInfo.get(id)[1]);
        hasFile = Boolean.parseBoolean(peerInfo.get(id)[2]);

        // Initializing client
        client = new Client(this);
        server = new Server(this);
        
    }

}

