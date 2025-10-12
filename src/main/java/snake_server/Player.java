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

public class Player {
    private Socket playerSocket;
    private int pid;
    private String name;
    private String color;
    private Position headPosition;
    private int length;
    private List<BodySegment> body;

    public Player(Socket playerSocket, int pid, String name, String color) {
        this.playerSocket = playerSocket;
        this.pid = pid;
        this.name = name;
        this.color = color;
        this.length = 6;
        this.body = new ArrayList<>();
    }

    public Socket getPlayerSocket() { return playerSocket; }

    public int getPid() { return pid; }

    public String getName() { return name; }

    public String getColor() { return color; }

    public Position getHeadPosition() { return headPosition; }
    public int getXPos() { return headPosition.getX(); }
    public int getYPos() { return headPosition.getY(); }

    public void setHeadPos(int x, int y) { headPosition = new Position(x, y); }

    public int getLength() { return length; }

    public int setLength(int length) { return this.length = length; }

    public List<BodySegment> getBody() { return body; }

    public void move(int newX, int newY, PlayingField playingField) {
        // Clear previous head position from the playing field
        playingField.getField()[headPosition.getY()][headPosition.getX()].clear();
    
        // Add new head position to the body
        body.add(0, new BodySegment(headPosition.getX(), headPosition.getY()));
        
        // Remove the last segment if the length is exceeded
        if (body.size() > length - 1) { // length - 1 because length includes the head
            BodySegment removedSegment = body.remove(body.size() - 1);
            // Clear the removed segment position from the playing field
            playingField.getField()[removedSegment.getPosition().getY()][removedSegment.getPosition().getX()].clear();
        }
        
        // Update head position
        headPosition = new Position(newX, newY);
    
        // Update the playing field with the new head position
        playingField.getField()[newY][newX].setType("head");
        playingField.getField()[newY][newX].setPlayerID(pid);
    
        // Update the playing field with the new body segments positions
        for (BodySegment segment : body) {
            playingField.getField()[segment.getPosition().getY()][segment.getPosition().getX()].setType("playerbody");
            playingField.getField()[segment.getPosition().getY()][segment.getPosition().getX()].setPlayerID(pid);
        }

    }
}
