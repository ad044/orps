package ad044.orps.model.lobby;

import ad044.orps.model.game.GameSettings;

public final class LobbySettings extends GameSettings {
    private boolean inviteOnly;

    public LobbySettings(int timeForMove, int scoreGoal, boolean inviteOnly) {
        super(timeForMove, scoreGoal);
        this.inviteOnly = inviteOnly;
    }

    public boolean isInviteOnly() {
        return inviteOnly;
    }

    public void setInviteOnly(boolean inviteOnly) {
        this.inviteOnly = inviteOnly;
    }
}
