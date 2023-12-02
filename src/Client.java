import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Client extends Thread
{ //client part of the peer: reads data from port ****
    byte[] message_sent;                //message send to the server
    byte[] message_received;                //message read from the server
    private LinkedHashMap<Integer, String[]> peerInfo;
    private boolean completedHandshake = false;
    public Peer peer;
    Client(Peer p, LinkedHashMap<Integer, String[]> pInfo)
    {
        peer = p;
        peerInfo = pInfo;
        System.out.println("New peer " + peer.peerID + " listening on port " + peer.portNumber);
    }

    public void run()
    {
        LinkedHashMap<Integer, Socket>  requestSockets = new LinkedHashMap<>();      //socket connect to the server
        try {
            // send handshake to all clients (technically each peers server)
            requestSockets = new LinkedHashMap<>();
            for (int id = 1001; id < peer.peerID; id++) {
                int peerPort = Integer.parseInt(peerInfo.get(id)[1]);
                String hostName = peerInfo.get(id)[0];
                requestSockets.put(id, new Socket(hostName, peerPort));
                System.out.println("Connected to " + hostName + " in port " + peerPort);
                DataOutputStream out = new DataOutputStream(requestSockets.get(id).getOutputStream());
                out.flush();
                DataInputStream in = new DataInputStream(requestSockets.get(id).getInputStream());
                // handshake
                byte[] handshakeMessage = Messages.getHandshakeMessage(peer.peerID);
                sendMessage(handshakeMessage, out);
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
//        catch ( ClassNotFoundException e ) {
//            System.err.println("Class not found");
//        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        for (Map.Entry<Integer, Socket> entry : requestSockets.entrySet())
        {
            new Thread(() -> {
                Socket socket = entry.getValue();
                // Might be poorly named, the not local peer
                int destPeerID = entry.getKey();
                try {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    while (true) {
                        // Process handshake and send initial bitfield
                        if (!completedHandshake) {
                            byte[] buffer = new byte[32];
                            int bytesRead = in.read(buffer);
                            byte[] headerBytes = Arrays.copyOfRange(buffer, 0, 18);
                            byte[] expectedHeader = "P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8);
                            if (Arrays.equals(headerBytes, expectedHeader)) {
                                String peerIDStr = "";
                                for (int i = 0; i < 4; i++) {
                                    peerIDStr += (char) buffer[28 + i];
                                }
                                System.out.println("Handshake completed with " + peerIDStr);
                                if (peer.hasFile) {
                                    int numOfPieces = peer.fileSize / peer.pieceSize + 1;

                                    buffer = new byte[5 + (numOfPieces + 7) / 8];
                                    for (int i = 0; i < numOfPieces; i++) {
                                        buffer[i / 8] |= 1 << (7 - (i % 8));
                                    }
                                    byte[] bitfield = Messages.getBitfieldMessage(buffer);
                                    peer.setOwnBitfield(bitfield);
                                    sendMessage(bitfield, out);
                                    System.out.println("Bitfield sent");
                                }
                            }
                        }
                        // Add new choked peer
                        peer.chokePeer(destPeerID);
                        // Add new uninterested peer
                        peer.setInterestPeer(destPeerID);
                        // set neighbors
                        peer.setInitialNeighbors();
                        // Message format for all other messages
                        byte[] lengthBuffer = new byte[4];
                        int length;
                        // Read the type of the message
                        byte[] typeBuffer = new byte[1];
                        int type;
                        byte[] messageBuffer;

                        // Send Bitfield
                        // TO DO: I know I redid this, you can simplify it if you'd like

                        // Receive Next Message
                        in.read(lengthBuffer);
                        length = ByteBuffer.wrap(lengthBuffer).getInt();
                        in.read(typeBuffer);
                        type = typeBuffer[0];
                        messageBuffer = new byte[length - 1];
                        in.read(messageBuffer);

                        // Bitfield Message Received
                        if (type == 5) {
                            System.out.println("Set bitfield for " + destPeerID);
                            peer.setPeerPiecesBitfield(destPeerID, messageBuffer);
                            peer.setHasFile(destPeerID);
                            byte[] msg;
                            if (messageBuffer.length > peer.bitfield.length)
                            {
                                // If there's physically more pieces
                                msg = Messages.getInterestMessage();
                                sendMessage(msg, out);
                            }
                            else {
                                for (int i = 0; i < messageBuffer.length; i++) {
                                    if (messageBuffer[i] != peer.bitfield[i]) {
                                        msg = Messages.getInterestMessage();
                                        sendMessage(msg, out);
                                        break;
                                    }
                                    if (i == messageBuffer.length - 1) {
                                        msg = Messages.getUnInterestMessage();
                                        sendMessage(msg, out);
                                    }
                                }
                            }
                        }
                        // Interested Message Received
                        if (type == 2)
                        {
                            // Switch bool to true for interested peer
                            System.out.println("Set interest for " + destPeerID);
                            peer.setInterestPeer(destPeerID);
                        }
                        // Uninterested Message Received
                        if (type == 3)
                        {
                            // not sure if this is redundant but
                            System.out.println("Set uninterest for " + destPeerID);
                            peer.unSetInterestPeer(destPeerID);
                        }
                        // Unchoked message received
                        if (type == 1)
                        {
                            // Determine what other peer has that it doesn't
                            byte[] localBitfield = peer.bitfield;
                            byte[] peerBitfield = peer.hasPiecesPeers.get(destPeerID);

                            int maxInd = Math.min(localBitfield.length, peerBitfield.length);
                            ArrayList<Integer> pieceIndices = new ArrayList<>();
                            for (int i = 0; i < maxInd; i++)
                            {
                                if (localBitfield[i] != peerBitfield[i])
                                    pieceIndices.add(i);
                            }
                            if (peerBitfield.length > localBitfield.length)
                            {
                                int difference = peerBitfield.length - localBitfield.length;
                                for (int i = localBitfield.length; i < difference; i++)
                                {
                                    pieceIndices.add(i);
                                }
                            }

                            Random rand = new Random();
                            int randomIndex = rand.nextInt(pieceIndices.size());
                            int randomPieceIndex = pieceIndices.get(randomIndex);

                            ByteBuffer buffer = ByteBuffer.allocate(4);
                            buffer.putInt(randomPieceIndex);
                            byte[] indexField = buffer.array();

                            // send request message
                            byte[] requestMessage = Messages.getRequestMessage(indexField);
                            sendMessage(requestMessage, out);
                        }
                        if (type == 0)
                        {
                            // Genuinely might only need to log this
                        }
                        if (type == 6)
                        {

                        }


                    }
                } catch (IOException e) {
                    System.err.println("Error reading from socket: " + e.getMessage());
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Error closing socket: " + e.getMessage());
                    }
                }
            }).start();
        }
    }
//    void closeConnections()
//    {
//        try{
//            in.close();
//            out.close();
//            requestSocket.close();
//        }
//        catch(IOException ioException){
//            ioException.printStackTrace();
//        }
//    }
    public static void sendMessage(byte[] msg, DataOutputStream out)
    {
        try{
            //stream write the message
            out.write(msg);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

}
