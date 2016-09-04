/**
 * Created by tracer on 6/22/2016.
 */
public class DSBot {
    private static final String token = "MTkyNzczMTc5NzcxNTg0NTEy.CkNtgw.Z1XKpbDLteSwLnx4K6paVcnasog";

    //private static final Logger log = LoggerFactory.getLogger(DSBot.class);

    public static void main(String[] args) {
        Instance bot = new Instance(token);
        try {
            bot.login();
        } catch (Exception e) {
            //log.warn("Bot could not start", e);
        }
    }

}
