import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
                    new Handler(listener.accept(), clientNum, peer.peerID).start();
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
        private byte[] clientMessage;    //message received from the client
        private byte[] serverMessage;    //message sent to client
        private Socket connection;
        private DataInputStream in;	//stream read from the socket
        private DataOutputStream out;    //stream write to the socket
        private int no;		//The index number of the client

        private int peerID;
        private boolean completedHandshake = false;

        public Handler(Socket connection, int no, int p)
        {
            this.connection = connection;
            this.no = no;
            this.peerID = p;
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
                    //receive the message sent from the client
                    byte[] buffer = new byte[32];
                    int bytesRead = in.read(buffer);
                    byte[] headerBytes = Arrays.copyOfRange(buffer, 0, 18);
                    byte[] expectedHeader = "P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8);


                    if (Arrays.equals(headerBytes, expectedHeader))
                    {
                        String peerIDStr = "";
                        for (int i = 0; i < 4; i++)
                        {
                            peerIDStr += (char) buffer[28 + i];
                        }
                        System.out.println("Handshake recieved from peer " + peerIDStr);
                        byte[] handshakeMessage = Messages.getHandshakeMessage(peerID);
                        sendMessage(handshakeMessage);
                        // Checks if it receives a handshake back.
                        in.read(buffer);
                        String receivedPeerID = "";
                        for (int i = 0; i < 4; i++)
                        {
                            receivedPeerID += (char) buffer[28 + i];
                        }
                        // We are supposed to check something about the Peer ID not sure what
                        if (true)
                        {
                            System.out.println("Handshake Complete after " + receivedPeerID + " shook back.");
                        }
                        else
                        {
                            System.out.println("Handshake went terribly wrong");
                        }

                    }
                    else
                    {
                        System.out.println(Arrays.toString(buffer) + " != " + Arrays.toString(expectedHeader));
                        continue;
                    }
                    if (completedHandshake)
                    {
                        int messageType = 0;
                        try
                        {
                            messageType = 0;
                        } catch (NumberFormatException nfe) {
                            // do something
                        }
                        // the prints are placeholders for what will likely be function calls
                        switch (messageType) {
                            case 0:
                                byte[] chokeMessage = Messages.getChokeMessage();
                                sendMessage(chokeMessage);
                                break;
                            case 1:
                                byte[] unChokeMessage = Messages.getUnChokeMessage();
                                sendMessage(unChokeMessage);
                                break;
                            case 2:
                                byte[] interestedMessage = Messages.getInterestMessage();
                                sendMessage(interestedMessage);
                                break;
                            case 3:
                                byte[] unInterestedMessage = Messages.getUnInterestMessage();
                                sendMessage(unInterestedMessage);
                                break;
                            case 4:
                                byte[] hasFileMessage = Messages.getHasFileMessage("placeholder".getBytes());
                                sendMessage(hasFileMessage);
                                break;
                            case 5:
                                byte[] bitfieldMessage = Messages.getBitfieldMessage("placeholder".getBytes());
                                sendMessage(bitfieldMessage);
                                break;
                            case 6:
                                byte[] requestMessage = Messages.getRequestMessage("placeholder".getBytes());
                                sendMessage(requestMessage);
                                break;
                            case 7:
                                byte[] piecesMessage = Messages.getPiecesMessage("placeholder".getBytes(), "placeholder".getBytes());
                                sendMessage(piecesMessage);
                                break;
                            default:
                                sendMessage("placeholder".getBytes());
                        }
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
        public void sendMessage(byte[] msg) {
            try {
                out.write(msg);
                out.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

}
