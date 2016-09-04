import com.ballmerpeakindustries.jPandora.Client;
import com.ballmerpeakindustries.jPandora.SearchResponse;
import com.ballmerpeakindustries.jPandora.Song;
import com.ballmerpeakindustries.jPandora.Station;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.EventSubscriber;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.AudioChannel;
import sx.blah.discord.handle.impl.events.AudioPlayEvent;
import sx.blah.discord.handle.impl.events.AudioStopEvent;
import sx.blah.discord.handle.impl.events.UserVoiceChannelMoveEvent;
import sx.blah.discord.util.DiscordException;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by tracer on 7/24/2016.
 */
public class PandoraClientHelper implements Configurable {
    private Client client;
    private boolean isActive = false;
    private boolean isPaused = false;
    private boolean isYoutube = false;
    private String ytTitle = "";
    private float volume = (float) 10;
    private Station currentStation;
    private Song currentSong;
    private File currentSongFile;
    private AudioChannel currentDiscordAudioChannel;
    private IDiscordClient myClient;

    public PandoraClientHelper(JsonObject config) {
        this.isActive = false;
        this.isPaused = false;
    }
    public PandoraClientHelper() {
        this.isActive = false;
        this.isPaused = false;
    }

    public void setClient(IDiscordClient id){
        this.myClient = id;
    }

    public void login(String user, String pass) {
        this.client = new Client(user, pass, Discord4J.LOGGER);
    }

    public List<Station> getStationList() {
        return this.client.getStationList();
    }

    public void setCurrentStation(Station currentStation) {
        this.currentStation = currentStation;
        System.out.println(currentStation.getStationName());
        Song s = client.playNextSongFromStation(currentStation);
        System.out.println(s == null);
        this.currentSong = client.playNextSongFromStation(currentStation);
    }

    public Station getCurrentStation() {
        return this.currentStation;
    }

    public List<SearchResponse> searchNewStations(String query) {
        client.searchStation(query);
        List<SearchResponse> responseList = new ArrayList<SearchResponse>();
        if (client.getSearchResponseStatus()) {
            responseList = client.getSearchResponses();
        }
        return responseList;
    }

    public void addNewStationFromSearchResponse(int index) {
        if(client.getSearchResponseStatus()) {
            client.addStation(index);
        }else{
            System.out.println("No search results to add.");
        }
    }

    public boolean hasSearchResults() {
        return client.getSearchResponseStatus();
    }

    public void setCurrentSong(Song currentSong) {
        this.currentSong = currentSong;
    }

    public Song getCurrentSong() {
        return this.currentSong;
    }

    public AudioChannel getCurrentDiscordAudioChannel() {
        return currentDiscordAudioChannel;
    }

    public void setCurrentDiscordAudioChannel(AudioChannel currentDiscordAudioChannel) {
        this.currentDiscordAudioChannel = currentDiscordAudioChannel;
        this.setVolume(volume);
    }

    public void setPaused(boolean isPaused) {
        if(this.isActive){
            this.isPaused = isPaused;
        }
        updateStatus(myClient);
    }

    public boolean isPaused() {
        return this.isPaused;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setVolume(float newVolume){
        this.volume=newVolume;
        if(isYoutube){
            newVolume = newVolume / 50;
        }else{
            newVolume = newVolume / 100;
        }
        this.currentDiscordAudioChannel.setVolume(newVolume);
        System.out.println("Volume set to " + newVolume);
    }

    public float getVolume() {
        return this.volume;
    }

    public File getCurrentSongFile() {
        if (this.currentSongFile == null) {
            this.currentSongFile = downloadCurrentSongFile();
        }
        return this.currentSongFile;
    }

    private File downloadCurrentSongFile() {
        try {
            File tempSong = new File("tmp." + this.currentSong.getSongName() + ".mp3");
            FileUtils.copyURLToFile(new URL(this.currentSong.getAudioURL()), tempSong);
            return tempSong;
        } catch (Exception e) {
            e.printStackTrace();
            advanceSong();
            downloadCurrentSongFile();
        }
        throw new RuntimeException("Couldn't get song file.");

    }

    private void advanceSong() {
        if (currentSongFile != null) {
            currentSongFile.delete();
            currentSongFile = null;
        }
        this.currentSong = this.client.playNextSongFromStation(currentStation);
    }

    public void playNextToPlayer() {
        this.isActive=true;
        if (!this.isPaused) {
            advanceSong();
            this.currentDiscordAudioChannel.queueFile(getCurrentSongFile());
        }else{
            this.isPaused = false;
            this.currentDiscordAudioChannel.resume();
        }
        this.setVolume(this.getVolume());
    }

    public void playYoutube(File song, String title){
        Song newSong = new Song("",title,"","","");
        this.ytTitle=title;
        this.isYoutube=true;
        this.currentSong = newSong;
        this.currentSongFile = song;
        this.isActive=true;
        this.isPaused=false;
        this.currentDiscordAudioChannel.clearQueue();
        this.currentDiscordAudioChannel.queueFile(song);
        this.setVolume(this.getVolume());
    }

    public void updateStatus(IDiscordClient dClient){
        String status = currentSong.getArtistName() + " - " + currentSong.getSongName();
        if(this.isPaused){
            dClient.updatePresence(false, Optional.of("Paused"));
        }else {
            if(this.isYoutube){
                status=ytTitle;
            }
            status = status.replace("_"," ");
            dClient.updatePresence(false, Optional.of(status));
        }
    }

    @EventSubscriber
    public void onSongStart(AudioPlayEvent e){
        this.isActive=true;
        this.isPaused=false;
        this.isYoutube=false;
        updateStatus(e.getClient());
    }

    @EventSubscriber
    public void onSongEnd(AudioStopEvent e) {
        this.isActive=false;
        if(!this.isPaused){
            this.playNextToPlayer();
        }
        this.updateStatus(e.getClient());
    }

    @EventSubscriber
    public void onJoinedChannel(UserVoiceChannelMoveEvent e) {
        try{
            this.currentDiscordAudioChannel=e.getNewChannel().getAudioChannel();
            this.setVolume(volume);
            updateStatus(e.getClient());
        }catch(DiscordException ex){

        }
    }

    @Override
    public void configure(JsonObject config){
        float volume = config.get("volume").getAsFloat();
        this.volume = volume;
    }

    @Override
    public JsonObject getConfig(){
        JsonObject root = new JsonObject();
        root.addProperty("volume",this.volume);
        return root;
    }

    @Override
    public String getConfigName(){
        return "playerConfig";
    }

}
