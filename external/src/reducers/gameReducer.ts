import { GameEvent, PlayerEntity } from "../types";
import { isPlayerEntity, isPlayerEntityArray } from "../utils/typeGuards";

type GameState = {
  gameWinner: PlayerEntity | null;
  players: PlayerEntity[];
  roundNumber: number;
  roundWinner: PlayerEntity | null;
  displayRoundWinner: boolean;
  startCountdown: number;
  selectedMove: string;
};

const reducer = (state: GameState, event: GameEvent): GameState => {
  const { id, data } = event;

  switch (id) {
    case "PLAYER_WON_GAME": {
      const { gameWinner } = data;
      if (!isPlayerEntity(gameWinner)) {
        return state;
      }

      return { ...state, gameWinner: gameWinner };
    }
    case "COUNTDOWN_UPDATE": {
      const { currentTimerValue } = data;

      if (typeof currentTimerValue !== "number") {
        return state;
      }

      return { ...state, startCountdown: currentTimerValue };
    }
    case "PLAYER_MADE_MOVE": {
      const { playerUuid } = data;

      if (typeof playerUuid !== "string") {
        return state;
      }

      return {
        ...state,
        players: state.players.map((player) =>
          player.uuid === playerUuid ? { ...player, move: "UNKNOWN" } : player
        ),
      };
    }
    case "DISPLAY_AUTHOR_MOVE": {
      const { authorUuid, move } = data;

      if (typeof authorUuid !== "string") {
        return state;
      }

      if (typeof move !== "string") {
        return state;
      }

      return {
        ...state,
        players: state.players.map((player) =>
          player.uuid === authorUuid ? { ...player, move: move } : player
        ),
        selectedMove: move,
      };
    }
    case "RECEIVE_ROUND_RESULT": {
      const { playerData, winner } = data;

      if (!isPlayerEntityArray(playerData)) {
        return state;
      }

      if (winner !== null && !isPlayerEntity(winner)) {
        return state;
      }

      return {
        ...state,
        displayRoundWinner: true,
        players: playerData,
        roundWinner: winner,
        selectedMove: "",
      };
    }
    case "START_NEXT_ROUND": {
      const { roundNumber, timeToPick } = data;

      if (typeof roundNumber !== "number") {
        return state;
      }

      if (typeof timeToPick !== "number") {
        return state;
      }

      return {
        ...state,
        startCountdown: 0,
        roundNumber: roundNumber,
        displayRoundWinner: false,
        roundWinner: null,
        players: state.players.map((player) =>
          player.uuid.startsWith("Bot")
            ? { ...player, move: "UNKNOWN" }
            : { ...player, move: "NO_MOVE" }
        ),
      };
    }
    default:
      return state;
  }
};

export default reducer;
