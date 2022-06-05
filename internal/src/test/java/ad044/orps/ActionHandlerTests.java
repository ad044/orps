package ad044.orps;

import ad044.orps.model.Category;
import ad044.orps.model.action.Action;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.Event;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.ActionDispatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ActionHandlerTests {
    @Autowired
    ActionDispatcherService actionDispatcherService;

    OrpsUserDetails author;

    @BeforeEach
    public void reinitialize() {
        this.author = new OrpsUserDetails("user1", "uuid1");
    }

    @Test
    public void generalAction_failsWhenInvalidAction() {
        Action action = new Action("123", Category.GENERAL, Collections.emptyMap(), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.INVALID_ACTION);
        assertEquals(event.getData().get("category"), "GENERAL");
        assertEquals(event.getData().get("action"), "123");
    }

    @Test
    public void lobbyAction_failsWhenInvalidAction() {
        Action action = new Action("123", Category.LOBBY, Map.of("lobbyUri", "test"), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.INVALID_ACTION);
        assertEquals(event.getData().get("category"), "LOBBY");
        assertEquals(event.getData().get("action"), "123");
    }

    @Test
    public void gameAction_failsWhenInvalidAction() {
        Action action = new Action("123", Category.GAME, Map.of("gameUri", "test"), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.INVALID_ACTION);
        assertEquals(event.getData().get("category"), "GAME");
        assertEquals(event.getData().get("action"), "123");
    }

    @Test
    public void gameAction_failsWhenNoGameUri() {
        Action action = new Action("SUBMIT_MOVE", Category.GAME, Collections.emptyMap(), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.DATA_FIELD_MISSING);
        assertEquals(event.getData().get("fieldName"), "gameUri");
    }

    @Test
    public void lobbyAction_failsWhenNoLobbyUri() {
        Action action = new Action("ADD_BOT", Category.LOBBY, Collections.emptyMap(), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.DATA_FIELD_MISSING);
        assertEquals(event.getData().get("fieldName"), "lobbyUri");
    }
}
