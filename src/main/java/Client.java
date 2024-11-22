import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1235;
    private static String prefixRange = "FGHIJKLMNOPQRSTUVWXYZ0123456789";

    private Socket socket;
//    private PrintWriter out;
    private BufferedWriter out;
    private BufferedReader in;

    private boolean found = false;

    PasswordCracker cracker;

    public Client(Socket socket) {
        try {
            this.socket = socket;
//            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {

        }

    }

    public void sendMessage() {
        try {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("found")) {
//                out.println("FOUNT");
                    out.write("FOUND");
                    out.newLine();
                    out.flush();
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

        }
    }

    public void checkTerminate() {
        try {
            if (found) {
                out.write("FOUND");
                out.newLine();
                out.flush();
            }
        } catch (IOException e) {

        }
    }

    public void listenForMessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String serverMessage;

//                    while((serverMessage = in.readLine()) != null) {
                    while (socket.isConnected()) {
                        serverMessage = in.readLine();
                        if (serverMessage == null) break;

                        System.out.println("Message received: " + serverMessage);
                        if (serverMessage.startsWith("START: ")) {
                            prefixRange = serverMessage.substring(6);
                            System.out.println("Received prefix range: " + prefixRange);
//                            break;
                        } else if ("HASH_INFO".startsWith(serverMessage)) {
                            String salt = in.readLine();
                            String saltPasswordHash = in.readLine();
                            prefixRange = in.readLine();
                            System.out.println("Received salt: " + salt);
                            System.out.println("Received hash: " + saltPasswordHash);
                            System.out.println("Prefix received: " + prefixRange);
                            cracker = new PasswordCracker(prefixRange, salt, saltPasswordHash);
                            found = cracker.bruteForceThreads();
                        } else if (serverMessage.equals("STOP")) {
                            System.out.println("Received stop command");
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
                            System.exit(0);
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server");
                    System.exit(0);
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            Client client = new Client(socket);

            client.listenForMessages();
            client.sendMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
