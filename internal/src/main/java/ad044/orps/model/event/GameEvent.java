package ad044.orps.model.event;

import ad044.orps.dto.PlayerDTO;
import ad044.orps.model.game.GameMove;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GameEvent extends Event<GameEvent.ID> {
    public enum ID {
        START_NEXT_ROUND,
        RECEIVE_ROUND_RESULT,
        PLAYER_WON_GAME,
        COUNTDOWN_UPDATE,
        DISPLAY_AUTHOR_MOVE,
        PLAYER_MADE_MOVE,
        PLAYER_LEAVE,
        GOT_KICKED
    }
    private final String gameUri;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GameEvent(@JsonProperty("id") ID id, @JsonProperty("gameUri") String gameUri) {
        super(id);
        this.gameUri = gameUri;
    }

    public String getGameUri() {
        return gameUri;
    }

    public static GameEvent startNextRound(String gameUri, int roundNumber, int timeForMove) {
        GameEvent event = new GameEvent(GameEvent.ID.START_NEXT_ROUND, gameUri);
        event.putData("roundNumber", roundNumber);
        event.putData("timeToPick", timeForMove);

        return event;
    }

    public static GameEvent receiveRoundResult(String gameUri, List<PlayerDTO> playerData, PlayerDTO winner) {
        GameEvent event = new GameEvent(GameEvent.ID.RECEIVE_ROUND_RESULT, gameUri);
        event.putData("playerData", playerData);
        event.putData("winner", winner);

        return event;
    }

    public static GameEvent receiveRoundResult(String gameUri, List<PlayerDTO> playerData) {
        GameEvent event = new GameEvent(GameEvent.ID.RECEIVE_ROUND_RESULT, gameUri);
        event.putData("playerData", playerData);
        event.putData("winner", null);

        return event;
    }

    public static GameEvent playerWonGame(String gameUri, PlayerDTO winner) {
        GameEvent event = new GameEvent(GameEvent.ID.PLAYER_WON_GAME, gameUri);
        event.putData("gameWinner", winner);

        return event;
    }

    public static GameEvent countdownUpdate(String gameUri, int timerValue) {
        GameEvent event = new GameEvent(GameEvent.ID.COUNTDOWN_UPDATE, gameUri);
        event.putData("currentTimerValue", timerValue);

        return event;
    }

    public static GameEvent displayAuthorMove(String gameUri, String authorUuid, GameMove move) {
        GameEvent event = new GameEvent(GameEvent.ID.DISPLAY_AUTHOR_MOVE, gameUri);
        event.putData("authorUuid", authorUuid);
        event.putData("move", move);

        return event;
    }

    public static GameEvent playerMadeMove(String gameUri, String playerUuid) {
        GameEvent event = new GameEvent(GameEvent.ID.PLAYER_MADE_MOVE, gameUri);
        event.putData("playerUuid", playerUuid);

        return event;
    }

    public static GameEvent playerLeave(String gameUri, String playerUuid) {
        GameEvent event = new GameEvent(GameEvent.ID.PLAYER_LEAVE, gameUri);
        event.putData("playerUuid", playerUuid);

        return event;
    }

    public static GameEvent gotKicked(String gameUri, String parentLobbyUri) {
        GameEvent event = new GameEvent(ID.GOT_KICKED, gameUri);
        event.putData("parentLobbyUri", parentLobbyUri);

        return event;
    }

    public static GameEvent gotKicked(String gameUri) {
        return new GameEvent(ID.GOT_KICKED, gameUri);
    }
}
