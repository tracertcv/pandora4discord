import org.apache.http.NameValuePair;
import sx.blah.discord.handle.obj.IMessage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by tracer on 7/23/2016.
 */
public class Command {
    private Class definitionsClass = CommandDefinitions.class;
    private CommandDefinitions definitions;
    private String UID;
    private String executeString;
    private String helpInfo;
    private int permissionLevel;
    private ArrayList<NameValuePair> arguments;
    private HashMap<String, Object> extras;
    private Method commandToInvoke;

    public Command(String UID, String executeString, int permissionLevel, ArrayList<NameValuePair> arguments, HashMap<String, Object> extras) throws NoSuchMethodException {
        this.UID = UID;
        this.executeString = executeString;
        this.permissionLevel = permissionLevel;
        this.arguments = arguments;
        this.extras = extras;
        this.definitions = new CommandDefinitions();
        Class[] params;
        if (extras.keySet().isEmpty()) {
            params = new Class[]{IMessage.class, ArrayList.class};
        } else {
            params = new Class[]{IMessage.class, ArrayList.class, HashMap.class};
        }
        commandToInvoke = definitionsClass.getDeclaredMethod(UID, params);
        Instance.log("Registered command " + UID + " with exec syntax " + executeString + " " + arguments + " " + extras.keySet());
    }

    public void execute(IMessage commandMessage, List<String> args) {
        Instance.log("Invoking command " + UID);
        ArrayList<Object> params = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            NameValuePair n = arguments.get(i);
            Instance.log("Parsed arg " + args.get(i) + " for " + UID);
            if (n.getValue().equals("int")) {
                params.add(Integer.parseInt(args.get(i)));
            } else {
                params.add(args.get(i));
            }
        }
        try {
            if (extras.keySet().isEmpty()) {
                commandToInvoke.invoke(definitions, commandMessage, params);
            } else {
                commandToInvoke.invoke(definitions, commandMessage, params, this.extras);
            }
        } catch (Exception e) {
            if (e.getCause() == null) {
                e.printStackTrace();
            } else {
                Instance.log("Failed to invoke command " + UID + ". Cause: " + e.getCause());
                e.printStackTrace();
            }
        }
    }

    public String getUID() {
        return UID;
    }

    public String getExecuteString() {
        return executeString;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public boolean hasArgument(String argument) {
        return arguments.contains(argument);
    }

    public NameValuePair getArgument(int index) throws IndexOutOfBoundsException {
        return arguments.get(index);
    }

    public ArrayList<NameValuePair> getArguments(){
        return this.arguments;
    }

    public Set<String> getExtrasNames(){
        return extras.keySet();
    }

    public int getArgCount() {
        return arguments.size();
    }

    public String getHelpInfo(){
        return this.helpInfo;
    }

    public void setHelpInfo(String s){
        this.helpInfo = s;
    }

}