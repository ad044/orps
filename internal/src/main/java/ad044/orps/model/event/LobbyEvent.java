package ad044.orps.model.event;

import ad044.orps.dto.GameDTO;
import ad044.orps.dto.LobbyDTO;
import ad044.orps.dto.UserDTO;
import ad044.orps.model.Category;
import ad044.orps.model.user.OrpsUserDetails;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.CheckedOutputStream;

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
    public LobbyEvent(@JsonProperty("id") ID id, @JsonProperty("lobbyUri") String lobbyUri,
                      @JsonProperty("recipientUuids") List<String> recipientUuids) {
        super(id, Category.LOBBY, recipientUuids);
        this.lobbyUri = lobbyUri;
    }

    public LobbyEvent(ID id, String lobbyUri, String recipientUuid) {
        super(id, Category.LOBBY, Collections.singletonList(recipientUuid));
        this.lobbyUri = lobbyUri;
    }

    public String getLobbyUri() {
        return lobbyUri;
    }

    public static LobbyEvent addBot(List<String> recipients, String lobbyUri, UserDTO botData) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.MEMBER_JOIN, lobbyUri, recipients);
        event.putData("memberData", botData);

        return event;
    }

    public static LobbyEvent newTextMessage(List<String> recipients, String lobbyUri, UserDTO author, String messageContent) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.NEW_TEXT_MESSAGE, lobbyUri, recipients);
        event.putData("messageAuthor", author);
        event.putData("messageContent", messageContent);

        return event;
    }

    public static LobbyEvent memberJoin(List<String> recipients, String lobbyUri, UserDTO memberThatJoined) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.MEMBER_JOIN, lobbyUri, recipients);
        event.putData("memberData", memberThatJoined);

        return event;
    }

    public static LobbyEvent receiveLobbyData(String recipient, String lobbyUri, LobbyDTO lobbyData) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.RECEIVE_LOBBY_DATA, lobbyUri, recipient);
        event.putData("lobbyData", lobbyData);

        return event;
    }

    public static LobbyEvent ownerUpdated(List<String> recipients, String lobbyUri, String newOwnerUuid) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.OWNER_UPDATED, lobbyUri, recipients);
        event.putData("newOwnerUuid", newOwnerUuid);

        return event;
    }

    public static LobbyEvent memberLeave(List<String> recipients, String lobbyUri, String userThatLeftUuid) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.MEMBER_LEAVE, lobbyUri, recipients);
        event.putData("memberUuid", userThatLeftUuid);

        return event;
    }

    public static LobbyEvent memberKick(List<String> recipients, String lobbyUri, String memberUuid) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.MEMBER_KICK, lobbyUri, recipients);
        event.putData("memberUuid", memberUuid);

        return event;
    }

    public static LobbyEvent gotKicked(String recipient, String lobbyUri) {
        return new LobbyEvent(ID.GOT_KICKED, lobbyUri, recipient);
    }

    public static LobbyEvent createdGame(List<String> recipients, String lobbyUri, GameDTO gameData) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.CREATED_GAME, lobbyUri, recipients);
        event.putData("gameData", gameData);

        return event;
    }

    public static LobbyEvent settingsUpdated(List<String> recipients, String lobbyUri, String settingName, String settingValue) {
        LobbyEvent event = new LobbyEvent(LobbyEvent.ID.SETTINGS_UPDATED, lobbyUri, recipients);
        event.putData("settingName", settingName);
        event.putData("settingValue", settingValue);

        return event;
    }
}
