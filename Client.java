import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

public class Client {
    private PrintWriter out;
    private Socket socket;
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton connectButton;
    private JButton sendButton;
    private JLabel statusLabel;
    private String username;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().initialize());
    }

    private void initialize() {
        createGUI();
        username = JOptionPane.showInputDialog(frame, "Enter your username:", "Username", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            username = "User" + (int)(Math.random() * 1000);
        }
    }

    private void createGUI() {
        // Main frame setup
        frame = new JFrame("Chat Client - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setMinimumSize(new Dimension(400, 300));
        frame.setLocationRelativeTo(null);

        // Main panel with border
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Status bar
        statusLabel = new JLabel(" Disconnected");
        statusLabel.setBorder(new MatteBorder(1, 0, 0, 0, Color.GRAY));
        statusLabel.setPreferredSize(new Dimension(frame.getWidth(), 20));
        statusLabel.setForeground(Color.RED);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(new CompoundBorder(
            new MatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY),
            new EmptyBorder(5, 5, 5, 5)));

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        inputField = new JTextField();
        inputField.setEnabled(false);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(new CompoundBorder(
            new MatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY),
            new EmptyBorder(5, 5, 5, 5)));

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        
        connectButton = new JButton("Connect");
        connectButton.setFocusPainted(false);
        connectButton.addActionListener(e -> connectToServer());
        styleButton(connectButton, new Color(70, 130, 180), Color.WHITE);
        
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(e -> sendMessage());
        styleButton(sendButton, new Color(34, 139, 34), Color.WHITE);

        buttonPanel.add(connectButton);
        buttonPanel.add(sendButton);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // Add components to main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        // Input field action listener
        inputField.addActionListener(e -> sendMessage());

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void styleButton(JButton button, Color bgColor, Color textColor) {
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setBorder(new CompoundBorder(
            new LineBorder(bgColor.darker(), 1),
            new EmptyBorder(5, 15, 5, 15)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void connectToServer() {
        if (socket != null && !socket.isClosed()) {
            appendToChat("Already connected to server.");
            return;
        }

        connectButton.setEnabled(false);
        statusLabel.setText(" Connecting...");
        statusLabel.setForeground(Color.ORANGE);

        new Thread(() -> {
            try {
                socket = new Socket("localhost", 12345);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Send username first
                out.println(username);

                SwingUtilities.invokeLater(() -> {
                    inputField.setEnabled(true);
                    sendButton.setEnabled(true);
                    statusLabel.setText(" Connected to server");
                    statusLabel.setForeground(new Color(0, 100, 0));
                    inputField.requestFocus();
                    connectButton.setText("Disconnect");
                    connectButton.setEnabled(true);
                    connectButton.setBackground(new Color(220, 20, 60));
                    connectButton.addActionListener(e -> disconnect());
                });

                String line;
                while ((line = in.readLine()) != null) {
                    appendToChat(line);
                }

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    appendToChat("Connection failed: " + e.getMessage());
                    statusLabel.setText(" Disconnected");
                    statusLabel.setForeground(Color.RED);
                    connectButton.setEnabled(true);
                    connectButton.setText("Connect");
                    connectButton.setBackground(new Color(70, 130, 180));
                    inputField.setEnabled(false);
                    sendButton.setEnabled(false);
                });
            }
        }).start();
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            statusLabel.setText(" Disconnected");
            statusLabel.setForeground(Color.RED);
            connectButton.setText("Connect");
            connectButton.setBackground(new Color(70, 130, 180));
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
            appendToChat("Disconnected from server");
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            appendToChat("You: " + message);
            inputField.setText("");
        }
    }

    private void appendToChat(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
}