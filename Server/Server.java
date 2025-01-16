package Server;

import Classes.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private DatagramSocket socket;
    private Tris board = new Tris();
    private boolean currentPlayer = true; // true: X, false: O
    private Map<InetAddress, ClientInfo> clients = new HashMap<>();
    private boolean gameStarted = false;  // Aggiungiamo questa variabile

    private class ClientInfo {
        boolean isX;
        int port;

        ClientInfo(boolean isX, int port) {
            this.isX = isX;
            this.port = port;
        }
    }

    public static void main(String[] args) {
        new Server().start();
    }

    public void start() {
        try {
            socket = new DatagramSocket(PORT);
            System.out.println("Server avviato. In attesa di giocatori...");

            while (true) {
                try {
                    byte[] buffer = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    InetAddress clientAddress = packet.getAddress();
                    int clientPort = packet.getPort();
                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
                    
                    System.out.println("Ricevuto messaggio: " + message + " da " + clientAddress + ":" + clientPort);

                    if (message.equals("CONNECT")) {
                        if (!clients.containsKey(clientAddress) || 
                            (clients.containsKey(clientAddress) && clients.get(clientAddress).port != clientPort)) {
                            
                            if (clients.size() < 2) {
                                boolean isX = clients.isEmpty();
                                ClientInfo clientInfo = new ClientInfo(isX, clientPort);
                                clients.put(clientAddress, clientInfo);
                                
                                String symbol = isX ? "X" : "O";
                                sendMessage(clientAddress, clientPort, symbol);
                                
                                System.out.println("Nuovo giocatore connesso: " + symbol + 
                                                 " da " + clientAddress + ":" + clientPort);
                                System.out.println("Giocatori connessi: " + clients.size());

                                // Se abbiamo due giocatori e il gioco non è ancora iniziato
                                if (clients.size() == 2 && !gameStarted) {
                                    gameStarted = true;
                                    System.out.println("Invio START a tutti i client");
                                    // Invia START a tutti i client
                                    for (Map.Entry<InetAddress, ClientInfo> entry : clients.entrySet()) {
                                        sendMessage(entry.getKey(), entry.getValue().port, "START");
                                        System.out.println("START inviato a " + entry.getKey() + ":" + entry.getValue().port);
                                    }
                                }
                            }
                        }
                    } else if (clients.containsKey(clientAddress)) {
                        if (!gameStarted || clients.size() < 2) {
                            sendMessage(clientAddress, clientPort, "WAIT");
                            continue;
                        }

                        if (message.contains(",")) {
                            String[] parts = message.split(",");
                            int row = Integer.parseInt(parts[0]);
                            int col = Integer.parseInt(parts[1]);

                            ClientInfo clientInfo = clients.get(clientAddress);
                            if (makeMove(row, col, clientInfo.isX)) {
                                broadcast("MOVE," + row + "," + col + "," + (clientInfo.isX ? "X" : "O"));

                                if (board.checkWin(clientInfo.isX)) {
                                    broadcast("WIN," + (clientInfo.isX ? "X" : "O"));
                                    resetGame();
                                } else if (board.isFull()) {
                                    broadcast("DRAW");
                                    resetGame();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Errore durante la gestione del client: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Errore fatale del server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleConnection(InetAddress clientAddress, int clientPort) {
        if (!clients.containsKey(clientAddress) || 
            (clients.containsKey(clientAddress) && clients.get(clientAddress).port != clientPort)) {
            
            if (clients.size() < 2) {
                boolean isX = clients.isEmpty();
                ClientInfo clientInfo = new ClientInfo(isX, clientPort);
                clients.put(clientAddress, clientInfo);
                
                String symbol = isX ? "X" : "O";
                sendMessage(clientAddress, clientPort, symbol);
                
                System.out.println("Nuovo giocatore connesso: " + symbol + 
                                 " da " + clientAddress + ":" + clientPort);
                System.out.println("Giocatori connessi: " + clients.size());

                if (clients.size() == 2) {
                    System.out.println("Due giocatori connessi, invio messaggio START");
                    // Aggiungi un piccolo delay prima di inviare START
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    broadcast("START");
                    System.out.println("Messaggio START inviato a tutti i client");
                }
            }
        }
    }

    private void handleMove(String input, InetAddress clientAddress) {
        String[] parts = input.split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);

        ClientInfo clientInfo = clients.get(clientAddress);
        if (makeMove(row, col, clientInfo.isX)) {
            broadcast("MOVE," + row + "," + col + "," + (clientInfo.isX ? "X" : "O"));

            if (board.checkWin(clientInfo.isX)) {
                broadcast("WIN," + (clientInfo.isX ? "X" : "O"));
                resetGame();
            } else if (board.isFull()) {
                broadcast("DRAW");
                resetGame();
            }
        }
    }

    private synchronized boolean makeMove(int row, int col, boolean player) {
        if (player == currentPlayer && board.occupaCasella(row, col, player)) {
            currentPlayer = !currentPlayer;
            return true;
        }
        return false;
    }

    private void sendMessage(InetAddress address, int port, String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
            System.out.println("Messaggio inviato a " + address + ":" + port + " -> " + message);
        } catch (Exception e) {
            System.err.println("Errore nell'invio del messaggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcast(String message) {
        for (Map.Entry<InetAddress, ClientInfo> entry : clients.entrySet()) {
            sendMessage(entry.getKey(), entry.getValue().port, message);
        }
    }

    private void resetGame() {
        board = new Tris();
        currentPlayer = true;
        gameStarted = true;  // Il gioco rimane attivo dopo il reset
        broadcast("RESET");
    }
}