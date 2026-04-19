import java.io.Serializable;

class GameStateDTO implements Serializable {
    static final long serialVersionUID = 1L;

    GameStateDTO(int winnerID, Piece[][] board, int playerRedID, int playerBlackID) {
        this.winnerID = winnerID;
        this.board = board;
        this.playerRedID = playerRedID;
        this.playerBlackID = playerBlackID;
    }

    // set to -1 if no winner yet, 0 if draw
    final int winnerID;
    final Piece[][] board;
    final int playerRedID;
    final int playerBlackID;

    public static class Piece implements Serializable {
        static final long serialVersionUID = 1L;
        final boolean isRed;
        final boolean isKing;

        public Piece(boolean isRed, boolean isKing) {
            this.isRed = isRed;
            this.isKing = isKing;
        }
    }
}


