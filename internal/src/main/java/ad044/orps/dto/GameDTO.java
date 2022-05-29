package ad044.orps.dto;

import ad044.orps.model.game.Game;
import ad044.orps.model.game.GameSettings;

import java.util.List;
import java.util.stream.Collectors;

public class GameDTO {
    private final List<PlayerDTO> players;
    private final String parentLobbyUri;
    private final String uri;
    private final GameSettings settings;

    private GameDTO(String uri, List<PlayerDTO> players, GameSettings settings, String parentLobbyUri) {
        this.players = players;
        this.settings = settings;
        this.parentLobbyUri = parentLobbyUri;
        this.uri = uri;
    }
    private GameDTO(String uri, List<PlayerDTO> players, GameSettings settings) {
        this.players = players;
        this.settings = settings;
        this.parentLobbyUri = null;
        this.uri = uri;
    }

    public static GameDTO from(Game game) {
        List<PlayerDTO> playerDTOS = game.getPlayers().stream().map(PlayerDTO::from).collect(Collectors.toList());
        GameSettings gameSettings = game.getSettings();

        return game.getParentLobbyUri()
                .map(parentLobbyUri -> new GameDTO(game.getUri(), playerDTOS, gameSettings, parentLobbyUri))
                .orElse(new GameDTO(game.getUri(), playerDTOS, gameSettings));
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public GameSettings getSettings() {
        return settings;
    }

    public String getParentLobbyUri() {
        return parentLobbyUri;
    }

    public String getUri() {
        return uri;
    }
}
