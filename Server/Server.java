package Server;

import Classes.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static Tris board = new Tris();
    private static boolean currentPlayer = true; // true: X, false: O

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server avviato. In attesa di giocatori...");

            while (clients.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler client = new ClientHandler(clientSocket, clients.size() == 0);
                clients.add(client);
                new Thread(client).start();
                System.out.println("Giocatore connesso: " + (clients.size() == 1 ? "X" : "O"));
            }
        } catch (IOException e) {
            System.err.println("Errore nel server: " + e.getMessage());
        }
    }

    synchronized static boolean makeMove(int row, int col, boolean player) {
        if (player == currentPlayer && board.occupaCasella(row, col, player)) {
            currentPlayer = !currentPlayer;
            return true;
        }
        return false;
    }

    synchronized static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    synchronized static void resetGame() {
        board = new Tris();
        currentPlayer = true;
        broadcast("RESET");
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedWriter out;
        private BufferedReader in;
        private boolean isX;

        public ClientHandler(Socket socket, boolean isX) {
            this.socket = socket;
            this.isX = isX;
            try {
                this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                sendMessage(isX ? "X" : "O");
            } catch (IOException e) {
                System.err.println("Errore nella configurazione del client: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                String input;
                while ((input = in.readLine()) != null) {
                    String[] parts = input.split(",");
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);

                    if (makeMove(row, col, isX)) {
                        broadcast("MOVE," + row + "," + col + "," + (isX ? "X" : "O"));

                        if (board.checkWin(isX)) {
                            broadcast("WIN," + (isX ? "X" : "O"));
                            resetGame();
                        } else if (board.isFull()) {
                            broadcast("DRAW");
                            resetGame();
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Errore nel gestire il client: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void sendMessage(String message) {
            try {
                out.write(message);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                System.err.println("Errore nell'invio del messaggio: " + e.getMessage());
            }
        }
    }
}
