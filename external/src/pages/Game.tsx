import { Message } from "@stomp/stompjs";
import { useCallback, useMemo, useReducer } from "react";
import { useStompClient, useSubscription } from "react-stomp-hooks";
import { sendGameAction } from "../utils/stompUtils";
import { isGameEvent } from "../utils/typeGuards";
import useTimer from "../hooks/useTimer";
import useWindowDimensions from "../hooks/useWindowDimensions";
import reducer from "../reducers/gameReducer";
import { GameEntity } from "../types";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import scissorsImg from "../static/img/scissors.png";
import rockImg from "../static/img/rock.png";
import paperImg from "../static/img/paper.png";
import crossImg from "../static/img/cross.png";
import questionMarkImg from "../static/img/questionmark.png";
import BlueButton from "../components/BlueButton";

const Game = () => {
  const stompClient = useStompClient();

  const navigate = useNavigate();

  const { uri, players, settings, parentLobbyUri } = useLocation()
    .state as GameEntity;

  const [state, dispatch] = useReducer(reducer, {
    players: players,
    roundNumber: 1,
    roundWinner: null,
    displayRoundWinner: false,
    startCountdown: 5,
    selectedMove: "NO_MOVE",
    gameWinner: null,
  });

  const moveImgMapping: { [key: string]: string } = useMemo(
    () => ({
      ROCK: rockImg,
      PAPER: paperImg,
      SCISSORS: scissorsImg,
      NO_MOVE: crossImg,
      UNKNOWN: questionMarkImg,
    }),
    []
  );

  const { time, stopTimer, startTimer } = useTimer();

  const handleMoveClick = (moveName: string) => {
    if (stompClient) {
      sendGameAction(stompClient, "SUBMIT_MOVE", uri, {
        move: moveName,
      });
    }
  };

  const handleEventReply = useCallback(
    (message: Message) => {
      const messageBody: unknown = JSON.parse(message.body);

      console.log(messageBody);

      if (!isGameEvent(messageBody)) {
        return;
      }

      const { id, data, gameUri } = messageBody;

      if (gameUri !== uri) {
        return;
      }

      // apply reducer
      dispatch(messageBody);

      // additional side effects
      switch (id) {
        case "START_NEXT_ROUND": {
          const { timeToPick } = data;

          if (typeof timeToPick !== "number") {
            return;
          }

          startTimer(timeToPick);
          break;
        }
      }
    },
    [startTimer, uri]
  );

  useSubscription("/user/queue/reply/game", handleEventReply);
  useSubscription("/user/queue/reply/error", (e) => {
    console.log(e.body);
  });

  const { width, height } = useWindowDimensions();
  const radius = useMemo(() => width / 5, [width]);

  const headerText = useMemo(() => {
    if (state.gameWinner) {
      return `${state.gameWinner.username} won the game.`;
    }

    if (state.startCountdown > 0) {
      return `Game starting in ${state.startCountdown}...`;
    }

    if (state.displayRoundWinner) {
      if (state.roundWinner) {
        return `${state.roundWinner.username} won the round.`;
      } else {
        return "Draw.";
      }
    } else {
      return `${time} seconds left to pick...`;
    }
  }, [
    state.displayRoundWinner,
    state.gameWinner,
    state.roundWinner,
    state.startCountdown,
    time,
  ]);

  return (
    <>
      <div className="text-4xl text-center">{headerText}</div>
      <div className="mt-5 text-3xl text-center">Round {state.roundNumber}</div>
      {state.players.map((player, idx) => {
        const angle = ((2 * Math.PI) / state.players.length) * idx;
        const x = Math.round(width / 2.5 + radius * Math.cos(angle));
        const y = Math.round(height / 2.5 + (radius / 2) * Math.sin(angle));
        return (
          <div
            key={idx}
            style={{
              position: "fixed",
              left: `${x}px`,
              top: `${y}px`,
              textAlign: "center",
            }}
          >
            {player.username}
            <br />
            Score: {player.score}
            <br />
            <img
              src={moveImgMapping[player.move]}
              className="h-36"
              alt={player.move}
            />
          </div>
        );
      })}

      <div className="grid grid-flow-col absolute bottom-0 left-1/2 transform -translate-x-1/2">
        {!state.gameWinner ? (
          state.startCountdown === 0 &&
          ["ROCK", "PAPER", "SCISSORS"].map((move) => (
            <button
              onClick={() => handleMoveClick(move)}
              name={move}
              className={`${
                state.selectedMove === move ? "outline outline-2" : ""
              }`}
            >
              <img src={moveImgMapping[move]} className="h-56" alt={move} />
            </button>
          ))
        ) : (
          <div className="text-4xl">
            {/* {parentLobbyUri && <BlueButton innerText={`Go back to Lobby ${parentLobbyUri}`} onClick={}/>} */}
            <BlueButton
              innerText="Back to Home"
              onClick={() => {
                navigate("/");
              }}
            />
          </div>
        )}
      </div>
    </>
  );
};

export default Game;
