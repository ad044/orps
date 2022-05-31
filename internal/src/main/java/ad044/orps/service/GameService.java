package ad044.orps.service;

import ad044.orps.dto.PlayerDTO;
import ad044.orps.model.action.GameAction;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.GameEvent;
import ad044.orps.model.game.*;
import ad044.orps.model.lobby.Lobby;
import ad044.orps.model.message.EventMessage;
import ad044.orps.model.user.OrpsUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class GameService {
    Logger logger = LoggerFactory.getLogger(GameService.class);

    Map<String, Game> gameSessions = new ConcurrentHashMap<>();

    @Autowired
    UserMessagingService userMessagingService;

    // After this many rounds of not selecting a move, the player will get kicked.
    private final int MISSED_MOVE_THRESHOLD = 3;

    private final AtomicBoolean isDoingGameTicks = new AtomicBoolean(false);


    @PostConstruct
    private void postConstruct() {
        Runnable loop = () -> {
            while (isDoingGameTicks.get()) {
                List<EventMessage> messages = new ArrayList<>();

                gameSessions.values().forEach(game -> {
                    long currTime = Calendar.getInstance().getTimeInMillis();
                    messages.addAll(gameTick(game, currTime));
                });

                userMessagingService.sendEventMessage(messages);
            }
        };

        new Thread(loop).start();
        isDoingGameTicks.set(true);
    }

    public List<EventMessage> gameTick(Game game, long currTime) {
        if (game.getCountDownValue() >= 0) {
            List<EventMessage> messages = new ArrayList<>();

            if (currTime >= game.getLastCountDownUpdateTime() + 1000) {
                GameEvent countdownUpdateEvent = GameEvent.countdownUpdate(game.getUri(), game.getCountDownValue());
                messages.add(EventMessage.game(game.getPlayers(), countdownUpdateEvent));

                game.decrementCountDownValue(currTime);
            }

            return messages;
        }

        if (!game.isRoundFinished() && currTime >= game.getRoundFinishTime()) {
            game.finishRound(currTime);

            if (game.isFinished()) {
                gameSessions.remove(game.getUri());
            }

            return getRoundResult(game);
        }

        if (game.isRoundFinished() && currTime >= game.getNextRoundStartTime()) {
            game.startNextRound(currTime);

            GameEvent roundStartEvent =
                    GameEvent.startNextRound(game.getUri(), game.getRoundNumber(), game.getSettings().getTimeForMove());

            return Collections.singletonList(EventMessage.game(game.getPlayers(), roundStartEvent));
        }

        return Collections.emptyList();
    }


    public Optional<Game> getGame(String uri) {
        return Optional.ofNullable(gameSessions.get(uri));
    }

    public List<Game> getAllGamesWithUser(String uuid) {
        return gameSessions
                .values()
                .stream().filter(game -> game.hasPlayer(uuid))
                .collect(Collectors.toList());
    }

    public Game createLobbyGame(Lobby lobby) {
        Game game = Game.from(lobby);

        gameSessions.put(game.getUri(), game);

        logger.info(String.format("Created new game with URI: %s for lobby with URI: %s", game.getUri(), lobby.getUri()));

        return game;
    }

    public Game createGame(List<Player> players, GameSettings gameSettings) {
        Game game = new Game(players, gameSettings);

        gameSessions.put(game.getUri(), game);

        logger.info(String.format("Created new game with URI: %s", game.getUri()));

        return game;
    }


    private List<EventMessage> getRoundResult(Game game) {
        List<EventMessage> messages = new ArrayList<>();
        game.getPlayers().forEach(player -> {
            if (player.move.equals(GameMove.NO_MOVE)) {
                player.missedMoveCount++;

                if (player.missedMoveCount == MISSED_MOVE_THRESHOLD) {
                    messages.addAll(kickPlayer(game, player.getUuid()));
                }
            }
        });

        List<PlayerDTO> playerData = game.getPlayers().stream().map(PlayerDTO::from).collect(Collectors.toList());

        game.getRoundWinner().ifPresentOrElse(winner -> {
            GameEvent roundResultEvent = GameEvent.receiveRoundResult(game.getUri(), playerData, PlayerDTO.from(winner));
            messages.add(EventMessage.game(game.getPlayers(), roundResultEvent));

            if (game.isFinished()) {
                GameEvent playerWonGameEvent = GameEvent.playerWonGame(game.getUri(), PlayerDTO.from(winner));
                messages.add(EventMessage.game(game.getPlayers(), playerWonGameEvent));
            }
        }, () -> {
            GameEvent roundResultEvent = GameEvent.receiveRoundResult(game.getUri(), playerData);
            messages.add(EventMessage.game(game.getPlayers(), roundResultEvent));
        });

        return messages;
    }

    private List<EventMessage> kickPlayer(Game game, String kickedPlayerUuid) {
        List<EventMessage> messages = handlePlayerLeave(game, kickedPlayerUuid);

        GameEvent gotKickedEvent = game.getParentLobbyUri()
                .map(lobbyUri -> GameEvent.gotKicked(game.getUri(), lobbyUri))
                .orElse(GameEvent.gotKicked(game.getUri()));
        messages.add(EventMessage.game(kickedPlayerUuid, gotKickedEvent));

        return messages;
    }

    private List<EventMessage> handleSubmitMove(Game game, Player authorPlayer, GameMove move) {
        String authorUuid = authorPlayer.getUuid();

        if (game.isRoundFinished()) {
            ErrorEvent errorEvent = ErrorEvent.roundAlreadyFinished(game.getUri());
            return Collections.singletonList(EventMessage.error(authorUuid, errorEvent));
        }

        authorPlayer.move = move;

        List<EventMessage> messages = new ArrayList<>();

        GameEvent playerMadeMoveEvent = GameEvent.playerMadeMove(game.getUri(), authorUuid);
        messages.add(EventMessage.game(game.getPlayersExcept(authorUuid), playerMadeMoveEvent));

        GameEvent displayAuthorMoveEvent = GameEvent.displayAuthorMove(game.getUri(), authorUuid, move);
        messages.add(EventMessage.game(authorUuid, displayAuthorMoveEvent));

        return messages;
    }

    public List<EventMessage> handlePlayerLeave(Game game, String authorUuid) {
        game.removePlayer(authorUuid);

        if (game.getPlayers().size() == 0) {
            gameSessions.remove(game.getUri());
            logger.info(String.format("Deleted game %s because all players left.", game.getUri()));

            return Collections.emptyList();
        }

        List<EventMessage> messages = new ArrayList<>();

        GameEvent leaveEvent = GameEvent.playerLeave(game.getUri(), authorUuid);
        messages.add(EventMessage.game(game.getPlayers(), leaveEvent));

        if (game.getPlayers().size() == 1) {
            GameEvent playerWonGameEvent = GameEvent.playerWonGame(game.getUri(), PlayerDTO.from(game.getPlayers().get(0)));
            messages.add(EventMessage.game(game.getPlayers(), playerWonGameEvent));
        }

        return messages;
    }

    public List<EventMessage> handleGameAction(GameAction action) {
        GameAction.ID actionId = action.getId();
        OrpsUserDetails author = action.getAuthor();
        String gameUri = action.getTargetGameUri();

        Optional<Game> optionalGame = getGame(gameUri);
        if (optionalGame.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.gameNotFound(gameUri);
            return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
        }
        Game game = optionalGame.get();

        Optional<Player> optionalAuthorPlayer = game.getPlayer(author.getUuid());
        if (optionalAuthorPlayer.isEmpty()) {
            ErrorEvent errorEvent = ErrorEvent.playerNotInGame(author.getUuid(), gameUri);
            return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
        }
        Player authorPlayer = optionalAuthorPlayer.get();

        switch (actionId) {
            case SUBMIT_MOVE: {
                Optional<String> optionalMoveString = action.getDataByKey("move");
                if (optionalMoveString.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.dataFieldMissing("move");
                    return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
                }

                String moveString = optionalMoveString.get();
                Optional<GameMove> optionalMove = GameMove.parseMove(moveString);
                if (optionalMove.isEmpty()) {
                    ErrorEvent errorEvent = ErrorEvent.invalidMove(gameUri, moveString);
                    return Collections.singletonList(EventMessage.error(author.getUuid(), errorEvent));
                }

                return handleSubmitMove(game, authorPlayer, optionalMove.get());
            }
            case PLAYER_LEAVE: {
                return handlePlayerLeave(game, author.getUuid());
            } default: {
                return Collections.emptyList();
            }
        }
    }
}
