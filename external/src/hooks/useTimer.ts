import { useEffect, useMemo, useState } from "react";

const useTimer = () => {
  const [time, setTime] = useState<number>(0);
  const [intervalID, setIntervalID] = useState<NodeJS.Timer | null>(null);

  const isTimerGoing = useMemo(() => time <= 0, [time]);

  const update = () => {
    setTime((prev: number) => prev - 1);
  };

  const startTimer = (time: number) => {
    if (intervalID !== null) {
      clearInterval(intervalID);
    }
    setIntervalID(setInterval(update, 1000));
    setTime(time);
  };

  const stopTimer = () => {
    if (intervalID !== null) {
      clearInterval(intervalID);
    }

    setIntervalID(null);
  };

  useEffect(() => {
    if (isTimerGoing && intervalID !== null) {
      clearInterval(intervalID);
      setIntervalID(null);
    }
  }, [isTimerGoing, intervalID]);

  useEffect(
    () => () => {
      if (intervalID !== null) {
        clearInterval(intervalID);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
  );

  return {
    time,
    startTimer,
    stopTimer,
  };
};

export default useTimer;
