package Server;

import Classes.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static Tris board = new Tris();
    private static boolean currentPlayer = true; // true: X, false: O
    private static HashMap<Boolean, InetSocketAddress> players = new HashMap<>();
    private static DatagramSocket socket;

    public static void main(String[] args) 
    {
        try {
            socket = new DatagramSocket(PORT);
            System.out.println("Server avviato. In attesa di giocatori...");

            while (players.size() < 2) 
            {
                DatagramPacket packet = receivePacket();
                String message = new String(packet.getData(), 0, packet.getLength());

                if (message.equals("JOIN")) 
                {
                    boolean isX = players.isEmpty();
                    players.put(isX, (InetSocketAddress) packet.getSocketAddress());
                    sendMessage(isX ? "X" : "O", (InetSocketAddress)packet.getSocketAddress());
                    System.out.println("Giocatore connesso: " + (isX ? "X" : "O"));
                }
            }

            while (true) 
            {
                DatagramPacket packet = receivePacket();
                String message = new String(packet.getData(), 0, packet.getLength());
                handleClientMessage(message, (InetSocketAddress) packet.getSocketAddress());
            }

        } catch (IOException e) 
        {
            System.err.println("Errore nel server: " + e.getMessage());
        }
    }

    private static DatagramPacket receivePacket() throws IOException 
    {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    private static void sendMessage(String message, InetSocketAddress address) throws IOException 
    {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address);
        socket.send(packet);
    }

    private static void broadcast(String message) 
    {
        players.values().forEach(address -> {
            try {
                sendMessage(message, address);
            } catch (IOException e) {
                System.err.println("Errore nell'invio del messaggio: " + e.getMessage());
            }
        });
    }

    private static synchronized boolean makeMove(int row, int col, boolean player) 
    {
        if (player == currentPlayer && board.occupaCasella(row, col, player)) {
            currentPlayer = !currentPlayer;
            return true;
        }

        return false;
    }

    private static synchronized void resetGame() 
    {
        board = new Tris();
        currentPlayer = true;
        broadcast("RESET");
    }

    private static void handleClientMessage(String message, InetSocketAddress clientAddress) 
    {
        String[] parts = message.split(";");

        switch (parts[0]) 
        {
            case "MOVE":
                int row = Integer.parseInt(parts[1]);
                int col = Integer.parseInt(parts[2]);
                boolean isX = players.get(true).equals(clientAddress);

                if (makeMove(row, col, isX)) 
                {
                    broadcast("MOVE;" + row + ";" + col + ";" + (isX ? "X" : "O"));

                    if (board.checkWin(isX)) 
                    {
                        broadcast("WIN;" + (isX ? "X" : "O"));
                        resetGame();
                    } 
                    
                    else if (board.isFull()) 
                    {
                        broadcast("DRAW");
                        resetGame();
                    }
                }
                break;

            default:
                System.err.println("Messaggio non riconosciuto: " + message);
        }
    }
}
