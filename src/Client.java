import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Client {
    Socket requestSocket;           //socket connect to the server
    ObjectOutputStream out;         //stream write to the socket
    ObjectInputStream in;          //stream read from the socket
    String message_sent;                //message send to the server
    String message_received;                //message read from the server
    private int portNumber;

    private int peerID;
    Client(Peer peer) {
        portNumber = peer.portNumber;
        peerID = peer.peerID;
        System.out.println("New peer " + peerID + " listening on port " + portNumber);
    }

    void run()
    {
        try{
            //create a socket to connect to the server
            requestSocket = new Socket("localhost", 8000);
            System.out.println("Connected to localhost in port 8000");
            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());
            handshakeMessage();
            while(true)
            {
                // message sent on port
                message_received = (String)in.readObject();
                if (message_received.contains("P2PFILESHARINGPROJ") && !message_received.contains(String.valueOf(peerID))){
                    handshakeMessage();
                }
                // check message
                System.out.println("Receive message: " + message_received) ;
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch ( ClassNotFoundException e ) {
            System.err.println("Class not found");
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //Close connections
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
    //send a message to the output stream
    void sendMessage(String msg)
    {
        try{
            //stream write the message
            out.writeObject(msg);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    void handshakeMessage()
    {
        // This might have to be sent as a byte array. Fine for now
        sendMessage("P2PFILESHARINGPROJ" + "0000000000000000" + peerID);
    }
}
