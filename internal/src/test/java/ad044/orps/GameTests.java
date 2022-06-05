package ad044.orps;

import ad044.orps.dto.PlayerDTO;
import ad044.orps.model.ActionHandlerResponse;
import ad044.orps.model.Category;
import ad044.orps.model.action.Action;
import ad044.orps.model.action.ScheduledAction;
import ad044.orps.model.action.ServerAction;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.Event;
import ad044.orps.model.event.GameEvent;
import ad044.orps.model.game.Game;
import ad044.orps.model.game.GameMove;
import ad044.orps.model.game.GameSettings;
import ad044.orps.model.game.Player;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.ActionDispatcherService;
import ad044.orps.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class GameTests {
    Logger logger = LoggerFactory.getLogger(GameTests.class);

    @Autowired
    ActionDispatcherService actionDispatcherService;

    @Autowired
    GameService gameService;

    Game game;
    Player player1;
    Player player2;


    @BeforeEach
    public void reinitialize() {
        this.player1 = new Player(new OrpsUserDetails("player1", "player1uuid"));
        this.player2 = new Player(new OrpsUserDetails("player2", "player2uuid"));
        this.game = gameService.createGame(List.of(this.player1, this.player2), new GameSettings(3, 5));
    }

    @Test
    public void action_failsWhenGameDoesntExist() {
        Action action = new Action("SUBMIT_MOVE", Category.GAME, Map.of("gameUri", "test"), player1);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(player1.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.GAME_NOT_FOUND);
        assertEquals(event.getData().get("gameUri"), "test");
    }

    @Test
    public void action_failsWhenPlayerNotInGame() {
        Player player3 = new Player(new OrpsUserDetails("player3", "player3uuid"));
        Action action = new Action("SUBMIT_MOVE", Category.GAME, Map.of("gameUri", game.getUri()), player3);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), Collections.singletonList(player3.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.PLAYER_NOT_IN_GAME);
        assertEquals(event.getData().get("gameUri"), game.getUri());
        assertEquals(event.getData().get("playerUuid"), player3.getUuid());
    }

    @Test
    public void submitsMove() {
        game.startNextRound();
        Map<String, String> data = Map.of("gameUri", game.getUri(), "move", "SCISSORS");
        Action action = new Action("SUBMIT_MOVE", Category.GAME, data, player1);

        assertEquals(player1.move, GameMove.NO_MOVE);


        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 2);

        GameEvent playerMadeMoveEvent = (GameEvent) response.get(0);
        assertEquals(playerMadeMoveEvent.getCategory(), Category.GAME);
        assertEquals(playerMadeMoveEvent.getRecipientUuids(),
                game.getPlayerUuidsExcept(player1.getUuid()));
        assertEquals(playerMadeMoveEvent.getId(), GameEvent.ID.PLAYER_MADE_MOVE);
        assertEquals(playerMadeMoveEvent.getGameUri(), game.getUri());
        assertEquals(playerMadeMoveEvent.getData().get("playerUuid"), player1.getUuid());

        GameEvent displayAuthorMoveEvent = (GameEvent) response.get(1);
        assertEquals(displayAuthorMoveEvent.getCategory(), Category.GAME);
        assertEquals(displayAuthorMoveEvent.getRecipientUuids(), List.of(player1.getUuid()));
        assertEquals(displayAuthorMoveEvent.getId(), GameEvent.ID.DISPLAY_AUTHOR_MOVE);
        assertEquals(displayAuthorMoveEvent.getGameUri(), game.getUri());
        assertEquals(displayAuthorMoveEvent.getData().get("authorUuid"), player1.getUuid());
        assertEquals(displayAuthorMoveEvent.getData().get("move"), GameMove.SCISSORS);

        assertEquals(player1.move, GameMove.SCISSORS);
    }

    @Test
    public void submitsMove_failsWhenNoMoveProvided() {
        game.startNextRound();
        Map<String, String> data = Map.of("gameUri", game.getUri());
        Action action = new Action("SUBMIT_MOVE", Category.GAME, data, player1);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(player1.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.DATA_FIELD_MISSING);
        assertEquals(event.getData().get("fieldName"), "move");

        assertEquals(player1.move, GameMove.NO_MOVE);
    }

    @Test
    public void submitsMove_failsWhenInvalidMove() {
        game.startNextRound();
        Map<String, String> data = Map.of("gameUri", game.getUri(), "move", "invalid");
        Action action = new Action("SUBMIT_MOVE", Category.GAME, data, player1);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(player1.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.INVALID_MOVE);
        assertEquals(event.getData().get("move"), "invalid");
        assertEquals(event.getData().get("gameUri"), game.getUri());

        assertEquals(player1.move, GameMove.NO_MOVE);
    }

    @Test
    public void submitsMove_failsWhenRoundFinished() {
        Map<String, String> data = Map.of("gameUri", game.getUri(), "move", "SCISSORS");
        Action action = new Action("SUBMIT_MOVE", Category.GAME, data, player1);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        Event<?> event = response.get(0);
        assertEquals(event.getCategory(), Category.ERROR);
        assertEquals(event.getRecipientUuids(), List.of(player1.getUuid()));
        assertEquals(event.getId(), ErrorEvent.ID.ROUND_ALREADY_FINISHED);
        assertEquals(event.getData().get("gameUri"), game.getUri());

        assertEquals(player1.move, GameMove.NO_MOVE);
    }

    @Test
    public void leavesGame() {
        Map<String, String> data = Map.of("gameUri", game.getUri());
        Action action = new Action("PLAYER_LEAVE", Category.GAME, data, player1);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 2);

        GameEvent playerLeaveEvent = (GameEvent) response.get(0);
        assertEquals(playerLeaveEvent.getCategory(), Category.GAME);
        assertEquals(playerLeaveEvent.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(playerLeaveEvent.getId(), GameEvent.ID.PLAYER_LEAVE);
        assertEquals(playerLeaveEvent.getGameUri(), game.getUri());
        assertEquals(playerLeaveEvent.getData().get("playerUuid"), player1.getUuid());

        GameEvent playerWonGameEvent = (GameEvent) response.get(1);
        assertEquals(playerWonGameEvent.getCategory(), Category.GAME);
        assertEquals(playerWonGameEvent.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(playerWonGameEvent.getId(), GameEvent.ID.PLAYER_WON_GAME);
        assertEquals(playerWonGameEvent.getGameUri(), game.getUri());
        PlayerDTO winner = (PlayerDTO) playerWonGameEvent.getData().get("gameWinner");

        assertEquals(winner.getUuid(), player2.getUuid());

        assertEquals(game.getPlayers().size(), 1);
    }

    @Test
    public void leavesGame_whenMoreThanTwoPlayers() {
        game.addPlayer(new Player(new OrpsUserDetails("player3", "uuid3")));
        Map<String, String> data = Map.of("gameUri", game.getUri());
        Action action = new Action("PLAYER_LEAVE", Category.GAME, data, player1);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 1);

        GameEvent playerLeaveEvent = (GameEvent) response.get(0);
        assertEquals(playerLeaveEvent.getCategory(), Category.GAME);
        assertEquals(playerLeaveEvent.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(playerLeaveEvent.getId(), GameEvent.ID.PLAYER_LEAVE);
        assertEquals(playerLeaveEvent.getGameUri(), game.getUri());
        assertEquals(playerLeaveEvent.getData().get("playerUuid"), player1.getUuid());

        assertEquals(game.getPlayers().size(), 2);
    }

    @Test
    public void leavesGame_whenOnlyOnePlayer() {
        game.removePlayer(player2.getUuid());

        assertEquals(game.getPlayers().size(), 1);

        Map<String, String> data = Map.of("gameUri", game.getUri());
        Action action = new Action("PLAYER_LEAVE", Category.GAME, data, player1);

        List<Event<?>> response = actionDispatcherService.handleAction(action).getEvents();
        assertEquals(response.size(), 0);
    }

    @Test
    public void finishesRoundWithNoWinner() {
        long startTime = Calendar.getInstance().getTimeInMillis();
        game.startNextRound();

        ServerAction finishRoundAction = ServerAction.game("FINISH_ROUND", game.getUri());

        ActionHandlerResponse response = actionDispatcherService.handleAction(finishRoundAction);

        List<Event<?>> events = response.getEvents();
        List<ScheduledAction> scheduledActions = response.getScheduledActions();

        assertEquals(events.size(), 1);

        GameEvent roundResultEvent = (GameEvent) events.get(0);
        assertEquals(roundResultEvent.getCategory(), Category.GAME);
        assertEquals(roundResultEvent.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(roundResultEvent.getId(), GameEvent.ID.RECEIVE_ROUND_RESULT);
        assertEquals(roundResultEvent.getGameUri(), game.getUri());
        assertNull(roundResultEvent.getData().get("winner"));

        assertEquals(scheduledActions.size(), 1);
        ScheduledAction scheduledAction = scheduledActions.get(0);

        assertTrue(scheduledAction.getExecutionTime() >= startTime + game.SECONDS_BETWEEN_ROUNDS * 1000);
        assertEquals(scheduledAction.getAction().getCategory(), Category.GAME);
        assertEquals(scheduledAction.getAction().getIdString(), "START_NEXT_ROUND");
    }

    @Test
    public void finishesRoundWithWinner() {
        long startTime = Calendar.getInstance().getTimeInMillis();
        game.startNextRound();

        player1.move = GameMove.SCISSORS;

        ServerAction finishRoundAction = ServerAction.game("FINISH_ROUND", game.getUri());

        ActionHandlerResponse response = actionDispatcherService.handleAction(finishRoundAction);

        List<Event<?>> events = response.getEvents();
        List<ScheduledAction> scheduledActions = response.getScheduledActions();

        assertEquals(events.size(), 1);

        GameEvent roundResultEvent = (GameEvent) events.get(0);
        assertEquals(roundResultEvent.getCategory(), Category.GAME);
        assertEquals(roundResultEvent.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(roundResultEvent.getId(), GameEvent.ID.RECEIVE_ROUND_RESULT);
        assertEquals(roundResultEvent.getGameUri(), game.getUri());
        assertEquals(((PlayerDTO) roundResultEvent.getData().get("winner")).getUuid(), player1.getUuid());

        assertEquals(scheduledActions.size(), 1);
        ScheduledAction scheduledAction = scheduledActions.get(0);

        assertTrue(scheduledAction.getExecutionTime() >= startTime + 500);
        assertEquals(scheduledAction.getAction().getCategory(), Category.GAME);
        assertEquals(scheduledAction.getAction().getIdString(), "START_NEXT_ROUND");
    }

    @Test
    public void finishesGame() {
        game.startNextRound();

        player2.score = game.getSettings().getScoreGoal() - 1;
        player2.move = GameMove.ROCK;
        player1.move = GameMove.SCISSORS;

        ServerAction finishRoundAction = ServerAction.game("FINISH_ROUND", game.getUri());

        ActionHandlerResponse response = actionDispatcherService.handleAction(finishRoundAction);

        List<Event<?>> events = response.getEvents();
        List<ScheduledAction> scheduledActions = response.getScheduledActions();

        assertEquals(events.size(), 2);

        GameEvent roundResultEvent = (GameEvent) events.get(0);
        assertEquals(roundResultEvent.getCategory(), Category.GAME);
        assertEquals(roundResultEvent.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(roundResultEvent.getId(), GameEvent.ID.RECEIVE_ROUND_RESULT);
        assertEquals(roundResultEvent.getGameUri(), game.getUri());
        assertEquals(((PlayerDTO) roundResultEvent.getData().get("winner")).getUuid(), player2.getUuid());

        GameEvent wonGameEvent = (GameEvent) events.get(1);
        assertEquals(wonGameEvent.getCategory(), Category.GAME);
        assertEquals(wonGameEvent.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(wonGameEvent.getId(), GameEvent.ID.PLAYER_WON_GAME);
        assertEquals(wonGameEvent.getGameUri(), game.getUri());
        assertEquals(((PlayerDTO) wonGameEvent.getData().get("gameWinner")).getUuid(), player2.getUuid());

        assertEquals(scheduledActions.size(), 0);
    }

    @Test
    public void finishesGamePrematurely() {
        game.startNextRound();

        player2.inactive = true;
        player1.inactive = true;

        ServerAction finishRoundAction = ServerAction.game("FINISH_ROUND", game.getUri());

        ActionHandlerResponse response = actionDispatcherService.handleAction(finishRoundAction);

        List<Event<?>> events = response.getEvents();
        List<ScheduledAction> scheduledActions = response.getScheduledActions();

        assertEquals(events.size(), 1);

        GameEvent event = (GameEvent) events.get(0);
        assertEquals(event.getCategory(), Category.GAME);
        assertEquals(event.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(event.getId(), GameEvent.ID.ENDED_PREMATURELY);
        assertEquals(event.getGameUri(), game.getUri());
        assertEquals(event.getData().get("reason"), "Game ended because all players were inactive.");

        assertEquals(scheduledActions.size(), 0);
    }

    @Test
    public void updatesCountdown() {
        game.countDownValue = 5;
        for (int i = 5; i > 0; i--) {
            long startTime = Calendar.getInstance().getTimeInMillis();
            ServerAction updateCountdownAction = ServerAction.game("UPDATE_COUNTDOWN", game.getUri());

            ActionHandlerResponse response = actionDispatcherService.handleAction(updateCountdownAction);

            List<Event<?>> events = response.getEvents();
            List<ScheduledAction> scheduledActions = response.getScheduledActions();

            assertEquals(events.size(), 1);

            GameEvent event = (GameEvent) events.get(0);
            assertEquals(event.getCategory(), Category.GAME);
            assertEquals(event.getRecipientUuids(), game.getPlayerUuids());
            assertEquals(event.getId(), GameEvent.ID.UPDATE_COUNTDOWN);
            assertEquals(event.getGameUri(), game.getUri());
            assertEquals(event.getData().get("currentTimerValue"), i);

            assertEquals(game.countDownValue, i - 1);

            assertEquals(scheduledActions.size(), 1);
            ScheduledAction scheduledAction = scheduledActions.get(0);

            assertTrue(scheduledAction.getExecutionTime() >= startTime + 1000);
            assertEquals(scheduledAction.getAction().getCategory(), Category.GAME);
            assertEquals(scheduledAction.getAction().getIdString(), "UPDATE_COUNTDOWN");
        }
    }

    @Test
    public void startsRoundWhenCountdownFinishes() {
        long startTime = Calendar.getInstance().getTimeInMillis();
        game.countDownValue = -1;
        ServerAction updateCountdownAction = ServerAction.game("UPDATE_COUNTDOWN", game.getUri());

        ActionHandlerResponse response = actionDispatcherService.handleAction(updateCountdownAction);

        List<Event<?>> events = response.getEvents();
        List<ScheduledAction> scheduledActions = response.getScheduledActions();

        assertEquals(events.size(), 1);

        GameEvent event = (GameEvent) events.get(0);
        assertEquals(event.getCategory(), Category.GAME);
        assertEquals(event.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(event.getId(), GameEvent.ID.UPDATE_COUNTDOWN);
        assertEquals(event.getGameUri(), game.getUri());
        assertEquals(event.getData().get("currentTimerValue"), -1);

        assertEquals(scheduledActions.size(), 1);
        ScheduledAction scheduledAction = scheduledActions.get(0);

        assertTrue(scheduledAction.getExecutionTime() >= startTime + 1000);
        assertEquals(scheduledAction.getAction().getCategory(), Category.GAME);
        assertEquals(scheduledAction.getAction().getIdString(), "START_NEXT_ROUND");
    }

    @Test
    public void startsNextRound() {
        long startTime = Calendar.getInstance().getTimeInMillis();
        ServerAction startNextRoundAction = ServerAction.game("START_NEXT_ROUND", game.getUri());

        ActionHandlerResponse response = actionDispatcherService.handleAction(startNextRoundAction);

        List<Event<?>> events = response.getEvents();
        List<ScheduledAction> scheduledActions = response.getScheduledActions();

        assertEquals(events.size(), 1);

        GameEvent event = (GameEvent) events.get(0);
        assertEquals(event.getCategory(), Category.GAME);
        assertEquals(event.getRecipientUuids(), game.getPlayerUuids());
        assertEquals(event.getId(), GameEvent.ID.START_NEXT_ROUND);
        assertEquals(event.getGameUri(), game.getUri());
        assertEquals(event.getData().get("timeToPick"), game.getSettings().getTimeForMove());
        assertEquals(event.getData().get("roundNumber"), 1);

        assertEquals(scheduledActions.size(), 1);
        ScheduledAction scheduledAction = scheduledActions.get(0);

        assertTrue(scheduledAction.getExecutionTime() >= startTime + game.getSettings().getTimeForMove() * 1000L);
        assertEquals(scheduledAction.getAction().getCategory(), Category.GAME);
        assertEquals(scheduledAction.getAction().getIdString(), "FINISH_ROUND");
    }
}
