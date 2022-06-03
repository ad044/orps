import React, { ChangeEvent, FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useStompClient, useSubscription } from "react-stomp-hooks";
import { sendGeneralAction, sendLobbyAction } from "../utils/stompUtils";
import { isEvent, isLobbyEntity } from "../utils/typeGuards";
import BlueButton from "../components/BlueButton";
import Header from "../components/Header";
import { Message } from "@stomp/stompjs";
import { LobbyEntity } from "../types";

const Home = () => {
  const stompClient = useStompClient();
  const navigate = useNavigate();

  const [lobbyUri, setLobbyUri] = useState("");

  const handleLobbyUriChange = (e: ChangeEvent<HTMLInputElement>) => {
    e.preventDefault();
    setLobbyUri(e.target.value);
  };

  const handleJoinLobby = (e: FormEvent) => {
    e.preventDefault();
    if (stompClient) {
      sendLobbyAction(stompClient, "USER_JOIN", lobbyUri);
      setLobbyUri("");
    }
  };

  const handleErrorReply = (message: Message) => {
    console.log(message.body);
  };

  const handleReply = (message: Message) => {
    const messageBody: unknown = JSON.parse(message.body);

    if (!isEvent(messageBody)) {
      return;
    }

    const { id, data } = messageBody;

    if (id === "CREATED_LOBBY" || id === "RECEIVE_LOBBY_DATA") {
      const { lobbyData } = data;

      if (!isLobbyEntity(lobbyData)) {
        return;
      }

      navigate("/lobby", { state: lobbyData });
    }
  };

  const startGame = (e: React.MouseEvent) => {
    e.preventDefault();
    if (stompClient) {
      sendGeneralAction(stompClient, "CREATE_LOBBY");
    }
  };

  useSubscription("/user/queue/reply/general", handleReply);
  useSubscription("/user/queue/reply/lobby", handleReply);
  useSubscription("/user/queue/reply/error", handleErrorReply);

  return (
    <div className="grid place-items-center mt-20">
      <Header innerText="Competitive Rock Paper Scissors" />
      <div className="mt-10">
        <BlueButton innerText="Create Lobby" onClick={startGame} />
      </div>
      <form className="w-full max-w-sm bottom-0" onSubmit={handleJoinLobby}>
        <div className="flex border-b border-blue-500 py-2">
          <input
            className="appearance-none bg-transparent border-none w-full text-gray-700 mr-3 py-1 px-2 leading-tight focus:outline-none"
            type="text"
            onChange={handleLobbyUriChange}
            value={lobbyUri}
            required
          />
          <BlueButton innerText="Join Lobby" />
        </div>
      </form>
    </div>
  );
};

export default Home;
