package fr.flwrian.Game;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the current state of a chess game.
 * Tracks position, move history, and time control.
 */
public class GameState {
    private String startFen;
    private final List<String> moves;
    private final TimeControl timeControl;
    private boolean whiteToMove;
    private int fullMoveNumber;
    private int halfMoveClock;
    
    // Engine evaluation data
    private Integer whiteScore = null;  // Centipawn score from white's perspective
    private Integer blackScore = null;
    private Integer whiteDepth = null;
    private Integer blackDepth = null;
    private Integer whiteSelDepth = null;  // Selective depth
    private Integer blackSelDepth = null;
    private Long whiteNodes = null;  // Nodes searched
    private Long blackNodes = null;
    private Long whiteNps = null;  // Nodes per second
    private Long blackNps = null;
    private String whitePv = null;
    private String blackPv = null;

    public GameState(TimeControl timeControl) {
        this("startpos", timeControl);
    }

    public GameState(String startFen, TimeControl timeControl) {
        this.startFen = startFen;
        this.moves = new ArrayList<>();
        this.timeControl = timeControl;
        this.whiteToMove = true;
        this.fullMoveNumber = 1;
        this.halfMoveClock = 0;
    }

    public void addMove(String move, long elapsedMs) {
        moves.add(move);
        timeControl.applyMove(elapsedMs, whiteToMove);
        whiteToMove = !whiteToMove;
        
        if (whiteToMove) {
            fullMoveNumber++;
        }
        
        // Simplified: assume non-pawn, non-capture moves for now
        halfMoveClock++;
    }

    public String getPositionCommand() {
        StringBuilder sb = new StringBuilder("position ");
        
        // Use "position startpos" or "position fen <fen>"
        if (startFen.equals("startpos")) {
            sb.append("startpos");
        } else {
            sb.append("fen ").append(startFen);
        }
        
        if (!moves.isEmpty()) {
            sb.append(" moves");
            for (String move : moves) {
                sb.append(" ").append(move);
            }
        }
        
        return sb.toString();
    }

    public String getGoCommand() {
        return String.format("go wtime %d btime %d winc %d binc %d",
            timeControl.getWhiteTime(),
            timeControl.getBlackTime(),
            timeControl.getWhiteIncrement(),
            timeControl.getBlackIncrement());
    }

    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public int getMoveCount() {
        return moves.size();
    }

    public TimeControl getTimeControl() {
        return timeControl;
    }

    public boolean hasTimeLeft() {
        return timeControl.hasTimeLeft(whiteToMove);
    }

    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    public int getFullMoveNumber() {
        return fullMoveNumber;
    }

    public List<String> getMoves() {
        return new ArrayList<>(moves);
    }
    
    /**
     * Store evaluation data from the engine.
     * @param isWhite true if white engine, false if black
     * @param score centipawn score (positive = better for side to move)
     * @param depth search depth
     * @param selDepth selective depth
     * @param nodes nodes searched
     * @param nps nodes per second
     * @param pv principal variation (best move)
     */
    public void setLastEvaluation(boolean isWhite, Integer score, Integer depth, Integer selDepth, 
                                   Long nodes, Long nps, String pv) {
        if (isWhite) {
            this.whiteScore = score;
            this.whiteDepth = depth;
            this.whiteSelDepth = selDepth;
            this.whiteNodes = nodes;
            this.whiteNps = nps;
            this.whitePv = pv;
        } else {
            this.blackScore = score;
            this.blackDepth = depth;
            this.blackSelDepth = selDepth;
            this.blackNodes = nodes;
            this.blackNps = nps;
            this.blackPv = pv;
        }
    }
    
    public Integer getWhiteScore() { return whiteScore; }
    public Integer getBlackScore() { return blackScore; }
    public Integer getWhiteDepth() { return whiteDepth; }
    public Integer getBlackDepth() { return blackDepth; }
    public Integer getWhiteSelDepth() { return whiteSelDepth; }
    public Integer getBlackSelDepth() { return blackSelDepth; }
    public Long getWhiteNodes() { return whiteNodes; }
    public Long getBlackNodes() { return blackNodes; }
    public Long getWhiteNps() { return whiteNps; }
    public Long getBlackNps() { return blackNps; }
    public String getWhitePv() { return whitePv; }
    public String getBlackPv() { return blackPv; }
}
