import cli.InvalidCommandLineArgumentException;
import net.Client;
import net.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        boolean isClient = args.length != 0 && "client".equals(args[0]);
        boolean isServer = args.length != 0 && "server".equals(args[0]);

        if (isClient) {
            try (Client c = new Client(args)) {
                c.run();
            } catch (IOException | InvalidCommandLineArgumentException e) {
                System.err.println(e.getMessage());
            }
        } else if (isServer) {
            try (Server s = new Server(args)) {
                s.run();
            } catch (IOException | InvalidCommandLineArgumentException e) {
                System.err.println(e.getMessage());
            }
        } else {
            System.err.println("You should specify 'client' or 'server'");
        }
    }
}
