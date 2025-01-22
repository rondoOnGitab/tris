package Client;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private BufferedWriter out;
    private BufferedReader in;
    private boolean isMyTurn = false;
    private String mySymbol;
    private String opponentSymbol;
    private JButton[][] buttons = new JButton[3][3];
    private JFrame frame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new Client().start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void start() throws IOException {
        DatagramSocket socket = new DatagramSocket(SERVER_PORT, InetAddress.getByName(SERVER_ADDRESS));
        //contiene messaggio
        String messaggio = "Hello, server!";
        byte[] buffer = messaggio.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.send(packet);

        //out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        mySymbol = in.readLine();
        opponentSymbol = mySymbol.equals("X") ? "O" : "X";
        isMyTurn = mySymbol.equals("X");

        createGUI();

        new Thread(this::listenForMessages).start();
    }

    private void createGUI() {
        frame = new JFrame("Tic Tac Toe - Player: " + mySymbol);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 450);
        frame.setLayout(new GridLayout(3, 3));

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
                frame.add(button);
            }
        }

        frame.setVisible(true);
    }

    private void handleButtonClick(int row, int col) {
        if (isMyTurn && buttons[row][col].getText().isEmpty()) {
            try {
                out.write(row + "," + col);
                out.newLine();
                out.flush();
                isMyTurn = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(",");

                switch (parts[0]) {
                    case "MOVE":
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);
                        String symbol = parts[3];
                        buttons[row][col].setText(symbol);
                        buttons[row][col].setEnabled(false);
                        buttons[row][col].setBackground(symbol.equals("X") ? new Color(200, 220, 240) : new Color(240, 200, 220));
                        isMyTurn = symbol.equals(opponentSymbol);
                        break;

                    case "WIN":
                        String winner = parts[1];
                        JOptionPane.showMessageDialog(frame, "Player " + winner + " wins!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                        resetBoard();
                        break;

                    case "DRAW":
                        JOptionPane.showMessageDialog(frame, "It's a draw!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                        resetBoard();
                        break;

                    case "RESET":
                        resetBoard();
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetBoard() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    buttons[i][j].setText("");
                    buttons[i][j].setEnabled(true);
                    buttons[i][j].setBackground(new Color(240, 240, 240));
                }
            }
            isMyTurn = mySymbol.equals("X");
        });
    }
}
