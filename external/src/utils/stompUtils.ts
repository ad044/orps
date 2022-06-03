import { Client } from "@stomp/stompjs";

const sendSocketMessage = (client: Client, url: string, body: object) => {
  client.publish({
    destination: url,
    body: JSON.stringify(body),
  });
};

export const sendGeneralAction = (
  client: Client,
  idString: string,
  data?: object
) => {
  sendSocketMessage(client, "/app/user-action", {
    idString: idString,
    category: "GENERAL",
    data: data,
  });
};

export const sendLobbyAction = (
  client: Client,
  idString: string,
  lobbyUri: string,
  data?: object
) => {
  sendSocketMessage(client, "/app/user-action", {
    idString: idString,
    category: "LOBBY",
    data: { ...data, lobbyUri: lobbyUri },
  });
};

export const sendGameAction = (
  client: Client,
  idString: string,
  gameUri: string,
  data?: object
) => {
  sendSocketMessage(client, "/app/user-action", {
    idString: idString,
    category: "GAME",
    data: { ...data, gameUri: gameUri },
  });
};
