import java.io.*;
import java.nio.ByteBuffer;

public class Messages {
    public static byte[] getHandshakeMessage(int peerID)
    {
        byte[] handshakeMessage = new byte[32]; //32 byte handshake message: 18, 10, 4
        byte[] peerIDbytes = Integer.toString(peerID).getBytes();
        byte[] header = "P2PFILESHARINGPROJ".getBytes();

        //put header into handshake message array
        System.arraycopy(header, 0, handshakeMessage, 0, header.length);

        for(int i = 18; i < 28; i++)//start in array at index 18(after header)
        {
            handshakeMessage[i] = 0; //put in 0 bits for 10 bytes
        }

        System.arraycopy(peerIDbytes, 0, handshakeMessage, 28, 4);
        return handshakeMessage;
    }
    public static byte[] getChokeMessage()
    {
        int messageType = 0; // "choke" message type

        // Create a byte array to store the message
        byte[] chokeMessage = new byte[5]; // 4 bytes for length, 1 byte for message type

        // Calculate the message length (1 byte for the type, no payload)
        int messageLength = 1;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, chokeMessage, 0, 4);

        // Set the message type
        chokeMessage[4] = (byte) messageType;

        // Simulate sending the "choke" message to the peer
        return chokeMessage;

    }
    public static byte[] getUnChokeMessage()
    {
        int messageType = 1; // "unchoke" message type

        // Create a byte array to store the message
        byte[] unchokeMessage = new byte[5]; // 4 bytes for length, 1 byte for message type

        // Calculate the message length (1 byte for the type, no payload)
        int messageLength = 1;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, unchokeMessage, 0, 4);

        // Set the message type
        unchokeMessage[4] = (byte) messageType;

        // Send message to neighbor peers
        return unchokeMessage;

    }
    public static byte[] getInterestMessage()
    {
        int messageType = 2; // "interest" message type

        // Create a byte array to store the message
        byte[] interestMessage = new byte[5]; // 4 bytes for length, 1 byte for message type

        // Calculate the message length (1 byte for the type, no payload)
        int messageLength = 1;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, interestMessage, 0, 4);

        // Set the message type
        interestMessage[4] = (byte) messageType;

        // Send peer message
        return interestMessage;

    }
    public static byte[] getUnInterestMessage()
    {
        int messageType = 3; // "uninterest" message type

        // Create a byte array to store the message
        byte[] uninterestMessage = new byte[5]; // 4 bytes for length, 1 byte for message type

        // Calculate the message length (1 byte for the type, no payload)
        int messageLength = 1;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, uninterestMessage, 0, 4);

        // Set the message type
        uninterestMessage[4] = (byte) messageType;

        // Send peer message
        return uninterestMessage;

    }
    public static byte[] getHasFileMessage(byte[] indexField)
    {
        int messageType = 4; // "hasFile" message type

        // Create a byte array to store the message
        byte[] hasFileMessage = new byte[9]; // 4 bytes for length, 1 byte for message type, 4 bytes for payload

        // Calculate the message length (1 byte for the type)
        int messageLength = 5;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, hasFileMessage, 0, 4);

        // Set the message type
        hasFileMessage[4] = (byte) messageType;

        // Calculate the payload length (4 bytes for the indexField)
        System.arraycopy(indexField, 0, hasFileMessage, 5, 4);

        // Send peer message
        return hasFileMessage;

    }
    public static byte[] getBitfieldMessage(byte[] bitfield)
    {
        int messageType = 5; // "BitField" message type

        // Create a byte array to store the message
        byte[] bitfieldMessage = new byte[5 + bitfield.length]; // 4 bytes for length, 1 byte for message type, variable payload

        // Calculate the message length (1 byte for the type)
        int messageLength = 1 + bitfield.length;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, bitfieldMessage, 0, 4);

        // Set the message type
        bitfieldMessage[4] = (byte) messageType;

        // Calculate the payload length (4 bytes for the indexField)
        System.arraycopy(bitfield, 0, bitfieldMessage, 5, bitfield.length);

        // Send peer message
        return bitfieldMessage;

    }
    public static byte[] getRequestMessage(byte[] indexField)
    {
        int messageType = 6; // "request" message type

        // Create a byte array to store the message
        byte[] requestMessage = new byte[9]; // 4 bytes for length, 1 byte for message type, 4 bytes for payload

        // Calculate the message length (1 byte for the type)
        int messageLength = 5;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, requestMessage, 0, 4);

        // Set the message type
        requestMessage[4] = (byte) messageType;

        // Calculate the payload length (4 bytes for the indexField)
        System.arraycopy(indexField, 0, requestMessage, 5, 4);

        // Send peer message
        return requestMessage;

    }
    public static byte[] getPiecesMessage(byte[] indexField, byte[] pieceContent)
    {
        int messageType = 7; // "pieces" message type

        // Create a byte array to store the message
        // 4 bytes for length, 1 byte for message type, 4 bytes for index field, variable pieceContent length
        byte[] sendPiecesMessage = new byte[9 + pieceContent.length];

        // Calculate the message length (1 byte for the type)
        int messageLength = 5 + pieceContent.length;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, sendPiecesMessage, 0, 4);

        // Set the message type
        sendPiecesMessage[4] = (byte) messageType;

        // Calculate the payload length (4 bytes for the in dexField)
        System.arraycopy(indexField, 0, sendPiecesMessage, 5, 4);
        System.arraycopy(pieceContent, 0, sendPiecesMessage, 9, pieceContent.length);
        // Send peer message
        return sendPiecesMessage;

    }

    public static byte[] getHasFile()
    {
        int messageType = 8; // "choke" message type

        // Create a byte array to store the message
        byte[] hasFileMessage = new byte[5]; // 4 bytes for length, 1 byte for message type

        // Calculate the message length (1 byte for the type, no payload)
        int messageLength = 1;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(messageLength);
        System.arraycopy(buffer.array(), 0, hasFileMessage, 0, 4);

        // Set the message type
        hasFileMessage[4] = (byte) messageType;

        // Simulate sending the "choke" message to the peer
        return hasFileMessage;

    }
}
