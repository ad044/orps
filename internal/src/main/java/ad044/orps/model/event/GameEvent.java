package ad044.orps.model.event;

import ad044.orps.dto.PlayerDTO;
import ad044.orps.model.Category;
import ad044.orps.model.game.GameMove;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class GameEvent extends Event<GameEvent.ID> {
    public enum ID {
        START_NEXT_ROUND,
        RECEIVE_ROUND_RESULT,
        PLAYER_WON_GAME,
        UPDATE_COUNTDOWN,
        DISPLAY_AUTHOR_MOVE,
        PLAYER_MADE_MOVE,
        PLAYER_LEAVE,
        GOT_KICKED,
        ENDED_PREMATURELY
    }
    private final String gameUri;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GameEvent(@JsonProperty("id") ID id, @JsonProperty("gameUri") String gameUri,
                     @JsonProperty("recipientUuids") List<String> recipientUuids) {
        super(id, Category.GAME, recipientUuids);
        this.gameUri = gameUri;
    }

    public GameEvent(ID id, String gameUri, String recipientUuid) {
        super(id, Category.GAME, Collections.singletonList(recipientUuid));
        this.gameUri = gameUri;
    }

    public String getGameUri() {
        return gameUri;
    }

    public static GameEvent startNextRound(List<String> recipients, String gameUri, int roundNumber, int timeForMove) {
        GameEvent event = new GameEvent(GameEvent.ID.START_NEXT_ROUND, gameUri, recipients);
        event.putData("roundNumber", roundNumber);
        event.putData("timeToPick", timeForMove);

        return event;
    }

    public static GameEvent receiveRoundResult(List<String> recipients, String gameUri, List<PlayerDTO> playerData, PlayerDTO winner) {
        GameEvent event = new GameEvent(GameEvent.ID.RECEIVE_ROUND_RESULT, gameUri, recipients);
        event.putData("playerData", playerData);
        event.putData("winner", winner);

        return event;
    }

    public static GameEvent receiveRoundResult(List<String> recipients, String gameUri, List<PlayerDTO> playerData) {
        GameEvent event = new GameEvent(GameEvent.ID.RECEIVE_ROUND_RESULT, gameUri, recipients);
        event.putData("playerData", playerData);
        event.putData("winner", null);

        return event;
    }

    public static GameEvent playerWonGame(List<String> recipients, String gameUri, PlayerDTO winner) {
        GameEvent event = new GameEvent(GameEvent.ID.PLAYER_WON_GAME, gameUri, recipients);
        event.putData("gameWinner", winner);

        return event;
    }

    public static GameEvent countdownUpdate(List<String> recipients, String gameUri, int timerValue) {
        GameEvent event = new GameEvent(GameEvent.ID.UPDATE_COUNTDOWN, gameUri, recipients);
        event.putData("currentTimerValue", timerValue);

        return event;
    }

    public static GameEvent displayAuthorMove(String recipient, String gameUri, String authorUuid, GameMove move) {
        GameEvent event = new GameEvent(GameEvent.ID.DISPLAY_AUTHOR_MOVE, gameUri, recipient);
        event.putData("authorUuid", authorUuid);
        event.putData("move", move);

        return event;
    }

    public static GameEvent playerMadeMove(List<String> recipients, String gameUri, String playerUuid) {
        GameEvent event = new GameEvent(GameEvent.ID.PLAYER_MADE_MOVE, gameUri, recipients);
        event.putData("playerUuid", playerUuid);

        return event;
    }

    public static GameEvent playerLeave(List<String> recipients, String gameUri, String playerUuid) {
        GameEvent event = new GameEvent(GameEvent.ID.PLAYER_LEAVE, gameUri, recipients);
        event.putData("playerUuid", playerUuid);

        return event;
    }

    public static GameEvent gotKicked(String recipient, String gameUri, String parentLobbyUri) {
        GameEvent event = new GameEvent(ID.GOT_KICKED, gameUri, recipient);
        event.putData("parentLobbyUri", parentLobbyUri);

        return event;
    }

    public static GameEvent gotKicked(String recipient, String gameUri) {
        return new GameEvent(ID.GOT_KICKED, gameUri, recipient);
    }

    public static GameEvent endedPrematurely(List<String> recipients, String gameUri, String reason) {
        GameEvent event = new GameEvent(ID.ENDED_PREMATURELY, gameUri, recipients);
        event.putData("reason", reason);

        return event;
    }
}
