import React from "react";
import { useNavigate } from "react-router-dom";
import { useStompClient, useSubscription } from "react-stomp-hooks";
import { sendGeneralAction } from "../utils/stompUtils";
import { isEvent, isLobbyEntity } from "../utils/typeGuards";
import BlueButton from "../components/BlueButton";
import Header from "../components/Header";

const Home = () => {
  const stompClient = useStompClient();
  const navigate = useNavigate();

  useSubscription("/user/queue/reply/general", (message) => {
    const messageBody: unknown = JSON.parse(message.body);

    if (!isEvent(messageBody)) {
      return;
    }

    const { id, data } = messageBody;

    if (id === "CREATED_LOBBY") {
      const { lobbyData } = data;

      if (!isLobbyEntity(lobbyData)) {
        return;
      }

      navigate("/lobby", { state: lobbyData });
    }
  });

  const startGame = (e: React.MouseEvent) => {
    e.preventDefault();
    if (stompClient) {
      sendGeneralAction(stompClient, "CREATE_LOBBY");
    }
  };

  useSubscription("/user/queue/reply/error", (e) => console.log(e));

  return (
    <div className="grid place-items-center mt-20">
      <Header innerText="Competitive Rock Paper Scissors" />
      <div className="mt-10">
        <BlueButton innerText="Create Lobby" onClick={startGame} />
      </div>
    </div>
  );
};

export default Home;
