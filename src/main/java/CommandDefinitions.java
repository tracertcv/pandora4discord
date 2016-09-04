import com.ballmerpeakindustries.jPandora.SearchResponse;
import com.ballmerpeakindustries.jPandora.Station;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.AudioChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by tracer on 7/23/2016.
 */
public class CommandDefinitions {
    public CommandDefinitions() {

    }

    /*

    Implement all command functions here.

     */

    /*

    General commands

     */

    public void joinChannelByName(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        String channelName = (String) arguments.get(0);
        IDiscordClient client = message.getClient();
        pandoraClient.setClient(client);
        for (IVoiceChannel channel : client.getVoiceChannels()) {
            if (channel.getName().equals(channelName)) {
                channel.join();
                try {
                    pandoraClient.setCurrentDiscordAudioChannel(channel.getAudioChannel());
                    if(pandoraClient.getCurrentDiscordAudioChannel()==null){
                        pandoraClient.setCurrentDiscordAudioChannel(channel.getAudioChannel());
                    }
                } catch (Exception e) {
                    System.out.println("Couldn't connect to voice channel " + channel.getName() + ". Cause: " + e.getCause());
                }
            }
        }
    }

    public void joinChannelByUser(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        IUser sender = message.getAuthor();
        pandoraClient.setClient(message.getClient());
        IVoiceChannel channel = sender.getVoiceChannel().get();
        channel.join();
        try {
            pandoraClient.setCurrentDiscordAudioChannel(channel.getAudioChannel());
            if(pandoraClient.getCurrentDiscordAudioChannel()==null){
                pandoraClient.setCurrentDiscordAudioChannel(channel.getAudioChannel());
            }
        } catch (Exception e) {
            System.out.println("Couldn't connect to voice channel " + channel.getName() + ". Cause: " + e.getCause());
        }
    }

    public void getMusicList(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        IDiscordClient client = message.getClient();
        StringBuilder sb = new StringBuilder();
        List<Station> stationList = pandoraClient.getStationList();
        Station currentStation = pandoraClient.getCurrentStation();
        String currentStationName;

        if (currentStation == null) {
            currentStationName = "None";
        } else {
            currentStationName = currentStation.getStationName();
        }

        sb.append("\n===Current Station===\n" + currentStationName);
        sb.append("\n===Available Stations===\n");
        for (int i = 0; i < stationList.size(); i++) {
            Station station = stationList.get(i);
            sb.append(i + " - " + station.getStationName() + "\n");
        }
        reply(message, sb.toString());
    }

    public void pickStation(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        List<Station> stationList = pandoraClient.getStationList();
        int index = (int) arguments.get(0);
        pandoraClient.setCurrentStation(stationList.get(index));
        if(pandoraClient.isActive()){
            pandoraClient.getCurrentDiscordAudioChannel().clearQueue();
            pandoraClient.playNextToPlayer();
        }

        reply(message, "Station changed to " + stationList.get(index).getStationName());
    }

    public void playMusic(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        if (pandoraClient.getCurrentStation() != null) {
            pandoraClient.playNextToPlayer();
            pandoraClient.setVolume(pandoraClient.getVolume());
        } else {
            reply(message, "No station currently selected.");
        }
    }

    public void pause(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        AudioChannel audio = pandoraClient.getCurrentDiscordAudioChannel();
        pandoraClient.setPaused(true);
        audio.pause();
    }

    public void unpause(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        AudioChannel audio = pandoraClient.getCurrentDiscordAudioChannel();
        audio.resume();
        pandoraClient.setPaused(false);
    }

    public void setVolume(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        float newVol = Float.parseFloat(arguments.get(0).toString());
        if (!(newVol < 0 || newVol > 20)) {
            pandoraClient.setVolume(newVol);
        } else {
            reply(message, "Please choose a volume between 0 and 20.");
        }
    }

    public void searchForStation(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        String query = (String) arguments.get(0);
        List<SearchResponse> responses = pandoraClient.searchNewStations((String) arguments.get(0));
        StringBuilder sb = new StringBuilder();
        sb.append("\n===Search results for \'" + query + "\' ===\n");

        for (int i = 0; i < responses.size(); i++) {
            SearchResponse s = responses.get(i);
            sb.append(i + " - " + s.getName() + " - " + s.getType() + "\n");
        }
        reply(message, sb.toString());
    }

    public void addNewStation(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras) {
        PandoraClientHelper pandoraClient = (PandoraClientHelper) extras.get("pandora");
        if (pandoraClient.hasSearchResults()) {
            int index = (int) arguments.get(0);
            pandoraClient.addNewStationFromSearchResponse(index);
            reply(message, "Successfully added station.");
        } else {
            reply(message, "No search results to add. Try searching first.");
        }
    }

    public void getHelpMessage(IMessage message, ArrayList<Object> arguments,HashMap<String,Object> extras){
        CommandHandler parent = (CommandHandler) extras.get("commandHandler");
        PermissionsHandler ph = parent.getPermissionsHandler();
        HashMap<String,Command> commandList = parent.getCommandList();
        StringBuilder sb = new StringBuilder();
        sb.append("\n===Available Commands===\n");
        for(String key : commandList.keySet()){
            Command c = commandList.get(key);
            if(!c.getHelpInfo().equals("unlisted")&&ph.canUserExecute(message.getAuthor().getID(),c)) {
                sb.append(c.getExecuteString());
                for (int i = 0; i < c.getArgCount(); i++) {
                    sb.append(" <" + c.getArgument(i).getName() + ">");
                }
                sb.append(": " + c.getHelpInfo() + "\n\n");
            }
        }
        reply(message, sb.toString());
    }
    /*

    Youtube music

     */

    public void playYoutube(IMessage message, ArrayList<Object> arguments, HashMap<String, Object> extras){
        YoutubeClientHelper youtube = (YoutubeClientHelper) extras.get("youtube");
        String url =(String) arguments.get(0);
        PandoraClientHelper pandoaClient = (PandoraClientHelper) extras.get("pandora");
        try{
            File song = youtube.getSongFromYoutube(url);
            pandoaClient.playYoutube(song,song.getName());
            reply(message,"Playing mp3 from youtube.");
        }catch(Exception e){
            reply(message,"Couldn't download mp3 for this track.");
        }
    }

    /*

    User auth commands

     */
    public void selfAuth(IMessage message, ArrayList<Object> arguments,HashMap<String,Object> extras){
        CommandHandler ch = (CommandHandler)extras.get("commandHandler");
        PermissionsHandler ph = ch.getPermissionsHandler();
        String userID = message.getAuthor().getID();
        if(!ph.containsUser(userID)){
            ph.addUser(userID,0);
            reply(message, "Added user " + userID + " with permissions level " + 0 + ".");
        }else{
            reply(message,"UserID " + userID + " already in user list.");
        }
    }

    public void addUserByName(IMessage message, ArrayList<Object> arguments,HashMap<String,Object> extras){
        CommandHandler ch = (CommandHandler)extras.get("commandHandler");
        PermissionsHandler ph = ch.getPermissionsHandler();
        String userName = (String) arguments.get(0);
        int permissionsLevel = (int) arguments.get(1);
        String userID = getUserIDByName(message,userName);
        if(!userID.equals("NOTFOUND")) {
            if (!ph.containsUser(userID)) {
                ph.addUser(userID, permissionsLevel);
                reply(message, "Added user " + userID + " with permissions level " + permissionsLevel + ".");
            } else {
                reply(message, "UserID " + userID + " already in user list.");
            }
        }else{
            reply(message, "User " + userName + "not found on this server.");
        }
    }

    public void addUserByID(IMessage message, ArrayList<Object> arguments,HashMap<String,Object> extras){
        CommandHandler ch = (CommandHandler)extras.get("commandHandler");
        PermissionsHandler ph = ch.getPermissionsHandler();
        String userID = (String) arguments.get(0);
        int permissionsLevel = (int) arguments.get(1);
        if(!ph.containsUser(userID)){
            ph.addUser(userID,permissionsLevel);
            reply(message, "Added user " + userID + " with permissions level " + permissionsLevel + ".");
        }else{
            reply(message,"UserID " + userID + " already in user list.");
        }
    }

    public void setUserPermissions(IMessage message, ArrayList<Object> arguments,HashMap<String,Object> extras){
        CommandHandler ch = (CommandHandler)extras.get("commandHandler");
        PermissionsHandler ph = ch.getPermissionsHandler();
        String userID = (String) arguments.get(0);
        int permissionsLevel = (int) arguments.get(1);
        if(ph.containsUser(userID)){
            ph.setUserPermissions(userID,permissionsLevel);
            reply(message, "Updated permissions for user " + userID + " to " + permissionsLevel + ".");
        }else{
            reply(message,"UserID " + userID + " not in user list.");
        }
    }
    /*

    System commands

     */

    public void shutdownBot(IMessage message, ArrayList<Object> arguments,HashMap<String,Object> extras){
        Instance instance = (Instance) extras.get("system");
        //reply(message,"Shutting down bot.");
        instance.terminate();
    }

    /*

    Debug commands

     */

    public void debugMessageChannel(IMessage message, ArrayList<Object> arguments){
        String channelName = (String) arguments.get(0);
        String messageToSend = (String) arguments.get(1);

    }

    /*

    Helper methods

     */

    private void reply(IMessage message, String reply) {
        try {
            IPrivateChannel channel = message.getClient().getOrCreatePMChannel(message.getAuthor());
            channel.sendMessage(reply);
        } catch (Exception e) {
            System.out.println("Couldn't create PM channel with user " + message.getAuthor().getID());
        }
    }

    private String getUserIDByName(IMessage message, String username){
        List<IUser> userList = new ArrayList<IUser>();
        userList.addAll(message.getGuild().getUsers());
        String userID = "NOTFOUND";
        for(IUser user : userList){
            if(user.getName().equals(username)){
                userID = user.getID();
            }
        }
        return userID;
    }

}
