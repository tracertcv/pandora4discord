import sx.blah.discord.Discord4J;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.EventSubscriber;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.DiscordDisconnectedEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.HTTP429Exception;
import sx.blah.discord.util.MissingPermissionsException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Instance {

    //private static final Logger log = LoggerFactory.getLogger(Instance.class);

    private volatile IDiscordClient client;
    private String token;
    private String configFileName = "config.json";
    private ServiceCredentialHandler credentialHandler;
    private PandoraClientHelper pc;
    private CommandHandler commandHandler;
    private YoutubeClientHelper youtube;
    private PermissionsHandler permissionsHandler;

    private final AtomicBoolean reconnect = new AtomicBoolean(true);
    private ConfigurationLoader config;

    public Instance(String token) {
        this.config = new ConfigurationLoader(configFileName);
        this.token = token;
        this.youtube = new YoutubeClientHelper();
    }

    public void Configure(){
        commandHandler = new CommandHandler();
        permissionsHandler = new PermissionsHandler();
        credentialHandler = new ServiceCredentialHandler();
        pc = new PandoraClientHelper();
        pc.setClient(client);
        commandHandler.addExtra("pandora", pc);
        commandHandler.addExtra("system",this);
        commandHandler.addExtra("youtube",youtube);
        commandHandler.setPermissionsHandler(permissionsHandler);
        config.addConfigurable(commandHandler);
        config.addConfigurable(permissionsHandler);
        config.addConfigurable(pc);
        config.addConfigurable(youtube);
        config.addConfigurable(credentialHandler);
        config.configureAll();
    }

    public void login() throws DiscordException {
        Instance.log("Initializing...");
        client = new ClientBuilder().withToken(token).login();
        client.updatePresence(false, Optional.of("-"));
        this.Configure();
        client.getDispatcher().registerListener(this);
        client.getDispatcher().registerListener(pc);
        doPandoraLogin(credentialHandler.getCredentials("pandora"));
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        Instance.log("Bot armed and ready.");
    }

    @EventSubscriber
    public void onDisconnect(DiscordDisconnectedEvent event) {
        CompletableFuture.runAsync(() -> {
            if (reconnect.get()) {
                Instance.log("Reconnecting bot");
                try {
                    login();
                } catch (DiscordException e) {
                    e.printStackTrace();
                    Instance.log("Failed to reconnect bot");
                }
            }
        });
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent e) {
        try {
            if(e.getMessage().getAuthor().getName().equals("Bad Catholic")){
                if(Math.random()<0.5){
                    e.getMessage().getChannel().sendMessage("THANKS RYAN");
                }
            }
            IMessage message = e.getMessage();
            String response = commandHandler.doCommand(message);
            if(!response.isEmpty()) {
                e.getMessage().getChannel().sendMessage(response);
            }
        } catch (Exception ex) {
            if (ex instanceof DiscordException) {
               Instance.log("DiscordException caught while trying to send message.");
            }
            if (ex instanceof MissingPermissionsException) {
                Instance.log("Missing permissions for channel "+e.getMessage().getChannel().getName());
            }
        }
    }

    public void terminate() {
        reconnect.set(false);
        config.saveConfigFile();
        try {
            client.logout();
            Instance.log("Shutting down.");
        } catch (HTTP429Exception | DiscordException e) {
            Instance.log("Logout failed");
        }
    }

    public static void log(String message){
        Discord4J.LOGGER.debug(message);
    }

    private void doPandoraLogin(String[] credentials){
        pc.login(credentials[0],credentials[1]);
    }

}
