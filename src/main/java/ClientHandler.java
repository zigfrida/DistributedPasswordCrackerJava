import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Server server;
    private Socket socket;
    private BufferedWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            out.write(message);
            out.newLine();
            out.flush();
        } catch (IOException e) {

        }
    }

    public boolean isConnected() {
        return !socket.isClosed();
    }

    private void closeEverything() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equals("FOUND")) {
                    String password = in.readLine();
                    System.out.println("Client " + socket + " says password found. Password: " + password);
                    server.passwordFound();
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket);
        } finally {
            closeEverything();
        }
    }
}
