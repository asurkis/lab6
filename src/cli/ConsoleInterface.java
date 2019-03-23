package cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ConsoleInterface {
    private Map<String, Command> commands = new HashMap<>();
    private Scanner scanner;

    public ConsoleInterface(Scanner scanner) {
        this.scanner = scanner;
    }

    public void setCommand(String name, Command command) {
        commands.put(name, command);
    }

    public void execNextLine() throws UnknownCommandException {
        String line = scanner.nextLine().trim();
        int splitIndex;
        for (splitIndex = 0; splitIndex < line.length(); splitIndex++) {
            if (Character.isWhitespace(line.charAt(splitIndex))) {
                break;
            }
        }

        String name = line.substring(0, splitIndex);
        String arguments = line.substring(splitIndex);

        if (commands.containsKey(name)) {
            commands.get(name).exec(arguments);
        } else {
            throw new UnknownCommandException(name);
        }
    }
}
