package snake_server;

public class BodySegment {
    private Position position;

    public BodySegment(int xPos, int yPos) {
        this.position = new Position(xPos, yPos);
    }

    public Position getPosition() { return position; }
}
