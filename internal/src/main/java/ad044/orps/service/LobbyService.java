package ad044.orps.service;

import ad044.orps.dto.GameDTO;
import ad044.orps.dto.LobbyDTO;
import ad044.orps.dto.UserDTO;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.message.EventMessage;
import ad044.orps.model.user.BotUserDetails;
import ad044.orps.model.action.LobbyAction;
import ad044.orps.model.event.LobbyEvent;
import ad044.orps.model.game.Game;
import ad044.orps.model.lobby.*;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.util.ParseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LobbyService {
    Logger logger = LoggerFactory.getLogger(LobbyService.class);

    Map<String, Lobby> lobbySessions = new ConcurrentHashMap<>();

    @Autowired
    GameService gameService;

    public Lobby createLobby(OrpsUserDetails author) {
        LobbySettings settings = new LobbySettings(3, 5, true);

        Lobby lobby = new Lobby(author, settings);

        lobbySessions.put(lobby.getUri(), lobby);

        logger.info(String.format("Created new lobby with URI: %s", lobby.getUri()));

        return lobby;
    }

    public List<Lobby> getAllLobbiesWithUser(String uuid) {
        return lobbySessions
                .values()
                .stream().filter(lobby -> lobby.hasMember(uuid))
                .collect(Collectors.toList());
    }

    public Optional<Lobby> getLobby(String uri) {
        return Optional.ofNullable(lobbySessions.get(uri));
    }

    private EventMessage handleAddBot(Lobby lobby, String authorUuid) {
        if (!lobby.isOwner(authorUuid)) {
            ErrorEvent errorEvent = ErrorEvent.insufficientPermissions(lobby.getUri());
            return EventMessage.error(authorUuid, errorEvent);
        }

        BotUserDetails botUser = new BotUserDetails(UUID.randomUUID().toString());
        lobby.addMember(botUser);

        logger.info(String.format("Added bot with UUID %s to lobby %s", botUser.getUuid(), lobby.getUri()));

        LobbyEvent addBotEvent = LobbyEvent.addBot(lobby.getUri(), UserDTO.from(botUser));
        return EventMessage.lobby(lobby.getMembers(), addBotEvent);
    }

    private EventMessage handleNewTextMessage(Lobby lobby, OrpsUserDetails author, String messageContent) {
        if (!lobby.hasMember(author.getUuid())) {
            ErrorEvent errorEvent = ErrorEvent.userNotInLobby(lobby.getUri(), lobby.getUri());
            return EventMessage.error(author.getUuid(), errorEvent);
        }

        if (messageContent.length() < 1) {
            ErrorEvent errorEvent = ErrorEvent.badTextMessage(lobby.getUri(), "Message can't be empty.");
            return EventMessage.error(author.getUuid(), errorEvent);
        }

        LobbyEvent newTextMessageEvent = LobbyEvent.newTextMessage(lobby.getUri(), UserDTO.from(author), messageContent);
        return EventMessage.lobby(lobby.getMembers(), newTextMessageEvent);
    }

    private List<EventMessage> handleUserJoin(Lobby lobby, OrpsUserDetails author) {
        List<EventMessage> messages = new ArrayList<>();

        if (!lobby.hasMember(author.getUuid())) {
            lobby.addMember(author);

            LobbyEvent joinEvent = LobbyEvent.memberJoin(lobby.getUri(), UserDTO.from(author));
            messages.add(EventMessage.lobby(lobby.getMembersExcept(author.getUuid()), joinEvent));
        }

        LobbyEvent receiveLobbyDataEvent = LobbyEvent.receiveLobbyData(lobby.getUri(), LobbyDTO.from(lobby));
        messages.add(EventMessage.lobby(author.getUuid(), receiveLobbyDataEvent));

        return messages;
    }

    public List<EventMessage> handleUserLeave(Lobby lobby, String userThatLeftUuid) {
        if (!lobby.hasMember(userThatLeftUuid)) {
            return Collections.emptyList();
        }

        lobby.removeMember(userThatLeftUuid);

        // TODO Keep last action time or something similar inside Lobbies
        // and have a process that checks for inactive ones, and deletes them if neccessary.

        List<EventMessage> messages = new ArrayList<>();
        if (lobby.isOwner(userThatLeftUuid)) {
            List<OrpsUserDetails> nonBotMembers = lobby.getNonBotMembers();

            if (!nonBotMembers.isEmpty()) {
                OrpsUserDetails newOwner = nonBotMembers.get(0);

                lobby.setOwner(newOwner);

                LobbyEvent ownerUpdatedEvent = LobbyEvent.ownerUpdated(lobby.getUri(), newOwner.getUuid());
                messages.add(EventMessage.lobby(lobby.getMembers(), ownerUpdatedEvent));

                logger.info(String.format("Set %s as lobby owner in %s", newOwner.getUuid(), lobby.getUri()));
            }
        }

        LobbyEvent memberLeaveEvent = LobbyEvent.memberLeave(lobby.getUri(), userThatLeftUuid);
        messages.add(EventMessage.lobby(lobby.getMembers(), memberLeaveEvent));

        return messages;
    }

    private List<EventMessage> handleMemberKick(Lobby lobby, String authorUuid, String memberToKickUuid) {
        if (!lobby.isOwner(authorUuid)) {
            ErrorEvent errorEvent = ErrorEvent.insufficientPermissions(lobby.getUri());
            return Collections.singletonList(EventMessage.error(authorUuid, errorEvent));
        }

        if (!lobby.hasMember(memberToKickUuid)) {
            ErrorEvent errorEvent = ErrorEvent.userNotInLobby(memberToKickUuid, lobby.getUri());
            return Collections.singletonList(EventMessage.error(authorUuid, errorEvent));
        }

        List<EventMessage> messages = new ArrayList<>();

        lobby.removeMember(memberToKickUuid);

        LobbyEvent memberKickEvent = LobbyEvent.memberKick(lobby.getUri(), memberToKickUuid);
        messages.add(EventMessage.lobby(lobby.getMembersExcept(memberToKickUuid), memberKickEvent));

        LobbyEvent gotKickedEvent = LobbyEvent.gotKicked(lobby.getUri());
        messages.add(EventMessage.lobby(memberToKickUuid, gotKickedEvent));

        return messages;
    }

    private EventMessage handleStartGame(Lobby lobby, String authorUuid) {
        if (lobby.getMembers().size() < 2) {
            ErrorEvent errorEvent = ErrorEvent.insufficientPlayers(lobby.getUri());
            return EventMessage.error(authorUuid, errorEvent);
        }

        if (lobby.isGameOngoing()) {
            ErrorEvent errorEvent = ErrorEvent.gameAlreadyStarted(lobby.getUri());
            return EventMessage.error(authorUuid, errorEvent);
        }

        if (!lobby.isOwner(authorUuid)) {
            ErrorEvent errorEvent = ErrorEvent.insufficientPermissions(lobby.getUri());
            return EventMessage.error(authorUuid, errorEvent);
        }

        lobby.setGameOngoing(true);

        Game newGame = gameService.createLobbyGame(lobby);

        LobbyEvent createdGameEvent = LobbyEvent.createdGame(lobby.getUri(), GameDTO.from(newGame));
        return EventMessage.lobby(lobby.getMembers(), createdGameEvent);
    }

    private EventMessage handleUpdateSettings(Lobby lobby, String authorUuid, String settingName, String settingValue) {
        if (!lobby.isOwner(authorUuid)) {
            ErrorEvent errorEvent = ErrorEvent.insufficientPermissions(lobby.getUri());
            return EventMessage.error(authorUuid, errorEvent);
        }

        switch (settingName) {
            case "inviteOnly": {
                if (!ParseUtil.isBoolean(settingValue)) {
                    ErrorEvent errorEvent =
                            ErrorEvent.invalidFieldDataType(settingName, "boolean string (\"true\" or \"false\")");
                    return EventMessage.error(authorUuid, errorEvent);
                }

                lobby.getSettings().setInviteOnly(Boolean.parseBoolean(settingValue));
            } break;
            case "timeForMove": {
                if (!ParseUtil.isUnsignedInt(settingValue)) {
                    ErrorEvent errorEvent = ErrorEvent.invalidFieldDataType(settingName, "unsigned int");
                    return EventMessage.error(authorUuid, errorEvent);
                }

                int newVal = Integer.parseInt(settingValue);
                if (newVal < 3 || newVal > 10) {
                    String message = "Time for move value must be in range 3 <= n <= 10";
                    ErrorEvent errorEvent = ErrorEvent.lobbyParameterNotAllowed(lobby.getUri(), message);
                    return EventMessage.error(authorUuid, errorEvent);
                }

                lobby.getSettings().setTimeForMove(newVal);
            } break;
            case "scoreGoal": {
                if (!ParseUtil.isUnsignedInt(settingValue)) {
                    ErrorEvent errorEvent = ErrorEvent.invalidFieldDataType(settingName, "unsigned int");
                    return EventMessage.error(authorUuid, errorEvent);
                }

                int newVal = Integer.parseInt(settingValue);
                if (newVal < 1 || newVal > 50) {
                    String message = "Score goal value must be in range 1 <= n <= 50";
                    ErrorEvent errorEvent = ErrorEvent.lobbyParameterNotAllowed(lobby.getUri(), message);
                    return EventMessage.error(authorUuid, errorEvent);
                }

                lobby.getSettings().setScoreGoal(newVal);
            } break;
            default: {
                ErrorEvent errorEvent = ErrorEvent.invalidSettingName(lobby.getUri(), settingName);
                return EventMessage.error(authorUuid, errorEvent);
            }
        }

        LobbyEvent settingsUpdateEvent = LobbyEvent.settingsUpdated(lobby.getUri(), settingName, settingValue);
        return EventMessage.lobby(lobby.getMembers(), settingsUpdateEvent);
    }

    public List<EventMessage> handleAction(LobbyAction action) {
        LobbyAction.ID actionId = action.getId();
        OrpsUserDetails author = action.getAuthor();
        String lobbyUri = action.getTargetLobbyUri();

        Optional<Lobby> optionalLobby = getLobby(lobbyUri);
        if (optionalLobby.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.lobbyNotFound(lobbyUri);
            return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
        }

        Lobby lobby = optionalLobby.get();

        switch (actionId) {
            case ADD_BOT: {
                return Collections.singletonList(handleAddBot(lobby, author.getUuid()));
            }
            case NEW_TEXT_MESSAGE: {
                Optional<String> optionalMessageContent = action.getDataByKey("messageContent");
                if (optionalMessageContent.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing("messageContent");
                    return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
                }

                return Collections.singletonList(handleNewTextMessage(lobby, author, optionalMessageContent.get()));
            }
            case USER_JOIN: {
                return handleUserJoin(lobby, author);
            }
            case USER_LEAVE: {
                return handleUserLeave(lobby, author.getUuid());
            }
            case MEMBER_KICK: {
                Optional<String> optionalMemberToKickUuid = action.getDataByKey("memberToKickUuid");
                if (optionalMemberToKickUuid.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing("memberToKickUuid");
                    return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
                }

                return handleMemberKick(lobby, author.getUuid(), optionalMemberToKickUuid.get());
            }
            case START_GAME: {
                return Collections.singletonList(handleStartGame(lobby, author.getUuid()));
            }
            case UPDATE_SETTINGS: {
                Optional<String> settingName = action.getDataByKey("settingName");
                Optional<String> settingValue = action.getDataByKey("settingValue");

                if (settingName.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing("settingName");
                    return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
                }

                if (settingValue.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing("settingValue");
                    return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
                }

                return Collections.singletonList(handleUpdateSettings(lobby, author.getUuid(), settingName.get(), settingValue.get()));
            }
            default: {
                return Collections.emptyList();
            }
        }
    }
}
