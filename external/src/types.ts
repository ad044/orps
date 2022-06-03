export type UserEntity = {
  username: string;
  uuid: string;
};

export type PlayerEntity = UserEntity & {
  score: number;
  move: string;
};

export type LobbyEntity = {
  users: UserEntity[];
  uri: string;
  settings: LobbySettings;
};

export type GameEntity = {
  parentLobbyUri: string | null;
  players: PlayerEntity[];
  settings: GameSettings;
  uri: string;
};

export type RoundEntity = {
  roundNumber: number;
  winnerUuid: string | undefined;
  moveMap: { [key: string]: string };
};

export type EventData = { [key: string]: unknown };

export type Event = {
  id: string;
  data: EventData;
};

export type GameSettings = {
  timeForMove: number;
  scoreGoal: number;
};

export type LobbySettings = GameSettings & {
  inviteOnly: boolean;
};

export type LobbyEvent = Event & { lobbyUri: string };

export type GameEvent = Event & { gameUri: string };

export type UpdateSettings = (
  settingName: string,
  settingValue: string
) => void;