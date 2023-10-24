import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Client { //client part of the peer: reads data from port ****
    Socket requestSocket;           //socket connect to the server
    ObjectOutputStream out;         //stream write to the socket
    ObjectInputStream in;          //stream read from the socket
    String message_sent;                //message send to the server
    String message_received;                //message read from the server
    private int portNumber;
    private int peerID;
    private boolean completedHandshake = false;
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
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while(true)
            {
                // This will be done automatically later
                if (!completedHandshake)
                {
                    System.out.print("Hello, please input P2PFILESHARINGPROJ to initiate handshake:");
                }
                else
                {
                    System.out.print("Enter message type (num):");
                }

                //read a sentence from the standard input
                message_sent = bufferedReader.readLine();
                // message sent on port
                sendMessage(message_sent);
                message_received = (String)in.readObject();
                if (message_received.contains("P2PFILESHARINGPROJ")) {
                    completedHandshake=true;
                }

                // check message
                System.out.println(peerID + " received: " + message_received);
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
