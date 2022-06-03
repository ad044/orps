import { MouseEvent } from "react";

type BlueButtonProps = {
  innerText: string;
  onClick?: (e: MouseEvent<HTMLElement>) => void;
  readonly?: boolean;
};

const BlueButton = (props: BlueButtonProps) => {
  return (
    <button
      onClick={props.onClick}
      className={`${
        props.readonly
          ? "bg-gray-500 hover:bg-gray-700"
          : "bg-blue-500 hover:bg-blue-700"
      } text-white font-bold py-2 px-4`}
    >
      {props.innerText}
    </button>
  );
};

export default BlueButton;
