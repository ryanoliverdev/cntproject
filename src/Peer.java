import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class Peer
{
    FileData fileData;
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
    boolean p2pFinished;
    int numOfPieces;
    int numOfPiecesHave;
    byte[] bitfield;
    int optimisticallyUnChokedNeighbor = -1;
    ConcurrentHashMap<Integer, Integer> piecesSent = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Socket> connections = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Object> connectionLocks = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Boolean> isChokedPeer = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Boolean> isInterestedPeer = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Boolean> hasFilePeers = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, byte[]> hasPiecesPeers = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Double> neighbors = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Peer> otherPeers = new ConcurrentHashMap<>();
    LinkedHashMap<Integer, String[]> pInfo;
    // Stores neighbors and download rate (as a double)

    // PeerID's of preferredNeighbors along with download rates (maybe can get rid of these after sorting)\
    public ArrayList<Integer> preferredNeighbors = new ArrayList<>();
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
        hasPiecesPeers.putIfAbsent(srcPeerID, bitfield);
    }
    public void setHasFile(int srcPeerID)
    {
        hasFilePeers.put(srcPeerID, true);
    }
    public void setOwnBitfield(byte[] bf)
    {
        bitfield = bf;
    }
    FileLogger logger;

    public Peer(int id, LinkedHashMap<String, String> commonInfo, LinkedHashMap<Integer, String[]> peerInfo)
    {

        p2pFinished = false;
        peerID = id;
        unchokingInterval = Integer.parseInt(commonInfo.get("UnchokingInterval"));
        optimisticUnchokingInterval = Integer.parseInt(commonInfo.get("OptimisticUnchokingInterval"));
        fileName = commonInfo.get("FileName");
        fileSize = Integer.parseInt(commonInfo.get("FileSize"));
        pieceSize = Integer.parseInt(commonInfo.get("PieceSize"));
        kNeighbors = Integer.parseInt(commonInfo.get("NumberOfPreferredNeighbors"));
        fileData = new FileData(fileSize, pieceSize, fileName);
        // Reading in all PeerInfo.cfg Info
        numOfPieces = (int) Math.ceil((double) fileSize / pieceSize);
        bitfield = new byte[numOfPieces];
        Arrays.fill(bitfield, (byte) 0);
        System.out.println(numOfPieces);
        hostName = peerInfo.get(id)[0];
        portNumber = Integer.parseInt(peerInfo.get(id)[1]);
        hasFile = Integer.parseInt(peerInfo.get(id)[2]) == 1;
        pInfo = peerInfo;
        logger = new FileLogger(peerID);
        logger.loggingStart();

        if (hasFile)
        {
            numOfPiecesHave = (int) Math.ceil((double) fileSize / pieceSize);
        }
        else
        {
            numOfPiecesHave = 0;
        }

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
        ArrayList<Integer> interestedNeighbors = new ArrayList<>();
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

        // If this peer has the file, then preferred neighbors are a random list of k interested
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
            interestedNeighbors.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return neighbors.get(o1).compareTo(neighbors.get(o2));
                }
            });

            Collections.reverse(interestedNeighbors);
            int endIndex = Math.min(k, interestedNeighbors.size());
            preferredNeighbors = new ArrayList<>(interestedNeighbors.subList(0, endIndex));

        }
    }
    public ArrayList<Integer> getPreferredNeighbors()
    {
        ArrayList<Integer> prefNeighbors = new ArrayList<>(preferredNeighbors);
        System.out.println("Number of preferred neighbors: " + prefNeighbors.size());

        return prefNeighbors;
    }

    public void setInitialNeighbors()
    {
        // Choked contains all available neighbors (unchoked or choked)
        int numOfNeighbors = isChokedPeer.size();
        ConcurrentHashMap<Integer, Double> newNeighbors = new ConcurrentHashMap<>();

        for (Map.Entry<Integer, Boolean> entry : isChokedPeer.entrySet())
        {
            Integer peerID = entry.getKey();
            // initial download rate = 0.0
            newNeighbors.putIfAbsent(peerID, 0.0);
        }

        this.neighbors = newNeighbors;
   }

   public void setOptimisticallyUnChokedNeighbor()
   {
       // get all choked neighbors that are interested
       ArrayList<Integer> possibleOptimisticNeighbors = new ArrayList<>();

       for (Map.Entry<Integer, Boolean> entry : isChokedPeer.entrySet())
       {
           Integer peerID = entry.getKey();
           Boolean choked = entry.getValue();
           Boolean interested = isInterestedPeer.get(peerID);

           if (choked && interested)
           {
               possibleOptimisticNeighbors.add(peerID);
           }
       }

       if(possibleOptimisticNeighbors.size() == 0)
       {
           optimisticallyUnChokedNeighbor = -1;
           return;
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
                logger.writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // unchoke message type
            case 1:
                unChokePeer(srcPeerID);
                logger.writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // setInterest message type
            case 2:
                setInterestPeer(srcPeerID);
                logger.writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // unsetInterest message type
            case 3:
                unSetInterestPeer(srcPeerID);
                logger.writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // hasFile message type
            case 4:
                setPeerPiecesBitfield(srcPeerID, message);
                logger.writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // bitfield message type
            case 5:
                sendBitfield(srcPeerID);
                logger.writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // request message type
            case 6:
                requestPieces(srcPeerID);
                logger.writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
            // pieces message type
            case 7:
                sendPieces(srcPeerID);
                logger.writeLogMessage(type, peerID, srcPeerID, 0, 0);
                break;
        }
    }

    // Still needs more implementation


}