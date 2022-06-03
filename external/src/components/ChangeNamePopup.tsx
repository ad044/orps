import React, { useCallback, useState } from "react";
import { useStompClient } from "react-stomp-hooks";
import { sendGeneralAction } from "../utils/stompUtils";

const ChangeNamePopup = () => {
  const stompClient = useStompClient();
  const [username, setUsername] = useState("");

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (stompClient) {
      sendGeneralAction(stompClient, "CHANGE_NAME", { newName: username });
    }
  };
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    e.preventDefault();
    setUsername(e.target.value);
  };

  return (
    <form onSubmit={handleSubmit}>
      <label>
        new name:
        <input type="text" value={username} onChange={handleChange} />
      </label>
      <input type="submit" value="Submit" />
    </form>
  );
};

export default ChangeNamePopup;
