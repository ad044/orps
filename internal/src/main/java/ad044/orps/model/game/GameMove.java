package ad044.orps.model.game;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum GameMove {
    ROCK {
        @Override
        public boolean beats(GameMove other) {
            return other == SCISSORS || other == NO_MOVE;
        }
    }, PAPER {
        @Override
        public boolean beats(GameMove other) {
            return other == ROCK || other == NO_MOVE;
        }
    }, SCISSORS {
        @Override
        public boolean beats(GameMove other) {
            return other == PAPER || other == NO_MOVE;
        }
    }, NO_MOVE {
        @Override
        public boolean beats(GameMove other) {
            return false;
        }
    };

    public static Optional<GameMove> parseMove(String value) {
        GameMove res = null;
        switch (value.toUpperCase()) {
            case "ROCK":
                res = ROCK;
                break;
            case "PAPER":
                res = PAPER;
                break;
            case "SCISSORS":
                res = SCISSORS;
        }

        return Optional.ofNullable(res);
    }

    public abstract boolean beats(GameMove other);

    public static GameMove getRandomMove() {
        List<GameMove> possibleMoves = Arrays
                .stream(GameMove.values())
                .filter(move -> !move.equals(GameMove.NO_MOVE))
                .collect(Collectors.toList());

        return possibleMoves.get((int) (Math.random() * (possibleMoves.size())));
    }
}
