package ad044.orps.model.event;

import ad044.orps.dto.LobbyDTO;
import ad044.orps.model.Category;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class GeneralEvent extends Event<GeneralEvent.ID>{
    public enum ID {
        CREATED_LOBBY,
        USER_CHANGED_NAME
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GeneralEvent(@JsonProperty("id") GeneralEvent.ID id, @JsonProperty("recipientUuids") List<String> recipientUuids) {
        super(id, Category.GENERAL, recipientUuids);
    }

    public GeneralEvent(@JsonProperty("id") GeneralEvent.ID id, String recipientUuid) {
        super(id, Category.GENERAL, Collections.singletonList(recipientUuid));
    }

    public static GeneralEvent createdLobby(String recipient, LobbyDTO lobbyData) {
        GeneralEvent event = new GeneralEvent(GeneralEvent.ID.CREATED_LOBBY, recipient);
        event.putData("lobbyData", lobbyData);

        return event;
    }

    public static GeneralEvent userChangedName(List<String> recipients, String uuid, String newName) {
        GeneralEvent event = new GeneralEvent(GeneralEvent.ID.USER_CHANGED_NAME, recipients);
        event.putData("userUuid", uuid);
        event.putData("newName", newName);

        return event;
    }
}
