import React, { ChangeEvent, useEffect, useState } from "react";
import BlueButton from "./BlueButton";
import { UpdateSettings } from "../types";

type LobbyConfigTextFieldProps = {
  name: string;
  label: string;
  serverValue: number;
  min: number;
  max: number;
  sendUpdate: UpdateSettings;
};

const LobbyConfigSlider = (props: LobbyConfigTextFieldProps) => {
  const [saved, setSaved] = useState(false);
  const [clientValue, setClientValue] = useState(props.serverValue.toString());

  const onChange = (e: ChangeEvent<HTMLInputElement>) => {
    setClientValue(e.target.value);
  };

  const onSave = (e: React.MouseEvent) => {
    e.preventDefault();
    props.sendUpdate(props.name, clientValue);
  };

  useEffect(() => {
    setSaved(props.serverValue.toString() === clientValue);
  }, [clientValue, props.serverValue]);

  useEffect(() => {
    setClientValue(props.serverValue.toString());
  }, [props.serverValue]);

  return (
    <form className="text-gray-800">
      <label className="form-label">
        {props.label} : {clientValue}
      </label>
      <br />
      <input
        type="range"
        min={props.min}
        max={props.max}
        className="form-range"
        onChange={onChange}
        value={clientValue}
        readOnly
      />
      {<BlueButton innerText="Save" onClick={onSave} readonly={saved} />}
    </form>
  );
};

export default LobbyConfigSlider;
