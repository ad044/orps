package ad044.orps.service;

import ad044.orps.dto.LobbyDTO;
import ad044.orps.model.action.GeneralAction;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.GeneralEvent;
import ad044.orps.model.lobby.Lobby;
import ad044.orps.model.message.EventMessage;
import ad044.orps.model.user.OrpsUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class GeneralActionService {
    Logger logger = LoggerFactory.getLogger(GeneralActionService.class);
    @Autowired
    LobbyService lobbyService;

    @Autowired
    GameService gameService;

    private EventMessage handleCreateLobby(OrpsUserDetails author) {
        Lobby newLobby = lobbyService.createLobby(author);

        GeneralEvent createdLobbyEvent = GeneralEvent.createdLobby(LobbyDTO.from(newLobby));
        return EventMessage.general(author.getUuid(), createdLobbyEvent);
    }

    private List<EventMessage> handleDisconnect(String disconnectedUuid) {
        List<EventMessage> messages = new ArrayList<>();
        lobbyService.getAllLobbiesWithUser(disconnectedUuid).forEach(lobby -> {
            messages.addAll(lobbyService.handleUserLeave(lobby, disconnectedUuid));
        });
        gameService.getAllGamesWithUser(disconnectedUuid).forEach(game ->  {
            messages.addAll(gameService.handlePlayerLeave(game, disconnectedUuid));
        });

        return messages;
    }

    private EventMessage handleChangeName(OrpsUserDetails author, String newName) {
        String invalidReason = null;

        if (!newName.matches("^[a-zA-Z0-9]*$")) {
            invalidReason = "Name must be alphanumeric.";
        }

        if (newName.length() < 3 || newName.length() > 16) {
            invalidReason = "Name length must be >= 3 and <= 16";
        }

        if (invalidReason != null) {
            logger.info(String.format("User with UUID %s tried to change name to %s", author.getUuid(), newName));

            ErrorEvent errorEvent = ErrorEvent.nameNotAccepted(newName, invalidReason);
            return EventMessage.error(author.getUuid(), errorEvent);
        }

        author.setUsername(newName);

        logger.info(String.format("User with UUID %s changed name to %s", author.getUuid(), newName));

        List<OrpsUserDetails> recipients = new ArrayList<>();

        recipients.add(author);

        lobbyService.getAllLobbiesWithUser(author.getUuid()).forEach(lobby -> {
            recipients.addAll(lobby.getMembers());
        });

        gameService.getAllGamesWithUser(author.getUuid()).forEach(game -> {
            recipients.addAll(game.getPlayers());
        });

        GeneralEvent userNameChangeEvent = GeneralEvent.userChangedName(author.getUuid(), newName);
        return EventMessage.general(recipients, userNameChangeEvent);
    }

    public List<EventMessage> handleGeneralAction(GeneralAction action) {
        OrpsUserDetails author = action.getAuthor();

        switch (action.getId()) {
            case CREATE_LOBBY: {
                return Collections.singletonList(handleCreateLobby(author));
            }
            case CHANGE_NAME: {
                Optional<String> optionalNewName = action.getDataByKey("newName");
                if (optionalNewName.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing("newName");
                    return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
                }

                return Collections.singletonList(handleChangeName(author, optionalNewName.get()));
            }
            case USER_DISCONNECT: {
                return handleDisconnect(author.getUuid());
            }
        }

        return Collections.emptyList();
    }
}
