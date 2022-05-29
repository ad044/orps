package ad044.orps.model.event;

import ad044.orps.dto.LobbyDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GeneralEvent extends Event<GeneralEvent.ID>{
    public enum ID {
        CREATED_LOBBY,
        USER_CHANGED_NAME
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GeneralEvent(@JsonProperty("id") GeneralEvent.ID id) {
        super(id);
    }

    public static GeneralEvent createdLobby(LobbyDTO lobbyData) {
        GeneralEvent event = new GeneralEvent(GeneralEvent.ID.CREATED_LOBBY);
        event.putData("lobbyData", lobbyData);

        return event;
    }

    public static GeneralEvent userChangedName(String uuid, String newName) {
        GeneralEvent event = new GeneralEvent(GeneralEvent.ID.USER_CHANGED_NAME);
        event.putData("userUuid", uuid);
        event.putData("newName", newName);

        return event;
    }
}
