import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Paths;

/**
 * Created by tracer on 7/29/2016.
 */
public class YoutubeClientHelper implements  Configurable{
    private String urlEncodeMethod = "ISO-8859-1";
    private File outFile;
    private String path;
    private String baseAPIEndpoint = "http://www.youtubeinmp3.com/fetch/?";
    public YoutubeClientHelper(){

    }

    public void setPath(String path){
        String absolutePath = Paths.get("").toAbsolutePath().toString();
        this.path = absolutePath + "\\";
    }

    public File getSongFromYoutube(String sourceURL) throws IOException{
        String url = baseAPIEndpoint + "format=JSON&video="+ urlEncode(sourceURL);
        try{
            URL connURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)connURL.openConnection();
            //Set up the connection
            conn.setRequestMethod("POST");//Always POST
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
            conn.setRequestProperty("Content-Type", "text/plain");
            conn.setRequestProperty("Accept", "*/*");
            conn.connect();
            String line;
            JsonElement response = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while((line=in.readLine())!=null){
                System.out.println(line);
                response = new JsonParser().parse(line);
                if(response.isJsonObject()){
                    break;
                }
            }

            JsonObject respObject = response.getAsJsonObject();
            String songName = respObject.get("title").getAsString();
            songName = songName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String dataUrl = respObject.get("link").getAsString();
            File tempSong = new File("tmp."+songName+".mp3");
            FileUtils.copyURLToFile(new URL(dataUrl), tempSong);
            this.outFile=tempSong;
        }catch(Exception e){
            throw new IOException("Couldn't get song file.");
        }
        return outFile;
    }

    @Override
    public void configure(JsonObject config){
        String path = config.get("path").getAsString();
        this.setPath(path);
    }

    @Override
    public JsonObject getConfig(){
        JsonObject root = new JsonObject();
        root.addProperty("path",this.path);
        return root;
    }

    @Override
    public String getConfigName(){
        return "youtubeConfig";
    }

    private String urlEncode(String url){
        try{
            return URLEncoder.encode(url,urlEncodeMethod);
        }catch(Exception e){
            throw new RuntimeException("Couldn't encode URL.");
        }
    }
}
