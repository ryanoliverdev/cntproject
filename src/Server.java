import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

public class Server 
{

    Server(Peer peer){
        int portNum = peer.portNumber;
        System.out.println("Server running on port " + portNum);
        ServerSocket listener;
        try {
            listener = new ServerSocket(portNum);
            int clientNum = 1;
            try {
                while(true) {
                    new Handler(listener.accept(), clientNum, peer).start();
                }
            } finally {
                listener.close();
            }
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    private static class Handler extends Thread
    {
        private byte[] clientMessage = new byte[128];    //message received from the client
        private byte[] serverMessage = new byte[128];    //message sent to client
        private Socket connection;
        private DataInputStream in;	//stream read from the socket
        private DataOutputStream out;    //stream write to the socket
        private int no;		//The index number of the client

        private Peer peer;

        private int destPeerID = -1;
        private boolean completedHandshake = false;

        public Handler(Socket connection, int no, Peer p)
        {
            this.connection = connection;
            this.no = no;
            this.peer = p;
        }

        public void run() 
        {
            try
            {
                //initialize Input and Output streams
                out = new DataOutputStream(connection.getOutputStream());
                out.flush();
                in = new DataInputStream(connection.getInputStream());
                while(true)
                {
                    // Handshake server send and send initial bitfield
                    if (!completedHandshake){
                        byte[] buffer = new byte[32];
                        int bytesRead = in.read(buffer);
                        byte[] headerBytes = Arrays.copyOfRange(buffer, 0, 18);
                        byte[] expectedHeader = "P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8);
                        if (Arrays.equals(headerBytes, expectedHeader)) {
                            String peerIDStr = "";
                            for (int i = 0; i < 4; i++) {
                                peerIDStr += (char) buffer[28 + i];
                            }
                            System.out.println("Handshake received from peer " + peerIDStr);

                            // Writing the connection method to the log file
                            destPeerID = Integer.parseInt(peerIDStr);
                            peer.logger.writeLogMessage(0, peer.peerID, destPeerID, 0, 0);
                            final Object lock = new Object();
                            peer.connections.put(destPeerID, connection);
                            peer.connectionLocks.put(destPeerID, lock);
                            // Set default values
                            peer.unSetInterestPeer(destPeerID);
                            peer.chokePeer(destPeerID);
                            // Perform handshake
                            byte[] handshakeMessage = Messages.getHandshakeMessage(peer.peerID);
                            sendMessage(handshakeMessage, out);
                            completedHandshake = true;

                            // Send Bitfield
                            // TO DO: I know I redid this, you can simplify it if you'd like
                            if (peer.hasFile) {
                                int numOfPieces = (int) Math.ceil((double) peer.fileSize / peer.pieceSize);

                                byte[] bitfield = new byte[(numOfPieces + 7)/8];
                                for (int i = 0; i < numOfPieces; i++) {
                                    bitfield[i/8] |= 1 << (7 - (i % 8));
                                }
                                byte[] bitfieldMsg = Messages.getBitfieldMessage(bitfield);
                                peer.setOwnBitfield(bitfield);
                                sendMessage(bitfieldMsg, out);
                                System.out.println("Bitfield sent");
                            }
                        }
                    }
                    if (!completedHandshake) {
                        // make sure handshake completes
                        continue;
                    }
                    // Handshake over, process messages based on length and type

                    peer.setInitialNeighbors();
                    // Message format for all other messages
                    byte[] lengthBuffer = new byte[4];
                    int length;
                    // Read the type of the message
                    byte[] typeBuffer = new byte[1];
                    int type;
                    byte[] messageBuffer = new byte[0];

                    // Send Bitfield
                    // TO DO: I know I redid this, you can simplify it if you'd like

                    // Receive Next Message
                    in.read(lengthBuffer);
                    length = ByteBuffer.wrap(lengthBuffer).getInt();
                    in.read(typeBuffer);
                    type = typeBuffer[0];
                    System.out.println("MESSAGE OF TYPE: " + type + " RECEIVED WITH LENGTH: " + length);
                    if (length > 1) {
                        messageBuffer = new byte[length - 1];
                        in.read(messageBuffer);
                    }


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
                        peer.logger.writeLogMessage(7, peer.peerID, destPeerID, 0, 0);

                    }
                    // Uninterested Message Received
                    if (type == 3)
                    {
                        // not sure if this is redundant but
                        System.out.println("Set uninterest for " + destPeerID);
                        peer.unSetInterestPeer(destPeerID);

                        peer.logger.writeLogMessage(8, peer.peerID, destPeerID, 0, 0);
                    }
                    // Unchoked message received
                    if (type == 1)
                    {
                        System.out.println("Set unchoked for " + destPeerID);
//                        if (peer.isChokedPeer.get(destPeerID)){
//                            continue;
//                        }
                        peer.unChokePeer(destPeerID);
                        // Determine what other peer has that it doesn't
                        byte[] localBitfield = peer.bitfield;
                        byte[] peerBitfield = peer.hasPiecesPeers.get(destPeerID);

                        BitSet localBitSet = BitSet.valueOf(localBitfield);
                        BitSet peerBitSet = BitSet.valueOf(peerBitfield);

                        // Clone peerBitSet because andNot() modifies the BitSet in place
                        BitSet diff = (BitSet) peerBitSet.clone();
                        // diff now contains bits that are in peerBitSet but not in localBitSet
                        diff.andNot(localBitSet);

                        // Convert diff to list of indices
                        ArrayList<Integer> pieceIndices = new ArrayList<>();
                        for (int i = diff.nextSetBit(0); i >= 0; i = diff.nextSetBit(i+1)) {
                            pieceIndices.add(i);
                        }

                        Random rand = new Random();
                        int randomIndex = rand.nextInt(pieceIndices.size());
                        int randomPieceIndex = pieceIndices.get(randomIndex);

                        ByteBuffer buffer = ByteBuffer.allocate(4);
                        buffer.putInt(randomPieceIndex);
                        byte[] indexField = buffer.array();

                        // send unchoke log
                        peer.logger.writeLogMessage(4, peer.peerID, destPeerID, 0, 0);

                        // send request message
                        byte[] requestMessage = Messages.getRequestMessage(indexField);
                        sendMessage(requestMessage, out);
                    }
                    if (type == 0)
                    {
                        // This sends out the choked log
                        peer.chokePeer(destPeerID);
                        peer.logger.writeLogMessage(5, peer.peerID, destPeerID, 0, 0);
                        System.out.println("Choked by " + destPeerID);

                    }
                    if (type == 6)
                    {
                        System.out.println("Received Request Messsage");
                        // Create a new array for the index field and copy the first 4 bytes of messageBuffer
                        byte[] indexField = new byte[4];
                        System.arraycopy(messageBuffer, 0, indexField, 0, 4);
                        // Get piece content
                        String filePath = "./project_config_file_small/" + peer.peerID + "/" + peer.fileName;
                        byte[] pieceContent = peer.fileData.getData(indexField, filePath);
                        byte[] piecesMessage = Messages.getPiecesMessage(indexField, pieceContent);
                        sendMessage(piecesMessage, out);

                    }
                    // Receive piece
                    if (type == 7)
                    {
                        // Take in data
                        // Create a new array for the index field and copy the first 4 bytes of messageBuffer
                        // Stop if choked
                        if (peer.isChokedPeer.get(destPeerID)){
                            out.flush();
                        }
                        System.out.println("Received Piece Message");
                        byte[] indexField = new byte[4];
                        System.arraycopy(messageBuffer, 0, indexField, 0, 4);

                        // Create a new array for the piece content and copy the rest of messageBuffer
                        byte[] pieceContent = new byte[messageBuffer.length - 4];
                        System.arraycopy(messageBuffer, 4, pieceContent, 0, messageBuffer.length - 4);
                        // Download piece
                        peer.fileData.setData(indexField,pieceContent, peer.peerID);
                        int index = ByteBuffer.wrap(indexField).getInt();
                        int indexInt = index / 8;
                        int indexRem = index % 8;
                        // Received piece, set bitfield accordingly
                        peer.bitfield[indexInt] = (byte) (peer.bitfield[indexInt] | (1 << indexRem));
                        System.out.println(peer.numOfPiecesHave);
                        System.out.println(peer.numOfPieces);
                        if (peer.numOfPiecesHave < peer.numOfPieces)
                        {

                            // Generate another random piece
                            byte[] localBitfield = peer.bitfield;
                            byte[] peerBitfield = peer.hasPiecesPeers.get(destPeerID);

                            BitSet localBitSet = BitSet.valueOf(localBitfield);
                            BitSet peerBitSet = BitSet.valueOf(peerBitfield);

                            // Clone peerBitSet because andNot() modifies the BitSet in place
                            BitSet diff = (BitSet) peerBitSet.clone();
                            // diff now contains bits that are in peerBitSet but not in localBitSet
                            diff.andNot(localBitSet);

                            // Convert diff to list of indices
                            ArrayList<Integer> pieceIndices = new ArrayList<>();
                            for (int i = diff.nextSetBit(0); i >= 0; i = diff.nextSetBit(i+1)) {
                                pieceIndices.add(i);
                            }

                            Random rand = new Random();
                            int randomIndex = rand.nextInt(pieceIndices.size());
                            int randomPieceIndex = pieceIndices.get(randomIndex);

                            ByteBuffer buffer = ByteBuffer.allocate(4);
                            buffer.putInt(randomPieceIndex);

                            byte[] nextIndexField = buffer.array();
                            // Request
                            byte[] requestMessage = Messages.getRequestMessage(nextIndexField);
                            sendMessage(requestMessage, out);
                        }
                    }
                    if (type == 4)
                    {
                        byte[] indexField = new byte[4];
                        System.arraycopy(messageBuffer, 0, indexField, 0, 4);

                    }
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally
            {
                //Close connections
                try
                {
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException)
                {
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        //send a message to the output stream
        public void sendMessage(byte[] msg, DataOutputStream out) {
            Object lock = peer.connectionLocks.get(destPeerID);

            try {
                synchronized (lock) {
                    out.write(msg);
                    out.flush();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

        }
    }

}
