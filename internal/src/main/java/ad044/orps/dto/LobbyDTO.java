package ad044.orps.dto;

import ad044.orps.model.lobby.Lobby;
import ad044.orps.model.lobby.LobbySettings;

import java.util.List;
import java.util.stream.Collectors;

public class LobbyDTO {
    private final List<UserDTO> users;
    private final String uri;
    private final LobbySettings settings;

    public LobbyDTO(List<UserDTO> users, String uri, LobbySettings settings) {
        this.users = users;
        this.uri = uri;
        this.settings = settings;
    }

    public static LobbyDTO from(Lobby lobby) {
        List<UserDTO> userDTOS = lobby.getMembers().stream().map(UserDTO::from).collect(Collectors.toList());

        return new LobbyDTO(userDTOS, lobby.getUri(), lobby.getSettings());
    }

    public String getUri() {
        return uri;
    }

    public List<UserDTO> getUsers() {
        return users;
    }

    public LobbySettings getSettings() {
        return settings;
    }
}
