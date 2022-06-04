package ad044.orps.actionhandler;

import ad044.orps.dto.LobbyDTO;
import ad044.orps.model.ActionHandlerResponse;
import ad044.orps.model.action.GeneralAction;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.Event;
import ad044.orps.model.event.GeneralEvent;
import ad044.orps.model.lobby.Lobby;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.GameService;
import ad044.orps.service.LobbyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GeneralActionHandler {
    Logger logger = LoggerFactory.getLogger(GeneralActionHandler.class);
    @Autowired
    LobbyService lobbyService;

    @Autowired
    LobbyActionHandler lobbyActionHandler;

    @Autowired
    GameService gameService;

    @Autowired
    GameActionHandler gameActionHandler;

    private GeneralEvent handleCreateLobby(OrpsUserDetails author) {
        Lobby newLobby = lobbyService.createLobby(author);

        return GeneralEvent.createdLobby(author.getUuid(), LobbyDTO.from(newLobby));
    }

    private List<Event<?>> handleDisconnect(String disconnectedUuid) {
        List<Event<?>> messages = new ArrayList<>();

        lobbyService.getAllLobbiesWithUser(disconnectedUuid).forEach(lobby -> {
            messages.addAll(lobbyActionHandler.handleUserLeave(lobby, disconnectedUuid));
        });
        gameService.getAllGamesWithUser(disconnectedUuid).forEach(game ->  {
            messages.addAll(gameActionHandler.handlePlayerLeave(game, disconnectedUuid));
        });

        return messages;
    }

    private Event<?> handleChangeName(OrpsUserDetails author, String newName) {
        String invalidReason = null;

        if (!newName.matches("^[a-zA-Z0-9]*$")) {
            invalidReason = "Name must be alphanumeric.";
        }

        if (newName.length() < 3 || newName.length() > 16) {
            invalidReason = "Name length must be >= 3 and <= 16";
        }

        if (invalidReason != null) {
            logger.info(String.format("User with UUID %s tried to change name to %s", author.getUuid(), newName));

            return ErrorEvent.nameNotAccepted(author.getUuid(), newName, invalidReason);
        }

        author.setUsername(newName);

        logger.info(String.format("User with UUID %s changed name to %s", author.getUuid(), newName));

        List<String> recipients = new ArrayList<>();

        recipients.add(author.getUuid());

        lobbyService.getAllLobbiesWithUser(author.getUuid()).forEach(lobby -> {
            recipients.addAll(lobby.getMemberUuids());
        });

        gameService.getAllGamesWithUser(author.getUuid()).forEach(game -> {
            recipients.addAll(game.getPlayerUuids());
        });

        List<String> uniqueRecipients = recipients.stream().distinct().collect(Collectors.toList());
        return GeneralEvent.userChangedName(uniqueRecipients, author.getUuid(), newName);
    }

    public ActionHandlerResponse handleGeneralAction(GeneralAction action) {
        OrpsUserDetails author = action.getAuthor();

        switch (action.getId()) {
            case CREATE_LOBBY: {
                GeneralEvent createLobbyEvents = handleCreateLobby(author);
                return new ActionHandlerResponse(createLobbyEvents);
            }
            case CHANGE_NAME: {
                Optional<String> optionalNewName = action.getDataByKey("newName");
                if (optionalNewName.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing(author.getUuid(), "newName");
                    return new ActionHandlerResponse(errorEvent);
                }

                Event<?> changeNameEvent = handleChangeName(author, optionalNewName.get());
                return new ActionHandlerResponse(changeNameEvent);
            }
            case USER_DISCONNECT: {
                List<Event<?>> disconnectEvents = handleDisconnect(author.getUuid());
                return new ActionHandlerResponse(disconnectEvents);
            }
        }

        return ActionHandlerResponse.empty();
    }
}
