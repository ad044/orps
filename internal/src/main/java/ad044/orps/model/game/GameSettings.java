package ad044.orps.model.game;

public class GameSettings {
    private int timeForMove;
    private int scoreGoal;

    public GameSettings(int timeForMove, int scoreGoal) {
        this.timeForMove = timeForMove;
        this.scoreGoal = scoreGoal;
    }

    public int getScoreGoal() {
        return scoreGoal;
    }

    public int getTimeForMove() {
        return timeForMove;
    }

    public void setScoreGoal(int scoreGoal) {
        this.scoreGoal = scoreGoal;
    }

    public void setTimeForMove(int timeForMove) {
        this.timeForMove = timeForMove;
    }
}
