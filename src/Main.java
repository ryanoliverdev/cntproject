import java.net.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {

        Client client = new Client();
        client.startConnection("127.0.0.1", 6666);

        String response = client.sendMessage("hello jonathan");

        System.out.println(response);
        System.out.println(args[0]);
    }}