package ad044.orps.service;

import ad044.orps.model.action.Action;
import ad044.orps.model.action.GameAction;
import ad044.orps.model.action.GeneralAction;
import ad044.orps.model.action.LobbyAction;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.message.EventMessage;
import ad044.orps.util.EnumStringValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ActionHandlerService {
    Logger logger = LoggerFactory.getLogger(LobbyService.class);
    private final AtomicBoolean isReadingActions = new AtomicBoolean(false);
    private final BlockingQueue<Action> actionQueue = new LinkedBlockingQueue<>();

    @Autowired
    LobbyService lobbyService;

    @Autowired
    GameService gameService;

    @Autowired
    GeneralActionService generalActionService;

    @Autowired
    UserMessagingService userMessagingService;

    @PostConstruct
    private void postConstruct() {
        Runnable loop = () -> {
            while (isReadingActions.get()) {
                try {
                    List<EventMessage> messages = handleAction(actionQueue.take());
                    userMessagingService.sendEventMessage(messages);
                } catch (InterruptedException e) {
                    logger.error("Failed to take action from queue", e);
                }
            }
        };

        new Thread(loop).start();
        isReadingActions.set(true);
    }


    public void putAction(Action action) {
        try {
            actionQueue.put(action);
        } catch (InterruptedException e) {
            String message = String.format("Failed to put action inside queue, Author ID: %s", action.getAuthor().getUuid());
            logger.error(message, e);
        }
    }

    private List<EventMessage> dispatchActionToLobbyService(Action action) {
        Optional<String> optionalLobbyUri = action.getDataByKey("lobbyUri");
        Optional<LobbyAction.ID> optionalActionId =
                EnumStringValueMapper.stringValueToEnum(action.getIdString(), LobbyAction.ID.class);

        if (optionalLobbyUri.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.dataFieldMissing("lobbyUri");
            return Collections.singletonList(EventMessage.error(action.getAuthor().getUuid(), errorEvent));
        }

        if (optionalActionId.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.invalidAction("LOBBY", action.getIdString());
            return Collections.singletonList(EventMessage.error(action.getAuthor().getUuid(), errorEvent));
        }

        LobbyAction lobbyAction = new LobbyAction(action, optionalActionId.get(), optionalLobbyUri.get());

        return lobbyService.handleAction(lobbyAction);
    }

    private List<EventMessage> dispatchActionToGameService(Action action) {
        Optional<String> optionalGameUri = action.getDataByKey("gameUri");
        Optional<GameAction.ID> optionalActionId =
                EnumStringValueMapper.stringValueToEnum(action.getIdString(), GameAction.ID.class);

        if (optionalGameUri.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.dataFieldMissing("gameUri");
            return Collections.singletonList(EventMessage.error(action.getAuthor().getUuid(), errorEvent));
        }

        if (optionalActionId.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.invalidAction("GAME", action.getIdString());
            return Collections.singletonList(EventMessage.error(action.getAuthor().getUuid(), errorEvent));
        }

        GameAction gameAction = new GameAction(action, optionalActionId.get(), optionalGameUri.get());

        return gameService.handleGameAction(gameAction);
    }

    private List<EventMessage> dispatchActionToGeneralActionService(Action action) {
        Optional<GeneralAction.ID> optionalActionId =
                EnumStringValueMapper.stringValueToEnum(action.getIdString(), GeneralAction.ID.class);

        if (optionalActionId.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.invalidAction("GENERAL", action.getIdString());
            return Collections.singletonList(EventMessage.error(action.getAuthor().getUuid(), errorEvent));
        }

        GeneralAction generalAction = new GeneralAction(action, optionalActionId.get());

        return generalActionService.handleGeneralAction(generalAction);
    }

    public List<EventMessage> handleAction(Action action) {
        switch (action.getCategory()) {
            case LOBBY: return dispatchActionToLobbyService(action);
            case GAME: return dispatchActionToGameService(action);
            case GENERAL: return dispatchActionToGeneralActionService(action);
            default: return Collections.emptyList();
        }
    }
}
