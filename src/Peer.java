import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.ByteBuffer;

public class Peer
{
    int unchokingInterval;
    int optimisticUnchokingInterval;
    String fileName;
    int fileSize;
    int pieceSize;
    int peerID;
    String hostName;
    int portNumber;
    boolean hasFile;
    int kNeighbors;

    byte[] bitfield = new byte[0];
    int optimisticallyUnChokedNeighbor;
    HashMap<Integer, Boolean> isChokedPeer = new HashMap<>();
    HashMap<Integer, Boolean> isInterestedPeer = new HashMap<>();
    HashMap<Integer, Boolean> hasFilePeers = new HashMap<>();
    HashMap<Integer, byte[]> hasPiecesPeers = new HashMap<>();
    HashMap<Integer, Double> neighbors = new HashMap<>();
    LinkedHashMap<Integer, String[]> pInfo;
    // Stores neighbors and download rate (as a double)

    // PeerID's of preferredNeighbors along with download rates (maybe can get rid of these after sorting)
    public ArrayList<Integer> preferredNeighbors = new ArrayList<>();
    public ArrayList<Integer> interestedNeighbors = new ArrayList<>();
    public void chokePeer(int srcPeerID)
    {
        isChokedPeer.put(srcPeerID, true);
    }
    public void unChokePeer(int srcPeerID)
    {
        isChokedPeer.put(srcPeerID, false);
    }
    public void setInterestPeer(int srcPeerID)
    {
        isInterestedPeer.put(srcPeerID, true);
    }
    public void unSetInterestPeer(int srcPeerID)
    {
        isInterestedPeer.put(srcPeerID, false);
    }
    public void setPeerPiecesBitfield(int srcPeerID, byte[] bitfield)
    {
        hasPiecesPeers.put(srcPeerID, bitfield);
    }
    public void setHasFile(int srcPeerID)
    {
        hasFilePeers.put(srcPeerID, true);
    }
    public void setOwnBitfield(byte[] bf)
    {
        this.bitfield = bf;
    }

    public Peer(int id, LinkedHashMap<String, String> commonInfo, LinkedHashMap<Integer, String[]> peerInfo)
    {
        peerID = id;
        unchokingInterval = Integer.parseInt(commonInfo.get("UnchokingInterval"));
        optimisticUnchokingInterval = Integer.parseInt(commonInfo.get("OptimisticUnchokingInterval"));
        fileName = commonInfo.get("FileName");
        fileSize = Integer.parseInt(commonInfo.get("FileSize"));
        pieceSize = Integer.parseInt(commonInfo.get("PieceSize"));
        kNeighbors = Integer.parseInt(commonInfo.get("NumberOfPreferredNeighbors"));
        // Reading in all PeerInfo.cfg Info

        hostName = peerInfo.get(id)[0];
        portNumber = Integer.parseInt(peerInfo.get(id)[1]);
        hasFile = Integer.parseInt(peerInfo.get(id)[2]) == 1;
        pInfo = peerInfo;


    }
    public void sendBitfield(int srcPeerID)
    {
        /*‘bitfield’ messages is only sent as the first message right after handshaking is done when
        a connection is established. ‘bitfield’ messages have a bitfield as its payload. Each bit in
        the bitfield payload represents whether the peer has the corresponding piece or not. The
        first byte of the bitfield corresponds to piece indices 0 – 7 from high bit to low bit,
        respectively. The next one corresponds to piece indices 8 – 15, etc. Spare bits at the end
        are set to zero. Peers that don’t have anything yet may skip a ‘bitfield’ message */
    }
    public void requestPieces(int srcPeerID)
    {
        /*request’ messages have a payload which consists of a 4-byte piece index field. Note
        that ‘request’ message payload defined here is different from that of BitTorrent. We don’t
        divide a piece into smaller subpieces.
        */
    }
    public void sendPieces(int srcPeerID)
    {
        /*
         * piece’ messages have a payload which consists of a 4-byte piece index field and the
            content of the piece.   
         */
    }
    public int getDownloadRate(int peerID)
    {
        // needs implementation
        return 0;
    }
    public void setPreferredNeighbors(int k)
    {
        // Collect interested neighbors into an array
        for (Map.Entry<Integer, Boolean> entry : isInterestedPeer.entrySet())
        {
            Integer neighborID = entry.getKey();
            Boolean interested = entry.getValue();
            if (interested)
            {
                interestedNeighbors.add(neighborID);
            }
        }
        if (hasFile)
        {
            // preferred neighbors become a random list of k interested
            Collections.shuffle(interestedNeighbors);
            int endIndex = Math.min(k, interestedNeighbors.size());
            preferredNeighbors = new ArrayList<>(interestedNeighbors.subList(0, endIndex));
        }
        else
        {
            // This should even work on initialization
            Collections.sort(interestedNeighbors, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return neighbors.get(o1).compareTo(neighbors.get(o2));
                }
            });
            int endIndex = Math.min(k, interestedNeighbors.size());
            preferredNeighbors = new ArrayList<>(interestedNeighbors.subList(0, endIndex));
        }
    }
    public ArrayList<Integer> getPreferredNeighbors()
    {
        ArrayList<Integer> prefNeighbors = new ArrayList<>(preferredNeighbors);
        return prefNeighbors;
    }

    public void setInitialNeighbors()
    {
        // Choked contains all available neighbors (unchoked or choked)
        int numOfNeighbors = isChokedPeer.size();
        HashMap<Integer, Double> newNeighbors = new HashMap<>();
        for (Map.Entry<Integer, Boolean> entry : isChokedPeer.entrySet()) {
            Integer peerID = entry.getKey();
            // initial download rate = 0.0
            newNeighbors.put(peerID, 0.0);
        }
        this.neighbors = newNeighbors;
   }

   public void setOptimisticallyUnChokedNeighbor()
   {
       // get all choked neighbors that are interested
       ArrayList<Integer> possibleOptimisticNeighbors = new ArrayList<>();
       for (Map.Entry<Integer, Boolean> entry : isChokedPeer.entrySet()) {
           Integer peerID = entry.getKey();
           Boolean choked = entry.getValue();
           if (choked && isInterestedPeer.get(peerID))
           {
               possibleOptimisticNeighbors.add(peerID);
           }
       }
       Random rand = new Random();
       int randomIndex = rand.nextInt(possibleOptimisticNeighbors.size());
       optimisticallyUnChokedNeighbor = possibleOptimisticNeighbors.get(randomIndex);

   }
   public int getOptimisticallyUnChokedNeighbor()
   {
       return optimisticallyUnChokedNeighbor;
   }
    public void interpretPeerMessage(int srcPeerID, byte[] message)
    {
        // message type
        byte type = message[4];
        switch(type) {
            // choke peer type
            case 0:
                chokePeer(srcPeerID);
                writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // unchoke message type
            case 1:
                unChokePeer(srcPeerID);
                writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // setInterest message type
            case 2:
                setInterestPeer(srcPeerID);
                writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // unsetInterest message type
            case 3:
                unSetInterestPeer(srcPeerID);
                writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // hasFile message type
            case 4:
                setPeerPiecesBitfield(srcPeerID, message);
                writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // bitfield message type
            case 5:
                sendBitfield(srcPeerID);
                writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // request message type
            case 6:
                requestPieces(srcPeerID);
                writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // pieces message type
            case 7:
                sendPieces(srcPeerID);
                writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
        }
    }

    // Still needs more implementation
    public void writeLogMessage(int typeOfMessage, int peerID, int peerID2, int pieceIndex, int totalPieces)
    {
        String filePath = "/log_peer_" + peerID + ".log";

        // Creating a FileWriter and BufferedWriter to create and write to the log file
        FileWriter fw = null;
        BufferedWriter bw = null;
        File logFile = new File(filePath);

        try
        {
            fw = new FileWriter(logFile.getName(), true);
            bw = new BufferedWriter(fw);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        String logFileData = "";
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentTime.format(formatter);

        switch(typeOfMessage) 
        {
            case 0:
                // Peer 1 connects to Peer 2
                logFileData = formattedDateTime + ": Peer " + peerID + " makes a connection to Peer " + peerID2 + ".";
                break;
            case 1:
                // Confirms connection from Peer 2 to Peer 1
                logFileData = formattedDateTime + ": Peer " + peerID + " is connected from Peer " + peerID2 + ".";
                break;
            case 2:
                // Case for if Peer 1 changes its preferred neighbors
                logFileData = formattedDateTime + ": Peer " + peerID + " has the preferred neighbors ";
                // getting the preferred neighbors from the arraylist
                for (int i = 0; i < preferredNeighbors.size(); i++) {
                    logFileData += preferredNeighbors.get(i) + ", ";
                }
                logFileData += ".";
                break;
            case 3:
                // If Peer 1 optimistically unchokes a neighbor
                logFileData = formattedDateTime + ": Peer " + peerID + " has the optimistically unchoked neighbor " + peerID2 + ".";
                break;
            case 4:
                // If peer 1 is unchoked by peer 2
                logFileData = formattedDateTime + ": Peer " + peerID + " is unchoked by " + peerID2 + ".";
                break;
            case 5:
                // If peer 1 is choked by peer 2
                logFileData = formattedDateTime + ": Peer " + peerID + " is choked by " + peerID2 + ".";
                break;
            case 6:
                // If peer 1 receives a have message from peer 2
                logFileData = formattedDateTime + ": Peer " + peerID + " received the ‘have’ message from " + peerID2 + " for the piece" + pieceIndex + ".";
                break;
            case 7:
                // If peer 1 receives an interested message from peer 2
                logFileData = formattedDateTime + ": Peer " + peerID + "  received the ‘interested’ message from " + peerID2 + ".";
                break;
            case 8:
                // If peer 1 receives a not interested message from peer 2
                logFileData = formattedDateTime + ": Peer " + peerID + " received the ‘not interested’ message from " + peerID2 + ".";
                break;
            case 9:
                // If peer 1 receives a piece message from peer 2 adds that the download finishes and was successful
                logFileData = formattedDateTime + ": Peer " + peerID + " has downloaded the piece " + pieceIndex + " from " + peerID2 + "." + " Now the number of pieces it has is " + totalPieces + ".";
                totalPieces++;
                break;
            case 10:
                // If peer 1 finishes downloading the file
                logFileData = formattedDateTime + ": Peer " + peerID + " has downloaded the complete file.";
                break;
            default:
                // If there is some error message
                System.out.println("Error: Type of message not recognized.");
                break;
        }

        // Cleaning up and writing to the log file
        try
        {
            bw.write(logFileData);
            bw.close();
            fw.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }


}