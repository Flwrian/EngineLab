package fr.flwrian.Task;

import java.util.concurrent.Callable;

import fr.flwrian.Engine.Engine;
import fr.flwrian.Game.GameManager;
import fr.flwrian.Game.TimeControl;
import fr.flwrian.Result.GameResult;

/**
 * Represents a single game task that can be executed by a thread pool.
 * Uses GameManager to run the actual game with proper time control.
 */
public class GameTask implements Callable<GameResult> {
    private final Engine white;
    private final Engine black;
    private final int id;
    private final String startFen;
    private final TimeControl timeControl;

    public GameTask(int id, Engine white, Engine black, TimeControl timeControl) {
        this(id, white, black, "startpos", timeControl);
    }

    public GameTask(int id, Engine white, Engine black, String startFen, TimeControl timeControl) {
        this.id = id;
        this.white = white;
        this.black = black;
        this.startFen = startFen;
        this.timeControl = timeControl.copy(); // Copy to avoid shared state
    }

    @Override
    public GameResult call() {
        try {
            System.out.println("Game " + id + " started: White vs Black [" + 
                (startFen.equals("startpos") ? "Standard" : "FEN") + "]");
            
            GameManager manager = new GameManager(id, white, black, startFen, timeControl, 
                "Engine", "Engine");
            GameResult result = manager.run();
            
            System.out.println("Game " + id + " finished: " + result.getResult() + 
                " (" + result.getReason() + ")");
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new GameResult(id, "*", "task_exception: " + e.getMessage());
        }
    }
}
