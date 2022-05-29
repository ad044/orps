package ad044.orps.model.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorEvent extends Event<ErrorEvent.ID> {
    public enum ID {
        LOBBY_GAME_ALREADY_STARTED,
        ROUND_ALREADY_FINISHED,
        INSUFFICIENT_PLAYERS,
        INVALID_FIELD_DATA_TYPE,
        DATA_FIELD_MISSING,
        PLAYER_NOT_IN_GAME,
        USER_NOT_IN_LOBBY,
        NAME_NOT_ACCEPTED,
        INVALID_ACTION,
        LOBBY_NOT_FOUND,
        GAME_NOT_FOUND,
        BAD_TEXT_MESSAGE,
        INSUFFICIENT_PERMISSIONS,
        SOMETHING_WENT_WRONG,
        LOBBY_PARAMETER_VALUE_NOT_ALLOWED,
        INVALID_MOVE,
        INVALID_SETTING_NAME
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ErrorEvent(@JsonProperty("id") ID id) {
        super(id);
    }

    public static ErrorEvent dataFieldMissing(String fieldName) {
        ErrorEvent errorEvent = new ErrorEvent(ID.DATA_FIELD_MISSING);
        errorEvent.putData("fieldName", fieldName);

        return errorEvent;
    }

    public static ErrorEvent invalidFieldDataType(String fieldName, String expectedType) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INVALID_FIELD_DATA_TYPE);
        errorEvent.putData("fieldName", fieldName);
        errorEvent.putData("expectedType", expectedType);

        return errorEvent;
    }

    public static ErrorEvent playerNotInGame(String playerUuid, String gameUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.PLAYER_NOT_IN_GAME);
        errorEvent.putData("playerUuid", playerUuid);
        errorEvent.putData("gameUri", gameUri);

        return errorEvent;
    }

    public static ErrorEvent userNotInLobby(String userUuid, String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.USER_NOT_IN_LOBBY);
        errorEvent.putData("userUuid", userUuid);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent invalidAction(String category, String action) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INVALID_ACTION);
        errorEvent.putData("category", category);
        errorEvent.putData("action", action);

        return errorEvent;
    }

    public static ErrorEvent lobbyNotFound(String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.LOBBY_NOT_FOUND);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent gameNotFound(String gameUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.GAME_NOT_FOUND);
        errorEvent.putData("gameUri", gameUri);

        return errorEvent;
    }

    public static ErrorEvent nameNotAccepted(String triedName, String reason) {
        ErrorEvent errorEvent = new ErrorEvent(ID.NAME_NOT_ACCEPTED);
        errorEvent.putData("triedName", triedName);
        errorEvent.putData("reason", reason);

        return errorEvent;
    }

    public static ErrorEvent insufficientPlayers(String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INSUFFICIENT_PLAYERS);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent gameAlreadyStarted(String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.LOBBY_GAME_ALREADY_STARTED);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent badTextMessage(String lobbyUri, String reason) {
        ErrorEvent errorEvent = new ErrorEvent(ID.BAD_TEXT_MESSAGE);
        errorEvent.putData("lobbyUri", lobbyUri);
        errorEvent.putData("reason", reason);

        return errorEvent;
    }

    public static ErrorEvent roundAlreadyFinished(String gameUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.ROUND_ALREADY_FINISHED);
        errorEvent.putData("gameUri", gameUri);

        return errorEvent;
    }

    public static ErrorEvent insufficientPermissions(String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INSUFFICIENT_PERMISSIONS);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent somethingWentWrong() {
        return new ErrorEvent(ID.SOMETHING_WENT_WRONG);
    }

    public static ErrorEvent lobbyParameterNotAllowed(String lobbyUri, String message) {
        ErrorEvent errorEvent = new ErrorEvent(ID.LOBBY_PARAMETER_VALUE_NOT_ALLOWED);
        errorEvent.putData("lobbyUri", lobbyUri);
        errorEvent.putData("message", message);

        return errorEvent;
    }

    public static ErrorEvent invalidMove(String gameUri, String moveName) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INVALID_MOVE);
        errorEvent.putData("gameUri", gameUri);
        errorEvent.putData("move", moveName);

        return errorEvent;
    }

    public static ErrorEvent invalidSettingName(String lobbyUri, String settingName) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INVALID_SETTING_NAME);
        errorEvent.putData("lobbyUri", lobbyUri);
        errorEvent.putData("settingName", settingName);

        return errorEvent;
    }
}
