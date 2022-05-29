package ad044.orps.model.action;

public class LobbyAction extends Action {
    public enum ID {
        ADD_BOT,
        NEW_TEXT_MESSAGE,
        USER_JOIN,
        USER_LEAVE,
        MEMBER_KICK,
        UPDATE_SETTINGS,
        START_GAME,
    }

    private final ID id;
    private final String targetLobbyUri;

    public LobbyAction(Action action, ID id, String targetLobbyUri) {
        super(action.getIdString(), action.getCategory(), action.getData(), action.getAuthor());

        this.id = id;
        this.targetLobbyUri = targetLobbyUri;
    }

    public ID getId() {
        return id;
    }

    public String getTargetLobbyUri() {
        return targetLobbyUri;
    }
}
