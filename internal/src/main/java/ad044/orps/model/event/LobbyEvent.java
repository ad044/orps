package ad044.orps.model.event;

import ad044.orps.dto.GameDTO;
import ad044.orps.dto.LobbyDTO;
import ad044.orps.dto.UserDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LobbyEvent extends Event<LobbyEvent.ID> {
    public enum ID {
        MEMBER_JOIN,
        MEMBER_LEAVE,
        MEMBER_KICK,
        GOT_KICKED,
        NEW_TEXT_MESSAGE,
        CREATED_GAME,
        OWNER_UPDATED,
        SETTINGS_UPDATED,
        RECEIVE_LOBBY_DATA;
    }
    private final String lobbyUri;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public LobbyEvent(@JsonProperty("id") ID id, @JsonProperty("lobbyUri") String lobbyUri) {
        super(id);
        this.lobbyUri = lobbyUri;
    }

    public String getLobbyUri() {
        return lobbyUri;
    }

    public static LobbyEvent addBot(String lobbyUri, UserDTO botData) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.MEMBER_JOIN, lobbyUri);
        event.putData("memberData", botData);

        return event;
    }

    public static LobbyEvent newTextMessage(String lobbyUri, UserDTO author, String messageContent) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.NEW_TEXT_MESSAGE, lobbyUri);
        event.putData("messageAuthor", author);
        event.putData("messageContent", messageContent);

        return event;
    }

    public static LobbyEvent memberJoin(String lobbyUri, UserDTO memberThatJoined) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.MEMBER_JOIN, lobbyUri);
        event.putData("memberData", memberThatJoined);

        return event;
    }

    public static LobbyEvent receiveLobbyData(String lobbyUri, LobbyDTO lobbyData) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.RECEIVE_LOBBY_DATA, lobbyUri);
        event.putData("lobbyData", lobbyData);

        return event;
    }

    public static LobbyEvent ownerUpdated(String lobbyUri, String newOwnerUuid) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.OWNER_UPDATED, lobbyUri);
        event.putData("newOwnerUuid", newOwnerUuid);

        return event;
    }

    public static LobbyEvent memberLeave(String lobbyUri, String userThatLeftUuid) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.MEMBER_LEAVE, lobbyUri);
        event.putData("memberUuid", userThatLeftUuid);

        return event;
    }

    public static LobbyEvent memberKick(String lobbyUri, String memberUuid) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.MEMBER_KICK, lobbyUri);
        event.putData("memberUuid", memberUuid);

        return event;
    }

    public static LobbyEvent gotKicked(String lobbyUri) {
        return new LobbyEvent(ID.GOT_KICKED, lobbyUri);
    }

    public static LobbyEvent createdGame(String lobbyUri, GameDTO gameData) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.CREATED_GAME, lobbyUri);
        event.putData("gameData", gameData);

        return event;
    }

    public static LobbyEvent settingsUpdated(String lobbyUri, String settingName, String settingValue) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.SETTINGS_UPDATED, lobbyUri);
        event.putData("settingName", settingName);
        event.putData("settingValue", settingValue);

        return event;
    }
}
