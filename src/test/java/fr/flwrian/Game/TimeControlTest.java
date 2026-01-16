package fr.flwrian.Game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeControlTest {

    @Test
    void testTimeControlCreation() {
        TimeControl tc = new TimeControl(60000, 1000);
        
        assertEquals(60000, tc.getWhiteTime());
        assertEquals(60000, tc.getBlackTime());
        assertEquals(1000, tc.getWhiteIncrement());
        assertEquals(1000, tc.getBlackIncrement());
    }

    @Test
    void testCopy() {
        TimeControl original = new TimeControl(30000, 500);
        TimeControl copy = original.copy();
        
        assertEquals(original.getWhiteTime(), copy.getWhiteTime());
        assertEquals(original.getBlackTime(), copy.getBlackTime());
        assertEquals(original.getWhiteIncrement(), copy.getWhiteIncrement());
        assertEquals(original.getBlackIncrement(), copy.getBlackIncrement());
        assertNotSame(original, copy); // Different instances
    }

    @Test
    void testApplyMoveWhite() {
        TimeControl tc = new TimeControl(10000, 100);
        
        // White takes 2 seconds, gets 0.1s increment
        tc.applyMove(2000, true);
        
        // 10000 - 2000 + 100 = 8100
        assertEquals(8100, tc.getWhiteTime());
        assertEquals(10000, tc.getBlackTime()); // Unchanged
    }

    @Test
    void testApplyMoveBlack() {
        TimeControl tc = new TimeControl(10000, 100);
        
        // Black takes 1.5 seconds, gets 0.1s increment
        tc.applyMove(1500, false);
        
        // 10000 - 1500 + 100 = 8600
        assertEquals(10000, tc.getWhiteTime()); // Unchanged
        assertEquals(8600, tc.getBlackTime());
    }

    @Test
    void testHasTimeLeft() {
        TimeControl tc = new TimeControl(5000, 0);
        
        assertTrue(tc.hasTimeLeft(true));
        assertTrue(tc.hasTimeLeft(false));
        
        // Simulate timeout
        tc.applyMove(10000, true); // White uses more time than available
        assertFalse(tc.hasTimeLeft(true)); // Should be false (no time left)
        assertTrue(tc.hasTimeLeft(false)); // Black still has time
    }

    @Test
    void testMultipleMoves() {
        TimeControl tc = new TimeControl(10000, 1000);
        
        // Move 1: White 2s
        tc.applyMove(2000, true);
        assertEquals(9000, tc.getWhiteTime()); // 10000 - 2000 + 1000
        
        // Move 2: Black 1s
        tc.applyMove(1000, false);
        assertEquals(10000, tc.getBlackTime()); // 10000 - 1000 + 1000
        
        // Move 3: White 3s
        tc.applyMove(3000, true);
        assertEquals(7000, tc.getWhiteTime()); // 9000 - 3000 + 1000
    }

    @Test
    void testToString() {
        TimeControl tc = new TimeControl(60000, 1000);
        String str = tc.toString();
        
        assertTrue(str.contains("60.0"));
        assertTrue(str.contains("1.0"));
    }
}
