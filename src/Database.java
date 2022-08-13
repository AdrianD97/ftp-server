import java.io.FileReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.*;
import org.json.simple.parser.*;

abstract class Collection {
};

abstract class Entry {
};

class User extends Entry {
    String username;
    String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
};

class UsersCollection extends Collection {
    List<User> users;

    public UsersCollection() {
        this.users = new LinkedList<User>(); // you should use a set because need unique values for username
    }

    public void addUser(User user) {
        this.users.add(user);
    }

    public void list() {
        for (User user : this.users) {
            System.out.println(user.username + ", " + user.password);
        }
    }

    public User findUserByUsername(String username) {
        for (User user : this.users) {
            if (user.username.equals(username)) {
                return user;
            }
        }

        return null;
    }
}

public class Database {
    private String path;
    private UsersCollection userCollection = new UsersCollection();

    public Database(String path) {
        this.path = path;
    }

    public void load() throws Exception {
        JSONParser parser = new JSONParser();

        JSONObject db = (JSONObject) parser.parse(new FileReader(this.path));

        JSONArray users = (JSONArray) db.get("users");

        Iterator<JSONObject> iterator = users.iterator();

        while (iterator.hasNext()) {
            JSONObject user = (JSONObject) iterator.next();

            this.userCollection.addUser(new User((String) user.get("username"), (String) user.get("password")));
        }

        // this.userCollection.list();
    }

    public UsersCollection getUsersColection() {
        return this.userCollection;
    }
};
