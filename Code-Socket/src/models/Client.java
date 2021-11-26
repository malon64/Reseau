package models;


/**
 * Class Client to handle his creation and username
 */
public class Client {
    private String username;

    public Client(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


}
