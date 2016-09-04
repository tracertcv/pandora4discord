/**
 * Created by tracer on 7/25/2016.
 */
public class User {
    private String userID;
    private int permissionsLevel;

    public User(String userID,int permissionsLevel){
        this.userID = userID;
        this.permissionsLevel = permissionsLevel;
    }

    public String getUserID() {
        return userID;
    }

    public int getPermissionsLevel() {
        return permissionsLevel;
    }

    public void setPermissionsLevel(int permissionsLevel) {
        this.permissionsLevel = permissionsLevel;
    }
}
