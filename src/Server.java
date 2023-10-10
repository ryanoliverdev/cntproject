import java.net.*;
import java.io.*;

public class Server {
    private Socket clientSocket;
    private ServerSocket serverSocket;
    private PrintWriter output;
    private BufferedReader input;

    public void startServer(int port)
    {
        try
        {
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();

            output = new PrintWriter(clientSocket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String test = input.readLine();

            if("hello jonathan".equals(test))
            {
                output.println("hello friendly client");
            }
            else
            {
                output.println("Greeting not working");
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            input.close();
            output.close();
            clientSocket.close();
            serverSocket.close();
        }
        catch (IOException error)
        {
            throw new RuntimeException(error);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer(6666);
    }

}
