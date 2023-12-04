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
        //socket connect to the server
        LinkedHashMap<Integer, Socket>  requestSockets = new LinkedHashMap<>();

        try {
            // send handshake to all clients (technically each peers server)
            requestSockets = new LinkedHashMap<>();

            for (int id = 1001; id < peer.peerID; id++) {

                int peerPort = Integer.parseInt(peerInfo.get(id)[1]);
                String hostName = peerInfo.get(id)[0];
                requestSockets.put(id, new Socket(hostName, peerPort));

                // Add new choked peer
                peer.chokePeer(id);

                // Add new uninterested peer
                peer.unSetInterestPeer(id);

                peer.piecesSent.putIfAbsent(id, 0);

                System.out.println("Connected to " + hostName + " in port " + peerPort);

                DataOutputStream out = new DataOutputStream(requestSockets.get(id).getOutputStream());
                out.flush();
                DataInputStream in = new DataInputStream(requestSockets.get(id).getInputStream());

                // handshake
                byte[] handshakeMessage = Messages.getHandshakeMessage(peer.peerID);
                sendMessage(handshakeMessage, out);
            }

        }
        catch (ConnectException e)
        {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
//        catch ( ClassNotFoundException e ) {
//            System.err.println("Class not found");
//        }
        catch(UnknownHostException unknownHost)
        {
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException)
        {
            ioException.printStackTrace();
        }
        for (Map.Entry<Integer, Socket> entry : requestSockets.entrySet())
        {
            new Thread(() -> {
                Socket socket = entry.getValue();

                // Might be poorly named, the not local peer
                int destPeerID = entry.getKey();

                try
                {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    while (true) {
                        // Process handshake and send initial bitfield
                        if (!completedHandshake)
                        {
                            byte[] buffer = new byte[32];
                            int bytesRead = in.read(buffer);
                            byte[] headerBytes = Arrays.copyOfRange(buffer, 0, 18);
                            byte[] expectedHeader = "P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8);

                            if (Arrays.equals(headerBytes, expectedHeader))
                            {
                                String peerIDStr = "";
                                for (int i = 0; i < 4; i++) {
                                    peerIDStr += (char) buffer[28 + i];
                                }

                                System.out.println("Handshake completed with " + peerIDStr);
                                completedHandshake = true;

                                peer.logger.writeLogMessage(0, peer.peerID, destPeerID, 0, 0);

                                if (peer.hasFile)
                                {
                                    int numOfPieces = peer.numOfPieces;

                                    byte[] bitfield = new byte[numOfPieces];

                                    for (int i = 0; i < numOfPieces; i++)
                                    {
                                        bitfield[i/8] |= 1 << (7 - (i % 8));
                                    }

                                    byte[] bitfieldMsg = Messages.getBitfieldMessage(bitfield);
                                    peer.setOwnBitfield(bitfield);
                                    sendMessage(bitfieldMsg, out);

                                    System.out.println("Bitfield sent");
                                }
                                else
                                {
                                    int numOfPieces = peer.numOfPieces;

                                    byte[] bitfield = new byte[numOfPieces];

                                    for (int i = 0; i < numOfPieces; i++)
                                    {
                                        bitfield[i/8] |= 0;
                                    }
                                    peer.setOwnBitfield(bitfield);
                                }
                            }
                        }

                        // Update neighbors
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

                        // getting the type from buffer
                        in.read(typeBuffer);
                        type = typeBuffer[0];

                        System.out.println("MESSAGE OF TYPE: " + type + " RECEIVED WITH LENGTH: " + length);

                        if (length > 1)
                        {
                            messageBuffer = new byte[length - 1];
                            in.read(messageBuffer);
                        }

                        // Bitfield Message Received
                        if (type == 5)
                        {
                            System.out.println("Set bitfield for " + destPeerID);

                            peer.setPeerPiecesBitfield(destPeerID, messageBuffer);
                            peer.setHasFile(destPeerID);

                            System.out.println(messageBuffer.length);

                            byte[] msg;

                            if (messageBuffer.length > peer.bitfield.length)
                            {
                                // If there's physically more pieces
                                msg = Messages.getInterestMessage();
                                sendMessage(msg, out);
                            }
                            else
                            {
                                for (int i = 0; i < messageBuffer.length; i++)
                                {
                                    if (messageBuffer[i] != peer.bitfield[i])
                                    {
                                        msg = Messages.getInterestMessage();
                                        sendMessage(msg, out);
                                        break;
                                    }

                                    if (i == messageBuffer.length - 1)
                                    {
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
//                            if (peer.isChokedPeer.get(destPeerID)){
//                                continue;
//                            }

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

                            for (int i = diff.nextSetBit(0); i >= 0; i = diff.nextSetBit(i+1))
                            {
                                if (i < peer.numOfPieces) {
                                    pieceIndices.add(i);
                                }
                            }
                            if (pieceIndices.size() == 0)
                            {
                                continue;
                            }
                            Random rand = new Random();
                            int randomIndex = rand.nextInt(pieceIndices.size());
                            System.out.println("pieceIndices.size(): " + pieceIndices.size());
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
                            peer.chokePeer(destPeerID);
                            peer.logger.writeLogMessage(5, peer.peerID, destPeerID, 0, 0);
                            System.out.println("Choked " + destPeerID);
                        }

                        if (type == 6)
                        {
                            System.out.println("Received Request Messsage");

                            // Create a new array for the index field and copy the first 4 bytes of messageBuffer
                            byte[] indexField = new byte[4];
                            System.arraycopy(messageBuffer, 0, indexField, 0, 4);

                            // Get piece content
                            String filePath = "./project_config_file_large/" + peer.peerID + "/" + peer.fileName;
                            byte[] pieceContent = peer.fileData.getData(indexField, filePath);
                            byte[] piecesMessage = Messages.getPiecesMessage(indexField, pieceContent);

                            sendMessage(piecesMessage, out);
                            peer.piecesSent.put(destPeerID, peer.piecesSent.get(destPeerID) + 1);

                        }

                        if (type == 7)
                        {
                            // Take in data
                            // Create a new array for the index field and copy the first 4 bytes of messageBuffer
                            // Stop if choked
                            if (peer.isChokedPeer.get(destPeerID))
                            {
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
                            peer.numOfPiecesHave++;
                            peer.logger.writeLogMessage(9, peer.peerID, destPeerID, index, peer.numOfPiecesHave);

                            // Received piece, set bitfield accordingly
                            peer.bitfield[indexInt] = (byte) (peer.bitfield[indexInt] | (1 << indexRem));
                            System.arraycopy(messageBuffer, 0, indexField, 0, 4);

                            for (Map.Entry<Integer, Socket> ent : peer.connections.entrySet())
                            {
                                Socket requestSocket = ent.getValue();
                                Integer peerID = ent.getKey();
                                Object lock = peer.connectionLocks.get(peerID);
                                synchronized (lock) {
                                    DataOutputStream outS = new DataOutputStream(requestSocket.getOutputStream());
                                    outS.flush();
                                    byte[] hasMessage = Messages.getHasFileMessage(indexField);
                                    sendMessage(hasMessage, outS);
                                }
                            }
                            
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

                                for (int i = diff.nextSetBit(0); i >= 0; i = diff.nextSetBit(i+1))
                                {
                                    if (i < peer.numOfPieces) {
                                        pieceIndices.add(i);
                                    }
                                }

                                Random rand = new Random();
                                int randomPieceIndex;
                                if (pieceIndices.size() == 0)
                                {
                                    continue;
                                }
                                int randomIndex = rand.nextInt(pieceIndices.size());
                                randomPieceIndex = pieceIndices.get(randomIndex);
                                ByteBuffer buffer = ByteBuffer.allocate(4);
                                buffer.putInt(randomPieceIndex);

                                byte[] nextIndexField = buffer.array();
                                // Request
                                byte[] requestMessage = Messages.getRequestMessage(nextIndexField);
                                sendMessage(requestMessage, out);
                            }

                            peer.hasFile = true;
                            peer.logger.writeLogMessage(10, 0, 0, 0, 0);
                        }

                        if (type == 4)
                        {
                            byte[] indexField = new byte[4];
                            int index = ByteBuffer.wrap(indexField).getInt();
                            int indexInt = index / 8;
                            int indexRem = index % 8;

                            peer.bitfield[indexInt] = (byte) (peer.bitfield[indexInt] | (1 << indexRem));
                            peer.logger.writeLogMessage(6, peer.peerID, destPeerID, index, 0);
                        }

                       /* Boolean p2pFinished = true;

                        for (Map.Entry<Integer, Boolean> entry : peer.hasFilePeers.entrySet())
                        {
                            p2pFinished = p2pFinished && entry.getValue();
                        }

                        if (p2pFinished && peer.hasFile)
                        {
                            // can terminate, all peers have file
                        }*/
                    }
                }
                catch (IOException e)
                {
                    System.err.println("Error reading from socket: " + e.getMessage());
                }
                finally
                {
                    try
                    {
                        socket.close();
                    }
                    catch (IOException e)
                    {
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
    public void sendMessage(byte[] msg, DataOutputStream out)
    {
        try{
            out.write(msg);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

}
