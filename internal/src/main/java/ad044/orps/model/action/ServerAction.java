package ad044.orps.model.action;

import ad044.orps.model.Category;
import ad044.orps.model.user.OrpsUserDetails;

import java.util.Map;

public class ServerAction extends Action {
    private ServerAction(String idString, Category category, Map<String, String> data) {
        super(idString, category, data, new OrpsUserDetails("SERVER", "SERVER"));
    }

    public static ServerAction game(String idString, String gameUri) {
        return new ServerAction(idString, Category.GAME, Map.of("gameUri", gameUri));
    }
}
