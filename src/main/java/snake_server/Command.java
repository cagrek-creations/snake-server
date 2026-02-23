package snake_server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Command {
    private String command;
    private List<String> parameters;

    private Command(String command, List<String> parameters) {
        this.command = command;
        this.parameters = parameters;
    }

    public String getCommand() {
        return command;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public static Command parse(String receivedData) {
        String[] parts = receivedData.split(";");
        String command = parts[0];
        List<String> parameters = new ArrayList<>();
        if (parts.length > 1) {
            parameters = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));
        }
        return new Command(command, parameters);
    }
}
