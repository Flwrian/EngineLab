package fr.flwrian.Game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchPairTest {

    @Test
    void testMatchPairCreation() {
        MatchPair pair = new MatchPair(1);
        
        assertEquals(1, pair.getPairId());
        assertEquals(2, pair.getGame1Id()); // pairId * 2
        assertEquals(3, pair.getGame2Id()); // pairId * 2 + 1
        assertEquals("startpos", pair.getStartFen());
    }

    @Test
    void testCustomFen() {
        String customFen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";
        MatchPair pair = new MatchPair(customFen, 5);
        
        assertEquals(5, pair.getPairId());
        assertEquals(10, pair.getGame1Id()); // 5 * 2
        assertEquals(11, pair.getGame2Id()); // 5 * 2 + 1
        assertEquals(customFen, pair.getStartFen());
    }

    @Test
    void testGameIdCalculation() {
        // Pair 1 -> Games 2, 3
        MatchPair pair1 = new MatchPair(1);
        assertEquals(2, pair1.getGame1Id());
        assertEquals(3, pair1.getGame2Id());
        
        // Pair 2 -> Games 4, 5
        MatchPair pair2 = new MatchPair(2);
        assertEquals(4, pair2.getGame1Id());
        assertEquals(5, pair2.getGame2Id());
        
        // Pair 10 -> Games 20, 21
        MatchPair pair10 = new MatchPair(10);
        assertEquals(20, pair10.getGame1Id());
        assertEquals(21, pair10.getGame2Id());
    }

    @Test
    void testToString() {
        MatchPair pair = new MatchPair(3);
        String str = pair.toString();
        
        assertTrue(str.contains("Pair"));
        assertTrue(str.contains("3"));
    }
}
