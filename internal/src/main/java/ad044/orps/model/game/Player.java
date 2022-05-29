package ad044.orps.model.game;

import ad044.orps.model.user.OrpsUserDetails;

public class Player extends OrpsUserDetails {
    public int score;
    public GameMove move;
    // Keeps track of how many rounds the player didn't make a move
    // This is used to automatically kick a player if they've been idle for "too long" (3 rounds for example)
    public int missedMoveCount;

    public Player(OrpsUserDetails userDetails) {
        super(userDetails.getUsername(), userDetails.getUuid());
        this.score = 0;
        this.move = GameMove.NO_MOVE;
    }
}
