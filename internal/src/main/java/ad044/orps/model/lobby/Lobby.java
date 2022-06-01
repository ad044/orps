package ad044.orps.model.lobby;

import ad044.orps.model.user.BotUserDetails;
import ad044.orps.model.user.OrpsUserDetails;
import net.bytebuddy.utility.RandomString;

import java.util.*;
import java.util.stream.Collectors;

public class Lobby {
    private final List<OrpsUserDetails> members;
    private final String uri = generateUri();
    private final LobbySettings settings;
    private boolean isGameOngoing = false;
    public long deletionDate = -1;
    private OrpsUserDetails owner;

    public Lobby(OrpsUserDetails creator, LobbySettings settings) {
        this.owner = creator;
        this.members = new ArrayList<>(List.of(creator));
        this.settings = settings;
    }

    private static String generateUri() {
        return RandomString.make(16);
    }

    public String getUri() {
        return uri;
    }

    public List<OrpsUserDetails> getMembers() {
        return members;
    }

    public List<OrpsUserDetails> getMembersExcept(String uuid) {
        return members.stream().filter(member -> !member.getUuid().equals(uuid)).collect(Collectors.toList());
    }

    public void addMember(OrpsUserDetails user) {
        members.add(user);
    }

    public boolean hasMember(String uuid) {
        return getMember(uuid).isPresent();
    }

    public Optional<OrpsUserDetails> getMember(String uuid) {
        return members.stream().filter(user -> Objects.equals(user.getUuid(), uuid)).findFirst();
    }

    public void removeMember(String uuid) {
        members.removeIf(member -> member.getUuid().equals(uuid));
    }

    public void setOwner(OrpsUserDetails owner) {
        this.owner = owner;
    }

    public LobbySettings getSettings() {
        return settings;
    }

    public boolean isGameOngoing() {
        return isGameOngoing;
    }

    public boolean isOwner(String uuid) {
        return owner.getUuid().equals(uuid);
    }

    public void setGameOngoing(boolean gameOngoing) {
        isGameOngoing = gameOngoing;
    }

    public List<OrpsUserDetails> getNonBotMembers() {
        return members.stream()
                .filter(member -> !(member instanceof BotUserDetails)).collect(Collectors.toList());
    }
}
