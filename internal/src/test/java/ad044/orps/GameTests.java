package ad044.orps;

import ad044.orps.dto.PlayerDTO;
import ad044.orps.model.Category;
import ad044.orps.model.action.Action;
import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.event.GameEvent;
import ad044.orps.model.game.Game;
import ad044.orps.model.game.GameMove;
import ad044.orps.model.game.GameSettings;
import ad044.orps.model.game.Player;
import ad044.orps.model.message.EventMessage;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.ActionHandlerService;
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
    ActionHandlerService actionHandlerService;

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

        // we don't need to test the game logic since it's handled inside GameLogicTests, so we just skip
        // the countdown
        while (game.getCountDownValue() > -1) {
            long currTime = Calendar.getInstance().getTimeInMillis();
            game.decrementCountDownValue(currTime);
        }
    }

    @Test
    public void action_failsWhenGameDoesntExist() {
        Action action = new Action("SUBMIT_MOVE", Category.GAME, Map.of("gameUri", "test"), player1);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(player1.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.GAME_NOT_FOUND);
        assertEquals(event.getData().get("gameUri"), "test");
    }

    @Test
    public void action_failsWhenPlayerNotInGame() {
        Player player3 = new Player(new OrpsUserDetails("player3", "player3uuid"));
        Action action = new Action("SUBMIT_MOVE", Category.GAME, Map.of("gameUri", game.getUri()), player3);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), Collections.singletonList(player3.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();
        assertEquals(event.getId(), ErrorEvent.ID.PLAYER_NOT_IN_GAME);
        assertEquals(event.getData().get("gameUri"), game.getUri());
        assertEquals(event.getData().get("playerUuid"), player3.getUuid());
    }

    @Test
    public void submitsMove() {
        game.startNextRound(Calendar.getInstance().getTimeInMillis());
        Map<String, String> data = Map.of("gameUri", game.getUri(), "move", "SCISSORS");
        Action action = new Action("SUBMIT_MOVE", Category.GAME, data, player1);

        assertEquals(player1.move, GameMove.NO_MOVE);


        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 2);

        EventMessage playerMadeMoveMessage = response.get(0);
        assertEquals(playerMadeMoveMessage.getCategory(), Category.GAME);
        assertEquals(playerMadeMoveMessage.getRecipientUuids(),
                game.getPlayersExcept(player1.getUuid())
                        .stream()
                        .map(Player::getUuid)
                        .collect(Collectors.toList()));

        GameEvent playerMadeMoveEvent = (GameEvent) playerMadeMoveMessage.getEvent();

        assertEquals(playerMadeMoveEvent.getId(), GameEvent.ID.PLAYER_MADE_MOVE);
        assertEquals(playerMadeMoveEvent.getGameUri(), game.getUri());
        assertEquals(playerMadeMoveEvent.getData().get("playerUuid"), player1.getUuid());

        EventMessage displayAuthorMoveMessage = response.get(1);
        assertEquals(displayAuthorMoveMessage.getCategory(), Category.GAME);
        assertEquals(displayAuthorMoveMessage.getRecipientUuids(), List.of(player1.getUuid()));


        GameEvent displayAuthorMoveEvent = (GameEvent) displayAuthorMoveMessage.getEvent();

        assertEquals(displayAuthorMoveEvent.getId(), GameEvent.ID.DISPLAY_AUTHOR_MOVE);
        assertEquals(displayAuthorMoveEvent.getGameUri(), game.getUri());
        assertEquals(displayAuthorMoveEvent.getData().get("authorUuid"), player1.getUuid());
        assertEquals(displayAuthorMoveEvent.getData().get("move"), GameMove.SCISSORS);

        assertEquals(player1.move, GameMove.SCISSORS);
    }

    @Test
    public void submitsMove_failsWhenNoMoveProvided() {
        game.startNextRound(Calendar.getInstance().getTimeInMillis());
        Map<String, String> data = Map.of("gameUri", game.getUri());
        Action action = new Action("SUBMIT_MOVE", Category.GAME, data, player1);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), List.of(player1.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();

        assertEquals(event.getId(), ErrorEvent.ID.DATA_FIELD_MISSING);
        assertEquals(event.getData().get("fieldName"), "move");

        assertEquals(player1.move, GameMove.NO_MOVE);
    }

    @Test
    public void submitsMove_failsWhenInvalidMove() {
        game.startNextRound(Calendar.getInstance().getTimeInMillis());
        Map<String, String> data = Map.of("gameUri", game.getUri(), "move", "invalid");
        Action action = new Action("SUBMIT_MOVE", Category.GAME, data, player1);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), List.of(player1.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();

        assertEquals(event.getId(), ErrorEvent.ID.INVALID_MOVE);
        assertEquals(event.getData().get("move"), "invalid");
        assertEquals(event.getData().get("gameUri"), game.getUri());

        assertEquals(player1.move, GameMove.NO_MOVE);
    }

    @Test
    public void submitsMove_failsWhenRoundFinished() {
        Map<String, String> data = Map.of("gameUri", game.getUri(), "move", "SCISSORS");
        Action action = new Action("SUBMIT_MOVE", Category.GAME, data, player1);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage message = response.get(0);
        assertEquals(message.getCategory(), Category.ERROR);
        assertEquals(message.getRecipientUuids(), List.of(player1.getUuid()));

        ErrorEvent event = (ErrorEvent) message.getEvent();

        assertEquals(event.getId(), ErrorEvent.ID.ROUND_ALREADY_FINISHED);
        assertEquals(event.getData().get("gameUri"), game.getUri());

        assertEquals(player1.move, GameMove.NO_MOVE);
    }

    @Test
    public void leavesGame() {
        Map<String, String> data = Map.of("gameUri", game.getUri());
        Action action = new Action("PLAYER_LEAVE", Category.GAME, data, player1);

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 2);

        EventMessage playerLeaveMessage = response.get(0);
        assertEquals(playerLeaveMessage.getCategory(), Category.GAME);
        assertEquals(playerLeaveMessage.getRecipientUuids(), game.getPlayers()
                .stream()
                .map(Player::getUuid)
                .collect(Collectors.toList()));

        GameEvent playerLeaveEvent = (GameEvent) playerLeaveMessage.getEvent();

        assertEquals(playerLeaveEvent.getId(), GameEvent.ID.PLAYER_LEAVE);
        assertEquals(playerLeaveEvent.getGameUri(), game.getUri());
        assertEquals(playerLeaveEvent.getData().get("playerUuid"), player1.getUuid());

        EventMessage playerWonGameMessage = response.get(1);
        assertEquals(playerWonGameMessage.getCategory(), Category.GAME);
        assertEquals(playerWonGameMessage.getRecipientUuids(), game.getPlayers()
                .stream()
                .map(Player::getUuid)
                .collect(Collectors.toList()));

        GameEvent playerWonGameEvent = (GameEvent) playerWonGameMessage.getEvent();

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

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 1);

        EventMessage playerLeaveMessage = response.get(0);
        assertEquals(playerLeaveMessage.getCategory(), Category.GAME);
        assertEquals(playerLeaveMessage.getRecipientUuids(), game.getPlayers()
                .stream()
                .map(Player::getUuid)
                .collect(Collectors.toList()));

        GameEvent playerLeaveEvent = (GameEvent) playerLeaveMessage.getEvent();

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

        List<EventMessage> response = actionHandlerService.handleAction(action);
        assertEquals(response.size(), 0);
    }
}
