import cli.ConsoleInterface;
import cli.UnknownCommandException;
import collection.CollectionElement;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.function.Predicate;

public class Main {
    public static void main(String[] args) {
        new Main().run();
    }

    private Queue<CollectionElement> collection = new PriorityQueue<>();
    private Gson gson = new Gson();
    private boolean shouldRun = true;

    private void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

        }));

        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleInterface cli = new ConsoleInterface(scanner);
            cli.setCommand("exit", line -> shouldRun = false);
            cli.setCommand("info", this::info);
            cli.setCommand("remove_first", this::removeFirst);
            cli.setCommand("remove_last", this::removeLast);
            cli.setCommand("add", line -> this.addOrRemove(line, collection::add));
            cli.setCommand("remove", line -> this.addOrRemove(line, collection::remove));
            cli.setCommand("show", this::show);

            while (shouldRun) {
                try {
                    cli.execNextLine();
                } catch (UnknownCommandException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    private void info(String line) {
        System.out.printf("%s of size: %d\n", collection.getClass().getName(), collection.size());
    }

    private void removeFirst(String line) {
        collection.poll();
    }

    private void removeLast(String line) {
        Queue<CollectionElement> buf = new PriorityQueue<>();
        while (collection.size() > 1)
            buf.add(collection.poll());
        collection = buf;
    }

    private void addOrRemove(String line, Predicate<CollectionElement> perform) {
        try {
            CollectionElement element = gson.fromJson(line, CollectionElement.class);
            if (element != null) {
                perform.test(element);
            } else {
                System.err.println("Input element is null");
            }
        } catch (JsonParseException e) {
            System.err.println(e.getMessage());
        }
    }

    private void show(String line) {
        Queue<CollectionElement> buf = new PriorityQueue<>();
        for (int i = 1; !collection.isEmpty(); i++) {
            CollectionElement element = collection.poll();
            buf.add(element);
            System.out.printf("#%d: %s\n", i, element);
        }
        collection = buf;
    }
}
