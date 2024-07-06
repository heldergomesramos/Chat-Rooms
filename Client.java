import java.nio.channels.SelectionKey;

public class Client {
    public String nick;
    public ChatRoom room;
    public State state;
    public SelectionKey key;
    public String bufferedMessage;
    
    public Client(SelectionKey key) {
        nick = null;
        room = null;
        state = State.INIT;
        this.key = key;
        clearBuffer();
    }

    public String toString() {
        return room == null ? nick + " null " + state + " " + key : nick + " " + room.name + " " + state + " " + key;
    }

    public void leaveRoom() {
        room.leave(this);
        room = null;
        state = State.OUTSIDE;
    }

    public void clearBuffer() {
        bufferedMessage = "";
    }
}