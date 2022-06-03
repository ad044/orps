import {
  GameEntity,
  GameSettings,
  LobbyEntity,
  LobbyEvent,
  PlayerEntity,
  RoundEntity,
  Event,
  UserEntity,
  LobbySettings,
  GameEvent,
} from "../types";

export const isEvent = (obj: any): obj is Event => {
  return typeof obj.id === "string" && obj.data.constructor === Object;
};

const isGameSettings = (obj: any): obj is GameSettings => {
  return (
    typeof obj.timeForMove === "number" && typeof obj.scoreGoal === "number"
  );
};

export const isLobbyEvent = (obj: any): obj is LobbyEvent => {
  return typeof typeof obj.lobbyUri === "string" && isEvent(obj);
};

export const isGameEvent = (obj: any): obj is GameEvent => {
  return typeof typeof obj.gameUri === "string" && isEvent(obj);
};

export const isUserEntity = (obj: any): obj is UserEntity => {
  return typeof obj.username === "string" && typeof obj.uuid === "string";
};

export const isPlayerEntity = (obj: any): obj is PlayerEntity => {
  return typeof obj.score === "number" && isUserEntity(obj);
};

export const isLobbySettings = (obj: any): obj is LobbySettings => {
  return typeof obj.inviteOnly === "boolean" && isGameSettings(obj);
};

export const isLobbyEntity = (obj: any): obj is LobbyEntity => {
  return (
    typeof obj.uri === "string" &&
    isLobbySettings(obj.settings) &&
    obj.users.constructor === Array &&
    obj.users.every((item: any) => isUserEntity(item))
  );
};

export const isPlayerEntityArray = (obj: any): obj is PlayerEntity[] => {
  return (
    obj.constructor === Array && obj.every((item: any) => isPlayerEntity(item))
  );
};

export const isGameEntity = (obj: any): obj is GameEntity => {
  return (
    isGameSettings(obj.settings) &&
    typeof obj.parentLobbyUri === "string" &&
    typeof obj.uri === "string" &&
    isPlayerEntityArray(obj.players)
  );
};
