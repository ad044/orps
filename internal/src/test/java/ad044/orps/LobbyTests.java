package ad044.orps;

import ad044.orps.dto.GameDTO;
import ad044.orps.dto.LobbyDTO;
import ad044.orps.dto.UserDTO;
import ad044.orps.model.Category;
import ad044.orps.model.action.Action;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.LobbyEvent;
import ad044.orps.model.lobby.Lobby;
import ad044.orps.model.message.EventMessage;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.ActionHandlerService;
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
    ActionHandlerService actionHandlerService;

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

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.LOBBY_NOT_FOUND);
        assertEquals(event.getData().get("lobbyUri"), "test");
    }

    @Test
    public void addsBot() {
        Action action = new Action("ADD_BOT", Category.LOBBY, Map.of("lobbyUri", lobby.getUri()), lobbyOwner);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.LOBBY);
        assertEquals(message.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));

        LobbyEvent event = (LobbyEvent) message.getEvent();
        assertEquals(event.getId(), LobbyEvent.ID.MEMBER_JOIN);
        assertTrue(((UserDTO) event.getData().get("memberData")).getUsername().startsWith("Bot"));
        assertTrue(((UserDTO) event.getData().get("memberData")).getUuid().startsWith("Bot"));
        assertEquals(event.getLobbyUri(), lobby.getUri());
    }

    @Test
    public void addsBot_failsWhenNotOwner() {
        OrpsUserDetails nonOwnerUser = new OrpsUserDetails("nonowner", "nonowneruuid");
        Action action = new Action("ADD_BOT", Category.LOBBY, Map.of("lobbyUri", lobby.getUri()), nonOwnerUser);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(nonOwnerUser.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.INSUFFICIENT_PERMISSIONS);
        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void receivesNewTextMessageAsOwner() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "messageContent", "test");
        Action action = new Action("NEW_TEXT_MESSAGE", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.LOBBY);
        assertEquals(message.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));

        LobbyEvent event = (LobbyEvent) message.getEvent();
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

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.LOBBY);
        assertEquals(message.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));

        LobbyEvent event = (LobbyEvent) message.getEvent();
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

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(nonOwnerUser.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.USER_NOT_IN_LOBBY);
        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void receivesNewTextMessage_failsWhenNoMessageContent() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("NEW_TEXT_MESSAGE", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.DATA_FIELD_MISSING);
        assertEquals(event.getData().get("fieldName"), "messageContent");
    }

    @Test
    public void receivesNewTextMessage_failsWhenMessageEmpty() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "messageContent", "");
        Action action = new Action("NEW_TEXT_MESSAGE", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.BAD_TEXT_MESSAGE);
        assertEquals(event.getData().get("reason"), "Message can't be empty.");
        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void userJoins() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("USER_JOIN", Category.LOBBY, data, newUser);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 2);

        EventMessage joinMessage = response.get(0);
        assertEquals(joinMessage.getCategory(), Category.LOBBY);
        assertEquals(joinMessage.getRecipientUuids(), lobby.getMembers()
                .stream()
                .map(OrpsUserDetails::getUuid)
                .filter(uuid -> !uuid.equals("randomuuid"))
                .collect(Collectors.toList()));

        LobbyEvent joinEvent = (LobbyEvent) joinMessage.getEvent();
        assertEquals(joinEvent.getId(), LobbyEvent.ID.MEMBER_JOIN);
        assertEquals(joinEvent.getLobbyUri(), lobby.getUri());
        assertEquals(((UserDTO) (joinEvent.getData().get("memberData"))).getUuid(), newUser.getUuid());

        EventMessage receiveLobbyDataMessage = response.get(1);
        assertEquals(receiveLobbyDataMessage.getCategory(), Category.LOBBY);
        assertEquals(receiveLobbyDataMessage.getRecipientUuids(), Collections.singletonList(newUser.getUuid()));

        LobbyEvent receiveLobbyDataEvent = (LobbyEvent) receiveLobbyDataMessage.getEvent();
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

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 0);
    }

    @Test
    public void userLeavesAndOwnerGetsUpdated() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        lobby.addMember(newUser);

        assertEquals(lobby.getMembers().size(), 2);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("USER_LEAVE", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 2);

        EventMessage ownerUpdatedMessage = messages.get(0);
        assertEquals(ownerUpdatedMessage.getCategory(), Category.LOBBY);
        assertEquals(ownerUpdatedMessage.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));

        LobbyEvent ownerUpdatedEvent = (LobbyEvent) ownerUpdatedMessage.getEvent();
        assertEquals(ownerUpdatedEvent.getId(), LobbyEvent.ID.OWNER_UPDATED);
        assertEquals(ownerUpdatedEvent.getLobbyUri(), lobby.getUri());
        assertEquals(ownerUpdatedEvent.getData().get("newOwnerUuid"), newUser.getUuid());

        EventMessage memberLeaveMessage = messages.get(1);
        assertEquals(memberLeaveMessage.getCategory(), Category.LOBBY);
        assertEquals(memberLeaveMessage.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));

        LobbyEvent memberLeaveEvent = (LobbyEvent) memberLeaveMessage.getEvent();
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

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 2);

        EventMessage memberKickMessage = messages.get(0);
        assertEquals(memberKickMessage.getCategory(), Category.LOBBY);
        assertEquals(memberKickMessage.getRecipientUuids(), lobby.getMembers().stream()
                .map(OrpsUserDetails::getUuid)
                .filter(uuid -> !uuid.equals("randomuuid"))
                .collect(Collectors.toList()));

        LobbyEvent memberKickEvent = (LobbyEvent) memberKickMessage.getEvent();
        assertEquals(memberKickEvent.getId(), LobbyEvent.ID.MEMBER_KICK);
        assertEquals(memberKickEvent.getLobbyUri(), lobby.getUri());
        assertEquals(memberKickEvent.getData().get("memberUuid"), "randomuuid");

        EventMessage gotKickedMessage = messages.get(1);
        assertEquals(gotKickedMessage.getCategory(), Category.LOBBY);
        assertEquals(gotKickedMessage.getRecipientUuids(), Collections.singletonList("randomuuid"));

        LobbyEvent gotKickedEvent = (LobbyEvent) gotKickedMessage.getEvent();
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

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(newUser.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.INSUFFICIENT_PERMISSIONS);
        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());

        assertEquals(lobby.getMembers().size(), 2);
    }

    @Test
    public void kicksMember_failsWhenNoSuchMember() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "memberToKickUuid", "nonexistantuuid");
        Action action = new Action("MEMBER_KICK", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.USER_NOT_IN_LOBBY);
        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
        assertEquals(event.getData().get("userUuid"), "nonexistantuuid");
    }

    @Test
    public void kicksMember_failsWhenNoMemberProvided() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("MEMBER_KICK", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.DATA_FIELD_MISSING);
        assertEquals(event.getData().get("fieldName"), "memberToKickUuid");
    }

    @Test
    public void startsGame() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        lobby.addMember(newUser);
        assertEquals(lobby.getMembers().size(), 2);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("START_GAME", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage gameDataMessage = messages.get(0);
        assertEquals(gameDataMessage.getCategory(), Category.LOBBY);
        assertEquals(gameDataMessage.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));

        LobbyEvent gameCreatedEvent = (LobbyEvent) gameDataMessage.getEvent();
        assertEquals(gameCreatedEvent.getId(), LobbyEvent.ID.CREATED_GAME);
        assertEquals(gameCreatedEvent.getLobbyUri(), lobby.getUri());
        assertTrue(gameCreatedEvent.getData().get("gameData") instanceof GameDTO);
    }

    @Test
    public void startsGame_failsWhenNotEnoughPlayers() {
        assertEquals(lobby.getMembers().size(), 1);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("START_GAME", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.INSUFFICIENT_PLAYERS);
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

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.LOBBY_GAME_ALREADY_STARTED);
        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void startsGame_failsWhenNotOwner() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        lobby.addMember(newUser);
        assertEquals(lobby.getMembers().size(), 2);

        Map<String, String> data = Map.of("lobbyUri", lobby.getUri());
        Action action = new Action("START_GAME", Category.LOBBY, data, newUser);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(newUser.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.INSUFFICIENT_PERMISSIONS);
        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void updatesSettingsInviteOnly() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "inviteOnly", "settingValue", "true");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.LOBBY);
        assertEquals(message.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));

        LobbyEvent event = (LobbyEvent) message.getEvent();
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

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.LOBBY);
        assertEquals(message.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));

        LobbyEvent event = (LobbyEvent) message.getEvent();
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

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.LOBBY);
        assertEquals(message.getRecipientUuids(), lobby.getMembers().stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList()));

        LobbyEvent event = (LobbyEvent) message.getEvent();
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

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.DATA_FIELD_MISSING);
        assertEquals(event.getData().get("fieldName"), "settingName");
    }

    @Test
    public void updatesSettings_failsWhenSettingValueMissing() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "inviteOnly");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.DATA_FIELD_MISSING);
        assertEquals(event.getData().get("fieldName"), "settingValue");
    }


    @Test
    public void updatesSettings_failsWhenNotOwner() {
        OrpsUserDetails newUser = new OrpsUserDetails("user1", "randomuuid");
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "inviteOnly", "settingValue", "true");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, newUser);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(newUser.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.INSUFFICIENT_PERMISSIONS);
        assertEquals(event.getData().get("lobbyUri"), lobby.getUri());
    }

    @Test
    public void updatesSettings_failsWhenNonBooleanInputOnInviteOnly() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "inviteOnly", "settingValue", "123");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.INVALID_FIELD_DATA_TYPE);
        assertEquals(event.getData().get("fieldName"), "inviteOnly");
        assertEquals(event.getData().get("expectedType"), "boolean string (\"true\" or \"false\")");
    }

    @Test
    public void updatesSettings_failsWhenInvalidInputOnTimeForMove() {
        // NOT THE CORRECT TYPE
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "timeForMove", "settingValue", "ff");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.INVALID_FIELD_DATA_TYPE);
        assertEquals(event.getData().get("fieldName"), "timeForMove");
        assertEquals(event.getData().get("expectedType"), "unsigned int");

        // UNDER THE MINIMUM VALUE
        Map<String, String> data2 = Map.of("lobbyUri", lobby.getUri(), "settingName", "timeForMove", "settingValue", "2");
        Action action2 = new Action("UPDATE_SETTINGS", Category.LOBBY, data2, lobbyOwner);

        List<EventMessage> messages2 = actionHandlerService.handleAction(action2);

        assertEquals(messages2.size(), 1);

        EventMessage message2 = messages2.get(0);
        assertEquals(message2.getCategory(), Category.ERROR);
        assertEquals(message2.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event2 = (ErrorEvent) message2.getEvent();
        assertEquals(event2.getId(), ErrorEvent.ID.LOBBY_PARAMETER_VALUE_NOT_ALLOWED);
        assertEquals(event2.getData().get("lobbyUri"), lobby.getUri());
        assertEquals(event2.getData().get("message"), "Time for move value must be in range 3 <= n <= 10");

        // OVER THE MAXIMUM VALUE
        Map<String, String> data3 = Map.of("lobbyUri", lobby.getUri(), "settingName", "timeForMove", "settingValue", "11");
        Action action3 = new Action("UPDATE_SETTINGS", Category.LOBBY, data3, lobbyOwner);

        List<EventMessage> messages3 = actionHandlerService.handleAction(action3);

        assertEquals(messages3.size(), 1);

        EventMessage message3 = messages3.get(0);
        assertEquals(message3.getCategory(), Category.ERROR);
        assertEquals(message3.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event3 = (ErrorEvent) message3.getEvent();
        assertEquals(event3.getId(), ErrorEvent.ID.LOBBY_PARAMETER_VALUE_NOT_ALLOWED);
        assertEquals(event3.getData().get("lobbyUri"), lobby.getUri());
        assertEquals(event3.getData().get("message"), "Time for move value must be in range 3 <= n <= 10");
    }

    @Test
    public void updatesSettings_failsWhenInvalidInputOnScoreGoal() {
        Map<String, String> data = Map.of("lobbyUri", lobby.getUri(), "settingName", "scoreGoal", "settingValue", "ff");
        Action action = new Action("UPDATE_SETTINGS", Category.LOBBY, data, lobbyOwner);

        List<EventMessage> messages = actionHandlerService.handleAction(action);

        assertEquals(messages.size(), 1);

        EventMessage message = messages.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.INVALID_FIELD_DATA_TYPE);
        assertEquals(event.getData().get("fieldName"), "scoreGoal");
        assertEquals(event.getData().get("expectedType"), "unsigned int");

        // UNDER THE MINIMUM VALUE
        Map<String, String> data2 = Map.of("lobbyUri", lobby.getUri(), "settingName", "scoreGoal", "settingValue", "0");
        Action action2 = new Action("UPDATE_SETTINGS", Category.LOBBY, data2, lobbyOwner);

        List<EventMessage> messages2 = actionHandlerService.handleAction(action2);

        assertEquals(messages2.size(), 1);

        EventMessage message2 = messages2.get(0);
        assertEquals(message2.getCategory(), Category.ERROR);
        assertEquals(message2.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event2 = (ErrorEvent) message2.getEvent();
        assertEquals(event2.getId(), ErrorEvent.ID.LOBBY_PARAMETER_VALUE_NOT_ALLOWED);
        assertEquals(event2.getData().get("lobbyUri"), lobby.getUri());
        assertEquals(event2.getData().get("message"), "Score goal value must be in range 1 <= n <= 50");

        // OVER THE MAXIMUM VALUE
        Map<String, String> data3 = Map.of("lobbyUri", lobby.getUri(), "settingName", "scoreGoal", "settingValue", "51");
        Action action3 = new Action("UPDATE_SETTINGS", Category.LOBBY, data3, lobbyOwner);

        List<EventMessage> messages3 = actionHandlerService.handleAction(action3);

        assertEquals(messages3.size(), 1);

        EventMessage message3 = messages3.get(0);
        assertEquals(message3.getCategory(), Category.ERROR);
        assertEquals(message3.getRecipientUuids(), Collections.singletonList(lobbyOwner.getUuid()));

        ErrorEvent event3 = (ErrorEvent) message3.getEvent();
        assertEquals(event3.getId(), ErrorEvent.ID.LOBBY_PARAMETER_VALUE_NOT_ALLOWED);
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
