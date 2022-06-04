package ad044.orps.service;

import ad044.orps.model.lobby.*;
import ad044.orps.model.user.OrpsUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LobbyService {
    Logger logger = LoggerFactory.getLogger(LobbyService.class);

    Map<String, Lobby> lobbySessions = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 1000 * 60)
    public void lobbyCleanupTask() {
        long currTime = Calendar.getInstance().getTimeInMillis();
        lobbySessions.values().forEach(lobby -> {
            if (lobby.deletionDate != -1 && currTime >= lobby.deletionDate) {
                logger.info(String.format("Deleted lobby %s during cleanup.", lobby.getUri()));
                lobbySessions.remove(lobby.getUri());
            }
        });
    }

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
}
