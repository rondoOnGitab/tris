package Client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;

public class Client 
{
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private boolean isMyTurn = false;
    private String mySymbol;
    private String opponentSymbol;
    private JButton[][] buttons = new JButton[3][3];
    private JFrame frame;

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> 
        {
            try 
            {
                new Client().start();
            } catch (IOException e) 
            {
                e.printStackTrace();
            }
        });
    }

    public void start() throws IOException 
    {
        socket = new DatagramSocket();
        serverAddress = InetAddress.getByName(SERVER_ADDRESS);

        sendMessage("JOIN");

        mySymbol = receiveMessage();
        opponentSymbol = mySymbol.equals("X") ? "O" : "X";
        isMyTurn = mySymbol.equals("X");

        createGUI();

        new Thread(this::listenForMessages).start();
    }

    private void createGUI() 
    {
        frame = new JFrame("Giocatore: " + mySymbol);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 450);
        frame.setLayout(new GridLayout(3, 3));

        for (int i = 0; i < 3; i++) 
        {
            for (int j = 0; j < 3; j++) 
            {
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

    private void handleButtonClick(int row, int col) 
    {
        if (isMyTurn && buttons[row][col].getText().isEmpty()) 
        {
            try 
            {
                String message = "MOVE," + row + "," + col;
                sendMessage(message);
                isMyTurn = false;
            } catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }

    private void listenForMessages() 
    {
        try 
        {
            while (true) 
            {
                String message = receiveMessage();
                String[] parts = message.split(",");

                switch (parts[0]) 
                {
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
                        JOptionPane.showMessageDialog(frame, "Giocatore " + winner + " vittoria!", "Fine partita", JOptionPane.INFORMATION_MESSAGE);
                        resetBoard();
                        break;

                    case "DRAW":
                        JOptionPane.showMessageDialog(frame, "Pareggio!", "Fine partita", JOptionPane.INFORMATION_MESSAGE);
                        resetBoard();
                        break;

                    case "RESET":
                        resetBoard();
                        break;
                }
            }
        } catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    private void resetBoard() 
    {
        SwingUtilities.invokeLater(() -> 
        {
            for (int i = 0; i < 3; i++) 
            {
                for (int j = 0; j < 3; j++) 
                {
                    buttons[i][j].setText("");
                    buttons[i][j].setEnabled(true);
                    buttons[i][j].setBackground(new Color(240, 240, 240));
                }
            }
            isMyTurn = mySymbol.equals("X");
        });
    }

    private void sendMessage(String message) throws IOException 
    {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_PORT);
        socket.send(packet);
    }

    private String receiveMessage() throws IOException 
    {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength());
    }
}
