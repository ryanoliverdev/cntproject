import java.net.*;
import java.io.*;
import java.util.LinkedHashMap;

public class Client 
{ //client part of the peer: reads data from port ****
    Socket requestSocket;           //socket connect to the server
    DataOutputStream out;         //stream write to the socket
    DataInputStream in;          //stream read from the socket
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
        try{
            // send handshake to all clients
            for (int id = 1001; id < peer.peerID; id++)
            {
                int peerPort = Integer.parseInt(peerInfo.get(id)[1]);
                String hostName = peerInfo.get(id)[0];
                requestSocket = new Socket(hostName, peerPort);
                System.out.println("Connected to " + hostName  + " in port " + peerPort);
                out = new DataOutputStream(requestSocket.getOutputStream());
                out.flush();
                in = new DataInputStream(requestSocket.getInputStream());
                MessageSender.sendHandshakeMessage(this);
            }


            //initialize inputStream and outputStream
//            while(true)
//            {
//                // This will be done automatically later
//                if (!completedHandshake)
//                {
//                    System.out.print("Hello, please input P2PFILESHARINGPROJ to initiate handshake:");
//                }
//                else
//                {
//                    System.out.print("Enter message type (num):");
//                }
//                message_received = in.readObject().toString().getBytes();
//                if (message_received.toString().contains("P2PFILESHARINGPROJ")) {
//                    completedHandshake=true;
//                }
//
//            }
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
}
