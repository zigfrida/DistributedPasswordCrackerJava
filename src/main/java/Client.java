import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final String SERVER_ADDRESS = "192.168.0.7";
    private static final int SERVER_PORT = 1235;
    private static String prefixRange = "FGHIJKLMNOPQRSTUVWXYZ0123456789";

    private Socket socket;
    private BufferedWriter out;
    private BufferedReader in;

    private Thread crackerThread;
    private boolean found = false;
    private String password = "";

    PasswordCracker cracker;

    public Client(Socket socket) {
        try {
            this.socket = socket;
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void sendMessage() {
        try {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("found")) {
                    out.write("FOUND");
                    out.newLine();
                    out.flush();
                    break;
                } else if (userInput.equalsIgnoreCase("quit")) {
                    System.out.println("Disconnecting from server...");
                    socket.close();
                    System.exit(0);
                    break;
                } else if (userInput.equalsIgnoreCase("range")) {
                    if (prefixRange != null) {
                        System.out.println("Current prefix range: " + prefixRange);
                    } else {
                        System.out.println("No prefix range assigned yet");
                    }
                }
            }
            scanner.close();
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void checkTerminate() {
        try {
            if (found) {
                password = cracker.getPassword();
                out.write("FOUND");
                out.newLine();
                out.write(password);
                out.newLine();
                out.flush();
            }
        } catch (IOException e) {

        }
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

    public void listenForMessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String serverMessage;

                    while (socket.isConnected()) {
                        serverMessage = in.readLine();
                        if (serverMessage == null) break;

                        System.out.println("Message received: " + serverMessage);
                        if (serverMessage.startsWith("START: ")) {
                            prefixRange = serverMessage.substring(6);
                            System.out.println("Received prefix range: " + prefixRange);
                        } else if ("HASH_INFO".startsWith(serverMessage)) {
                            String salt = in.readLine();
                            String saltPasswordHash = in.readLine();
                            prefixRange = in.readLine();
                            System.out.println("Received salt: " + salt);
                            System.out.println("Received hash: " + saltPasswordHash);
                            System.out.println("Prefix received: " + prefixRange);
                            cracker = new PasswordCracker(prefixRange, salt, saltPasswordHash);
                            crackerThread = new Thread(() -> {
                                found = cracker.bruteForceThreads();
                                checkTerminate();
                            });
                            crackerThread.start();
                        } else if (serverMessage.equals("STOP")) {
                            System.out.println("Received stop command. Terminating Client.");
                            closeEverything();
                            System.exit(0);
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                    closeEverything();
                    System.exit(0);
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            Client client = new Client(socket);
            System.out.println("--------------------------------------------------------------------");
            System.out.println("Client connected to server: " + SERVER_ADDRESS  + " on port: " + SERVER_PORT);
            System.out.println("--------------------------------------------------------------------");

            client.listenForMessages();
            client.sendMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
