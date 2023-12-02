import javax.xml.crypto.Data;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class PeerThread extends Thread{

    Peer peer;
    PeerThread(Peer p){
        peer = p;
    }
    public void run()
    {
        // Convert to milliseconds
        int unchokingInterval = peer.unchokingInterval * 1000;
        int optimisticUnchokingInterval = peer.optimisticUnchokingInterval * 1000;
        int k = peer.kNeighbors;
        while (true) {
            if (unchokingInterval < optimisticUnchokingInterval) {
                // Determine preferred neighbors
                try {
                    Thread.sleep(unchokingInterval);
                    // Old preferredNeighbors
                    ArrayList<Integer> oldPref = peer.getPreferredNeighbors();
                    // Set preferredNeighbors
                    peer.setPreferredNeighbors(k);
                    // Current preferredNeighbors
                    ArrayList<Integer> newPref = peer.getPreferredNeighbors();

                    // Figure out which ones need to be choked and which unchoked
                    ArrayList<Integer> toBeChoked = new ArrayList<>(oldPref);
                    toBeChoked.removeAll(newPref);

                    ArrayList<Integer> toBeUnChoked = new ArrayList<>(newPref);
                    toBeUnChoked.removeAll(oldPref);

                    // Send chokes
                    for (int i = 0; i < toBeChoked.size(); i++)
                    {
                        Socket serverSocket = peer.serverSockets.get(toBeChoked.get(i));
                        DataOutputStream out = new DataOutputStream(serverSocket.getOutputStream());
                        out.flush();
                        byte[] chokedMessage = Messages.getChokeMessage();
                        sendMessage(chokedMessage, out);
                        out.close();
                    }
                    // Send unchokes
                    for (int i = 0; i < toBeUnChoked.size(); i++)
                    {
                        Socket serverSocket = peer.serverSockets.get(toBeUnChoked.get(i));
                        DataOutputStream out = new DataOutputStream(serverSocket.getOutputStream());
                        out.flush();
                        byte[] unChokedMessage = Messages.getUnChokeMessage();
                        sendMessage(unChokedMessage, out);
                        out.close();
                    }

                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
                // Perform optimistic unchoking operation
                try {
                    Thread.sleep(optimisticUnchokingInterval - unchokingInterval);
                    int oldOptUnChokedNeighbor = peer.getOptimisticallyUnChokedNeighbor();
                    // Re choke if it is not in the preferred neighbors list. Otherwise, it should remain unchoked.
                    if (!peer.preferredNeighbors.contains(oldOptUnChokedNeighbor)){
                        Socket serverSocket = peer.serverSockets.get(oldOptUnChokedNeighbor);
                        DataOutputStream out = new DataOutputStream(serverSocket.getOutputStream());
                        out.flush();
                        byte[] chokedMessage = Messages.getChokeMessage();
                        sendMessage(chokedMessage, out);
                        out.close();
                    }
                    // By nature this is choked to begin with no need to check anything
                    peer.setOptimisticallyUnChokedNeighbor();
                    int newOptUnChokedNeighbor = peer.getOptimisticallyUnChokedNeighbor();
                    Socket serverSocket = peer.serverSockets.get(newOptUnChokedNeighbor);
                    DataOutputStream out = new DataOutputStream(serverSocket.getOutputStream());
                    out.flush();
                    byte[] unChokedMessage = Messages.getUnChokeMessage();
                    sendMessage(unChokedMessage, out);
                    out.close();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // Perform optimistic unchoking operation
                try {
                    Thread.sleep(optimisticUnchokingInterval);
                    int oldOptUnChokedNeighbor = peer.getOptimisticallyUnChokedNeighbor();
                    // Re choke if it is not in the preferred neighbors list. Otherwise, it should remain unchoked.
                    if (!peer.preferredNeighbors.contains(oldOptUnChokedNeighbor)){
                        Socket serverSocket = peer.serverSockets.get(oldOptUnChokedNeighbor);
                        DataOutputStream out = new DataOutputStream(serverSocket.getOutputStream());
                        out.flush();
                        byte[] chokedMessage = Messages.getChokeMessage();
                        sendMessage(chokedMessage, out);
                        out.close();
                    }
                    // By nature this is choked to begin with no need to check anything
                    peer.setOptimisticallyUnChokedNeighbor();
                    int newOptUnChokedNeighbor = peer.getOptimisticallyUnChokedNeighbor();
                    Socket serverSocket = peer.serverSockets.get(newOptUnChokedNeighbor);
                    DataOutputStream out = new DataOutputStream(serverSocket.getOutputStream());
                    out.flush();
                    byte[] unChokedMessage = Messages.getUnChokeMessage();
                    sendMessage(unChokedMessage, out);
                    out.close();
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
                // Determine preferred neighbors
                try {
                    Thread.sleep(unchokingInterval - optimisticUnchokingInterval);
                    // Old preferredNeighbors
                    ArrayList<Integer> oldPref = peer.getPreferredNeighbors();
                    // Set preferredNeighbors
                    peer.setPreferredNeighbors(k);
                    // Current preferredNeighbors
                    ArrayList<Integer> newPref = peer.getPreferredNeighbors();

                    // Figure out which ones need to be choked and which unchoked
                    ArrayList<Integer> toBeChoked = new ArrayList<>(oldPref);
                    toBeChoked.removeAll(newPref);

                    ArrayList<Integer> toBeUnChoked = new ArrayList<>(newPref);
                    toBeUnChoked.removeAll(oldPref);

                    // Send chokes
                    for (int i = 0; i < toBeChoked.size(); i++)
                    {
                        Socket serverSocket = peer.serverSockets.get(toBeChoked.get(i));
                        DataOutputStream out = new DataOutputStream(serverSocket.getOutputStream());
                        out.flush();
                        byte[] chokedMessage = Messages.getChokeMessage();
                        sendMessage(chokedMessage, out);
                        out.close();
                    }
                    // Send unchokes
                    for (int i = 0; i < toBeUnChoked.size(); i++)
                    {
                        Socket serverSocket = peer.serverSockets.get(toBeUnChoked.get(i));
                        DataOutputStream out = new DataOutputStream(serverSocket.getOutputStream());
                        out.flush();
                        byte[] unChokedMessage = Messages.getUnChokeMessage();
                        sendMessage(unChokedMessage, out);
                        out.close();
                    }
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }


            }
        }
    }
    public void sendMessage(byte[] msg, DataOutputStream out) {
        try {
            out.write(msg);
            out.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
