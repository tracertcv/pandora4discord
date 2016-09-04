import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Created by tracer on 7/25/2016.
 */
public class ConfigurationLoader {
    private String configFileName;
    private HashMap<String, JsonObject> configs;
    private HashMap<String, Configurable> configurables;
    private JsonObject rootConfig;


    public ConfigurationLoader(String filename){
        this.configFileName = filename;
        configurables = new HashMap<String,Configurable>();
        configs = new HashMap<String,JsonObject>();
        loadConfigFromFile();
    }

    public void addConfigurable(Configurable newConfigurable){
        configurables.put(newConfigurable.getConfigName(),newConfigurable);
    }

    private void loadConfigFromFile(){
        String currentPath = Paths.get("").toAbsolutePath().toString();
        try{
            JsonObject root = new JsonParser().parse(new FileReader(currentPath + "\\" + configFileName)).getAsJsonObject();
            this.rootConfig = root;
        }catch(FileNotFoundException e){
            Instance.log("Couldn't find config file at " + currentPath + "\\" + configFileName);
        }
    }

    public void configureAll(){
        for(String configName : configurables.keySet()){
            Instance.log("Loading config " + configName);
            configurables.get(configName).configure(rootConfig.get(configName).getAsJsonObject());
        }
    }

    public void saveConfigFile(){
        try{
            String currentPath = Paths.get("").toAbsolutePath().toString();
            JsonObject output = new JsonObject();
            Instance.log(configurables.keySet().toString());
            for(String configName : configurables.keySet()){
                Instance.log("Saving " + configName + "...");
                output.add(configName,configurables.get(configName).getConfig());
            }
            String formattedOutput = output.toString().replace("}","}\n");

            formattedOutput=formattedOutput.replace(",",",\n");
            FileWriter writer = new FileWriter(currentPath + "\\" + configFileName);
            System.out.println(formattedOutput);
            writer.write(formattedOutput);
            writer.flush();
            writer.close();
        }catch(IOException e){
            System.out.println("Couldn't save config file: " + e.getCause());
        }
    }
}
