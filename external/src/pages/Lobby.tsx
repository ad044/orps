import { Message } from "@stomp/stompjs";
import React, { useCallback, useReducer} from "react";
import { useStompClient, useSubscription } from "react-stomp-hooks";
import { LobbyEntity } from "../types";
import { sendLobbyAction } from "../utils/stompUtils";
import { isLobbyEvent, isGameEntity } from "../utils/typeGuards";
import BlueButton from "../components/BlueButton";
import LobbyConfigCheckbox from "../components/LobbyConfigCheckbox";
import Header from "../components/Header";
import LobbyChat from "../components/LobbyChat";
import LobbyConfigSlider from "../components/LobbyConfigSlider";
import { useLocation, useNavigate } from "react-router-dom";
import reducer from "../reducers/lobbyReducer";

const Lobby = () => {
  const stompClient = useStompClient();

  const { uri, settings, users } = useLocation().state as LobbyEntity;

  const [state, dispatch] = useReducer(reducer, {
    users: users,
    settings: settings,
  });

  const navigate = useNavigate();

  const handleLobbyReply = useCallback(
    (message: Message) => {
      const messageBody: unknown = JSON.parse(message.body);

      console.log(messageBody);
      if (!isLobbyEvent(messageBody)) {
        return;
      }

      const { id, data, lobbyUri } = messageBody;

      if (lobbyUri !== uri) {
        return;
      }

      if (id === "CREATED_GAME") {
        const { gameData } = data;
        if (!isGameEntity(gameData)) {
          return;
        }
        navigate("/game", { state: gameData });
      } else {
        dispatch(messageBody);
      }
    },
    [navigate, uri]
  );

  const handleStartGameClick = (e: React.MouseEvent) => {
    e.preventDefault();
    if (stompClient) {
      sendLobbyAction(stompClient, "START_GAME", uri);
    }
  };

  const handleAddBotClick = (e: React.MouseEvent) => {
    e.preventDefault();
    if (stompClient) {
      sendLobbyAction(stompClient, "ADD_BOT", uri);
    }
  };

  const handleLeaveLobbyClick = (e: React.MouseEvent) => {
    e.preventDefault();
    if (stompClient) {
      sendLobbyAction(stompClient, "USER_LEAVE", uri);
      navigate("/");
    }
  };

  const updateSettings = (settingName: string, settingValue: string) => {
    if (stompClient) {
      sendLobbyAction(stompClient, "UPDATE_SETTINGS", uri, {
        settingName: settingName,
        settingValue: settingValue,
      });
    }
  };

  useSubscription("/user/queue/reply/lobby", handleLobbyReply);
  useSubscription("/user/queue/reply/error", (e) => {
    console.log(JSON.parse(e.body));
  });

  return stompClient ? (
    <>
      <Header innerText={`${uri}`} />
      <div className="grid grid-cols-2 gap-5 px-20">
        <div className="space-y-5 overflow-hidden">
          <BlueButton innerText="Start Game" onClick={handleStartGameClick} />
          <BlueButton innerText="Add Bot" onClick={handleAddBotClick} />
          <button
            className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4"
            onClick={handleLeaveLobbyClick}
          >
            Leave Lobby
          </button>
          <LobbyChat uri={uri} stompClient={stompClient} />
        </div>
        <div className="space-y-5">
          <div className="border-2 border-blue-500 bg-blue-200 text-center h-96 max-h-96 overflow-y-scroll">
            {state.users.map((user) => (
              <h1 key={user.uuid}>{user.username}</h1>
            ))}
          </div>
          <div>
            <LobbyConfigCheckbox
              name="inviteOnly"
              label="Private Lobby"
              serverValue={state.settings.inviteOnly}
              sendUpdate={updateSettings}
            />
            <LobbyConfigSlider
              name="timeForMove"
              label="Time given to choose a move (in seconds)"
              min={3}
              max={10}
              serverValue={state.settings.timeForMove}
              sendUpdate={updateSettings}
            />
            <LobbyConfigSlider
              name="scoreGoal"
              label="Score goal (player to reach this score wins)"
              min={1}
              max={50}
              serverValue={state.settings.scoreGoal}
              sendUpdate={updateSettings}
            />
          </div>
        </div>
      </div>
    </>
  ) : (
    <h1>loading</h1>
  );
};

export default Lobby;
