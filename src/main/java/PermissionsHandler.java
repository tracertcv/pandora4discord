import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * Created by tracer on 7/25/2016.
 */
public class PermissionsHandler implements Configurable {

    private HashMap<String,User> userList;

    public PermissionsHandler(){
        this.userList=new HashMap<String,User>();
    }

    public PermissionsHandler(JsonObject config){
        this.userList = new HashMap<String,User>();
        buildUserList(config);
    }

    public void buildUserList(JsonObject userListObj){
        HashMap<String, User> newUserList = new HashMap<String,User>();

        for(JsonElement userJson : userListObj.get("users").getAsJsonArray()){
            JsonObject userObj = userJson.getAsJsonObject();
            String userID = userObj.get("userID").getAsString();
            int permissionsLevel = userObj.get("permissionsLevel").getAsInt();

            newUserList.put(userID,new User(userID,permissionsLevel));
        }
        this.userList=newUserList;
    }

    public boolean canUserExecute(String userID, Command command){
        if(this.userList.keySet().contains(userID)){
            User user = this.userList.get(userID);
            return user.getPermissionsLevel() >= command.getPermissionLevel();
        }else return command.getPermissionLevel() == -1;
    }

    public boolean containsUser(String userID){
        return this.userList.keySet().contains(userID);
    }

    public int getUserPermissions(String userID){
        if(this.userList.keySet().contains(userID)){
            return this.userList.get(userID).getPermissionsLevel();
        }
        return -1;
    }

    public void setUserPermissions(String userID,int permissionsLevel){
        if(userList.keySet().contains(userID)){
            userList.get(userID).setPermissionsLevel(permissionsLevel);
        }
    }

    public void addUser(String userID,int permissionsLevel){
        if(!this.userList.keySet().contains(userID)){
            this.userList.put(userID,new User(userID,permissionsLevel));
            User u = userList.get(userID);
            System.out.println("added "+u.getUserID() + ": \'" + u.getPermissionsLevel() + "\'");
        }
    }

    @Override
    public void configure(JsonObject config){
        this.buildUserList(config);
    }

    @Override
    public JsonObject getConfig(){
        JsonObject rootObject = new JsonObject();
        JsonArray users = new JsonArray();
        for(String userID : userList.keySet()){
            User user = userList.get(userID);
            JsonObject userObj = new JsonObject();
            userObj.addProperty("userID",user.getUserID());
            userObj.addProperty("permissionsLevel",user.getPermissionsLevel());
            users.add(userObj);
        }
        rootObject.add("users",users);
        return rootObject;
    }

    @Override
    public String getConfigName(){
        return "userConfig";
    }
}
