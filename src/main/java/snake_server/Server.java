package snake_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Random;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.InputStream;

// Server class
public class Server {

    private static int PORT = 12345;
    private static List<Socket> clientList = new ArrayList<>();
    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private static int playerIDCounter = 0;
    public static PlayingField playingField = new PlayingField(50, 25);

    private static double berryFrequency = 1.0;
    private static double inverseFrequency = 1.0;
    private static double speedFrequency = 1.0;

    private static boolean gameStarted = false;

    private static List<Double> counters = new ArrayList<>();
    private static List<Double> intervals = new ArrayList<>();
    private static List<Runnable> actions = new ArrayList<>();



    public static void main(String[] args) {
        // Load the "config" section from YAML
        Map<String, Object> config = readYaml("./config.yml", "config");

        System.out.println("Config: " + config);
        loadConfig(config);


        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT);

            // Start the game loop
            executorService.execute(() -> handleGame());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Add the client socket to the list
                synchronized (clientList) {
                    clientList.add(clientSocket);
                    clientList.notifyAll();
                }

                // Handle client communication in a separate thread
                executorService.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    
    private static void handleGame() {
        try {
            double elapsedTime;
            double lastTime = System.currentTimeMillis() / 1000.0;

            setupIntervals();

            synchronized (clientList) {
                while (clientList.size() < 0) {
                    broadcast("WAITING_FOR_PLAYERS");
                    clientList.wait();
                }
            }
    
            while (true) {
                double currentTime = System.currentTimeMillis() / 1000.0;
                elapsedTime = currentTime - lastTime;
                lastTime = currentTime;
    
                for (int i = 0; i < counters.size(); i++) {
                    if (counters.get(i) >= intervals.get(i) && intervals.get(i) != -1) {
                        actions.get(i).run();
                        counters.set(i, 0.0);
                    }
                }
    
                // Update counters
                for (int i = 0; i < counters.size(); i++) {
                    counters.set(i, counters.get(i) + elapsedTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    private static void handleClient(Socket clientSocket) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();
            String msg;
            byte[] buffer = new byte[1024];


            while (true) {
                // Wait for data from the client
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    // Client disconnected
                    System.out.println("Client disconnected: " + clientSocket.getInetAddress());
                    clientList.remove(clientSocket);
                    break;
                }

                // Process the received data and parse it into a command
                String receivedData = new String(buffer, 0, bytesRead);
                LoggerUtil.logMessage("Client " + clientSocket.getInetAddress() + ": " + receivedData);
                Command command = Command.parse(receivedData);

                String cmd = command.getCommand();
                List<String> params = command.getParameters();

                switch (cmd) {

                    case "NEW_PLAYER_JOINED": // NEW_PLAYER_JOINED;name             ?? What is this used for? 
                        msg = appendDelimitor("NEW_PLAYER_JOINED", params.get(0));
                        broadcast(msg, clientSocket);
                        break;
                    
                    case "PLAYER_UPDATE_POSITION": // PLAYER_UPDATE_POSITION;pid;xPos;yPos      // FIX NON EXISTENT SNAKE!!!!
                        int playerID = Integer.parseInt(params.get(0));
                        Player player = playingField.getPlayer(playerID);
                        int xPos = Integer.parseInt(params.get(1));
                        int yPos = Integer.parseInt(params.get(2));


                        if (xPos < 0 || xPos >= playingField.getWidth() || yPos < 0 || yPos >= playingField.getHeight()) {
                            System.out.println("Invalid position: " + xPos + ", " + yPos);
                            break;
                        }

                        String moveResponse = playingField.checkPosition(playerID, xPos, yPos); 

                        if (player.getXPos() == xPos && player.getYPos() == yPos) {
                            break; // Ignore if the player doesn't move
                        }

                        if (moveResponse == "berry"){ // SCORE_COLLECTED;pid;type;magnitude;xPos;yPos
                            player.setLength(player.getLength() + 1);
                            msg = appendDelimitor("SCORE_COLLECTED", params.get(0), "berry", 1, Integer.parseInt(params.get(1)), Integer.parseInt(params.get(2))); // SCORE_COLLECTED;pid;type;amount;xPos;yPos
                            broadcast(msg);
                        } 
                        else if (moveResponse == "inverse_self") {
                            msg = appendDelimitor("SCORE_COLLECTED", params.get(0), "inverse_self", -1, Integer.parseInt(params.get(1)), Integer.parseInt(params.get(2))); // SCORE_COLLECTED;pid;type;amount;xPos;yPos
                            broadcast(msg);
                        } 
                        else if (moveResponse == "inverse_other") {
                            msg = appendDelimitor("SCORE_COLLECTED", params.get(0), "inverse_other", 1, Integer.parseInt(params.get(1)), Integer.parseInt(params.get(2))); // SCORE_COLLECTED;pid;type;amount;xPos;yPos
                            broadcast(msg);
                        }
                        else if (moveResponse == "speed_self") {
                            msg = appendDelimitor("SCORE_COLLECTED", params.get(0), "speed_self", -1, Integer.parseInt(params.get(1)), Integer.parseInt(params.get(2))); // SCORE_COLLECTED;pid;type;amount;xPos;yPos
                            broadcast(msg);
                        } 
                        else if (moveResponse == "speed_other") {
                            msg = appendDelimitor("SCORE_COLLECTED", params.get(0), "speed_other", 1, Integer.parseInt(params.get(1)), Integer.parseInt(params.get(2))); // SCORE_COLLECTED;pid;type;amount;xPos;yPos
                            broadcast(msg);
                        }


                        // else if (moveResponse == "outOfBounds") {
                        //     msg = appendDelimitor("MOVE_OUT_OF_BOUNDS", params.get(0), params.get(1), params.get(2)); // MOVE_OUT_OF_BOUNDS;pid;xPos;yPos     -- Parameters might be uncessary
                        //     // send(msg, outputStream);
                        //     break;
                        // }

                        player.move(xPos, yPos, playingField);

                        msg = appendDelimitor("PLAYER_NEW_POS", params.get(0), params.get(1), params.get(2));
                        broadcast(msg, clientSocket); // PLAYER_NEW_POS;pid;xPos;yPos
                        break;

                    case "ADD_NEW_PLAYER": // ADD_NEW_PLAYER;name;color

                        Player newPlayer = new Player(clientSocket, playerIDCounter++, params.get(0), params.get(1)); // 0 = pid, 1 = name, 2 = color
                        playingField.addPlayer(newPlayer);
                        playingField.spawnPlayer(newPlayer);

                        msg = appendDelimitor("NEW_PLAYER_RESPONSE", newPlayer.getPid(), newPlayer.getXPos(), newPlayer.getYPos(), playingField.getWidth(), playingField.getHeight());
                        send(msg, outputStream); // NEW_PLAYER_RESPONSE;pid;xPos;yPos;fieldWidth;fieldHeight

                        sendGameState(newPlayer, outputStream);

                        //msg = appendDelimitor("PLAYING_FIELD", playingField.getWidth(), playingField.getHeight(), playingField.encodeField());
// SEND PLAYING FIELD   send(msg, outputStream); // PLAYING_FIELD;fieldWidth;fieldHeight;|e|e|e|h/1|e|e|e|b/1|e|

                        msg = appendDelimitor("NEW_PLAYER", newPlayer.getPid(), newPlayer.getName(), newPlayer.getColor(), newPlayer.getLength(), newPlayer.getXPos(), newPlayer.getYPos());
                        broadcast(msg, clientSocket); // NEW_PLAYER;pid;name;color;length;xPos;yPos
                        

                        break;


                    // TEST CALLS
                    case "BROADCAST_TEST": // BROADCAST_TEST;message
                        System.out.println(params.get(0));
                        broadcast(params.get(0));
                        break;

                    case "BERRY_TEST": // BERRY_TEST;(xPos;yPos)
                        if (params.size() >= 2) {
                            playingField.spawnScore("berry", 1, Integer.parseInt(params.get(0)), Integer.parseInt(params.get(1)));
                        } else {
                            playingField.spawnScore("berry", 1);
                        }
                        break;

                    case "XY": // XY;x;y
                        System.out.println("X:" + params.get(0) + " Y:" + params.get(1) + " = " + playingField.getField()[Integer.parseInt(params.get(1))][Integer.parseInt(params.get(0))].getType());
                        break;

                    case "TEST":
                        System.out.println("Test command received");
                        System.out.println(playingField.encodeField());
                        // playingField.printPlayingField();
                        break;

                    default:
                        System.out.println("Unknown command: " + command.getCommand());
                }


            }
        } catch (IOException e) {
            if (e instanceof java.net.SocketException && e.getMessage().equals("Connection reset")) {
                // Client disconnected abruptly
                System.out.println("Client disconnected abruptly: " + clientSocket.getInetAddress());
                removeClient(clientSocket);
            } else {
                e.printStackTrace();
            }
        }
    }

    private static void removeClient(Socket clientSocket) {
        try {
            for (Player p : playingField.getPlayers().values()) {
                if (p.getPlayerSocket() == clientSocket) {
                    playingField.removePlayer(p);
                    break;
                }
            }
            clientList.remove(clientSocket);
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    


    private static void send(String message, OutputStream outputStream) {
        try {
            System.out.println("Sending: " + message);
            message += "\n";
            outputStream.write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String appendDelimitor(Object... parameters) {
        StringBuilder sb = new StringBuilder();
        for (Object parameter : parameters) {
            sb.append(parameter).append(";");
        }
        // Remove the last semicolon
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }


    public static void broadcast(String message, Socket... excludeClients) {
        int broadcastCount = 0;
        String logMessage = message;
        message += "\n";
        
        for (Socket clientSocket : clientList) {
            if (Arrays.asList(excludeClients).contains(clientSocket)) {
                continue;
            }
            try {
                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(message.getBytes());
                broadcastCount++;
            } catch (SocketException e) {
                System.out.println("Client disconnected abruptly: " + clientSocket.getInetAddress());
                clientList.remove(clientSocket);
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        LoggerUtil.logMessage("Broadcasted: '" + logMessage + "' to " + broadcastCount + " clients.");
    }


    private static void sendGameState(Player newPlayer, OutputStream outputStream) throws IOException {
        String msg;
        // Send positions of all berries to the new player
        for (int y = 0; y < playingField.getHeight(); y++) {
            for (int x = 0; x < playingField.getWidth(); x++) {
                Square square = playingField.getField()[y][x];
                if (square.getType().equals("berry")) {
                    msg = appendDelimitor("BERRY_POSITION", x, y, square.getMagnitude());
                    send(msg, outputStream); // BERRY_POSITION;xPos;yPos;magnitude
                }
            }
        }

        // Send positions of all existing players to the new player
        for (Player p : playingField.getPlayers().values()) {
            if (p == newPlayer)
                continue;
            // Append head position
            StringBuilder playerInfo = new StringBuilder(
                    appendDelimitor("PLAYER_INFO", p.getPid(), p.getName(), p.getColor()));
            playerInfo.append(";").append(p.getXPos()).append(";").append(p.getYPos());

            // Append body segments positions
            for (BodySegment segment : p.getBody()) {
                playerInfo.append(";").append(segment.getPosition().getX()).append(";")
                        .append(segment.getPosition().getY());
            }

            msg = playerInfo.toString();
            send(msg, outputStream); // PLAYER_INFO;pid;name;color;headxPos,headyPos;31,12;31,13;
        }
    }

    private static Map<String, Object> readYaml(String resourcePath, String baseKey) {
        Yaml yaml = new Yaml();

        try (InputStream in = new FileInputStream("config.yml")) {
            if (in == null) {
                throw new RuntimeException("Could not find YAML file: " + resourcePath);
            }

            Map<String, Object> yamlMap = yaml.load(in);
            Map<String, Object> baseMap = (Map<String, Object>) yamlMap.get(baseKey);
            return baseMap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void loadConfig(Map<String, Object> config) {
        Map<String, Object> server = (Map<String, Object>) config.get("server");
        PORT = ((Number) server.get("port")).intValue();

        Map<String, Object> items = (Map<String, Object>) config.get("items");

        berryFrequency = ((Number) ((Map<String, Object>) items.get("berry")).get("frequency")).doubleValue();
        inverseFrequency = ((Number) ((Map<String, Object>) items.get("inverse")).get("frequency")).doubleValue();
        speedFrequency = ((Number) ((Map<String, Object>) items.get("speed")).get("frequency")).doubleValue();
    }

    private static void setupIntervals() {
        Random rand = new Random();
        counters = Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 0.0); // Initialize counters to 0
        intervals = Arrays.asList(berryFrequency, inverseFrequency, speedFrequency, 60.0, 75.0, 90.0); // intervals in seconds
        actions = Arrays.asList(
            () -> {
                playingField.spawnScore("berry", 1); // ADD_SCORE;type;magnitude;xPos;yPos
            },
            () -> {
                int chance = rand.nextInt(100) + 1;
                if (chance <= 50) {
                    playingField.spawnScore("inverse_self", 1);
                } else {
                    playingField.spawnScore("inverse_other", 1);
                }
            },
            () -> {
                int chance = rand.nextInt(100) + 1;
                if (chance <= 50) {
                    playingField.spawnScore("speed_self", 1);
                } else {
                    playingField.spawnScore("speed_other", 1);
                }
            },
            // TODO: Not implemented on client side yet...
            () -> {
                playingField.spawnScore("freeze", 1);
            },
            () -> {
                playingField.spawnScore("ghost", 1);
            },
            () -> {
                playingField.spawnScore("rage", 1);
            }
        );
    }
}
