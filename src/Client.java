import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class Client 
{ //client part of the peer: reads data from port ****
    Socket requestSocket;           //socket connect to the server
    static DataOutputStream out;         //stream write to the socket
    static DataInputStream in;          //stream read from the socket
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

    void run()
    {
        try {
            // send handshake to all clients
            for (int id = 1001; id < peer.peerID; id++) {
                int peerPort = Integer.parseInt(peerInfo.get(id)[1]);
                String hostName = peerInfo.get(id)[0];
                requestSocket = new Socket(hostName, peerPort);
                System.out.println("Connected to " + hostName + " in port " + peerPort);
                out = new DataOutputStream(requestSocket.getOutputStream());
                out.flush();
                in = new DataInputStream(requestSocket.getInputStream());
                // handshake
                byte[] handshakeMessage = Messages.getHandshakeMessage(peer.peerID);
                sendMessage(handshakeMessage);

                // Send back handshake to server
                byte[] buffer = new byte[32];
                int bytesRead = in.read(buffer);
                byte[] headerBytes = Arrays.copyOfRange(buffer, 0, 18);
                byte[] expectedHeader = "P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8);
                // If received handshake back
                if (Arrays.equals(headerBytes, expectedHeader)) {
                    String peerIDStr = "";
                    for (int i = 0; i < 4; i++) {
                        peerIDStr += (char) buffer[28 + i];
                    }
                    System.out.println("Handshake recieved from peer " + peerIDStr);
                }
                else
                {
                    System.out.println("Something went very wrong");
                }
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
    }
    void closeConnections()
    {
        try{
            in.close();
            out.close();
            requestSocket.close();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    public static void sendMessage(byte[] msg)
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
