import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Server {

    private static final int sPort = 8000;   //The server will be listening on this port number
    public static void main(String[] args) throws Exception {
        System.out.println("The server is running.");
        ServerSocket listener = new ServerSocket(sPort);
        int clientNum = 1;
        try {
            while(true) {
                new Handler(listener.accept(),clientNum).start();
                System.out.println("Client "  + clientNum + " is connected!");
                clientNum++;
            }
        } finally {
            listener.close();
        }

    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    private static class Handler extends Thread {
        private String clientMessage;    //message received from the client
        private String serverMessage;    //message sent to client
        private Socket connection;
        private ObjectInputStream in;	//stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;		//The index number of the client

        private boolean completedHandshake = false;
        public Handler(Socket connection, int no) {
            this.connection = connection;
            this.no = no;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                try{
                    while(true)
                    {
                        //receive the message sent from the client
                        clientMessage = (String)in.readObject();
                        //show the message to the user
                        System.out.println("Received message: " + clientMessage + " from client " + no);
                        //send message back
                        serverMessage = "hi";
                        if (clientMessage.contains("P2PFILESHARINGPROJ")) {
                            // In reality, this would be another peer sending another handshake
                            sendMessage(clientMessage);
                            System.out.println("Completing operations in server...");
                            completedHandshake = true;
                        }
                        if (!completedHandshake)
                        {
                            sendMessage("Need to complete handshake");
                        }
                        if (completedHandshake) {
                            // for testing
                            int messageType = 0;
                            try {

                                messageType = Integer.parseInt(clientMessage);
                            } catch (NumberFormatException nfe) {
                                // do something
                                System.out.println(clientMessage + " is not a number");
                            }
                            // the prints are placeholders for what will likely be function calls
                            switch (messageType) {
                                case 0 -> sendMessage("Choke");
                                case 1 -> sendMessage("Unchoke");
                                case 2 -> sendMessage("Interested");
                                case 3 -> sendMessage("Not Interested");
                                case 4 -> sendMessage("Have");
                                case 5 -> sendMessage("Bitfield");
                                case 6 -> sendMessage("Request");
                                case 7 -> sendMessage("Piece");
                                default -> sendMessage(clientMessage);

                            }
                        }
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        //send a message to the output stream
        public void sendMessage(String msg)
        {
            try{
                out.writeObject(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

    }

}
