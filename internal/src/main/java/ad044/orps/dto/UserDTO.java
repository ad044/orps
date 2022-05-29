package ad044.orps.dto;

import ad044.orps.model.user.OrpsUserDetails;

public class UserDTO {
    private final String username;
    private final String uuid;

    public UserDTO(String username, String uuid) {
        this.username = username;
        this.uuid = uuid;
    }

    public static UserDTO from(OrpsUserDetails orpsUserDetails) {
        return new UserDTO(orpsUserDetails.getUsername(), orpsUserDetails.getUuid());
    }

    public String getUsername() {
        return username;
    }

    public String getUuid() {
        return uuid;
    }
}
