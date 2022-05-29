package ad044.orps.model.action;

public class GameAction extends Action {
    public enum ID {
        SUBMIT_MOVE,
        PLAYER_LEAVE,
    }
    private final ID id;
    private final String targetGameUri;

    public GameAction(Action action, ID id, String targetGameUri) {
        super(action.getIdString(), action.getCategory(), action.getData(), action.getAuthor());

        this.id = id;
        this.targetGameUri = targetGameUri;
    }

    public ID getId() {
        return id;
    }

    public String getTargetGameUri() {
        return targetGameUri;
    }
}
