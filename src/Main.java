import cli.ConsoleInterface;
import cli.UnknownCommandException;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        new Main().run();
    }

    private boolean shouldRun = true;

    private void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

        }));

        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleInterface cli = new ConsoleInterface(scanner);
            cli.setCommand("exit", line -> shouldRun = false);

            while (shouldRun) {
                try {
                    cli.execNextLine();
                } catch (UnknownCommandException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}
