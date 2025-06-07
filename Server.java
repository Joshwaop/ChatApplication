import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

public class Server {
    private static final int PORT = 12345;
    private Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private JFrame frame;
    private JTextArea logArea;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton startButton;
    private JButton stopButton;
    private JButton sendButton;
    private JLabel clientCountLabel;
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Server().initialize());
    }

    private void initialize() {
        createGUI();
    }

    private void createGUI() {
        // Main frame setup
        frame = new JFrame("Chat Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setMinimumSize(new Dimension(600, 400));
        frame.setLocationRelativeTo(null);

        // Main panel with border
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Chat Server Control Panel", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        // Client count label
        clientCountLabel = new JLabel("Connected Clients: 0", SwingConstants.CENTER);
        clientCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clientCountLabel.setBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        titlePanel.add(clientCountLabel, BorderLayout.SOUTH);

        // Center panel with log and message areas
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new TitledBorder("Server Log"));

        // Message area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(new TitledBorder("Chat Messages"));

        centerPanel.add(logScroll);
        centerPanel.add(messageScroll);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        inputField = new JTextField();
        inputField.setEnabled(false);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendServerMessage());

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        
        startButton = new JButton("Start Server");
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> startServer());
        styleButton(startButton, new Color(70, 130, 180), Color.WHITE);
        
        stopButton = new JButton("Stop Server");
        stopButton.setFocusPainted(false);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopServer());
        styleButton(stopButton, new Color(220, 20, 60), Color.WHITE);

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(e -> sendServerMessage());
        styleButton(sendButton, new Color(34, 139, 34), Color.WHITE);

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(sendButton);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // Add components to main panel
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void updateClientCount() {
        SwingUtilities.invokeLater(() -> 
            clientCountLabel.setText("Connected Clients: " + clients.size()));
    }

    private void styleButton(JButton button, Color bgColor, Color textColor) {
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setBorder(new CompoundBorder(
            new LineBorder(bgColor.darker(), 1),
            new EmptyBorder(5, 15, 5, 15)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void startServer() {
        if (isRunning) return;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    sendButton.setEnabled(true);
                    inputField.setEnabled(true);
                    log("Server started on port " + PORT);
                });

                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        clients.add(clientHandler);
                        updateClientCount();
                        new Thread(clientHandler).start();
                    } catch (SocketException e) {
                        if (isRunning) {
                            log("Server socket error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log("Server error: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    sendButton.setEnabled(false);
                    inputField.setEnabled(false);
                });
                isRunning = false;
            }
        }).start();
    }

    private void stopServer() {
        if (!isRunning) return;

        isRunning = false;
        try {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.close();
                }
                clients.clear();
                updateClientCount();
            }
            
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            SwingUtilities.invokeLater(() -> {
                log("Server stopped");
                inputField.setText("");
            });
        } catch (IOException e) {
            log("Error stopping server: " + e.getMessage());
        }
    }

    private void sendServerMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            messageArea.append("Server: " + message + "\n");
            broadcast("Server: " + message, null);
            inputField.setText("");
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[LOG] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void messageLog(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }

    private void broadcast(String message, ClientHandler exclude) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != exclude) {
                    client.sendMessage(message);
                }
            }
        }
        messageLog(message);
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message is the username
                username = in.readLine();
                log(username + " connected from " + socket.getInetAddress());
                messageLog(username + " joined the chat");

                broadcast(username + " joined the chat", this);

                String message;
                while ((message = in.readLine()) != null) {
                    messageLog(username + ": " + message);
                    broadcast(username + ": " + message, this);
                }
            } catch (IOException e) {
                log(username + " disconnected: " + e.getMessage());
            } finally {
                try {
                    close();
                } catch (IOException e) {
                    log("Error closing client connection: " + e.getMessage());
                }
                clients.remove(this);
                updateClientCount();
                messageLog(username + " left the chat");
                broadcast(username + " left the chat", this);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void close() throws IOException {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        }
    }
}