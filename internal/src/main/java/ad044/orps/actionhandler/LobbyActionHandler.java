package ad044.orps.actionhandler;

import ad044.orps.dto.GameDTO;
import ad044.orps.dto.LobbyDTO;
import ad044.orps.dto.UserDTO;
import ad044.orps.model.ActionHandlerResponse;
import ad044.orps.model.action.LobbyAction;
import ad044.orps.model.action.ScheduledAction;
import ad044.orps.model.action.ServerAction;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.Event;
import ad044.orps.model.event.LobbyEvent;
import ad044.orps.model.game.Game;
import ad044.orps.model.lobby.Lobby;
import ad044.orps.model.user.BotUserDetails;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.GameService;
import ad044.orps.util.ParseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LobbyActionHandler {
    Logger logger = LoggerFactory.getLogger(LobbyActionHandler.class);

    @Autowired
    GameService gameService;

    private Event<?> handleAddBot(Lobby lobby, String authorUuid) {
        if (!lobby.isOwner(authorUuid)) {
            return ErrorEvent.insufficientPermissions(authorUuid, lobby.getUri());
        }

        BotUserDetails botUser = new BotUserDetails(UUID.randomUUID().toString());
        lobby.addMember(botUser);

        logger.info(String.format("Added bot with UUID %s to lobby %s", botUser.getUuid(), lobby.getUri()));

        return LobbyEvent.addBot(lobby.getMemberUuids(), lobby.getUri(), UserDTO.from(botUser));
    }

    private Event<?> handleNewTextMessage(Lobby lobby, OrpsUserDetails author, String messageContent) {
        if (!lobby.hasMember(author.getUuid())) {
            return ErrorEvent.userNotInLobby(author.getUuid(), lobby.getUri(), lobby.getUri());
        }

        if (messageContent.length() < 1) {
            return ErrorEvent.badTextMessage(author.getUuid(), lobby.getUri(), "Message can't be empty.");
        }

        return LobbyEvent.newTextMessage(lobby.getMemberUuids(), lobby.getUri(), UserDTO.from(author), messageContent);
    }

    private List<Event<?>> handleUserJoin(Lobby lobby, OrpsUserDetails author) {
        List<Event<?>> events = new ArrayList<>();

        if (!lobby.hasMember(author.getUuid())) {
            lobby.addMember(author);

            List<String> recipients = lobby.getMemberUuidsExcept(author.getUuid());
            LobbyEvent memberJoinEvent = LobbyEvent.memberJoin(recipients, lobby.getUri(), UserDTO.from(author));
            events.add(memberJoinEvent);
        }

        lobby.deletionDate = -1;

        LobbyEvent receiveLobbyDataEvent = LobbyEvent.receiveLobbyData(author.getUuid(), lobby.getUri(), LobbyDTO.from(lobby));
        events.add(receiveLobbyDataEvent);

        return events;
    }

    public List<Event<?>> handleUserLeave(Lobby lobby, String userThatLeftUuid) {
        if (!lobby.hasMember(userThatLeftUuid)) {
            return Collections.emptyList();
        }

        lobby.removeMember(userThatLeftUuid);

        List<OrpsUserDetails> nonBotMembers = lobby.getNonBotMembers();

        if (nonBotMembers.isEmpty()) {
            lobby.deletionDate = Calendar.getInstance().getTimeInMillis() + 1000 * 60;
            return Collections.emptyList();
        }

        List<Event<?>> events = new ArrayList<>();

        if (lobby.isOwner(userThatLeftUuid) && !nonBotMembers.isEmpty()) {
            OrpsUserDetails newOwner = nonBotMembers.get(0);

            lobby.setOwner(newOwner);

            LobbyEvent ownerUpdatedEvent = LobbyEvent.ownerUpdated(lobby.getMemberUuids(), lobby.getUri(), newOwner.getUuid());
            events.add(ownerUpdatedEvent);

            logger.info(String.format("Set %s as lobby owner in %s", newOwner.getUuid(), lobby.getUri()));
        }

        LobbyEvent memberLeaveEvent = LobbyEvent.memberLeave(lobby.getMemberUuids(), lobby.getUri(), userThatLeftUuid);
        events.add(memberLeaveEvent);

        return events;
    }

    private List<Event<?>> handleMemberKick(Lobby lobby, String authorUuid, String memberToKickUuid) {
        if (!lobby.isOwner(authorUuid)) {
            ErrorEvent errorEvent = ErrorEvent.insufficientPermissions(authorUuid, lobby.getUri());
            return Collections.singletonList(errorEvent);
        }

        if (!lobby.hasMember(memberToKickUuid)) {
            ErrorEvent errorEvent = ErrorEvent.userNotInLobby(authorUuid, memberToKickUuid, lobby.getUri());
            return Collections.singletonList(errorEvent);
        }

        List<Event<?>> events = new ArrayList<>();

        lobby.removeMember(memberToKickUuid);

        LobbyEvent memberKickEvent = LobbyEvent.memberKick(lobby.getMemberUuidsExcept(memberToKickUuid), lobby.getUri(), memberToKickUuid);
        events.add(memberKickEvent);

        LobbyEvent gotKickedEvent = LobbyEvent.gotKicked(memberToKickUuid, lobby.getUri());
        events.add(gotKickedEvent);

        return events;
    }

    private ActionHandlerResponse handleStartGame(Lobby lobby, String authorUuid) {
        if (lobby.getMembers().size() < 2) {
            return new ActionHandlerResponse(ErrorEvent.insufficientPlayers(authorUuid, lobby.getUri()));
        }

        if (lobby.isGameOngoing()) {
            return new ActionHandlerResponse(ErrorEvent.gameAlreadyStarted(authorUuid, lobby.getUri()));
        }

        if (!lobby.isOwner(authorUuid)) {
            return new ActionHandlerResponse(ErrorEvent.insufficientPermissions(authorUuid, lobby.getUri()));
        }

        lobby.setGameOngoing(true);

        Game createdGame = gameService.createLobbyGame(lobby);

        ServerAction updateCountdownAction = ServerAction.game("UPDATE_COUNTDOWN", createdGame.getUri());
        ScheduledAction scheduledAction
                = new ScheduledAction(updateCountdownAction, Calendar.getInstance().getTimeInMillis() + 1000);

        LobbyEvent createdGameEvent = LobbyEvent.createdGame(lobby.getMemberUuids(), lobby.getUri(), GameDTO.from(createdGame));
        return new ActionHandlerResponse(createdGameEvent, scheduledAction);
    }

    private Event<?> handleUpdateSettings(Lobby lobby, String authorUuid, String settingName, String settingValue) {
        if (!lobby.isOwner(authorUuid)) {
            return ErrorEvent.insufficientPermissions(authorUuid, lobby.getUri());
        }

        switch (settingName) {
            case "inviteOnly": {
                if (!ParseUtil.isBoolean(settingValue)) {
                    return ErrorEvent.invalidFieldDataType(authorUuid, settingName, "boolean string (\"true\" or \"false\")");
                }

                lobby.getSettings().setInviteOnly(Boolean.parseBoolean(settingValue));
            } break;
            case "timeForMove": {
                if (!ParseUtil.isUnsignedInt(settingValue)) {
                    return ErrorEvent.invalidFieldDataType(authorUuid, settingName, "unsigned int");
                }

                int newVal = Integer.parseInt(settingValue);
                if (newVal < 3 || newVal > 10) {
                    String message = "Time for move value must be in range 3 <= n <= 10";
                    return ErrorEvent.lobbyParameterNotAllowed(authorUuid, lobby.getUri(), message);
                }

                lobby.getSettings().setTimeForMove(newVal);
            } break;
            case "scoreGoal": {
                if (!ParseUtil.isUnsignedInt(settingValue)) {
                    return ErrorEvent.invalidFieldDataType(authorUuid, settingName, "unsigned int");
                }

                int newVal = Integer.parseInt(settingValue);
                if (newVal < 1 || newVal > 50) {
                    String message = "Score goal value must be in range 1 <= n <= 50";
                    return ErrorEvent.lobbyParameterNotAllowed(authorUuid, lobby.getUri(), message);
                }

                lobby.getSettings().setScoreGoal(newVal);
            } break;
            default: {
                return ErrorEvent.invalidSettingName(authorUuid, lobby.getUri(), settingName);
            }
        }

        return LobbyEvent.settingsUpdated(lobby.getMemberUuids(), lobby.getUri(), settingName, settingValue);
    }

    public ActionHandlerResponse handleAction(LobbyAction action) {
        LobbyAction.ID actionId = action.getId();
        OrpsUserDetails author = action.getAuthor();
        Lobby lobby = action.getTargetLobby();

        switch (actionId) {
            case ADD_BOT: {
                Event<?> addBotEvent = handleAddBot(lobby, author.getUuid());
                return new ActionHandlerResponse(addBotEvent);
            }
            case NEW_TEXT_MESSAGE: {
                Optional<String> optionalMessageContent = action.getDataByKey("messageContent");
                if (optionalMessageContent.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing(author.getUuid(), "messageContent");
                    return new ActionHandlerResponse(errorEvent);
                }

                Event<?> newTextMessage = handleNewTextMessage(lobby, author, optionalMessageContent.get());
                return new ActionHandlerResponse(newTextMessage);
            }
            case USER_JOIN: {
                List<Event<?>> userJoinEvents = handleUserJoin(lobby, author);
                return new ActionHandlerResponse(userJoinEvents);
            }
            case USER_LEAVE: {
                List<Event<?>> userLeaveEvents = handleUserLeave(lobby, author.getUuid());
                return new ActionHandlerResponse(userLeaveEvents);
            }
            case MEMBER_KICK: {
                Optional<String> optionalMemberToKickUuid = action.getDataByKey("memberToKickUuid");
                if (optionalMemberToKickUuid.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing(author.getUuid(), "memberToKickUuid");
                    return new ActionHandlerResponse(errorEvent);
                }

                List<Event<?>> memberKickEvents = handleMemberKick(lobby, author.getUuid(), optionalMemberToKickUuid.get());
                return new ActionHandlerResponse(memberKickEvents);
            }
            case START_GAME: {
                return handleStartGame(lobby, author.getUuid());
            }
            case UPDATE_SETTINGS: {
                Optional<String> settingName = action.getDataByKey("settingName");
                Optional<String> settingValue = action.getDataByKey("settingValue");

                if (settingName.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing(author.getUuid(), "settingName");
                    return new ActionHandlerResponse(errorEvent);
                }

                if (settingValue.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing(author.getUuid(), "settingValue");
                    return new ActionHandlerResponse(errorEvent);
                }

                Event<?> updateSettingsEvent = handleUpdateSettings(lobby, author.getUuid(), settingName.get(), settingValue.get());
                return new ActionHandlerResponse(updateSettingsEvent);
            }
            default: {
                return ActionHandlerResponse.empty();
            }
        }
    }
}
