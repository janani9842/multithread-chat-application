
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Multithreaded Chat Application
 *
 * Contains both server and client implementations in a single file. Run with: -
 * For server: java ChatApplication server - For client: java ChatApplication
 * client
 */
public class ChatApplication {

    // Server configuration
    private static final int PORT = 12345;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("  For server: java ChatApplication server");
            System.out.println("  For client: java ChatApplication client");
            return;
        }

        if (args[0].equalsIgnoreCase("server")) {
            startServer();
        } else if (args[0].equalsIgnoreCase("client")) {
            startClient();
        } else {
            System.out.println("Invalid argument. Use 'server' or 'client'");
        }
    }

    /**
     * Starts the chat server
     */
    private static void startServer() {
        System.out.println("Chat Server is running...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Starts the chat client
     */
    private static void startClient() {
        try (Socket socket = new Socket("localhost", PORT); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter out = new PrintWriter(socket.getOutputStream(), true); BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to chat server. Type '/quit' to exit.");

            // Start a thread to read messages from server
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server");
                }
            }).start();

            // Read user input and send to server
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                if ("/quit".equalsIgnoreCase(userInput)) {
                    out.println("/quit");
                    break;
                }
                out.println(userInput);
            }
        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }
    }

    /**
     * Handles communication with a single client
     */
    private static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Get client name
                out.println("Enter your name:");
                clientName = in.readLine();
                broadcastMessage(clientName + " has joined the chat!");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if ("/quit".equalsIgnoreCase(inputLine)) {
                        break;
                    }
                    broadcastMessage(clientName + ": " + inputLine);
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Couldn't close a socket");
                }
                clients.remove(this);
                broadcastMessage(clientName + " has left the chat!");
                System.out.println("Client disconnected: " + clientSocket);
            }
        }

        /**
         * Sends a message to all connected clients except this one
         */
        private void broadcastMessage(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client != this) {
                        client.out.println(message);
                    }
                }
            }
        }
    }
}
