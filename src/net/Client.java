package net;

import cli.ConsoleInterface;
import cli.InvalidCommandLineArgumentException;
import cli.UnknownCommandException;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class Client implements Runnable, Closeable {
    private boolean shouldRun = true;
    private Gson gson = new Gson();
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    public Client(String[] args) throws IOException, InvalidCommandLineArgumentException {
        if (args.length < 3) {
            throw new InvalidCommandLineArgumentException("You should specify IP and port to connect");
        }

        address = InetAddress.getByName(args[1]);

        try {
            port = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new InvalidCommandLineArgumentException("Port should be a number");
        }

        if (port < 1024 || port > 65_535) {
            throw new InvalidCommandLineArgumentException("Port should be between 1024 and 65 535");
        }

        socket = new DatagramSocket();
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleInterface cli = new ConsoleInterface(scanner);
            cli.setCommand("exit", line -> shouldRun = false);
            cli.setCommand("echo", this::sendEcho);

            while (shouldRun) {
                try {
                    cli.execNextLine();
                } catch (UnknownCommandException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    @Override
    public void close() {
        socket.close();
    }

    private void sendEcho(String line) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ObjectOutputStream oo = new ObjectOutputStream(byteArrayOutputStream)) {
            oo.writeObject(line);
        } catch (IOException ignored) {
        }

        byte[] byteArray = byteArrayOutputStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, address, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
