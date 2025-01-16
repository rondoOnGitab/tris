package Client;

import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private DatagramSocket socket;
    private volatile boolean isMyTurn = false;
    private String mySymbol;
    private String opponentSymbol;
    private JButton[][] buttons = new JButton[3][3];
    private JFrame frame;
    private volatile boolean gameStarted = false;  // Nuova variabile
    

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Client client = new Client();
                client.initialize();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "Errore durante l'avvio del client: " + e.getMessage(),
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void initialize() throws Exception {
        socket = new DatagramSocket();
        System.out.println("Socket creato sulla porta: " + socket.getLocalPort());
        
        createGUI();
        connectToServer();
        
        // Disabilita tutti i bottoni all'inizio
        enableButtons(false);
        
        new Thread(this::listenForMessages).start();
    }

    private void connectToServer() {
        try {
            String connectionMessage = "CONNECT";
            byte[] buffer = connectionMessage.getBytes();
            InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_PORT);
            
            System.out.println("Invio richiesta di connessione al server...");
            socket.send(packet);
            
            buffer = new byte[256];
            packet = new DatagramPacket(buffer, buffer.length);
            
            System.out.println("In attesa della risposta dal server...");
            socket.receive(packet);
            
            String received = new String(packet.getData(), 0, packet.getLength()).trim();
            System.out.println("Risposta ricevuta dal server: " + received);
            
            if (received.equals("X") || received.equals("O")) {
                mySymbol = received;
                opponentSymbol = mySymbol.equals("X") ? "O" : "X";
                isMyTurn = false; // Inizialmente nessuno può muovere
                gameStarted = false; // Il gioco non è ancora iniziato
                
                SwingUtilities.invokeLater(() -> {
                    frame.setTitle("Tic Tac Toe - Player: " + mySymbol + " - In attesa del secondo giocatore...");
                    enableButtons(false); // Disabilita tutti i bottoni
                });
                
                System.out.println("Connessione completata. Simbolo assegnato: " + mySymbol);
            } else {
                System.out.println("Risposta non valida dal server: " + received);
                JOptionPane.showMessageDialog(frame, 
                    "Errore di connessione: risposta non valida dal server",
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Errore durante la connessione: " + e.getMessage());
            JOptionPane.showMessageDialog(frame, 
                "Errore di connessione al server: " + e.getMessage(),
                "Errore",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enableButtons(boolean enable) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setEnabled(enable);
            }
        }
    }

    private void createGUI() {
        frame = new JFrame("Tic Tac Toe - Connecting...");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 450);
        frame.setLayout(new GridLayout(3, 3));

        // Posiziona la finestra in modo casuale
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) (Math.random() * (screenSize.width - 450));
        int y = (int) (Math.random() * (screenSize.height - 450));
        frame.setLocation(x, y);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                JButton button = new JButton();
                button.setFont(new Font("Arial", Font.BOLD, 50));
                button.setFocusPainted(false);
                button.setBackground(new Color(240, 240, 240));
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                buttons[i][j] = button;
                
                int row = i;
                int col = j;
                button.addActionListener(e -> handleButtonClick(row, col));
                button.setEnabled(false);
                frame.add(button);
            }
        }

        frame.setVisible(true);
    }

    private void handleButtonClick(int row, int col) {
        System.out.println("Click sul bottone [" + row + "," + col + "] - gameStarted: " + gameStarted + ", isMyTurn: " + isMyTurn);
        
        if (!gameStarted) {
            JOptionPane.showMessageDialog(frame, 
                "Attendi che si connetta il secondo giocatore!",
                "Attendi",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        if (!isMyTurn) {
            JOptionPane.showMessageDialog(frame, 
                "Non è il tuo turno!",
                "Attendi",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        if (buttons[row][col].getText().isEmpty()) {
            try {
                String message = row + "," + col;
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(
                    buffer, 
                    buffer.length, 
                    InetAddress.getByName(SERVER_ADDRESS), 
                    SERVER_PORT
                );
                socket.send(packet);
                System.out.println("Mossa inviata al server: " + message);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, 
                    "Errore durante l'invio della mossa: " + e.getMessage(),
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void listenForMessages() {
        while (true) {
            try {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("Messaggio ricevuto dal server: " + message);

                SwingUtilities.invokeLater(() -> {
                    if (message.equals("START")) {
                        System.out.println("Ricevuto messaggio START - Avvio del gioco");
                        gameStarted = true;
                        isMyTurn = mySymbol.equals("X");
                        
                        frame.setTitle("Tic Tac Toe - Player: " + mySymbol + 
                            (isMyTurn ? " - È il tuo turno!" : " - Turno dell'avversario"));
                        
                        enableButtons(isMyTurn);
                        
                        JOptionPane.showMessageDialog(frame, 
                            isMyTurn ? "Il gioco è iniziato! È il tuo turno!" : "Il gioco è iniziato! Attendi il tuo turno!",
                            "Inizio Gioco",
                            JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    if (!gameStarted) {
                        System.out.println("Messaggio ignorato perché il gioco non è ancora iniziato: " + message);
                        return;
                    }

                    String[] parts = message.split(",");
                    switch (parts[0]) {
                        case "MOVE":
                            handleMove(parts);
                            break;
                        case "WIN":
                            handleWin(parts[1]);
                            break;
                        case "DRAW":
                            handleDraw();
                            break;
                        case "RESET":
                            resetBoard();
                            break;
                    }
                });
            } catch (Exception e) {
                System.out.println("Errore durante l'ascolto dei messaggi: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
    }


    private void handleMove(String[] parts) {
        if (!gameStarted) return;
        
        int row = Integer.parseInt(parts[1]);
        int col = Integer.parseInt(parts[2]);
        String symbol = parts[3];
        
        // Se non è la nostra mossa, aggiorniamo la GUI
        if (!symbol.equals(mySymbol)) {
            buttons[row][col].setText(symbol);
            buttons[row][col].setEnabled(false);
            buttons[row][col].setBackground(
                symbol.equals("X") ? new Color(200, 220, 240) : new Color(240, 200, 220)
            );
        }
        
        isMyTurn = symbol.equals(opponentSymbol);
        if (isMyTurn) {
            enableButtons(true);
            frame.setTitle("Tic Tac Toe - Player: " + mySymbol + " - Il tuo turno!");
        } else {
            enableButtons(false);
            frame.setTitle("Tic Tac Toe - Player: " + mySymbol + " - Turno dell'avversario");
        }
    }

    private void handleWin(String winner) {
        JOptionPane.showMessageDialog(frame, 
            "Player " + winner + " wins!", 
            "Game Over", 
            JOptionPane.INFORMATION_MESSAGE);
        resetBoard();
    }

    private void handleDraw() {
        JOptionPane.showMessageDialog(frame, 
            "It's a draw!", 
            "Game Over", 
            JOptionPane.INFORMATION_MESSAGE);
        resetBoard();
    }

    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setBackground(new Color(240, 240, 240));
            }
        }
        
        if (gameStarted) {
            isMyTurn = mySymbol.equals("X");
            enableButtons(isMyTurn);
            frame.setTitle("Tic Tac Toe - Player: " + mySymbol + 
                (isMyTurn ? " - Il tuo turno!" : " - Turno dell'avversario"));
        }
    }
}
