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

public class PlayingField {    
    private int width;
    private int height;
    private Square[][] field;
    private Map<Integer, Player> players = new HashMap<>();

    public PlayingField(int width, int height) {
        this.width = width;
        this.height = height;
        this.field = new Square[height][width];
        this.players = new HashMap<>();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                field[i][j] = new Square();
            }
        }
    }

    public Square[][] getField() {
        return field;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void spawnPlayer(Player player) {

        int xPos, yPos;
        boolean validPosition;
        int playerID = player.getPid();
    
        do {
            xPos = new Random().nextInt(width);
            yPos = new Random().nextInt(height);
            validPosition = field[yPos][xPos].getType().equals("empty");
        } while (!validPosition);
    
        // Place the player on the field
        field[yPos][xPos].setType("head");
        field[yPos][xPos].setPlayerID(playerID);
    
        player.setHeadPos(xPos, yPos);
    }

    public void addPlayer(Player player) {
        players.put(player.getPid(), player);
    }

    public void removePlayer(Player player) {
        players.remove(player.getPid());

        // Clear the head position
        Position headPosition = player.getHeadPosition();
        if (headPosition != null) {
            field[headPosition.getY()][headPosition.getX()].clear();
        }

        // Clear the body positions
        for (BodySegment segment : player.getBody()) {
            Position pos = segment.getPosition();
            field[pos.getY()][pos.getX()].clear();
        }

    }

    public Player getPlayer(int playerID) {
        return players.get(playerID);
    }

    public Map<Integer, Player> getPlayers() {
        return players;
    }

    public String encodeField() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Square square = field[i][j];
                if (square.getType().equals("head")) {
                    sb.append("H");
                    sb.append(square.getPlayerID());
                } else if (square.getType().equals("playerbody")) {
                    sb.append("P");
                    sb.append(square.getPlayerID());
                } else if (square.getType().equals("berry")) {
                    sb.append("B");
                    sb.append(square.getMagnitude());
                } else if (!square.getType().equals("empty")) {
                    sb.append("?");
                } else {
                    sb.append(".");
                }
                sb.append("|");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void printPlayingField() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                System.out.print(field[i][j].getType() + " ");
            }
            System.out.println();
        }
    }

    // Spawns a score item on a random unoccupied square
    public void spawnScore(String type, int magnitude) {
        int xPos;
        int yPos;
        do {
            xPos = new Random().nextInt(width);
            yPos = new Random().nextInt(height);
        } while ( !field[yPos][xPos].getType().equals("empty") );

        field[yPos][xPos].setType(type);
        field[yPos][xPos].setMagnitude(magnitude);
        String msg = Server.appendDelimitor("ADD_SCORE", type, magnitude, xPos, yPos);
        Server.broadcast(msg);
    }

    // @Overload    Spawns a score item on a specific square
    public void spawnScore(String type, int magnitude, int xPos, int yPos) { 
        field[yPos][xPos].setType(type);
        field[yPos][xPos].setMagnitude(magnitude);
        String msg = Server.appendDelimitor("ADD_SCORE", type, magnitude, xPos, yPos);
        Server.broadcast(msg);
    }

    public String checkPosition(int playerID, int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return "outOfBounds";
        }

        String type = field[y][x].getType();

        if (type.equals("playerbody") || type.equals("head")) {
            System.out.println("GAME OVER");
        }

        if (!type.equals("empty")) {
            field[y][x].setType("empty");
            return type; // Return the type of the power-up found
        }

    return null;
    }

}
