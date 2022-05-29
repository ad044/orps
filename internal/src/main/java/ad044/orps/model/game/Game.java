package ad044.orps.model.game;

import ad044.orps.model.lobby.Lobby;
import ad044.orps.model.user.BotUserDetails;
import net.bytebuddy.utility.RandomString;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Game {
    // CONSTANTS
    public final int SECONDS_BETWEEN_ROUNDS = 2;

    // IMMUTABLE PROPS
    private final String uri = generateUri();
    private final String parentLobbyUri;
    private final GameSettings settings;

    // STATE
    private final List<Player> players;
    private int countDownValue = 5;
    private Player roundWinner = null;
    private boolean finished = false;
    private boolean roundFinished = true;
    private int roundNumber = 0;

    // TIMERS
    private long nextRoundStartTime = -1;
    private long roundFinishTime = -1;
    private long lastCountDownUpdateTime = -1;

    public Game(List<Player> players, GameSettings settings) {
        this.players = players;
        this.settings = settings;
        this.parentLobbyUri = null;
    }

    public Game(List<Player> players, GameSettings settings, String parentLobbyUri) {
        this.players = players;
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

    public List<Player> getPlayersExcept(String uuid) {
        return players.stream().filter(player -> !player.getUuid().equals(uuid)).collect(Collectors.toList());
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

    public void startNextRound(long currTime) {
        roundWinner = null;

        roundFinished = false;

        roundFinishTime = currTime + getSettings().getTimeForMove() * 1000L;

        roundNumber++;

        players.forEach(player -> {
            if (player instanceof BotPlayer) {
                player.move = GameMove.getRandomMove();
            } else {
                player.move = GameMove.NO_MOVE;
            }
        });
    }

    public void finishRound(long currTime) {
        roundFinished = true;

        nextRoundStartTime = currTime + SECONDS_BETWEEN_ROUNDS * 1000;

        Optional<Player> winner = determineWinner(players);

        winner.ifPresent(winnerPlayer -> {
            roundWinner = winnerPlayer;
            winnerPlayer.score++;
            if (winnerPlayer.score == getSettings().getScoreGoal()) {
                finished = true;
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

    public long getRoundFinishTime() {
        return roundFinishTime;
    }

    public long getNextRoundStartTime() {
        return nextRoundStartTime;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isRoundFinished() {
        return roundFinished;
    }

    public int getCountDownValue() {
        return countDownValue;
    }

    public void decrementCountDownValue(long currTime) {
        countDownValue--;
        lastCountDownUpdateTime = currTime;
    }

    public long getLastCountDownUpdateTime() {
        return lastCountDownUpdateTime;
    }
}