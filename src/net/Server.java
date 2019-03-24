package net;

import cli.InvalidCommandLineArgumentException;
import collection.CollectionElement;
import com.thoughtworks.xstream.XStream;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class Server implements Runnable, Closeable {
    private boolean shouldRun = true;
    private Queue<CollectionElement> collection = new PriorityBlockingQueue<>();
    private XStream xStream = new XStream();
    private DatagramSocket socket;
    private int port;

    public Server(String[] args) throws IOException, InvalidCommandLineArgumentException {
        if (args.length < 2) {
            throw new InvalidCommandLineArgumentException("You should specify port");
        }

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new InvalidCommandLineArgumentException("Port should be a number");
        }

        if (port < 1024 || port > 65_535) {
            throw new InvalidCommandLineArgumentException("Port should be between 1024 and 65 535");
        }

        socket = new DatagramSocket(port);
    }

    public void run() {
        byte[] buffer = new byte[0x1000];

        while (shouldRun) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);

            try (ObjectInputStream oi = new ObjectInputStream(byteArrayInputStream)) {
                Object obj = oi.readObject();
                System.out.println(obj);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        socket.close();
    }
}
