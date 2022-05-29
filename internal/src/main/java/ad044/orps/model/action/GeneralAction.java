package ad044.orps.model.action;

public class GeneralAction extends Action {
    public enum ID {
        CREATE_LOBBY,
        USER_DISCONNECT,
        CHANGE_NAME;
    }
    private final ID id;

    public GeneralAction(Action action, ID id) {
        super(action.getIdString(), action.getCategory(), action.getData(), action.getAuthor());

        this.id = id;
    }

    public ID getId() {
        return id;
    }
}
