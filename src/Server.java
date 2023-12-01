import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.Arrays;

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
                    System.out.println("Client " + clientNum + " is connected!");
                    clientNum++;
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
                int destPeerID = -1;
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
                            destPeerID = Integer.parseInt(peerIDStr);
                            byte[] handshakeMessage = Messages.getHandshakeMessage(peer.peerID);
                            sendMessage(handshakeMessage, out);
                            completedHandshake = true;

                            // Send Bitfield
                            // TO DO: I know I redid this, you can simplify it if you'd like
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
                    if (!completedHandshake) {
                        // make sure handshake completes
                        continue;
                    }

                    // Handshake over, process messages based on length and type

                    // Message format for all other messages
                    byte[] lengthBuffer = new byte[4];
                    int length;
                    // Read the type of the message
                    byte[] typeBuffer = new byte[1];
                    int type;
                    byte[] messageBuffer;


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
            try {
                out.write(msg);
                out.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

}
