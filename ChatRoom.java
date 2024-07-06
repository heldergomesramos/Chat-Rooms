import java.util.LinkedList;

public class ChatRoom {
    public LinkedList<Client> users;
    public String name;

    public ChatRoom(String name, Client firstUser) {
        users = new LinkedList<>();
        this.name = name;
        users.add(firstUser);
    }

    public String toString() {
        return name + ": " + users.toString();
    }

    public void join(Client toAdd) {
        users.add(toAdd);
    }

    public void leave(Client toRemove) {
        int i = 0;
        for(; i < users.size(); i++)
            if(toRemove == users.get(i))
                break;
        users.remove(i);
    }
}