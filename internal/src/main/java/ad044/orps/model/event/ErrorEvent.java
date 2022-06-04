package ad044.orps.model.event;

import ad044.orps.model.Category;

import java.util.Collections;
import java.util.List;

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

    public ErrorEvent(ID id, List<String> recipientUuids) {
        super(id, Category.ERROR, recipientUuids);
    }

    public ErrorEvent(ID id, String recipientUuid) {
        super(id, Category.ERROR, Collections.singletonList(recipientUuid));
    }

    public static ErrorEvent dataFieldMissing(String recipient, String fieldName) {
        ErrorEvent errorEvent = new ErrorEvent(ID.DATA_FIELD_MISSING, recipient);
        errorEvent.putData("fieldName", fieldName);

        return errorEvent;
    }

    public static ErrorEvent invalidFieldDataType(String recipient, String fieldName, String expectedType) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INVALID_FIELD_DATA_TYPE, recipient);
        errorEvent.putData("fieldName", fieldName);
        errorEvent.putData("expectedType", expectedType);

        return errorEvent;
    }

    public static ErrorEvent playerNotInGame(String recipient, String playerUuid, String gameUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.PLAYER_NOT_IN_GAME, recipient);
        errorEvent.putData("playerUuid", playerUuid);
        errorEvent.putData("gameUri", gameUri);

        return errorEvent;
    }

    public static ErrorEvent userNotInLobby(String recipient, String userUuid, String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.USER_NOT_IN_LOBBY, recipient);
        errorEvent.putData("userUuid", userUuid);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent invalidAction(String recipient, String category, String action) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INVALID_ACTION, recipient);
        errorEvent.putData("category", category);
        errorEvent.putData("action", action);

        return errorEvent;
    }

    public static ErrorEvent lobbyNotFound(String recipient, String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.LOBBY_NOT_FOUND, recipient);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent gameNotFound(String recipient, String gameUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.GAME_NOT_FOUND, recipient);
        errorEvent.putData("gameUri", gameUri);

        return errorEvent;
    }

    public static ErrorEvent nameNotAccepted(String recipient, String triedName, String reason) {
        ErrorEvent errorEvent = new ErrorEvent(ID.NAME_NOT_ACCEPTED, recipient);
        errorEvent.putData("triedName", triedName);
        errorEvent.putData("reason", reason);

        return errorEvent;
    }

    public static ErrorEvent insufficientPlayers(String recipient, String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INSUFFICIENT_PLAYERS, recipient);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent gameAlreadyStarted(String recipient, String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.LOBBY_GAME_ALREADY_STARTED, recipient);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent badTextMessage(String recipient, String lobbyUri, String reason) {
        ErrorEvent errorEvent = new ErrorEvent(ID.BAD_TEXT_MESSAGE, recipient);
        errorEvent.putData("lobbyUri", lobbyUri);
        errorEvent.putData("reason", reason);

        return errorEvent;
    }

    public static ErrorEvent roundAlreadyFinished(String recipient, String gameUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.ROUND_ALREADY_FINISHED, recipient);
        errorEvent.putData("gameUri", gameUri);

        return errorEvent;
    }

    public static ErrorEvent insufficientPermissions(String recipient, String lobbyUri) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INSUFFICIENT_PERMISSIONS, recipient);
        errorEvent.putData("lobbyUri", lobbyUri);

        return errorEvent;
    }

    public static ErrorEvent somethingWentWrong(String recipient) {
        return new ErrorEvent(ID.SOMETHING_WENT_WRONG, recipient);
    }

    public static ErrorEvent lobbyParameterNotAllowed(String recipient, String lobbyUri, String message) {
        ErrorEvent errorEvent = new ErrorEvent(ID.LOBBY_PARAMETER_VALUE_NOT_ALLOWED, recipient);
        errorEvent.putData("lobbyUri", lobbyUri);
        errorEvent.putData("message", message);

        return errorEvent;
    }

    public static ErrorEvent invalidMove(String recipient, String gameUri, String moveName) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INVALID_MOVE, recipient);
        errorEvent.putData("gameUri", gameUri);
        errorEvent.putData("move", moveName);

        return errorEvent;
    }

    public static ErrorEvent invalidSettingName(String recipient, String lobbyUri, String settingName) {
        ErrorEvent errorEvent = new ErrorEvent(ID.INVALID_SETTING_NAME, recipient);
        errorEvent.putData("lobbyUri", lobbyUri);
        errorEvent.putData("settingName", settingName);

        return errorEvent;
    }
}
