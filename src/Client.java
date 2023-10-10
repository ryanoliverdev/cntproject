import java.net.*;
import java.io.*;

public class Client {

    private Socket clientSocket;
    private PrintWriter output;
    private BufferedReader input;

    public void startConnection(String ip, int port)
    {
        try
        {
            clientSocket = new Socket(ip, port);
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String sendMessage(String msg)
    {
        output.println(msg);
        String resp;

        try
        {
            resp = input.readLine();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return resp;
    }

    public void stopConnection()
    {
        try
        {
            input.close();
            output.close();
        }
        catch (IOException error)
        {
            throw new RuntimeException(error);
        }
    }
}
