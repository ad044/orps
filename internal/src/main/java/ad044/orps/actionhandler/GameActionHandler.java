package ad044.orps.actionhandler;

import ad044.orps.dto.PlayerDTO;
import ad044.orps.model.ActionHandlerResponse;
import ad044.orps.model.action.GameAction;
import ad044.orps.model.action.ScheduledAction;
import ad044.orps.model.action.ServerAction;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.Event;
import ad044.orps.model.event.GameEvent;
import ad044.orps.model.game.Game;
import ad044.orps.model.game.GameMove;
import ad044.orps.model.game.Player;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GameActionHandler {
    Logger logger = LoggerFactory.getLogger(GameActionHandler.class);

    @Autowired
    GameService gameService;

    private List<Event<?>> getRoundResult(Game game) {
        List<Player> inactivePlayers = game.getInactivePlayers();

        if (inactivePlayers.size() == game.getPlayers().size()) {
            logger.info(String.format("Ended game %s prematurely because all players were inactive.", game.getUri()));
            GameEvent event = endGamePrematurely(game, "Game ended because all players were inactive.");
            return Collections.singletonList(event);
        }

        List<Event<?>> events = new ArrayList<>();
        inactivePlayers.forEach(inactivePlayer -> {
            logger.info(String.format("Kicked player %s due to inactivity.", inactivePlayer.getUuid()));
            events.addAll(kickPlayer(game, inactivePlayer.getUuid()));
        });

        List<PlayerDTO> playerData = game.getPlayers().stream().map(PlayerDTO::from).collect(Collectors.toList());

        game.getRoundWinner().ifPresentOrElse(winner -> {
            GameEvent roundResultEvent
                    = GameEvent.receiveRoundResult(game.getPlayerUuids(), game.getUri(), playerData, PlayerDTO.from(winner));
            events.add(roundResultEvent);

            if (game.isFinished()) {
                logger.info(String.format("Game %s finished", game.getUri()));
                GameEvent playerWonGameEvent = GameEvent.playerWonGame(game.getPlayerUuids(), game.getUri(), PlayerDTO.from(winner));
                events.add(playerWonGameEvent);
            }
        }, () -> {
            GameEvent roundResultEvent = GameEvent.receiveRoundResult(game.getPlayerUuids(), game.getUri(), playerData);
            events.add(roundResultEvent);
        });

        return events;
    }

    private GameEvent endGamePrematurely(Game game, String reason) {
        gameService.removeGame(game.getUri());

        return GameEvent.endedPrematurely(game.getPlayerUuids(), game.getUri(), reason);
    }

    private List<Event<?>> kickPlayer(Game game, String kickedPlayerUuid) {
        List<Event<?>> kickMessages = new ArrayList<>(handlePlayerLeave(game, kickedPlayerUuid));

        GameEvent gotKickedEvent = game.getParentLobbyUri()
                .map(lobbyUri -> GameEvent.gotKicked(kickedPlayerUuid, game.getUri(), lobbyUri))
                .orElse(GameEvent.gotKicked(kickedPlayerUuid, game.getUri()));
        kickMessages.add(gotKickedEvent);

        return kickMessages;
    }

    private List<Event<?>> handleSubmitMove(Game game, Player authorPlayer, GameMove move) {
        String authorUuid = authorPlayer.getUuid();

        if (game.isRoundFinished()) {
            ErrorEvent errorEvent = ErrorEvent.roundAlreadyFinished(authorUuid, game.getUri());
            return Collections.singletonList(errorEvent);
        }

        authorPlayer.move = move;

        List<Event<?>> events = new ArrayList<>();

        GameEvent playerMadeMoveEvent = GameEvent.playerMadeMove(game.getPlayerUuidsExcept(authorUuid), game.getUri(), authorUuid);
        events.add(playerMadeMoveEvent);

        GameEvent displayAuthorMoveEvent = GameEvent.displayAuthorMove(authorUuid, game.getUri(), authorUuid, move);
        events.add(displayAuthorMoveEvent);

        return events;
    }

    public List<Event<?>> handlePlayerLeave(Game game, String authorUuid) {
        game.removePlayer(authorUuid);

        if (game.getPlayers().size() == 0) {
            gameService.removeGame(game.getUri());
            logger.info(String.format("Deleted game %s because all players left.", game.getUri()));

            return Collections.emptyList();
        }

        List<Event<?>> events = new ArrayList<>();

        GameEvent leaveEvent = GameEvent.playerLeave(game.getPlayerUuids(), game.getUri(), authorUuid);
        events.add(leaveEvent);

        if (game.getPlayers().size() == 1) {
            logger.info(String.format("Game %s finished because only 1 player was left.", game.getUri()));
            GameEvent playerWonGameEvent
                    = GameEvent.playerWonGame(game.getPlayerUuids(), game.getUri(), PlayerDTO.from(game.getPlayers().get(0)));
            events.add(playerWonGameEvent);
        }

        return events;
    }

    private ActionHandlerResponse handleFinishRound(Game game) {
        game.finishRound();

        if (game.isFinished()) {
            gameService.removeGame(game.getUri());
        }

        ServerAction updateCountdownAction = ServerAction.game("START_NEXT_ROUND", game.getUri());
        ScheduledAction scheduledAction
                = new ScheduledAction(updateCountdownAction, Calendar.getInstance().getTimeInMillis() + 2500);

        return new ActionHandlerResponse(getRoundResult(game), scheduledAction);
    }

    private ActionHandlerResponse handleUpdateCountdown(Game game) {
        GameEvent countdownUpdateEvent = GameEvent.countdownUpdate(game.getPlayerUuids(), game.getUri(), game.getCountDownValue());
        game.countDownValue--;

        if (game.countDownValue >= 0) {
            ServerAction updateCountdownAction = ServerAction.game("UPDATE_COUNTDOWN", game.getUri());
            ScheduledAction scheduledAction
                    = new ScheduledAction(updateCountdownAction, Calendar.getInstance().getTimeInMillis() + 1000);
            return new ActionHandlerResponse(countdownUpdateEvent, scheduledAction);
        } else {
            ServerAction updateCountdownAction = ServerAction.game("START_NEXT_ROUND", game.getUri());
            ScheduledAction scheduledAction
                    = new ScheduledAction(updateCountdownAction, Calendar.getInstance().getTimeInMillis() + 1000);
            return new ActionHandlerResponse(countdownUpdateEvent, scheduledAction);
        }
    }

    private ActionHandlerResponse handleStartNextRound(Game game) {
        game.startNextRound();

        ServerAction finishRoundAction = ServerAction.game("FINISH_ROUND", game.getUri());
        long executionTime = Calendar.getInstance().getTimeInMillis() + game.getSettings().getTimeForMove() * 1000L;
        ScheduledAction scheduledAction = new ScheduledAction(finishRoundAction, executionTime);

        GameEvent startNextRoundEvent
                = GameEvent.startNextRound(game.getPlayerUuids(), game.getUri(), game.getRoundNumber(), game.getSettings().getTimeForMove());

        return new ActionHandlerResponse(startNextRoundEvent, scheduledAction);
    }

    // for server-emitted actions
    public ActionHandlerResponse handleGameServerAction(Game game, GameAction.ID actionId) {
        switch (actionId) {
            case FINISH_ROUND: {
                return handleFinishRound(game);
            }
            case UPDATE_COUNTDOWN: {
                return handleUpdateCountdown(game);
            }
            case START_NEXT_ROUND: {
                return handleStartNextRound(game);
            }
            default: {
                // TODO this should be an error
                return ActionHandlerResponse.empty();
            }
        }
    }

    // for client-requested actions
    public ActionHandlerResponse handleGameAction(GameAction action) {
        GameAction.ID actionId = action.getId();
        OrpsUserDetails author = action.getAuthor();
        Game game = action.getTargetGame();
        Player player = action.getAuthorPlayer();

        switch (actionId) {
            case SUBMIT_MOVE: {
                Optional<String> optionalMoveString = action.getDataByKey("move");
                if (optionalMoveString.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing(author.getUuid(), "move");
                    return new ActionHandlerResponse(errorEvent);
                }

                String moveString = optionalMoveString.get();
                Optional<GameMove> optionalMove = GameMove.parseMove(moveString);
                if (optionalMove.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.invalidMove(author.getUuid(), game.getUri(), moveString);
                    return new ActionHandlerResponse(errorEvent);
                }

                List<Event<?>> submitMoveEvents = handleSubmitMove(game, player, optionalMove.get());
                return new ActionHandlerResponse(submitMoveEvents);
            }
            case PLAYER_LEAVE: {
                List<Event<?>> playerLeaveEvents = handlePlayerLeave(game, author.getUuid());
                return new ActionHandlerResponse(playerLeaveEvents);
            } default: {
                // TODO this should be an error
                return ActionHandlerResponse.empty();
            }
        }
    }
}
