import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * Created by tracer on 8/27/2016.
 */
public class ServiceCredentialHandler implements Configurable {
    private HashMap<String,String[]> credentials;

    public ServiceCredentialHandler(){
        this.credentials=new HashMap<String,String[]>();
    }

    public String[] getCredentials(String service){
        String[] creds = {"",""};
        if(credentials.containsKey(service)){
            creds = credentials.get(service);
        }
        return creds;
    }

    @Override
    public void configure(JsonObject config){
        System.out.println(config.get("credentials"));
        for(JsonElement service : config.get("credentials").getAsJsonArray()){
            JsonObject serviceObject = service.getAsJsonObject();
            String serviceName = serviceObject.get("service").getAsString();
            String[] credentialsArray = new String[2];
            credentialsArray[0] = serviceObject.get("username").getAsString();
            credentialsArray[1] = serviceObject.get("password").getAsString();
            credentials.put(serviceName,credentialsArray);
            Instance.log("Loaded credentials for service: "+serviceName);
        }
    }

    @Override
    public JsonObject getConfig(){
        JsonArray data = new JsonArray();
        JsonObject root = new JsonObject();
        for(String key : credentials.keySet()){
            String[] creds = credentials.get(key);
            JsonObject credentialObject = new JsonObject();
            credentialObject.addProperty("service",key);
            credentialObject.addProperty("username",creds[0]);
            credentialObject.addProperty("password",creds[1]);
            data.add(credentialObject);
        }
        root.add("credentials",data);
        return root;
    }

    @Override
    public String getConfigName(){
        return "credentialsConfig";
    }
}
