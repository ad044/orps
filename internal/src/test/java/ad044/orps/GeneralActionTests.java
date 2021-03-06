package ad044.orps;

import ad044.orps.dto.LobbyDTO;
import ad044.orps.model.Category;
import ad044.orps.model.action.Action;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.Event;
import ad044.orps.model.event.GeneralEvent;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.ActionDispatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GeneralActionTests {
    @Autowired
    ActionDispatcherService actionDispatcherService;
    OrpsUserDetails author;

    @BeforeEach
    public void reinitialize() {
        this.author = new OrpsUserDetails("user1", "uuid1");
    }

    @Test
    public void createsLobby() {
        Action action = new Action("CREATE_LOBBY", Category.GENERAL, Collections.emptyMap(), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.GENERAL);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), GeneralEvent.ID.CREATED_LOBBY);
        assertTrue(event.getData().get("lobbyData") instanceof LobbyDTO);
    }

    @Test
    public void changesName() {
        Map<String, String> data = Map.of("newName", "testname");
        Action action = new Action("CHANGE_NAME", Category.GENERAL, data, author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.GENERAL);
        assertEquals(event.getRecipientUuids().stream().distinct().collect(Collectors.toList()), List.of(author.getUuid()));
        assertEquals(event.getId(), GeneralEvent.ID.USER_CHANGED_NAME);
        assertEquals(event.getData().get("userUuid"), author.getUuid());
        assertEquals(event.getData().get("newName"), "testname");

        assertEquals(author.getUsername(), "testname");
    }

    @Test
    public void changesName_failsWhenNoNameProvided() {
        Action action = new Action("CHANGE_NAME", Category.GENERAL, Collections.emptyMap(), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.DATA_FIELD_MISSING);
        assertEquals(event.getData().get("fieldName"), "newName");
    }

    @Test
    public void changesName_failsWhenNotAlphaNumeric() {
        Action action = new Action("CHANGE_NAME", Category.GENERAL, Map.of("newName", "::::"), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.NAME_NOT_ACCEPTED);
        assertEquals(event.getData().get("triedName"), "::::");
        assertEquals(event.getData().get("reason"), "Name must be alphanumeric.");
    }

    @Test
    public void changesName_failsWhenTooLong() {
        String tooLongName = "21345235632623623623623623623";
        Action action = new Action("CHANGE_NAME", Category.GENERAL, Map.of("newName", tooLongName), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.NAME_NOT_ACCEPTED);
        assertEquals(event.getData().get("triedName"), tooLongName);
        assertEquals(event.getData().get("reason"), "Name length must be >= 3 and <= 16");
    }

    @Test
    public void changesName_failsWhenTooShort() {
        String tooShortName = "12";
        Action action = new Action("CHANGE_NAME", Category.GENERAL, Map.of("newName", tooShortName), author);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(author.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.NAME_NOT_ACCEPTED);
        assertEquals(event.getData().get("triedName"), tooShortName);
        assertEquals(event.getData().get("reason"), "Name length must be >= 3 and <= 16");
    }
}
