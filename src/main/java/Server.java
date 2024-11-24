import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Server {

    private static final int PORT = 1235;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private String filename;
    private String username;
    private static String fileLine;
    private static String passwordHash;
    private static String hashMethod;
    private static String salt;
    private static String saltPasswordHash;

    private long start;

    public Server(String filename, String username) {
        this.filename = filename;
        this.username = username;
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("--------------------------------------------------------------------");
            System.out.println("Server started on port: " + PORT);
            System.out.println("--------------------------------------------------------------------");

            Thread commandThread = new Thread(() -> handleServerCommands());
            commandThread.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void passwordFound() {
        long elapsedTimeSeconds = (System.nanoTime() - start) / 1000000000;
        long minutes = elapsedTimeSeconds / 60;
        long reminderSeconds = elapsedTimeSeconds % 60;
        System.out.println("Elapsed time: " + minutes + " minutes and " + reminderSeconds + " seconds.");

        System.out.println("\nPassword found. Stopping all clients...");
        broadCastMessage("STOP");

        System.exit(0);
    }

    private void handleServerCommands() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            switch (command.toLowerCase()) {
                case "start":
                    sendCrackInfo();
                    start = System.nanoTime();
                    if (!clients.isEmpty()) {
                        System.out.println("Clients initiated attempt to crack password...");
                    }
                    break;
                case "stop":
                    System.out.println("Stopping all clients.");
                    broadCastMessage("STOP");
                    break;
                case "quit":
                    broadCastMessage("STOP");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown command. Available options: start, stop, quit.");
            }
        }
    }

    public void sendCrackInfo() {
        synchronized (clients) {
            readFile(filename, username);

            clients.removeIf(client -> !client.isConnected());

            if (clients.isEmpty()) {
                System.out.println("No clients connected.");
                return;
            } else {
                System.out.println("Number of clients: " + clients.size());
            }

            int charsetLength = CHARSET.length();
            int partitionSize = charsetLength / clients.size();

            for (int i = 0; i < clients.size(); i++) {
                int startIdx = i * partitionSize;
                int endIdx;
                boolean isLastClient = (i == clients.size() - 1);

                if (isLastClient) {
                    endIdx = charsetLength;
                } else {
                    endIdx = startIdx + partitionSize;
                }

                String prefixRange = CHARSET.substring(startIdx, endIdx);

                String message = "HASH_INFO";
                message += "\n";
                message += salt;
                message += "\n";
                message += saltPasswordHash;
                message += "\n";
                message += prefixRange;

                clients.get(i).sendMessage(message);
                System.out.println("Sent range: " + prefixRange + " to client " + i);
            }
        }
    }

    public void broadCastMessage(String message) {
        synchronized (clients) {
            clients.removeIf(client -> !client.isConnected());
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    public void readFile(String fileName, String username) {
        boolean usernameFound = false;
        try {
            File file = new File("src/main/java/" + fileName);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(":");
                if (tokens[0].equals(username)) {
                    usernameFound = true;
                    fileLine = tokens[1];
                    break;
                }
            }
            if (!usernameFound) {
                System.out.println("Username not found in Shadow file. Please start again.");
                System.exit(0);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("No Shadow file found.");
            System.exit(0);
        }

        String[] fields = fileLine.split("\\$");
        hashMethod = fields[1];
        salt = "$" + hashMethod + "$" + fields[2];
        passwordHash = fields[3];
        saltPasswordHash = salt + "$" + passwordHash;
        System.out.println("The salt: " + salt);
        System.out.println("The password hash: " + saltPasswordHash);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("--------------------------------------------------------------------");
        System.out.print("Enter file name: ");
        String filename = scanner.nextLine();

        System.out.print("Enter Username: ");
        String username = scanner.nextLine();

//        System.out.println("Which method would you like?\n(1) Brute Force\n(2) Dictionary");
//        String methodPicked = scanner.nextLine();

        Server server = new Server(filename, username);
        server.startServer();

        scanner.close();
    }
}
