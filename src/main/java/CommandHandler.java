import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import sx.blah.discord.handle.obj.IMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tracer on 7/23/2016.
 */
public class CommandHandler implements Configurable {
    private PermissionsHandler permissionsHandler;
    private HashMap<String, Command> commandList;
    private HashMap<String, Object> extraList;
    private JsonObject jsonCommands;

    public CommandHandler() {
        this.commandList = new HashMap<String, Command>();
        this.extraList = new HashMap<String, Object>();
        this.addExtra("commandHandler",this);
    }

    public CommandHandler(JsonObject commands) {
        this.commandList = new HashMap<String, Command>();
        this.extraList = new HashMap<String, Object>();
        this.addExtra("commandHandler",this);
        this.buildCommandList(commands);
    }

    public void buildCommandList(JsonObject commandListObj) {
        this.commandList = new HashMap<String, Command>();
        for (JsonElement commandElement : commandListObj.get("commands").getAsJsonArray()) {
            JsonObject command = commandElement.getAsJsonObject();
            String uid = command.get("commandUID").getAsString();
            String exec = command.get("commandExecuteString").getAsString();
            String helpinfo = command.get("helpinfo").getAsString();
            int permissions = command.get("permissionLevelRequired").getAsInt();
            JsonElement args = command.get("arguments");
            JsonElement extras = command.get("requiredExtras");

            ArrayList<NameValuePair> argTypes = new ArrayList<NameValuePair>();
            for (JsonElement arg : args.getAsJsonArray()) {
                JsonObject argObj = arg.getAsJsonObject();
                String argumentName = argObj.get("argument").getAsString();
                String argType = argObj.get("type").getAsString();
                argTypes.add(new BasicNameValuePair(argumentName, argType));
            }
            HashMap<String, Object> extrasToPut = new HashMap<String, Object>();
            if (extras != null) {
                for (JsonElement extra : extras.getAsJsonArray()) {
                    String extraName = extra.getAsString();
                    if (extraList.containsKey(extraName)) {
                        extrasToPut.put(extraName, extraList.get(extraName));
                    }
                }
            }
            try {
                Command newCommand = new Command(uid, exec, permissions, argTypes, extrasToPut);
                newCommand.setHelpInfo(helpinfo);
                this.commandList.put(exec, newCommand);
            } catch (Exception e) {
                System.out.println("Couldn't instantiate command \'" + uid + "\':");
                e.printStackTrace();
            }
        }
    }

    public String doCommand(IMessage commandMessage) {
        if (commandMessage.getChannel().isPrivate()) {
            String command = commandMessage.getContent();
            StringBuilder resp = new StringBuilder();
            String disectedCommand[] = command.split(" ");
            String exec = disectedCommand[0];
            ArrayList<String> args = parseArgs(command);
            String username = commandMessage.getAuthor().getName();
            Instance.log("Received command " + command + " from user " + username + ".");
            if (commandIsValid(disectedCommand,args)) {
                Command commandObject = commandList.get(exec);
                if(permissionsHandler.canUserExecute(commandMessage.getAuthor().getID(),commandObject)) {
                    commandObject.execute(commandMessage, args);
                }else{
                    resp.append("You don't have permission to execute command " + exec + ".");
                }
            } else {
                resp.append("\'" + exec + "\' is not a valid command or some arguments were incorrect.");
            }
            return resp.toString();
        }
        return "";
    }

    public HashMap<String,Command> getCommandList(){
        return commandList;
    }

    private boolean commandIsValid(String c[],ArrayList<String> args) {
        if (commandList.containsKey(c[0])) {
            Command command = commandList.get(c[0]);
            if(args.size()>command.getArgCount()){
                return false;
            }
            for (int i = 0; i < args.size(); i++) {
                if (command.getArgument(i).getValue().equals("int")) {
                    try {
                        Integer.parseInt(args.get(i));
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private ArrayList<String> parseArgs(String message){
        String disectedCommand[] = message.split(" ");
        ArrayList<String> args = new ArrayList<String>();
        if(disectedCommand.length>1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < disectedCommand.length; i++) {
                sb.append(disectedCommand[i] + " ");
            }
            sb.deleteCharAt(sb.length() - 1);
            String argsAsString = sb.toString();
            String regex = "((\"[\\w ]*\" ?)|(\\S* ?))";
            Matcher m = Pattern.compile(regex).matcher(argsAsString);
            while (m.find()) {
                StringBuilder sbArg = new StringBuilder();
                if(m.group(2)!=null){
                    if (!m.group(2).equals("")) {
                        sbArg.append(m.group(2));
                        sbArg.deleteCharAt(0);
                        sbArg.delete(sbArg.length() - 1, sbArg.length());
                    }
                } else if(m.group(3)!=null) {
                    if (!m.group(3).equals("")) {
                        sbArg.append(m.group(3));
                        if (sbArg.charAt(sbArg.length()-1) == ' ') {
                            sbArg.deleteCharAt(sbArg.length()-1);
                        }
                    }
                }
                String arg = sbArg.toString();
                if (!arg.equals("")) {
                    args.add(arg);
                }
            }
        }
        return args;
    }

    public PermissionsHandler getPermissionsHandler(){
        return this.permissionsHandler;
    }

    public void setPermissionsHandler(PermissionsHandler permissionsHandler) {
        this.permissionsHandler = permissionsHandler;
    }

    public void addExtra(String key, Object extra) {
        extraList.put(key, extra);
    }

    @Override
    public void configure(JsonObject config){
        this.buildCommandList(config);
    }

    @Override
    public JsonObject getConfig(){
        JsonArray data = new JsonArray();
        JsonObject root = new JsonObject();
        for(String commandName : commandList.keySet()){
            JsonObject commandJson = new JsonObject();
            Command c = commandList.get(commandName);
            JsonArray args = new JsonArray();
            JsonArray extras = new JsonArray();
            commandJson.addProperty("commandUID",c.getUID());
            commandJson.addProperty("commandExecuteString",c.getExecuteString());
            commandJson.addProperty("helpinfo",c.getHelpInfo());
            commandJson.addProperty("permissionLevelRequired",c.getPermissionLevel());
            for(NameValuePair arg : c.getArguments()){
                JsonObject argJson = new JsonObject();
                argJson.addProperty("argument",arg.getName());
                argJson.addProperty("type",arg.getValue());
                args.add(argJson);
            }
            for(String extraName : c.getExtrasNames()){
                extras.add(extraName);
            }
            commandJson.add("arguments",args);
            commandJson.add("requiredExtras",extras);
            data.add(commandJson);
        }
        root.add("commands",data);
        return root;
    }

    @Override
    public String getConfigName(){
        return "commandConfig";
    }

}
