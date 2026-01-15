package fr.flwrian.Task;

import java.util.concurrent.Callable;
import java.util.ArrayList;
import java.util.List;

import fr.flwrian.Engine.Engine;
import fr.flwrian.Game.GameManager;
import fr.flwrian.Game.MatchPair;
import fr.flwrian.Game.TimeControl;
import fr.flwrian.Result.GameResult;
import fr.flwrian.Result.PairResult;

/**
 * Represents a pair task where two engines play two games,
 * swapping colors between games.
 */
public class PairTask implements Callable<PairResult> {
    private final Engine engine1;
    private final Engine engine2;
    private final MatchPair pair;
    private final TimeControl baseTimeControl;
    private final String engine1Name;
    private final String engine2Name;

    public PairTask(MatchPair pair, Engine engine1, Engine engine2, TimeControl baseTimeControl, String engine1Name, String engine2Name) {
        this.pair = pair;
        this.engine1 = engine1;
        this.engine2 = engine2;
        this.baseTimeControl = baseTimeControl;
        this.engine1Name = engine1Name;
        this.engine2Name = engine2Name;
    }

    @Override
    public PairResult call() {
        List<GameResult> results = new ArrayList<>();
        String threadName = Thread.currentThread().getName();
        
        try {
            System.out.println("[" + threadName + "] === " + pair + " starting ===");
            System.out.println("[" + threadName + "] 🎮 Matchup: " + engine1Name + " vs " + engine2Name);
            
            // Display FEN (truncate if too long for readability)
            String fen = pair.getStartFen();
            if (fen != null && !fen.isEmpty()) {
                String displayFen = fen.length() > 70 ? fen.substring(0, 67) + "..." : fen;
                System.out.println("[" + threadName + "] 📋 FEN: " + displayFen);
            }
            
            // Game 1: engine1 = White, engine2 = Black
            System.out.println("[" + threadName + "] Starting game 1 of pair " + pair.getPairId());
            System.out.println("[" + threadName + "] ⚪ White: " + engine1Name + " | ⚫ Black: " + engine2Name);
            GameManager game1 = new GameManager(
                pair.getGame1Id(), 
                engine1, 
                engine2, 
                pair.getStartFen(), 
                baseTimeControl.copy(),
                engine1Name,
                engine2Name
            );
            GameResult result1 = game1.run();
            results.add(result1);
            System.out.println("[" + threadName + "] Game 1 of pair " + pair.getPairId() + " completed: " + result1.getResult());
            
            // Reset engines between games (clear queue + ucinewgame)
            System.out.println("[" + threadName + "] Resetting engines between games...");
            engine1.reset();
            engine2.reset();
            
            // Small delay between games
            Thread.sleep(100);
            
            // Game 2: engine2 = White, engine1 = Black (colors swapped)
            System.out.println("[" + threadName + "] Starting game 2 of pair " + pair.getPairId());
            System.out.println("[" + threadName + "] ⚪ White: " + engine2Name + " | ⚫ Black: " + engine1Name);
            GameManager game2 = new GameManager(
                pair.getGame2Id(), 
                engine2, 
                engine1, 
                pair.getStartFen(), 
                baseTimeControl.copy(),
                engine2Name,
                engine1Name
            );
            GameResult result2 = game2.run();
            results.add(result2);
            System.out.println("[" + threadName + "] Game 2 of pair " + pair.getPairId() + " completed: " + result2.getResult());
            
            System.out.println("[" + threadName + "] === " + pair + " completed ===");
            
            return new PairResult(pair.getPairId(), results, engine1Name, engine2Name);
            
        } catch (Exception e) {
            System.err.println("[" + threadName + "] ⚠️ Exception in pair " + pair.getPairId() + ": " + e.getMessage());
            e.printStackTrace();
            // If we have partial results, return them
            if (results.isEmpty()) {
                // Both games failed
                results.add(new GameResult(pair.getGame1Id(), "*", "pair_exception"));
                results.add(new GameResult(pair.getGame2Id(), "*", "pair_exception"));
            } else if (results.size() == 1) {
                // Second game failed
                results.add(new GameResult(pair.getGame2Id(), "*", "pair_exception"));
            }
            return new PairResult(pair.getPairId(), results, engine1Name, engine2Name);
        }
    }
}
