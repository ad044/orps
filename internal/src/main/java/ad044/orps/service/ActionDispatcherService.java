package ad044.orps.service;

import ad044.orps.actionhandler.GameActionHandler;
import ad044.orps.actionhandler.GeneralActionHandler;
import ad044.orps.actionhandler.LobbyActionHandler;
import ad044.orps.model.ActionHandlerResponse;
import ad044.orps.model.action.*;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.game.Game;
import ad044.orps.model.game.Player;
import ad044.orps.model.lobby.Lobby;
import ad044.orps.util.EnumStringValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ActionDispatcherService {
    Logger logger = LoggerFactory.getLogger(ActionDispatcherService.class);
    private final AtomicBoolean isReadingActions = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<Action> actionQueue = new ConcurrentLinkedQueue<>();
    private final PriorityQueue<ScheduledAction> scheduledActionQueue = new PriorityQueue<>();

    @Autowired
    LobbyService lobbyService;

    @Autowired
    LobbyActionHandler lobbyActionHandler;

    @Autowired
    GameService gameService;

    @Autowired
    GameActionHandler gameActionHandler;

    @Autowired
    GeneralActionHandler generalActionHandler;

    @Autowired
    UserMessagingService userMessagingService;

    @PostConstruct
    private void postConstruct() {
        Runnable actionQueueLoop = () -> {
            while (isReadingActions.get()) {
                Action action = actionQueue.poll();
                if (action != null) {
                    ActionHandlerResponse response = handleAction(action);
                    userMessagingService.sendEvent(response.getEvents());
                    response.getScheduledActions().forEach(this::scheduleAction);
                }

                ScheduledAction scheduledAction = scheduledActionQueue.peek();
                if (scheduledAction != null) {
                    long currTime = Calendar.getInstance().getTimeInMillis();
                    if (currTime >= scheduledAction.getExecutionTime()) {
                        putAction(scheduledActionQueue.remove().getAction());
                    }
                }
            }
        };

        new Thread(actionQueueLoop).start();

        isReadingActions.set(true);
    }


    public void putAction(Action action) {
        actionQueue.add(action);
    }

    public void scheduleAction(ScheduledAction scheduledAction) {
        scheduledActionQueue.add(scheduledAction);
    }

    private ActionHandlerResponse dispatchLobbyAction(Action action) {
        Optional<String> optionalLobbyUri = action.getDataByKey("lobbyUri");
        Optional<LobbyAction.ID> optionalActionId =
                EnumStringValueMapper.stringValueToEnum(action.getIdString(), LobbyAction.ID.class);

        if (optionalLobbyUri.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.dataFieldMissing(action.getAuthor().getUuid(), "lobbyUri");
            return new ActionHandlerResponse(errorEvent);
        }

        if (optionalActionId.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.invalidAction(action.getAuthor().getUuid(), "LOBBY", action.getIdString());
            return new ActionHandlerResponse(errorEvent);
        }

        String lobbyUri = optionalLobbyUri.get();

        Optional<Lobby> optionalLobby = lobbyService.getLobby(lobbyUri);
        if (optionalLobby.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.lobbyNotFound(action.getAuthor().getUuid(), lobbyUri);
            return new ActionHandlerResponse(errorEvent);
        }

        LobbyAction lobbyAction = new LobbyAction(action, optionalActionId.get(), optionalLobby.get());

        return lobbyActionHandler.handleAction(lobbyAction);
    }

    private ActionHandlerResponse dispatchGameAction(Action action) {
        String authorUuid = action.getAuthor().getUuid();

        Optional<String> optionalGameUri = action.getDataByKey("gameUri");
        Optional<GameAction.ID> optionalActionId =
                EnumStringValueMapper.stringValueToEnum(action.getIdString(), GameAction.ID.class);

        if (optionalGameUri.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.dataFieldMissing(authorUuid, "gameUri");
            return new ActionHandlerResponse(errorEvent);
        }

        if (optionalActionId.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.invalidAction(authorUuid, "GAME", action.getIdString());
            return new ActionHandlerResponse(errorEvent);
        }

        String gameUri = optionalGameUri.get();
        Optional<Game> optionalGame = gameService.getGame(gameUri);
        if (optionalGame.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.gameNotFound(authorUuid, gameUri);
            return new ActionHandlerResponse(errorEvent);
        }
        Game game = optionalGame.get();

        if (action instanceof ServerAction) {
            return gameActionHandler.handleGameServerAction(game, optionalActionId.get());
        } else {
            Optional<Player> optionalAuthorPlayer = game.getPlayer(authorUuid);
            if (optionalAuthorPlayer.isEmpty()) {
                ErrorEvent errorEvent = ErrorEvent.playerNotInGame(authorUuid, authorUuid, gameUri);
                return new ActionHandlerResponse(errorEvent);
            }
            Player authorPlayer = optionalAuthorPlayer.get();
            GameAction gameAction = new GameAction(action, optionalActionId.get(), game, authorPlayer);

            return gameActionHandler.handleGameAction(gameAction);
        }
    }

    private ActionHandlerResponse dispatchGeneralAction(Action action) {
        Optional<GeneralAction.ID> optionalActionId =
                EnumStringValueMapper.stringValueToEnum(action.getIdString(), GeneralAction.ID.class);

        if (optionalActionId.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.invalidAction(action.getAuthor().getUuid(), "GENERAL", action.getIdString());
            return new ActionHandlerResponse(errorEvent);
        }

        GeneralAction generalAction = new GeneralAction(action, optionalActionId.get());

        return generalActionHandler.handleGeneralAction(generalAction);
    }

    public ActionHandlerResponse handleAction(Action action) {
        switch (action.getCategory()) {
            case LOBBY: return dispatchLobbyAction(action);
            case GAME: return dispatchGameAction(action);
            case GENERAL: return dispatchGeneralAction(action);
            default: return ActionHandlerResponse.empty();
        }
    }
}
