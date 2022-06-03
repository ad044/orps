import { LobbyEvent, LobbySettings, UserEntity } from "../types";
import { isLobbyEntity, isUserEntity } from "../utils/typeGuards";

type LobbyState = {
  users: UserEntity[];
  settings: LobbySettings;
};

const reducer = (state: LobbyState, event: LobbyEvent): LobbyState => {
  const { id, data } = event;

  switch (id) {
    case "MEMBER_LEAVE": {
      const { memberUuid } = data;

      if (typeof memberUuid !== "string") {
        return state;
      }

      return {
        ...state,
        users: state.users.filter((user) => user.uuid !== memberUuid),
      };
    }
    case "MEMBER_JOIN": {
      const { memberData } = data;

      if (!isUserEntity(memberData)) {
        return state;
      }

      return {
        ...state,
        users: [...state.users, memberData],
      };
    }
    case "RECEIVE_LOBBY_DATA": {
      const { lobbyData } = data;

      if (!isLobbyEntity(lobbyData)) {
        return state;
      }

      return lobbyData;
    }
    case "SETTINGS_UPDATED": {
      const { settingName, settingValue } = data;

      if (typeof settingName !== "string") {
        return state;
      }

      if (typeof settingValue !== "string") {
        return state;
      }

      switch (settingName) {
        case "inviteOnly": {
          return state.settings
            ? {
                ...state,
                settings: {
                  ...state.settings,
                  [settingName]: settingValue === "true",
                },
              }
            : state;
        }
        case "scoreGoal":
        case "timeForMove": {
          return state.settings
            ? {
                ...state,
                settings: {
                  ...state.settings,
                  [settingName]: parseInt(settingValue),
                },
              }
            : state;
        }
        default:
          return state;
      }
    }
    default:
      return state;
  }
};

export default reducer;