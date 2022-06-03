import { ChangeEvent } from "react";
import { UpdateSettings } from "../types";

type LobbyConfigCheckboxProps = {
  name: string;
  label: string;
  serverValue: boolean;
  sendUpdate: UpdateSettings;
};

const LobbyConfigCheckbox = (props: LobbyConfigCheckboxProps) => {
  const onChange = (e: ChangeEvent<HTMLInputElement>) => {
    props.sendUpdate(props.name, e.target.checked ? "true" : "false");
  };

  return (
    <div className="form-check">
      <label className="form-check-label text-gray-800">{props.label} : </label>
      <input
        className="form-check-input ml-2"
        type="checkbox"
        checked={props.serverValue}
        onChange={onChange}
      />
    </div>
  );
};

export default LobbyConfigCheckbox;
