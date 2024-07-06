import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer
{
    // A pre-allocated buffer for the received data
    static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

    // Decoder for incoming text -- assume UTF-8
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();

    static private LinkedList<Client> clientList = new LinkedList<>();
    static private LinkedList<ChatRoom> roomList = new LinkedList<>();

    static private final String ERR = "ERROR\n";
    static private final String OK = "OK\n";

    static public void main(String args[]) throws Exception {
        // Parse port from command line
        int port = Integer.parseInt(args[0]);
        
        try {
        // Instead of creating a ServerSocket, create a ServerSocketChannel
        ServerSocketChannel ssc = ServerSocketChannel.open();

        // Set it to non-blocking, so we can use select
        ssc.configureBlocking(false);

        // Get the Socket connected to this channel, and bind it to the
        // listening port
        ServerSocket ss = ssc.socket();
        InetSocketAddress isa = new InetSocketAddress(port);
        ss.bind(isa);

        // Create a new Selector for selecting
        Selector selector = Selector.open();

        // Register the ServerSocketChannel, so we can listen for incoming
        // connections
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Listening on port "+port);

        while (true) {
            // See if we've had any activity -- either an incoming connection,
            // or incoming data on an existing connection
            int num = selector.select();

            // If we don't have any activity, loop around and wait again
            if (num == 0)
                continue;

            // Get the keys corresponding to the activity that has been
            // detected, and process them one by one
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();
            while (it.hasNext()) {
            // Get a key representing one of bits of I/O activity
            SelectionKey key = it.next();

            // What kind of activity is it?
            if (key.isAcceptable()) {

                // It's an incoming connection.  Register this socket with
                // the Selector so we can listen for input on it
                Socket s = ss.accept();
                System.out.println("Got connection from "+s);

                // Make sure to make it non-blocking, so we can use a selector
                // on it.
                SocketChannel sc = s.getChannel();
                sc.configureBlocking(false);

                // Register it with the selector, for reading
                sc.register(selector, SelectionKey.OP_READ);

            } else if (key.isReadable()) {

                SocketChannel sc = null;

                try {

                // It's incoming data on a connection -- process it
                sc = (SocketChannel)key.channel();
                boolean ok = processInput(sc, selector, key);

                // If the connection is dead, remove it from the selector
                // and close it
                if (!ok) {
                    key.cancel();
                    disconnect(key);
                    Socket s = null;
                    try {
                    s = sc.socket();
                    System.out.println("Closing connection to "+s);
                    s.close();
                    } catch(IOException ie) {
                    System.err.println("Error closing socket "+s+": "+ie);
                    }
                }

                } catch(IOException ie) {
                    disconnect(key);
                    // On exception, remove this channel from the selector
                    key.cancel();

                    try {
                        sc.close();
                    } catch(IOException ie2) { System.out.println(ie2); }

                    System.out.println("Closed "+sc);
                }
            }
            }

            // We remove the selected keys, because we've dealt with them.
            keys.clear();
        }
        } catch(IOException ie) {
        System.err.println(ie);
        }
    }


  // Just read the message from the socket and send it to stdout
    static private boolean processInput(SocketChannel sc, Selector selector , SelectionKey inputkey) throws IOException {
        buffer.clear();
        sc.read(buffer);
        buffer.flip();

        // If no data, close the connection
        if (buffer.limit()==0)
            return false;

        if (inputkey.attachment() == null) {
            Client newClient = new Client(inputkey);
            clientList.add(newClient);
			inputkey.attach(newClient);
        }

        Client curClient = (Client)inputkey.attachment();
        String message = decoder.decode(buffer).toString();
        message = curClient.bufferedMessage + message;
        curClient.clearBuffer();
        String textToSend = null;
        String[] commands = message.split("(?<=\n)");

        for(int i = 0; i < commands.length; i++) {
            String curCommand = commands[i];

            if(curCommand.charAt(curCommand.length() - 1) != '\n') {
                curClient.bufferedMessage += curCommand;
                return true;
            }
            
            String[] tokens = curCommand.split(" ");
            if(curCommand.charAt(0) == ' ')
                textToSend = message(curClient, curCommand);
            else if(tokens[0].charAt(0) == '/') {
                if (tokens[0].charAt(1) == '/')
                    textToSend = message(curClient, curCommand.substring(1));
                else
                    switch(tokens[0].replace("\n", "")) {
                        case("/nick"):
                            textToSend = nick(curClient, tokens);
                            break;
                        case("/join"):
                            textToSend = join(curClient, tokens);
                            break;
                        case("/leave"):
                            textToSend = leave(curClient, tokens);
                            break;
                        case("/bye"):
                            textToSend = bye(curClient, tokens);
                            break;
                        case("/priv"):
                            textToSend = priv(curClient, tokens);
                            break;
                        default:
                            textToSend = ERR;
                            break;
                    }
            }
            else 
                textToSend = message(curClient, curCommand);

            buffer.clear();
            buffer.put(textToSend.getBytes());
            buffer.flip();
            sc.write(buffer);
        }

        return true;
    }

    /* Main Methods */

    static String message(Client curClient, String message) {
        if(curClient.state != State.INSIDE)
            return ERR;
        broadcast("MESSAGE " + curClient.nick + " " + message, curClient.room, curClient.nick);
        return "MESSAGE " + curClient.nick + " " + message;
    }

    static String nick(Client curClient, String[] tokens) {
        if(tokens.length != 2)
            return ERR;

        String newNick = tokens[1].replace("\n", "");
        if(containsNick(newNick))
            return ERR;

        if(curClient.state == State.INSIDE)
            broadcast("NEWNICK " + curClient.nick + " " + newNick + "\n", curClient.room, curClient.nick);
        else
            curClient.state = State.OUTSIDE;
        curClient.nick = newNick;
        return OK;
    }

    static String join(Client curClient, String[] tokens) {
        if(tokens.length != 2)
            return ERR;
        String newRoomName = tokens[1].replace("\n", "");;

        switch(curClient.state) {
            case INIT:
                return ERR;
            case OUTSIDE:
                joinRoom(newRoomName, curClient);
                broadcast("JOINED " + curClient.nick + "\n", curClient.room, curClient.nick);
                return OK;
            case INSIDE:
                if(curClient.room.name.equals(newRoomName))
                    return ERR;
                broadcast("LEFT " + curClient.nick + "\n", curClient.room, curClient.nick);
                joinRoom(newRoomName, curClient);
                broadcast("JOINED " + curClient.nick + "\n", curClient.room, curClient.nick);
                return OK;
            default: return ERR;
        }
    }

    static String leave(Client curClient, String[] tokens) {
        if(tokens.length != 1)
            return ERR;
        if(curClient.state != State.INSIDE)
            return ERR;
        leaveRoom(curClient);
        return OK;
    }

    static String bye(Client curClient, String[] tokens) {
        if(tokens.length != 1)
            return ERR;
        leaveRoom(curClient);
        leaveChat(curClient);
        return "BYE\n";
    }

    static String priv(Client curClient, String[] tokens) {
        if(tokens.length < 3 || !containsNick(tokens[1]) || curClient.nick.equals(tokens[1]))
            return ERR;
        String message = "";
        for(int i = 2; i < tokens.length; i++)
            message += i == tokens.length - 1 ? tokens[i] : tokens[i] + " ";
        sendPrivate("PRIVATE " + curClient.nick + " " + message, tokens[1]);
        return OK;
    }

    static void disconnect(SelectionKey key) {
        if(key.attachment() == null)
            return;
        Client client = (Client)key.attachment();
        leaveRoom(client);
        leaveChat(client);
    }

    /* Aux Methods */

    static ChatRoom joinRoom(String toFind, Client whoAsked) {
        if(whoAsked.room != null)
            whoAsked.leaveRoom();
        ChatRoom foundRoom = findRoom(toFind);
        if(foundRoom == null) {
            foundRoom = new ChatRoom(toFind, whoAsked);
            roomList.add(foundRoom);
        }
        else
            foundRoom.join(whoAsked);
        whoAsked.room = foundRoom;
        whoAsked.state = State.INSIDE;
        return foundRoom;
    }

    static void leaveRoom(Client client) {
        if(client.room == null)
            return;
        broadcast("LEFT " + client.nick + "\n", client.room, client.nick);
        client.leaveRoom();
    }

    static void leaveChat(Client client) {
        client.key.attach(null);
        clientList.remove(client);
    }

    static ChatRoom findRoom(String toFind) {
        for(ChatRoom room: roomList)
            if(room.name.equals(toFind))
                return room;
        return null;
    }

    static Client findClient(String toFind) {
        for(Client client: clientList)
            if(client.nick.equals(toFind))
                return client;
        return null;
    }

    static boolean containsNick(String toFind) {
        for(Client client: clientList)
            if(client.nick != null && client.nick.equals(toFind))
                return true;
        return false;
    }

    static void broadcast(String message, ChatRoom room, String exceptThisOne) {
        if(room == null)
            return;
        try
		{
            for(Client client: room.users) {
                if(client.nick.equals(exceptThisOne))
                    continue;
                SocketChannel ch = (SocketChannel)client.key.channel();
                buffer.clear();
                buffer.put(message.getBytes());
                buffer.flip();
                ch.write(buffer);
            }
		}
		catch(Exception e)
		{
			System.err.println(e);
		}
    }

    static void sendPrivate(String message, String to) {
        try
		{
            SocketChannel ch = (SocketChannel)findClient(to).key.channel();
            buffer.clear();
            buffer.put(message.getBytes());
            buffer.flip();
            ch.write(buffer);
		}
		catch(Exception e)
		{
			System.err.println(e);
		}
    }
}

