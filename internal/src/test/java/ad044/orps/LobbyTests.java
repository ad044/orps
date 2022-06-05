package ad044.orps;

import ad044.orps.dto.GameDTO;
import ad044.orps.dto.LobbyDTO;
import ad044.orps.dto.UserDTO;
import ad044.orps.model.ActionHandlerResponse;
import ad044.orps.model.Category;
import ad044.orps.model.action.Action;
import ad044.orps.model.action.ScheduledAction;
import ad044.orps.model.action.ServerAction;
import ad044.orps.model.event.Event;
import ad044.orps.model.event.LobbyEvent;
import ad044.orps.model.lobby.Lobby;
import ad044.orps.model.event.Event;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.ActionDispatcherService;
import ad044.orps.service.LobbyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LobbyTests {
    Logger logger = LoggerFactory.getLogger(LobbyTests.class);

    private Lobby lobby;
    private OrpsUserDetails lobbyOwner;

    @Autowired
    ActionDispatcherService actionDispatcherService;

    @Autowired
    LobbyService lobbyService;

    @BeforeEach
    public void reinitialize() {
        this.lobbyOwner = new OrpsUserDetails("user1", "uuid1");
        this.lobby = lobbyService.createLobby(lobbyOwner);
    }

    @Test
    public void action_failsWhenLobbyDoesntExist() {
        Action action = new Action("ADD_BOT", Category.LOBBY, Map.of("lobbyUri", "test"), lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("lobbyUri"), "test");
    }

    @Test
    public void addsBot() {
        Action action = new Action("ADD_BOT", Category.LOBBY, Map.of("lobbyUri", lobby.getUri()), lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 1);

        LobbyEvent event = (LobbyEvent) events.get(0);
        assertEquals(event.getCategory(), Category.LOBBY);
        assertEquals(event.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));
        assertEquals(event.getId(), LobbyEvent.ID.MEMBER_JOIN);
        assertTrue(((UserDTO) event.getData().get("memberData")).getUsername().startsWith("Bot"));
        assertTrue(((UserDTO) event.getData().get("memberData")).getUuid().startsWith("Bot"));
        assertEquals(event.getLobbyUri(), lobby.getUri());
    }

    @Test
    public void addsBot_failsWhenNotOwner() {
        OrpsUserDetails nonOwnerUser = new OrpsUserDetails("nonowner", "nonowneruuid");
        Action action = new Action("ADD_BOT", Category.LOBBY, Map.of("lobbyUri", lobby.getUri()), nonOwnerUser);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(nonOwnerUser.getUuid()));

        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void receivesNewTextMessageAsOwner() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "messageContent", "test");
        Action action = new Action("NEW_TEXT_MESSAGE", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 1);

        LobbyEvent event = (LobbyEvent) events.get(0);
        assertEquals(event.getCategory(), Category.LOBBY);
        assertEquals(event.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));
        assertEquals(event.getId(), LobbyEvent.ID.NEW_TEXT_MESSAGE);
        assertEquals(event.getData().get("messageContent"), "test");
        assertEquals(((UserDTO) event.getData().get("messageAuthor")).getUuid(), lobbyOwner.getUuid());
        assertEquals(event.getLobbyUri(), lobby.getUri());
    }

    @Test
    public void receivesNewTextMessageAsNonOwner() {
        OrpsUserDetails nonOwnerUser = new OrpsUserDetails("nonowner", "nonowneruuid");
        lobby.addMember(nonOwnerUser);
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "messageContent", "test");
        Action action = new Action("NEW_TEXT_MESSAGE", Category.LOBBY, data, nonOwnerUser);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 1);

        LobbyEvent event = (LobbyEvent) events.get(0);
        assertEquals(event.getCategory(), Category.LOBBY);
        assertEquals(event.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));
        assertEquals(event.getId(), LobbyEvent.ID.NEW_TEXT_MESSAGE);
        assertEquals(event.getData().get("messageContent"), "test");
        assertEquals(((UserDTO) event.getData().get("messageAuthor")).getUuid(), nonOwnerUser.getUuid());
        assertEquals(event.getLobbyUri(), lobby.getUri());
    }

    @Test
    public void receivesNewTextMessage_failsWhenNotInLobby() {
        OrpsUserDetails nonOwnerUser = new OrpsUserDetails("nonowner", "nonowneruuid");
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "messageContent", "test");
        Action action = new Action("NEW_TEXT_MESSAGE", Category.LOBBY, data, nonOwnerUser);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(nonOwnerUser.getUuid()));

        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void receivesNewTextMessage_failsWhenNoMessageContent() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("NEW_TEXT_MESSAGE", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("fieldName"), "messageContent");
    }

    @Test
    public void receivesNewTextMessage_failsWhenMessageEmpty() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "messageContent", "");
        Action action = new Action("NEW_TEXT_MESSAGE", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("reason"), "Message can't be empty.");
        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void userJoins() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("USER_JOIN", Category.LOBBY, data, newUser);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 2);

        LobbyEvent joinEvent = (LobbyEvent) events.get(0);
        assertEquals(joinEvent.getCategory(), Category.LOBBY);
        assertEquals(joinEvent.getRecipientUuids(), lobby.getMembers()
                .stream()
                .map(OrpsUserDetails::getUuid)
                .filter(uuid -> !uuid.equals("randomuuid"))
                .collect(Collectors.toList()));
        assertEquals(joinEvent.getId(), LobbyEvent.ID.MEMBER_JOIN);
        assertEquals(joinEvent.getLobbyUri(), lobby.getUri());
        assertEquals(((UserDTO) (joinEvent.getData().get("memberData"))).getUuid(), newUser.getUuid());

        LobbyEvent receiveLobbyDataEvent = (LobbyEvent) events.get(1);
        assertEquals(receiveLobbyDataEvent.getCategory(), Category.LOBBY);
        assertEquals(receiveLobbyDataEvent.getRecipientUuids(), Collections.singletonList(newUser.getUuid()));
        assertEquals(receiveLobbyDataEvent.getId(), LobbyEvent.ID.RECEIVE_LOBBY_DATA);
        LobbyDTO lobbyData = (LobbyDTO) receiveLobbyDataEvent.getData().get("lobbyData");
        assertEquals(lobbyData.getUri(), lobby.getUri());

        assertEquals(lobby.getMembers().size(), 2);
    }

    @Test
    public void userLeavesWhileNoOtherPlayersInLobby() {
        assertEquals(lobby.getMembers().size(), 1);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("USER_LEAVE", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(events.size(), 0);
    }

    @Test
    public void userLeavesAndOwnerGetsUpdated() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        lobby.addMember(newUser);

        assertEquals(lobby.getMembers().size(), 2);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("USER_LEAVE", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 2);

        LobbyEvent ownerUpdatedEvent = (LobbyEvent) events.get(0);
        assertEquals(ownerUpdatedEvent.getCategory(), Category.LOBBY);
        assertEquals(ownerUpdatedEvent.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));
        assertEquals(ownerUpdatedEvent.getId(), LobbyEvent.ID.OWNER_UPDATED);
        assertEquals(ownerUpdatedEvent.getLobbyUri(), lobby.getUri());
        assertEquals(ownerUpdatedEvent.getData().get("newOwnerUuid"), newUser.getUuid());

        LobbyEvent memberLeaveEvent = (LobbyEvent) events.get(1);
        assertEquals(memberLeaveEvent.getCategory(), Category.LOBBY);
        assertEquals(memberLeaveEvent.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));
        assertEquals(memberLeaveEvent.getId(), LobbyEvent.ID.MEMBER_LEAVE);
        assertEquals(memberLeaveEvent.getLobbyUri(), lobby.getUri());
        assertEquals(memberLeaveEvent.getData().get("memberUuid"), lobbyOwner.getUuid());

        assertEquals(lobby.getMembers().size(), 1);
    }

    @Test
    public void kicksMember() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        lobby.addMember(newUser);

        assertEquals(lobby.getMembers().size(), 2);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "memberToKickUuid", "randomuuid");
        Action action = new Action("MEMBER_KICK", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 2);

        LobbyEvent memberKickEvent = (LobbyEvent) events.get(0);
        assertEquals(memberKickEvent.getCategory(), Category.LOBBY);
        assertEquals(memberKickEvent.getRecipientUuids(), lobby.getMembers().stream()
                .map(OrpsUserDetails::getUuid)
                .filter(uuid -> !uuid.equals("randomuuid"))
                .collect(Collectors.toList()));
        assertEquals(memberKickEvent.getId(), LobbyEvent.ID.MEMBER_KICK);
        assertEquals(memberKickEvent.getLobbyUri(), lobby.getUri());
        assertEquals(memberKickEvent.getData().get("memberUuid"), "randomuuid");

        LobbyEvent gotKickedEvent = (LobbyEvent) events.get(1);
        assertEquals(gotKickedEvent.getCategory(), Category.LOBBY);
        assertEquals(gotKickedEvent.getRecipientUuids(), Collections.singletonList("randomuuid"));
        assertEquals(gotKickedEvent.getId(), LobbyEvent.ID.GOT_KICKED);
        assertEquals(gotKickedEvent.getLobbyUri(), lobby.getUri());

        assertEquals(lobby.getMembers().size(), 1);
    }

    @Test
    public void kicksMember_failsWhenNotOwner() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        lobby.addMember(newUser);

        assertEquals(lobby.getMembers().size(), 2);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "memberToKickUuid", lobbyOwner.getUuid());
        Action action = new Action("MEMBER_KICK", Category.LOBBY, data, newUser);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(newUser.getUuid()));

        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());

        assertEquals(lobby.getMembers().size(), 2);
    }

    @Test
    public void kicksMember_failsWhenNoSuchMember() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "memberToKickUuid", "nonexistantuuid");
        Action action = new Action("MEMBER_KICK", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
        assertEquals(event.getData().get("userUuid"), "nonexistantuuid");
    }

    @Test
    public void kicksMember_failsWhenNoMemberProvided() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("MEMBER_KICK", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("fieldName"), "memberToKickUuid");
    }

    @Test
    public void startsGame() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        lobby.addMember(newUser);
        assertEquals(lobby.getMembers().size(), 2);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("START_GAME", Category.LOBBY, data, lobbyOwner);

        ActionHandlerResponse response = actionDispatcherService.handleAction(action);
        List<Event<?>> events = response.getEvents();

        assertEquals(events.size(), 1);

        LobbyEvent gameCreatedEvent = (LobbyEvent) events.get(0);
        assertEquals(gameCreatedEvent.getCategory(), Category.LOBBY);
        assertEquals(gameCreatedEvent.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));
        assertEquals(gameCreatedEvent.getId(), LobbyEvent.ID.CREATED_GAME);
        assertEquals(gameCreatedEvent.getLobbyUri(), lobby.getUri());
        GameDTO gameDTO = (GameDTO) gameCreatedEvent.getData().get("gameData");

        List<ScheduledAction> scheduledActions = response.getScheduledActions();

        assertEquals(scheduledActions.size(), 1);
        ScheduledAction scheduledAction = scheduledActions.get(0);

        assertEquals(scheduledAction.getAction().getIdString(), "UPDATE_COUNTDOWN");
        assertEquals(scheduledAction.getAction().getDataByKey("gameUri").get(), gameDTO.getUri());
        assertTrue(scheduledAction.getAction() instanceof ServerAction);
        // TODO also test execution time
    }

    @Test
    public void startsGame_failsWhenNotEnoughPlayers() {
        assertEquals(lobby.getMembers().size(), 1);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("START_GAME", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void startsGame_failsWhenGameAlreadyStarted() {
        lobby.setGameOngoing(true);
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        lobby.addMember(newUser);
        assertEquals(lobby.getMembers().size(), 2);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("START_GAME", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void startsGame_failsWhenNotOwner() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        lobby.addMember(newUser);
        assertEquals(lobby.getMembers().size(), 2);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("START_GAME", Category.LOBBY, data, newUser);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(newUser.getUuid()));

        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void updatesSettingsInviteOnly() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "inviteOnly", "settingValue", "true");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        LobbyEvent event = (LobbyEvent) events.get(0);
        assertEquals(event.getCategory(), Category.LOBBY);
        assertEquals(event.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));
        assertEquals(event.getId(), LobbyEvent.ID.SETTINGS_UPDATED);
        assertEquals(event.getLobbyUri(), lobby.getUri());
        assertEquals(event.getData().get("settingName"), "inviteOnly");
        assertEquals(event.getData().get("settingValue"), "true");

        assertTrue(lobby.getSettings().isInviteOnly());
    }

    @Test
    public void updatesSettingsTimeForMove() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "timeForMove", "settingValue", "10");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        LobbyEvent event = (LobbyEvent) events.get(0);
        assertEquals(event.getCategory(), Category.LOBBY);
        assertEquals(event.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));
        assertEquals(event.getId(), LobbyEvent.ID.SETTINGS_UPDATED);
        assertEquals(event.getLobbyUri(), lobby.getUri());
        assertEquals(event.getData().get("settingName"), "timeForMove");
        assertEquals(event.getData().get("settingValue"), "10");

        assertEquals(lobby.getSettings().getTimeForMove(), 10);
    }

    @Test
    public void updatesSettingsScoreGoal() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "scoreGoal", "settingValue", "44");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        LobbyEvent event = (LobbyEvent) events.get(0);
        assertEquals(event.getCategory(), Category.LOBBY);
        assertEquals(event.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));
        assertEquals(event.getId(), LobbyEvent.ID.SETTINGS_UPDATED);
        assertEquals(event.getLobbyUri(), lobby.getUri());
        assertEquals(event.getData().get("settingName"), "scoreGoal");
        assertEquals(event.getData().get("settingValue"), "44");

        assertEquals(lobby.getSettings().getScoreGoal(), 44);
    }

    @Test
    public void updatesSettings_failsWhenSettingNameMissing() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingValue", "44");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("fieldName"), "settingName");
    }

    @Test
    public void updatesSettings_failsWhenSettingValueMissing() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "inviteOnly");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("fieldName"), "settingValue");
    }


    @Test
    public void updatesSettings_failsWhenNotOwner() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "inviteOnly", "settingValue", "true");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, newUser);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(newUser.getUuid()));

        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void updatesSettings_failsWhenNonBooleanInputOnInviteOnly() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "inviteOnly", "settingValue", "123");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("fieldName"), "inviteOnly");
        assertEquals(event.getData().get("expectedType"), "boolean string (\"true\" or \"false\")");
    }

    @Test
    public void updatesSettings_failsWhenInvalidInputOnTimeForMove() {
        // NOT THE CORRECT TYPE
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "timeForMove", "settingValue", "ff");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("fieldName"), "timeForMove");
        assertEquals(event.getData().get("expectedType"), "unsigned int");

        // UNDER THE MINIMUM VALUE
        Map<String, String> data2 = Map.of("lobbyUri", lobby.getUri(), "settingName", "timeForMove", "settingValue", "2");
        Action action2 = new Action("UPDATE_SETTINGS", Category.LOBBY, data2, lobbyOwner);

        List<Event<?>> events2 = actionDispatcherService.handleAction(action2).getEvents();

        assertEquals(events2.size(), 1);

        Event<?> event2 = events2.get(0);
        assertEquals(event2.getCategory(), Category.ERROR);
        assertEquals(event2.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event2.getData().get("lobbyUri"), lobby.getUri());
        assertEquals(event2.getData().get("message"), "Time for move value must be in range 3 <= n <= 10");

        // OVER THE MAXIMUM VALUE
        Map<String, String> data3 = Map.of("lobbyUri", lobby.getUri(), "settingName", "timeForMove", "settingValue", "11");
        Action action3 = new Action("UPDATE_SETTINGS", Category.LOBBY, data3, lobbyOwner);

        List<Event<?>> events3 = actionDispatcherService.handleAction(action3).getEvents();

        assertEquals(events3.size(), 1);

        Event<?> event3 = events3.get(0);
        assertEquals(event3.getCategory(), Category.ERROR);
        assertEquals(event3.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event3.getData().get("lobbyUri"), lobby.getUri());
        assertEquals(event3.getData().get("message"), "Time for move value must be in range 3 <= n <= 10");
    }

    @Test
    public void updatesSettings_failsWhenInvalidInputOnScoreGoal() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "scoreGoal", "settingValue", "ff");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<Event<?>> events = actionDispatcherService.handleAction(action).getEvents();

        assertEquals(events.size(), 1);

        Event<?> event = events.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event.getData().get("fieldName"), "scoreGoal");
        assertEquals(event.getData().get("expectedType"), "unsigned int");

        // UNDER THE MINIMUM VALUE
        Map<String, String> data2 = Map.of("lobbyUri", lobby.getUri(), "settingName", "scoreGoal", "settingValue", "0");
        Action action2 = new Action("UPDATE_SETTINGS", Category.LOBBY, data2, lobbyOwner);

        List<Event<?>> events2 = actionDispatcherService.handleAction(action2).getEvents();

        assertEquals(events2.size(), 1);

        Event<?> event2 = events2.get(0);
        assertEquals(event2.getCategory(), Category.ERROR);
        assertEquals(event2.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event2.getData().get("lobbyUri"), lobby.getUri());
        assertEquals(event2.getData().get("message"), "Score goal value must be in range 1 <= n <= 50");

        // OVER THE MAXIMUM VALUE
        Map<String, String> data3 = Map.of("lobbyUri", lobby.getUri(), "settingName", "scoreGoal", "settingValue", "51");
        Action action3 = new Action("UPDATE_SETTINGS", Category.LOBBY, data3, lobbyOwner);

        List<Event<?>> events3 = actionDispatcherService.handleAction(action3).getEvents();

        assertEquals(events3.size(), 1);

        Event<?> event3 = events3.get(0);
        assertEquals(event3.getCategory(), Category.ERROR);
        assertEquals(event3.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        assertEquals(event3.getData().get("lobbyUri"), lobby.getUri());
        assertEquals(event3.getData().get("message"), "Score goal value must be in range 1 <= n <= 50");
    }

    @Test
    public void cleanupTest() {
        List<Lobby> lobbies = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Lobby lobby = lobbyService.createLobby(lobbyOwner);
            lobby.deletionDate = Calendar.getInstance().getTimeInMillis();
            lobbies.add(lobby);
        }

        lobbyService.lobbyCleanupTask();

        lobbies.forEach(lobby -> {
            Optional<Lobby> optionalLobby = lobbyService.getLobby(lobby.getUri());
            assertTrue(optionalLobby.isEmpty());
        });
    }
}
