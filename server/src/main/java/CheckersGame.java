public class CheckersGame {
    final public int gameID;
    public int playerBlackID = -1;
    public int playerRedID = -1;

    Board board;
    boolean redTurn = false;
    
    GameStateDTO toStateDTO(String redUsername, String blackUsername) {

        GameStateDTO.Piece[][] outBoard = new GameStateDTO.Piece[8][8];

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);

                if (piece != null) {
                    outBoard[row][col] = new GameStateDTO.Piece(piece.isRed, piece.isKing);
                }
            }
        }

        GameStateDTO state = new GameStateDTO(checkWinner(), outBoard, playerRedID, playerBlackID, redUsername, blackUsername);

        return state;
    }
    
    boolean hasWaitingPlayer() {
        return (playerRedID == -1 && playerBlackID != -1) || (playerRedID != -1 && playerBlackID == -1);
    }

    boolean hasNoPlayers() {
        return (playerRedID == -1 && playerBlackID == -1);
    }

    boolean hasBothPlayers() {
        return (playerRedID != -1 && playerBlackID != -1);
    }
    
    // returns true if player is addded as red
    boolean joinGame(int playerID) throws Exception {
        if (playerRedID == -1) {
            playerRedID = playerID;
            return true;
        }
        else if (playerBlackID == -1) {
            playerBlackID = playerID;
            return false;
        }

        throw new Exception("Game is full");
    }
    
    // reset the board, leave other state as is
    public void restart() {
        board = new Board();
        redTurn = false;
    }
    
    public class Piece {
        boolean isRed;
        boolean isKing;

        public Piece(boolean isRed) {
            this.isRed = isRed;
            this.isKing = false;
        }

        public void makeKing() {
            isKing = true;
        }
    }

    public class Board {
        Piece[][] grid = new Piece[8][8];

        public Board() {
            setupBoard();
        }

        private void setupBoard() {
            // Red pieces
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 8; col++) {
                    if ((row + col) % 2 == 1) {
                        grid[row][col] = new Piece(true);
                    }
                }
            }

            // Black pieces
            for (int row = 5; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if ((row + col) % 2 == 1) {
                        grid[row][col] = new Piece(false);
                    }
                }
            }
        }

        public Piece getPiece(int row, int col) {
            return grid[row][col];
        }

        public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
            if (grid[toRow][toCol] != null) { // piece is already occupied
                return false;
            }

            grid[toRow][toCol] = grid[fromRow][fromCol];
            grid[fromRow][fromCol] = null;

            // King promotion
            if (toRow == 0 || toRow == 7) {
                grid[toRow][toCol].makeKing();
            }

            return true;
        }

        public boolean hasMoves(boolean isRed) {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Piece p = grid[row][col];

                    if (p != null && p.isRed == isRed) {
                        if (canMove(row, col)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public int countPieces(boolean isRed) {
            int count = 0;

            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Piece p = grid[row][col];
                    if (p != null && p.isRed == isRed) {
                        count++;
                    }
                }
            }

            return count;
        }

        private boolean canMove(int row, int col) {
            Piece p = grid[row][col];
            int dir = p.isRed ? 1 : -1;

            // normal moves
            int[] cols = {-1, 1};

            for (int c : cols) {
                int newRow = row + dir;
                int newCol = col + c;

                if (isValid(newRow, newCol) && grid[newRow][newCol] == null) {
                    return true;
                }
            }

            // capture moves
            for (int c : cols) {
                int midRow = row + dir;
                int midCol = col + c;
                int jumpRow = row + 2 * dir;
                int jumpCol = col + 2 * c;

                if (isValid(jumpRow, jumpCol) &&
                    grid[jumpRow][jumpCol] == null &&
                    grid[midRow][midCol] != null &&
                    grid[midRow][midCol].isRed != p.isRed) {
                    return true;
                }
            }

            return false;
        }
    }
    
    public CheckersGame(int gameID, int playerRedID, int playerBlackID) {
        board = new Board();
        
        this.gameID = gameID;
        this.playerRedID = playerRedID;
        this.playerBlackID = playerBlackID;
    }

    public CheckersGame(int gameID) {
        board = new Board();

        this.gameID = gameID;
    }
    
    public String move(int fromRow, int fromCol, int toRow, int toCol, int playerID) {
        Piece piece = board.getPiece(fromRow, fromCol);

        if (piece == null) {
            return new String("Invalid piece.");
        }

        // Validate playerID is moving their assigned color
        if (piece.isRed && (playerID != playerRedID)) {
            return new String("Can't move a red piece.");
        }

        if (!piece.isRed && (playerID != playerBlackID)) {
            return new String("Can't move a black piece.");
        }

        if (piece.isRed != redTurn) {
            String error = redTurn ? new String("It's red's turn.") : new String("It's black's turn.");
            return error;
        }

        // red non-king pieces must move positive, black non-king pieces must move negative
        if (piece.isRed && !piece.isKing && (toRow <= fromRow)) {
            return new String("Can't move backward.");
        }
        else if (!piece.isRed && !piece.isKing && (toRow >= fromRow)) {
            return new String("Can't move backward.");
        }
        
        int rowDiff = toRow - fromRow;
        int colDiff = Math.abs(toCol - fromCol);

        if (Math.abs(rowDiff) == 1 && colDiff == 1) {
            if (!board.movePiece(fromRow, fromCol, toRow, toCol)) {
                return new String("Can't move a piece onto another piece.");
            }

            redTurn = !redTurn;
            return null;
        }

        // Capture move
        if (Math.abs(rowDiff) == 2 && colDiff == 2) {
            int midRow = (fromRow + toRow) / 2;
            int midCol = (fromCol + toCol) / 2;

            Piece captured = board.getPiece(midRow, midCol);

            if (captured != null && captured.isRed != piece.isRed) {
                if(!board.movePiece(fromRow, fromCol, toRow, toCol)) {
                    return new String("Can't move a piece onto another piece."); // jumping for capture, but landing on a piece
                }

                board.grid[midRow][midCol] = null;
                redTurn = !redTurn;
                return null;
            }
        }

        return new String("Invalid move position.");
    }

    // -1 if no winner, 0 if draw, else returns winner ID
    public int checkWinner() {
        int redPieces = board.countPieces(true);
        int blackPieces = board.countPieces(false);

        if (redPieces == 0) return playerBlackID;
        if (blackPieces == 0) return playerRedID;

        if (!board.hasMoves(true)) return playerBlackID;
        if (!board.hasMoves(false)) return playerRedID;

        return -1;
    }


    private boolean isValid(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }
}
