package ad044.orps.model;

import ad044.orps.model.action.ScheduledAction;
import ad044.orps.model.event.Event;

import java.util.Collections;
import java.util.List;

public class ActionHandlerResponse {
    private final List<Event<?>> events;
    private final List<ScheduledAction> scheduledActions;

    public ActionHandlerResponse(List<Event<?>> events, ScheduledAction scheduledAction) {
        this.events = events;
        this.scheduledActions = Collections.singletonList(scheduledAction);
    }

    public ActionHandlerResponse(List<Event<?>> events, List<ScheduledAction> scheduledActions) {
        this.events = events;
        this.scheduledActions = scheduledActions;
    }

    public ActionHandlerResponse(Event<?> events, ScheduledAction scheduledAction) {
        this(Collections.singletonList(events), Collections.singletonList(scheduledAction));
    }

    public ActionHandlerResponse(Event<?> events) {
        this(Collections.singletonList(events), Collections.emptyList());
    }

    public ActionHandlerResponse(List<Event<?>> events) {
        this(events, Collections.emptyList());
    }

    public static ActionHandlerResponse empty() {
        return new ActionHandlerResponse(Collections.emptyList(), Collections.emptyList());
    }

    public List<Event<?>> getEvents() {
        return events;
    }

    public List<ScheduledAction> getScheduledActions() {
        return scheduledActions;
    }
}
