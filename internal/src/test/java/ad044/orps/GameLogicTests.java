package ad044.orps;

import ad044.orps.model.game.Game;
import ad044.orps.model.game.GameMove;
import ad044.orps.model.game.GameSettings;
import ad044.orps.model.game.Player;
import ad044.orps.model.user.OrpsUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GameLogicTests {
    Logger logger = LoggerFactory.getLogger(GameLogicTests.class);
    private Game game;
    private String gameUri;
    private final String PARENT_LOBBY_URI = "lobbyUri";

    @BeforeEach
    public void reinitialize() {
        GameSettings gameSettings = new GameSettings(3, 5);
        Player player1 = new Player(new OrpsUserDetails("user1", "uuid1"));
        Player player2 = new Player(new OrpsUserDetails("user2", "uuid2"));

        List<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);

        this.game = new Game(players, gameSettings, PARENT_LOBBY_URI);
        this.gameUri = game.getUri();
    }

    @Test
    public void gameInitializesProperly() {
        assertEquals(game.getUri(), gameUri);

        assertTrue(game.getParentLobbyUri().isPresent() && game.getParentLobbyUri().get().equals(PARENT_LOBBY_URI));

        assertEquals(game.getSettings().getTimeForMove(), 3);
        assertEquals(game.getSettings().getScoreGoal(), 5);

        assertFalse(game.isFinished());

        assertTrue(game.isRoundFinished());

        assertEquals(game.getRoundNumber(), 0);

        assertEquals(game.getPlayers().size(), 2);

        game.getPlayers().forEach(player -> {
            assertEquals(player.move, GameMove.NO_MOVE);
            assertEquals(player.score, 0);
        });
    }

    @Test
    public void determineWinnerWorksWithTwoPeople() {
        assertEquals(Game.determineWinner(game.getPlayers()), Optional.empty());

        assertTrue(game.getPlayer("uuid1").isPresent());
        assertTrue(game.getPlayer("uuid2").isPresent());

        Player player1 = game.getPlayer("uuid1").get();
        Player player2 = game.getPlayer("uuid2").get();

        // player2 has NO_MOVE by default
        player1.move = GameMove.ROCK;

        List<Player> players = game.getPlayers();
        assertTrue(Game.determineWinner(players).isPresent() && Game.determineWinner(players).get().equals(player1));

        player1.move = GameMove.PAPER;
        player2.move = GameMove.SCISSORS;

        assertTrue(Game.determineWinner(players).isPresent() && Game.determineWinner(players).get().equals(player2));
    }

    @Test
    public void determineWinnerWorks() {
        Player player1 = new Player(new OrpsUserDetails("user1", "uuid1"));
        Player player2 = new Player(new OrpsUserDetails("user2", "uuid2"));
        Player player3 = new Player(new OrpsUserDetails("user3", "uuid3"));
        Player player4 = new Player(new OrpsUserDetails("user4", "uuid4"));

        List<Player> players = List.of(player1, player2, player3, player4);

        assertEquals(Game.determineWinner(players), Optional.empty());

        player1.move = GameMove.ROCK;
        assertTrue(Game.determineWinner(players).isPresent() && Game.determineWinner(players).get().equals(player1));

        player1.move = GameMove.ROCK;
        player2.move = GameMove.ROCK;
        assertTrue(Game.determineWinner(players).isEmpty());

        player1.move = GameMove.ROCK;
        player2.move = GameMove.ROCK;
        player3.move = GameMove.PAPER;
        assertTrue(Game.determineWinner(players).isPresent() && Game.determineWinner(players).get().equals(player3));

        player1.move = GameMove.ROCK;
        player2.move = GameMove.ROCK;
        player3.move = GameMove.PAPER;
        player4.move = GameMove.PAPER;
        assertTrue(Game.determineWinner(players).isEmpty());
    }
}
