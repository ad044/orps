package ad044.orps.dto;

import ad044.orps.model.game.GameMove;
import ad044.orps.model.game.Player;

public class PlayerDTO extends UserDTO {
    private final int score;
    private final GameMove move;

    public PlayerDTO(String username, String uuid, int score, GameMove move) {
        super(username, uuid);
        this.score = score;
        this.move = move;
    }

    public static PlayerDTO from(Player player) {
        return new PlayerDTO(player.getUsername(), player.getUuid(), player.score, player.move);
    }

    public int getScore() {
        return score;
    }

    public GameMove getMove() {
        return move;
    }
}
