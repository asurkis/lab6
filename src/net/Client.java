package net;

import cli.ConsoleInterface;
import cli.InvalidCommandLineArgumentException;
import cli.UnknownCommandException;
import collection.CollectionElement;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.Scanner;

public class Client implements Runnable, Closeable {
    private boolean shouldRun = true;
    private MessageProcessor messageProcessor = new MessageProcessor();
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
        socket.setSoTimeout(2000);

        messageProcessor.setResponseProcessor(Message.Head.INFO, msg -> System.out.println(msg.getBody()));
        messageProcessor.setResponseProcessor(Message.Head.SHOW, msg -> {
            if (msg.getBody() instanceof List) {
                List list = (List) msg.getBody();
                list.forEach(System.out::println);
            }
        });
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleInterface cli = new ConsoleInterface(scanner);
            cli.setCommand("exit", line -> shouldRun = false);
            cli.setCommand("stop",
                    line -> sendRequest(new Message(true, Message.Head.STOP, null)));
            cli.setCommand("info",
                    line -> sendRequest(new Message(true, Message.Head.INFO, null)));
            cli.setCommand("remove_first",
                    line -> sendRequest(new Message(true, Message.Head.REMOVE_FIRST, null)));
            cli.setCommand("remove_last",
                    line -> sendRequest(new Message(true, Message.Head.REMOVE_LAST, null)));
            cli.setCommand("add",
                    line -> sendRequest(messageWithElement(Message.Head.ADD, line)));
            cli.setCommand("remove",
                    line -> sendRequest(messageWithElement(Message.Head.REMOVE, line)));
            cli.setCommand("show",
                    line -> sendRequest(new Message(true, Message.Head.SHOW, null)));
            cli.setCommand("load",
                    line -> sendRequest(new Message(true, Message.Head.LOAD, null)));
            cli.setCommand("save",
                    line -> sendRequest(new Message(true, Message.Head.SAVE, null)));
            cli.setCommand("import",
                    line -> sendRequest(importMessage(line)));

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

    private void sendRequest(Message message) {
        if (message == null) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream oo = new ObjectOutputStream(outputStream)) {
            oo.writeObject(message);
        } catch (IOException ignored) {
        }

        byte[] sendBytes = outputStream.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);


        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            System.err.println("Could not send request to server");
            return;
        }

        if (!messageProcessor.hasResponseProcessor(message.getHead())) {
            return;
        }

        byte[] receiveBytes = new byte[0x10000];
        DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

        try {
            socket.receive(receivePacket);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(receiveBytes);
            try (ObjectInputStream oi = new ObjectInputStream(inputStream)) {
                Object obj = oi.readObject();
                if (obj instanceof Message) {
                    messageProcessor.process((Message) obj);
                }
            } catch (ClassNotFoundException ignored) {
            }
        } catch (IOException e) {
            System.err.println("Could not get response from server");
        }
    }

    private Message messageWithElement(Message.Head head, String line) {
        try {
            CollectionElement element = gson.fromJson(line, CollectionElement.class);
            return new Message(true, head, element);
        } catch (JsonParseException e) {
            System.err.println("Could not parse JSON object");
            return null;
        }
    }

    private Message importMessage(String line) {
        try {
            String str = new String(Files.readAllBytes(new File(line.trim()).toPath()));
            return new Message(true, Message.Head.IMPORT, str);
        } catch (IOException | InvalidPathException e) {
            System.err.println("Could not read file: " + e.getMessage());
            return null;
        }
    }
}
