package ad044.orps.model.action;

import ad044.orps.model.game.Game;
import ad044.orps.model.game.Player;

public class GameAction extends Action {
    public enum ID {
        SUBMIT_MOVE,
        UPDATE_COUNTDOWN,
        FINISH_ROUND,
        START_NEXT_ROUND,
        PLAYER_LEAVE,
    }
    private final ID id;
    private final Game targetGame;
    private final Player authorPlayer;

    public GameAction(Action action, ID id, Game targetGame, Player authorPlayer) {
        super(action.getIdString(), action.getCategory(), action.getData(), action.getAuthor());

        this.id = id;
        this.targetGame = targetGame;
        this.authorPlayer = authorPlayer;
    }

    public ID getId() {
        return id;
    }

    public Game getTargetGame() {
        return targetGame;
    }

    public Player getAuthorPlayer() {
        return authorPlayer;
    }
}
