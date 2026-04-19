public class CheckersGame {
    public int gameID;
    public int playerBlackID = -1;
    public int playerRedID = -1;

    Board board;
    boolean redTurn = true;
    
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

        public void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
            grid[toRow][toCol] = grid[fromRow][fromCol];
            grid[fromRow][fromCol] = null;

            // King promotion
            if (toRow == 0 || toRow == 7) {
                grid[toRow][toCol].makeKing();
            }
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
    
    // Returns false if invalid move
    public boolean move(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board.getPiece(fromRow, fromCol);

        if (piece == null) return false;

        if (piece.isRed != redTurn) return false;

        int rowDiff = toRow - fromRow;
        int colDiff = Math.abs(toCol - fromCol);

        if (Math.abs(rowDiff) == 1 && colDiff == 1) {
            board.movePiece(fromRow, fromCol, toRow, toCol);
            redTurn = !redTurn;
            return true;
        }

        // Capture move
        if (Math.abs(rowDiff) == 2 && colDiff == 2) {
            int midRow = (fromRow + toRow) / 2;
            int midCol = (fromCol + toCol) / 2;

            Piece captured = board.getPiece(midRow, midCol);

            if (captured != null && captured.isRed != piece.isRed) {
                board.movePiece(fromRow, fromCol, toRow, toCol);
                board.grid[midRow][midCol] = null;
                redTurn = !redTurn;
                return true;
            }
        }

        return false;
    }

    // 0 if no winner, else returns winner ID
    public int checkWinner() {
        int redPieces = board.countPieces(true);
        int blackPieces = board.countPieces(false);

        if (redPieces == 0) return playerBlackID;
        if (blackPieces == 0) return playerRedID;

        if (!board.hasMoves(true)) return playerBlackID;
        if (!board.hasMoves(false)) return playerRedID;

        return 0;
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }
}
