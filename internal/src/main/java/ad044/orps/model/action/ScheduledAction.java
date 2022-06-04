package ad044.orps.model.action;

import org.jetbrains.annotations.NotNull;

public class ScheduledAction implements Comparable<ScheduledAction> {
    private final Action action;
    private final long executionTime;

    public ScheduledAction(Action action, long executionTime) {
        this.action = action;
        this.executionTime = executionTime;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public int compareTo(@NotNull ScheduledAction scheduledAction) {
        return Long.compare(executionTime, scheduledAction.executionTime);
    }
}
