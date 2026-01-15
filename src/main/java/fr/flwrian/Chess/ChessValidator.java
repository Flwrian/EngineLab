package fr.flwrian.Chess;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import com.github.bhlangonijr.chesslib.move.MoveGeneratorException;

import java.util.List;

/**
 * Chess position validator and game state detector using chesslib.
 */
public class ChessValidator {
    private final Board board;

    public ChessValidator() {
        this.board = new Board();
    }

    public ChessValidator(String fen) {
        this.board = new Board();
        board.loadFromFen(fen);
    }

    /**
     * Apply a move in UCI format (e.g., "e2e4", "e7e8q").
     */
    public boolean applyMove(String uciMove) {
        try {
            Move move = parseUCIMove(uciMove);
            if (move == null) return false;
            
            board.doMove(move);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the current position is checkmate.
     */
    public boolean isCheckmate() {
        return board.isMated();
    }

    /**
     * Check if the current position is stalemate.
     */
    public boolean isStalemate() {
        return board.isStaleMate();
    }

    /**
     * Check if the game is drawn (stalemate, insufficient material, repetition).
     */
    public boolean isDraw() {
        return board.isDraw();
    }

    /**
     * Check if position is insufficient material.
     */
    public boolean isInsufficientMaterial() {
        return board.isInsufficientMaterial();
    }

    /**
     * Check if current side is in check.
     */
    public boolean isInCheck() {
        return board.isKingAttacked();
    }

    /**
     * Get all legal moves in current position.
     */
    public List<Move> getLegalMoves() {
        try {
            return MoveGenerator.generateLegalMoves(board);
        } catch (MoveGeneratorException e) {
            return List.of();
        }
    }

    /**
     * Check if a move is legal.
     */
    public boolean isMoveLegal(String uciMove) {
        Move move = parseUCIMove(uciMove);
        if (move == null) return false;
        
        try {
            List<Move> legalMoves = MoveGenerator.generateLegalMoves(board);
            return legalMoves.contains(move);
        } catch (MoveGeneratorException e) {
            return false;
        }
    }

    /**
     * Get current FEN.
     */
    public String getFen() {
        return board.getFen();
    }

    /**
     * Get game result: "1-0", "0-1", "1/2-1/2", or null if ongoing.
     */
    public String getResult() {
        if (isCheckmate()) {
            return board.getSideToMove().equals(com.github.bhlangonijr.chesslib.Side.WHITE) 
                ? "0-1" : "1-0";
        }
        if (isDraw() || isStalemate() || isInsufficientMaterial()) {
            return "1/2-1/2";
        }
        return null;
    }

    /**
     * Get termination reason.
     */
    public String getTerminationReason() {
        if (isCheckmate()) return "checkmate";
        if (isStalemate()) return "stalemate";
        if (isInsufficientMaterial()) return "insufficient_material";
        if (isDraw()) return "draw";
        return null;
    }

    /**
     * Parse UCI move string to Move object.
     */
    private Move parseUCIMove(String uci) {
        if (uci == null || uci.length() < 4) return null;
        
        try {
            Square from = Square.fromValue(uci.substring(0, 2).toUpperCase());
            Square to = Square.fromValue(uci.substring(2, 4).toUpperCase());
            
            // Handle promotion
            if (uci.length() == 5) {
                char promotionChar = uci.charAt(4);
                com.github.bhlangonijr.chesslib.Piece promotion = parsePromotion(promotionChar);
                return new Move(from, to, promotion);
            }
            
            return new Move(from, to);
        } catch (Exception e) {
            return null;
        }
    }

    private com.github.bhlangonijr.chesslib.Piece parsePromotion(char c) {
        com.github.bhlangonijr.chesslib.Side side = board.getSideToMove();
        switch (Character.toLowerCase(c)) {
            case 'q': return side == com.github.bhlangonijr.chesslib.Side.WHITE 
                ? com.github.bhlangonijr.chesslib.Piece.WHITE_QUEEN 
                : com.github.bhlangonijr.chesslib.Piece.BLACK_QUEEN;
            case 'r': return side == com.github.bhlangonijr.chesslib.Side.WHITE 
                ? com.github.bhlangonijr.chesslib.Piece.WHITE_ROOK 
                : com.github.bhlangonijr.chesslib.Piece.BLACK_ROOK;
            case 'b': return side == com.github.bhlangonijr.chesslib.Side.WHITE 
                ? com.github.bhlangonijr.chesslib.Piece.WHITE_BISHOP 
                : com.github.bhlangonijr.chesslib.Piece.BLACK_BISHOP;
            case 'n': return side == com.github.bhlangonijr.chesslib.Side.WHITE 
                ? com.github.bhlangonijr.chesslib.Piece.WHITE_KNIGHT 
                : com.github.bhlangonijr.chesslib.Piece.BLACK_KNIGHT;
            default: return side == com.github.bhlangonijr.chesslib.Side.WHITE 
                ? com.github.bhlangonijr.chesslib.Piece.WHITE_QUEEN 
                : com.github.bhlangonijr.chesslib.Piece.BLACK_QUEEN;
        }
    }

    public Board getBoard() {
        return board;
    }
}
