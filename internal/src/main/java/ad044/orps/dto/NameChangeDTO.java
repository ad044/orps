package ad044.orps.dto;

public class NameChangeDTO {
    private String username;

    public NameChangeDTO(String username) {
        this.username = username;
    }

    public NameChangeDTO() {}

    public String getUsername() {
        return username;
    }
}
