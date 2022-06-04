package ad044.orps.service;

import ad044.orps.model.game.*;
import ad044.orps.model.lobby.Lobby;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class GameService {
    Logger logger = LoggerFactory.getLogger(GameService.class);

    Map<String, Game> gameSessions = new ConcurrentHashMap<>();

    public Optional<Game> getGame(String uri) {
        return Optional.ofNullable(gameSessions.get(uri));
    }

    public void removeGame(String uri) {
        gameSessions.remove(uri);
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
}
