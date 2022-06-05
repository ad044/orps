package ad044.orps.model.game;

import ad044.orps.model.lobby.Lobby;
import ad044.orps.model.user.BotUserDetails;
import net.bytebuddy.utility.RandomString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Game {
    // CONSTANTS
    public final int SECONDS_BETWEEN_ROUNDS = 2;
    // After this many rounds of not selecting a move, the player will get kicked.
    public final int MISSED_MOVE_THRESHOLD = 3;

    // IMMUTABLE PROPS
    private final String uri = generateUri();
    private final String parentLobbyUri;
    private final GameSettings settings;

    // STATE
    public int countDownValue = 5;
    private final List<Player> players;
    private Player roundWinner = null;
    private boolean finished = false;
    private boolean roundFinished = true;
    private int roundNumber = 0;

    public Game(List<Player> players, GameSettings settings) {
        this.players = new ArrayList<>(players);
        this.settings = settings;
        this.parentLobbyUri = null;
    }

    public Game(List<Player> players, GameSettings settings, String parentLobbyUri) {
        this.players = new ArrayList<>(players);
        this.settings = settings;
        this.parentLobbyUri = parentLobbyUri;
    }

    public static Game from(Lobby lobby) {
        List<Player> players = lobby.getMembers()
                .stream()
                .map(member -> member instanceof BotUserDetails ? new BotPlayer((BotUserDetails) member) : new Player(member))
                .collect(Collectors.toList());

        return new Game(players, lobby.getSettings(), lobby.getUri());
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<String> getPlayerUuids() {
        return players.stream().map(Player::getUuid).collect(Collectors.toList());
    }

    public List<String> getPlayerUuidsExcept(String ignoredUuid) {
        return players
                .stream()
                .map(Player::getUuid)
                .filter(uuid -> !uuid.equals(ignoredUuid))
                .collect(Collectors.toList());
    }

    public List<Player> getPlayersExcept(String uuid) {
        return players.stream().filter(player -> !player.getUuid().equals(uuid)).collect(Collectors.toList());
    }

    public List<Player> getInactivePlayers() {
        return players.stream().filter(player -> player.inactive).collect(Collectors.toList());
    }

    public Optional<Player> getPlayer(String uuid) {
        return players.stream().filter(player -> Objects.equals(player.getUuid(), uuid)).findFirst();
    }

    public GameSettings getSettings() {
        return settings;
    }

    public Optional<String> getParentLobbyUri() {
        return Optional.ofNullable(parentLobbyUri);
    }

    public String getUri() {
        return uri;
    }

    public boolean hasPlayer(String uuid) {
        return getPlayer(uuid).isPresent();
    }

    public void removePlayer(String uuid) {
        players.removeIf(player -> player.getUuid().equals(uuid));
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void startNextRound() {
        roundWinner = null;

        roundFinished = false;

        roundNumber++;

        players.forEach(player -> {
            if (player instanceof BotPlayer) {
                player.move = GameMove.getRandomMove();
            } else {
                player.move = GameMove.NO_MOVE;
            }
        });
    }

    public void finishRound() {
        roundFinished = true;

        Optional<Player> winner = determineWinner(players);

        winner.ifPresent(winnerPlayer -> {
            roundWinner = winnerPlayer;
            winnerPlayer.score++;
            if (winnerPlayer.score == getSettings().getScoreGoal()) {
                finished = true;
            }
        });

        getPlayers().forEach(player -> {
            if (player.move.equals(GameMove.NO_MOVE)) {
                player.consecutiveMovesMissed++;
                if (player.consecutiveMovesMissed == MISSED_MOVE_THRESHOLD) {
                    player.inactive = true;
                }
            } else {
                player.consecutiveMovesMissed = 0;
                player.inactive = false;
            }
        });
    }

    public static Optional<Player> determineWinner(List<Player> players) {
        Player winner = null;
        for (Player player : players) {
            boolean beatsAll = players
                    .stream()
                    .filter(otherPlayer -> !otherPlayer.getUuid().equals(player.getUuid()))
                    .allMatch(otherPlayer -> player.move.beats(otherPlayer.move));

            if (beatsAll) {
                winner = player;
            }
        }

        return Optional.ofNullable(winner);
    }

    private static String generateUri() {
        return RandomString.make(16);
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public Optional<Player> getRoundWinner() {
        return Optional.ofNullable(roundWinner);
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isRoundFinished() {
        return roundFinished;
    }
}