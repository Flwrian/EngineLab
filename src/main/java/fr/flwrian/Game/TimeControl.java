package fr.flwrian.Game;

/**
 * Represents time control settings for a chess game (UCI format).
 * Handles time remaining and increment for both sides.
 */
public class TimeControl {
    private long whiteTime; // milliseconds
    private long blackTime; // milliseconds
    private final long whiteIncrement; // milliseconds
    private final long blackIncrement; // milliseconds

    public TimeControl(long baseTime, long increment) {
        this(baseTime, baseTime, increment, increment);
    }

    public TimeControl(long whiteTime, long blackTime, long whiteIncrement, long blackIncrement) {
        this.whiteTime = whiteTime;
        this.blackTime = blackTime;
        this.whiteIncrement = whiteIncrement;
        this.blackIncrement = blackIncrement;
    }

    /**
     * Deduct time for a move and add increment.
     * @param elapsed Time elapsed in milliseconds
     * @param isWhite true if white made the move
     */
    public void applyMove(long elapsed, boolean isWhite) {
        if (isWhite) {
            whiteTime -= elapsed;
            whiteTime += whiteIncrement;
        } else {
            blackTime -= elapsed;
            blackTime += blackIncrement;
        }
    }

    public long getWhiteTime() {
        return Math.max(0, whiteTime);
    }

    public long getBlackTime() {
        return Math.max(0, blackTime);
    }

    public long getWhiteIncrement() {
        return whiteIncrement;
    }

    public long getBlackIncrement() {
        return blackIncrement;
    }

    public boolean hasTimeLeft(boolean isWhite) {
        return isWhite ? whiteTime > 0 : blackTime > 0;
    }

    public TimeControl copy() {
        return new TimeControl(whiteTime, blackTime, whiteIncrement, blackIncrement);
    }

    @Override
    public String toString() {
        return String.format("White: %.1fs (+%.1fs), Black: %.1fs (+%.1fs)",
            whiteTime / 1000.0, whiteIncrement / 1000.0,
            blackTime / 1000.0, blackIncrement / 1000.0);
    }
}
