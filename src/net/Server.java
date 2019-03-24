package net;

import cli.InvalidCommandLineArgumentException;
import collection.CollectionElement;
import com.thoughtworks.xstream.XStream;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class Server implements Runnable, Closeable {
    private boolean shouldRun = true;
    private Queue<CollectionElement> collection = new PriorityBlockingQueue<>();

    private XStream xStream = new XStream();
    private DatagramChannel channel;

    public Server(String[] args) throws IOException, InvalidCommandLineArgumentException {
        if (args.length < 2) {
            throw new InvalidCommandLineArgumentException("You should specify port");
        }

        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new InvalidCommandLineArgumentException("Port should be a number");
        }

        if (port < 1024 || port > 65_535) {
            throw new InvalidCommandLineArgumentException("Port should be between 1024 and 65 535");
        }

        channel = DatagramChannel.open();
        channel.bind(new InetSocketAddress(port));
    }

    public void run() {
        MessageProcessor messageProcessor = new MessageProcessor();
        messageProcessor.setRequestProcessor(Message.Head.INFO, msg -> infoMessage());
        messageProcessor.setRequestProcessor(Message.Head.REMOVE_FIRST, msg -> {
            removeFirst();
            return null;
        });
        messageProcessor.setRequestProcessor(Message.Head.REMOVE_LAST, msg -> {
            removeLast();
            return null;
        });
        messageProcessor.setRequestProcessor(Message.Head.ADD, msg -> {
            if (msg.getBody() instanceof CollectionElement) {
                collection.add((CollectionElement) msg.getBody());
            }
            return null;
        });
        messageProcessor.setRequestProcessor(Message.Head.REMOVE, msg -> {
            if (msg.getBody() instanceof CollectionElement) {
                collection.remove((CollectionElement) msg.getBody());
            }
            return null;
        });
        messageProcessor.setRequestProcessor(Message.Head.SHOW, msg -> null);
        messageProcessor.setRequestProcessor(Message.Head.IMPORT, msg -> null);
        messageProcessor.setRequestProcessor(Message.Head.LOAD, msg -> null);
        messageProcessor.setRequestProcessor(Message.Head.SAVE, msg -> null);
        messageProcessor.setRequestProcessor(Message.Head.STOP, msg -> null);

        ByteBuffer buffer = ByteBuffer.allocate(0x10000);

        while (shouldRun) {
            SocketAddress remoteAddress;

            try {
                remoteAddress = channel.receive(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            byte[] bytes = buffer.array();
            InputStream inputStream = new ByteArrayInputStream(bytes);

            Message request;

            try (ObjectInputStream oi = new ObjectInputStream(inputStream)) {
                Object obj = oi.readObject();
                if (obj instanceof Message) {
                    request = (Message) obj;
                } else {
                    continue;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }

            if (!request.isRequest()) {
                continue;
            }

            new Thread(() -> {
                Message response = messageProcessor.process(request);
                if (response == null) {
                    return;
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try (ObjectOutputStream oo = new ObjectOutputStream(outputStream)) {
                    oo.writeObject(response);
                    channel.send(ByteBuffer.wrap(outputStream.toByteArray()), remoteAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).run();
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    private Message infoMessage() {
        return new Message(false, Message.Head.INFO,
                String.format("%s of size %d", collection.getClass().getTypeName(), collection.size()));
    }

    private void removeFirst() {

    }

    private void removeLast() {

    }
}
