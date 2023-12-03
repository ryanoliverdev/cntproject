import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;

public class FileData {
    String fileName;
    int fileSize;
    int pieceSize;

    public FileData(int fSize, int pSize, String fName) {
        fileSize = fSize;
        pieceSize = pSize;
        fileName = fName;
    }

    public void setData(byte[] indexField, byte[] piece, int peerID) {
        try {
            File f = new File("./project_config_file_small/" + peerID + "/" + fileName);
            RandomAccessFile file;

            // If the file doesn't exist, create it and set its length
            if (!f.exists()) {
                file = new RandomAccessFile(f, "rw");
                file.setLength(fileSize);
            } else {
                // If the file exists, just open it
                file = new RandomAccessFile(f, "rw");
            }

            // Convert the indexField byte array to an int
            int index = ByteBuffer.wrap(indexField).getInt();

            // Calculate the position in the file where the piece should be written
            // This depends on the size of each piece
            int position = index * pieceSize;

            // Seek to the position and write the piece
            file.seek(position);
            file.write(piece);

            // Close the file
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getData(byte[] indexField, String filePath) {
        byte[] pieceData = null;
        try {
            // Convert the indexField byte array to an int
            int index = ByteBuffer.wrap(indexField).getInt();

            // Calculate the position in the file where the piece starts
            int position = index * pieceSize;

            // Open the file
            RandomAccessFile file = new RandomAccessFile(filePath, "r");

            // Create a byte array to hold the piece data
            pieceData = new byte[pieceSize];

            // Seek to the position and read the piece
            file.seek(position);
            file.readFully(pieceData);

            // Close the file
            file.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
        return pieceData;
    }
}

