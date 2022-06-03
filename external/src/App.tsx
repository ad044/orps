import { MemoryRouter, Route, Routes } from "react-router-dom";
import { StompSessionProvider } from "react-stomp-hooks";
import Game from "./pages/Game";
import Home from "./pages/Home";
import Lobby from "./pages/Lobby";

const App = () => {
  return (
    <StompSessionProvider
      url={"ws://localhost:8080/ws"}
      // debug={(str) => {
      //   console.log(str);
      // }}>
    >
      <MemoryRouter>
        <Routes>
          <Route index element={<Home />} />
          <Route path="/lobby" element={<Lobby />} />
          <Route path="/game" element={<Game />} />
        </Routes>
      </MemoryRouter>
    </StompSessionProvider>
  );
};

export default App;
