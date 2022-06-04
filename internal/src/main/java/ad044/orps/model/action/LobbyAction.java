package ad044.orps.model.action;

import ad044.orps.model.lobby.Lobby;

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
    private final Lobby targetLobby;

    public LobbyAction(Action action, ID id, Lobby targetLobby) {
        super(action.getIdString(), action.getCategory(), action.getData(), action.getAuthor());

        this.id = id;
        this.targetLobby = targetLobby;
    }

    public ID getId() {
        return id;
    }

    public Lobby getTargetLobby() {
        return targetLobby;
    }
}
