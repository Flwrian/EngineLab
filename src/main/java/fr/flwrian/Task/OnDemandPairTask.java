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
 * Engines are created on-demand and closed after the pair completes.
 */
public class OnDemandPairTask implements Callable<PairResult> {
    private final String engine1Path;
    private final String engine2Path;
    private final MatchPair pair;
    private final TimeControl baseTimeControl;
    private final String engine1Name;
    private final String engine2Name;

    public OnDemandPairTask(MatchPair pair, String engine1Path, String engine2Path, 
                            TimeControl baseTimeControl, String engine1Name, String engine2Name) {
        this.pair = pair;
        this.engine1Path = engine1Path;
        this.engine2Path = engine2Path;
        this.baseTimeControl = baseTimeControl;
        this.engine1Name = engine1Name;
        this.engine2Name = engine2Name;
    }

    @Override
    public PairResult call() {
        List<GameResult> results = new ArrayList<>();
        String threadName = Thread.currentThread().getName();
        Engine engine1 = null;
        Engine engine2 = null;
        
        try {
            System.out.println("[" + threadName + "] === " + pair + " starting ===");
            System.out.println("[" + threadName + "] Matchup: " + engine1Name + " vs " + engine2Name);
            
            // Create engines on-demand
            System.out.println("[" + threadName + "] Creating engine instances...");
            engine1 = new Engine(engine1Path);
            engine2 = new Engine(engine2Path);
            System.out.println("[" + threadName + "] Engines created successfully");
            
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
            System.err.println("[" + threadName + "] Exception in pair " + pair.getPairId() + ": " + e.getMessage());
            e.printStackTrace();
            // If we have partial results, return them
            if (results.isEmpty()) {
                // Both games failed - return error results
                GameResult errorGame1 = new GameResult(
                    pair.getGame1Id(),
                    "0-0",
                    "Error: " + e.getMessage()
                );
                GameResult errorGame2 = new GameResult(
                    pair.getGame2Id(),
                    "0-0",
                    "Error: " + e.getMessage()
                );
                results.add(errorGame1);
                results.add(errorGame2);
            }
            return new PairResult(pair.getPairId(), results, engine1Name, engine2Name);
        } finally {
            // Always close engines after pair completes
            System.out.println("[" + threadName + "] Closing engines for pair " + pair.getPairId());
            if (engine1 != null) {
                try {
                    engine1.close();
                } catch (Exception e) {
                    System.err.println("[" + threadName + "] Error closing engine1: " + e.getMessage());
                }
            }
            if (engine2 != null) {
                try {
                    engine2.close();
                } catch (Exception e) {
                    System.err.println("[" + threadName + "] Error closing engine2: " + e.getMessage());
                }
            }
            System.out.println("[" + threadName + "] Engines closed for pair " + pair.getPairId());
        }
    }
}
