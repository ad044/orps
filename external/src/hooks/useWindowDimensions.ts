import { useState, useEffect } from "react";

type WindowDimensions = {
  height: number;
  width: number;
};

export default function useWindowDimensions() {
  const hasWindow = typeof window !== "undefined";
  const [windowDimensions, setWindowDimensions] = useState<WindowDimensions>({
    height: window.innerHeight,
    width: window.innerWidth,
  });

  useEffect(() => {
    const handleResize = () => {
      setWindowDimensions({
        width: window.innerWidth,
        height: window.innerHeight,
      });
    };

    if (hasWindow) {
      window.addEventListener("resize", handleResize);
      return () => window.removeEventListener("resize", handleResize);
    }
  }, [hasWindow]);

  return windowDimensions;
}
