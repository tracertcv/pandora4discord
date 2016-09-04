import com.google.gson.JsonObject;

/**
 * Created by tracer on 7/28/2016.
 */
public interface Configurable {
    void configure(JsonObject config);
    JsonObject getConfig();
    String getConfigName();
}
