import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class FileLogger {
    int peerID;
    String logFilePath;

    public FileLogger(int peerID) {
        this.peerID = peerID;
        this.logFilePath = "/log_peer_" + peerID + ".log";
    }

    public void loggingStart() {
        try {
            File logFile = new File(logFilePath);

            // If the log file doesn't exist, create it
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Log message for preferred neighbors
    public void writeLogMessage(int typeOfMessage, int peerID, int peerID2, int pieceIndex, int totalPieces, ArrayList<Integer> preferredNeighbors) {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentTime.format(formatter);

        try {
            // Open buffered writer to write to the log file
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFilePath, true));

            switch (typeOfMessage) {
                case 2:
                    // Case for if Peer 1 changes its preferred neighbors
                    String message = formattedDateTime + ": Peer " + peerID + " has the preferred neighbors \n";

                    for (int i = 0; i < preferredNeighbors.size(); i++) {
                        message += preferredNeighbors.get(i) + ", ";
                    }

                    message += ".\n";
                    bufferedWriter.write(message);

                    break;
            }

            bufferedWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLogMessage(int typeOfMessage, int peerID, int peerID2, int pieceIndex, int totalPieces)
    {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentTime.format(formatter);

        String logFileData = "";

        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFilePath, true));


            switch (typeOfMessage) {
                case 0:
                    // Peer 1 connects to Peer 2
                    logFileData = formattedDateTime + ": Peer " + peerID + " makes a connection to Peer " + peerID2 + ".";
                    break;
                case 1:
                    // Confirms connection from Peer 2 to Peer 1
                    logFileData = formattedDateTime + ": Peer " + peerID + " is connected from Peer " + peerID2 + ".";
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

            bufferedWriter.write(logFileData);
            bufferedWriter.newLine();
            bufferedWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
