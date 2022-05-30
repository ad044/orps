package ad044.orps;

import ad044.orps.dto.PlayerDTO;
import ad044.orps.model.Category;
import ad044.orps.model.event.GameEvent;
import ad044.orps.model.game.Game;
import ad044.orps.model.game.GameMove;
import ad044.orps.model.game.GameSettings;
import ad044.orps.model.game.Player;
import ad044.orps.model.message.EventMessage;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GameLogicTests {
    Logger logger = LoggerFactory.getLogger(GameLogicTests.class);
    private Game game;
    private String gameUri;
    private final String PARENT_LOBBY_URI = "lobbyUri";

    @Autowired
    GameService gameService;

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

    private long getCurrentTimeInMillis() {
        return Calendar.getInstance().getTimeInMillis();
    }

    @Test
    private void testCountDownResponse(List<EventMessage> messages, int expectedValue) {
        assertEquals(messages.size(), 1);

        EventMessage countDownUpdateMessage = messages.get(0);
        assertNotNull(countDownUpdateMessage);

        assertEquals(countDownUpdateMessage.getCategory(), Category.GAME);
        assertEquals(countDownUpdateMessage.getRecipientUuids(), game.getPlayers().stream().map(Player::getUuid).collect(Collectors.toList()));

        GameEvent event = (GameEvent) countDownUpdateMessage.getEvent();

        assertEquals(event.getGameUri(), game.getUri());
        assertEquals(event.getId(), GameEvent.ID.COUNTDOWN_UPDATE);
        assertEquals(event.getData().get("currentTimerValue"), expectedValue);
    }

    @Test
    private long testCountDown() {
        long currTime = getCurrentTimeInMillis();

        int iterationCount = 0;
        for (int i = 5; i > -1; i--) {
            currTime += iterationCount * 1000L;

            logger.info(String.format("Testing countdown iteration: %s, Clock value: %s", iterationCount, i));

            testCountDownResponse(gameService.gameTick(game, currTime), i);
            assertEquals(game.getCountDownValue(), i - 1);

            // if i reaches 0, next event is going to be START_NEXT_ROUND, which we want to test outside the loop.
            if (i > 0) {
                List<EventMessage> nextTickAtCurrentTime = gameService.gameTick(game, currTime);
                assertEquals(nextTickAtCurrentTime.size(), 0);
                List<EventMessage> nextTick999msLater = gameService.gameTick(game, currTime + 999);
                assertEquals(nextTick999msLater.size(), 0);
            }

            iterationCount++;
        }

        return currTime;
    }

    @Test
    private long testRound(long currTime, GameMove[] moves, int[] expectedScores, int expectedRoundNumber, String expectedWinnerUuid) {
        List<EventMessage> startNextRoundResponse = gameService.gameTick(game, currTime);
        assertEquals(startNextRoundResponse.size(), 1);
        EventMessage startNextRoundEventMessage = startNextRoundResponse.get(0);

        assertEquals(startNextRoundEventMessage.getCategory(), Category.GAME);
        assertEquals(startNextRoundEventMessage.getRecipientUuids(), game.getPlayers().stream().map(Player::getUuid).collect(Collectors.toList()));

        GameEvent startNextRoundEvent = (GameEvent) startNextRoundEventMessage.getEvent();

        assertEquals(startNextRoundEvent.getId(), GameEvent.ID.START_NEXT_ROUND);
        assertEquals(startNextRoundEvent.getGameUri(), game.getUri());
        assertEquals(startNextRoundEvent.getData().get("roundNumber"), expectedRoundNumber);
        assertEquals(startNextRoundEvent.getData().get("timeToPick"), game.getSettings().getTimeForMove());

        assertEquals(game.getRoundNumber(), expectedRoundNumber);

        Player player1 = game.getPlayer("uuid1").get();
        Player player2 = game.getPlayer("uuid2").get();

        player1.move = moves[0];
        player2.move = moves[1];

        // check that round result doesn't get sent UNTIL the time needed passes
        List<EventMessage> emptyResponse = gameService.gameTick(game, currTime + game.getSettings().getTimeForMove() * 1000L - 1);
        assertEquals(emptyResponse.size(), 0);

        currTime += game.getSettings().getTimeForMove() * 1000L;
        List<EventMessage> roundResultResponse = gameService.gameTick(game, currTime);
        if (expectedWinnerUuid != null && game.getPlayer(expectedWinnerUuid).get().score == game.getSettings().getScoreGoal()) {
            // this branch means that the game is finished, so we check for PlayerWonGameEvent aswell.
            assertEquals(roundResultResponse.size(), 2);

            EventMessage playerWonGameMessage = roundResultResponse.get(1);

            assertEquals(playerWonGameMessage.getCategory(), Category.GAME);
            assertEquals(playerWonGameMessage.getRecipientUuids(), game.getPlayers().stream().map(Player::getUuid).collect(Collectors.toList()));

            GameEvent playerWonGameEvent = (GameEvent) playerWonGameMessage.getEvent();

            assertEquals(playerWonGameEvent.getGameUri(), game.getUri());
            assertEquals(((PlayerDTO) playerWonGameEvent.getData().get("gameWinner")).getUuid(), expectedWinnerUuid);
        } else {
            assertEquals(roundResultResponse.size(), 1);
        }
        EventMessage roundResultMessage = roundResultResponse.get(0);

        assertEquals(roundResultMessage.getCategory(), Category.GAME);
        assertEquals(roundResultMessage.getRecipientUuids(), game.getPlayers().stream().map(Player::getUuid).collect(Collectors.toList()));

        GameEvent roundResultEvent = (GameEvent) roundResultMessage.getEvent();
        assertEquals(roundResultEvent.getId(), GameEvent.ID.RECEIVE_ROUND_RESULT);
        assertEquals(roundResultEvent.getGameUri(), game.getUri());

        List<PlayerDTO> playerData = (List<PlayerDTO>) roundResultEvent.getData().get("playerData");
        assertEquals(playerData.size(), 2);
        assertEquals((playerData.get(0)).getScore(), expectedScores[0]);
        assertEquals((playerData.get(1)).getScore(), expectedScores[1]);

        if (expectedWinnerUuid != null) {
            assertEquals(((PlayerDTO) roundResultEvent.getData().get("winner")).getUuid(), expectedWinnerUuid);
        } else {
            assertNull((roundResultEvent.getData().get("winner")));
        }

        List<EventMessage> emptyResponse2 = gameService.gameTick(game, currTime + game.SECONDS_BETWEEN_ROUNDS * 1000 - 1);
        assertEquals(emptyResponse2.size(), 0);

        return currTime + game.SECONDS_BETWEEN_ROUNDS * 1000;
    }

    @Test
    public void testGameFull() {
        long currTime = testCountDown();
        currTime = testRound(currTime,
                new GameMove[] {GameMove.ROCK, GameMove.SCISSORS},
                new int[] {1, 0},
                1,
                "uuid1");
        currTime = testRound(currTime,
                new GameMove[] {GameMove.SCISSORS, GameMove.ROCK},
                new int[] {1, 1},
                2,
                "uuid2");
        currTime = testRound(currTime,
                new GameMove[] {GameMove.PAPER, GameMove.PAPER},
                new int[] {1, 1},
                3,
                null);
        currTime = testRound(currTime,
                new GameMove[] {GameMove.PAPER, GameMove.SCISSORS},
                new int[] {1, 2},
                4,
                "uuid2");
        currTime = testRound(currTime,
                new GameMove[] {GameMove.NO_MOVE, GameMove.SCISSORS},
                new int[] {1, 3},
                5,
                "uuid2");
        currTime = testRound(currTime,
                new GameMove[] {GameMove.ROCK, GameMove.PAPER},
                new int[] {1, 4},
                6,
                "uuid2");
        currTime = testRound(currTime,
                new GameMove[] {GameMove.SCISSORS, GameMove.PAPER},
                new int[] {2, 4},
                7,
                "uuid1");
        currTime = testRound(currTime,
                new GameMove[] {GameMove.ROCK, GameMove.PAPER},
                new int[] {2, 5},
                8,
                "uuid2");
    }
}
