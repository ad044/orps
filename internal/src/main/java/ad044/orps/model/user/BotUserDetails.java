package ad044.orps.model.user;


public class BotUserDetails extends OrpsUserDetails {
    public BotUserDetails(String uuid) {
        super(formatBotUsername(uuid), formatBotUuid(uuid));
    }

    public static String formatBotUuid(String uuid) {
        return String.format("Bot-%s", uuid);
    }

    public static String formatBotUsername(String uuid) {
        return String.format("BotUser-%s", uuid);
    }
}
