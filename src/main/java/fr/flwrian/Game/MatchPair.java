package fr.flwrian.Game;

/**
 * Represents a pair of games where two engines play each other,
 * swapping colors between games.
 */
public class MatchPair {
    private final int pairId;
    private final String startFen;
    
    public MatchPair(int pairId) {
        this("startpos", pairId);
    }
    
    public MatchPair(String startFen, int pairId) {
        this.startFen = startFen;
        this.pairId = pairId;
    }
    
    public int getPairId() {
        return pairId;
    }
    
    public String getStartFen() {
        return startFen;
    }
    
    public int getGame1Id() {
        return pairId * 2;
    }
    
    public int getGame2Id() {
        return pairId * 2 + 1;
    }
    
    @Override
    public String toString() {
        return String.format("Pair %d (Games %d-%d) [%s]", 
            pairId, getGame1Id(), getGame2Id(), 
            startFen.equals("startpos") ? "Standard" : "FEN");
    }
}
