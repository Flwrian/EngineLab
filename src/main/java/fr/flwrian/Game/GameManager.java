package fr.flwrian.Game;

import fr.flwrian.Chess.ChessValidator;
import fr.flwrian.Engine.Engine;
import fr.flwrian.Result.GameResult;
import fr.flwrian.WebSocket.GameWebSocket;
import fr.flwrian.WebSocket.WSMessage;

import java.util.concurrent.TimeoutException;

/**
 * Manages a single chess game between two engines.
 * Handles the game loop, move validation, time control, and termination conditions.
 */
public class GameManager {
    private static final int MAX_MOVES = 500;
    private static final long MOVE_OVERHEAD_MS = 50; // Safety margin
    
    private final Engine whiteEngine;
    private final Engine blackEngine;
    private final GameState gameState;
    private final ChessValidator validator;
    private final int gameId;
    private final String whiteEngineName;
    private final String blackEngineName;

    public GameManager(int gameId, Engine whiteEngine, Engine blackEngine, TimeControl timeControl) {
        this(gameId, whiteEngine, blackEngine, "startpos", timeControl, "White", "Black");
    }

    public GameManager(int gameId, Engine whiteEngine, Engine blackEngine, String startFen, TimeControl timeControl) {
        this(gameId, whiteEngine, blackEngine, startFen, timeControl, "White", "Black");
    }

    public GameManager(int gameId, Engine whiteEngine, Engine blackEngine, String startFen, 
                       TimeControl timeControl, String whiteName, String blackName) {
        this.gameId = gameId;
        this.whiteEngine = whiteEngine;
        this.blackEngine = blackEngine;
        this.gameState = new GameState(startFen, timeControl);
        this.whiteEngineName = whiteName;
        this.blackEngineName = blackName;
        
        // Initialize chess validator
        if (startFen.equals("startpos")) {
            this.validator = new ChessValidator();
        } else {
            this.validator = new ChessValidator(startFen);
        }
    }

    /**
     * Runs the complete game and returns the result.
     */
    public GameResult run() {
        try {
            // Prepare both engines
            whiteEngine.newGame();
            blackEngine.newGame();

            // Broadcast game start
            broadcastGameStart();

            // Main game loop
            while (true) {
                // Check max moves
                if (gameState.getMoveCount() >= MAX_MOVES) {
                    GameResult result = new GameResult(gameId, "1/2-1/2", "max_moves");
                    broadcastGameEnd(result);
                    return result;
                }
                
                // Check chess-specific terminations using validator
                String chessResult = validator.getResult();
                if (chessResult != null) {
                    String reason = validator.getTerminationReason();
                    GameResult result = new GameResult(gameId, chessResult, reason);
                    broadcastGameEnd(result);
                    return result;
                }

                // Check time before move
                if (!gameState.hasTimeLeft()) {
                    String result = gameState.isWhiteToMove() ? "0-1" : "1-0";
                    GameResult gr = new GameResult(gameId, result, "time_forfeit");
                    broadcastGameEnd(gr);
                    return gr;
                }

                // Get current engine and opponent
                Engine currentEngine = gameState.isWhiteToMove() ? whiteEngine : blackEngine;
                boolean isWhite = gameState.isWhiteToMove();

                // Check if engine is alive
                if (!currentEngine.isAlive()) {
                    System.err.println(String.format("Game %d: Engine crashed (was %s to move)", 
                        gameId, isWhite ? "white" : "black"));
                    String result = isWhite ? "0-1" : "1-0";
                    GameResult gr = new GameResult(gameId, result, "engine_crash");
                    broadcastGameEnd(gr);
                    return gr;
                }

                // Send position to engine
                currentEngine.send(gameState.getPositionCommand());

                // Send go command and measure time
                long startTime = System.currentTimeMillis();
                currentEngine.send(gameState.getGoCommand());

                // Wait for bestmove with timeout
                String bestMove;
                try {
                    bestMove = waitForBestMove(currentEngine, isWhite);
                } catch (TimeoutException e) {
                    // Send stop command to engine to interrupt thinking
                    try {
                        currentEngine.send("stop");
                        // Try to read the bestmove that should come after stop
                        String line = currentEngine.pollLine(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                        if (line != null && line.startsWith("bestmove")) {
                            System.out.println("Engine responded to stop: " + line);
                        }
                    } catch (Exception stopException) {
                        System.err.println("Failed to stop engine after timeout: " + stopException.getMessage());
                    }
                    
                    String result = isWhite ? "0-1" : "1-0";
                    GameResult gr = new GameResult(gameId, result, "timeout");
                    broadcastGameEnd(gr);
                    return gr;
                }

                long elapsed = System.currentTimeMillis() - startTime;

                // Validate move format
                if (bestMove == null || bestMove.isEmpty()) {
                    String result = isWhite ? "0-1" : "1-0";
                    GameResult gr = new GameResult(gameId, result, "illegal_move");
                    broadcastGameEnd(gr);
                    return gr;
                }

                // Check for resignation or draw claims
                if (bestMove.equals("(none)") || bestMove.equals("0000")) {
                    String result = isWhite ? "0-1" : "1-0";
                    GameResult gr = new GameResult(gameId, result, "resignation");
                    broadcastGameEnd(gr);
                    return gr;
                }

                // Validate move legality with chess library
                if (!validator.isMoveLegal(bestMove)) {
                    String result = isWhite ? "0-1" : "1-0";
                    GameResult gr = new GameResult(gameId, result, "illegal_move");
                    broadcastGameEnd(gr);
                    return gr;
                }

                // Apply move to both tracker and validator
                validator.applyMove(bestMove);
                gameState.addMove(bestMove, elapsed);

                // Broadcast move to connected clients
                broadcastMove(bestMove);

                // Optional: Log progress
                if (gameState.getMoveCount() % 20 == 0) {
                    System.out.println(String.format("Game %d: Move %d, Time: %s",
                        gameId, gameState.getMoveCount(), gameState.getTimeControl()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            GameResult gr = new GameResult(gameId, "*", "exception: " + e.getMessage());
            broadcastGameEnd(gr);
            return gr;
        }
    }

    /**
     * Waits for bestmove response from engine with timeout based on remaining time.
     * Also captures the last evaluation score from info lines.
     */
    private String waitForBestMove(Engine engine, boolean isWhite) throws InterruptedException, TimeoutException {
        long timeLeft = isWhite ? 
            gameState.getTimeControl().getWhiteTime() : 
            gameState.getTimeControl().getBlackTime();
        
        // Set timeout to remaining time + increment + overhead
        long increment = isWhite ? 
            gameState.getTimeControl().getWhiteIncrement() : 
            gameState.getTimeControl().getBlackIncrement();
        
        long timeoutMs = timeLeft + increment + MOVE_OVERHEAD_MS;
        long deadline = System.currentTimeMillis() + timeoutMs;

        Integer lastScore = null; // Centipawn score
        Integer lastDepth = null;
        String lastPv = null;

        while (true) {
            // Check if engine is still alive before polling
            if (!engine.isAlive()) {
                System.err.println("Engine died while waiting for bestmove");
                throw new TimeoutException("Engine process died");
            }
            
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                throw new TimeoutException("Engine exceeded time limit");
            }

            // Poll with shorter timeout (max 1 second) to check engine health regularly
            long pollTimeout = Math.min(remaining, 1000);
            String line = engine.pollLine(pollTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            if (line == null) {
                // No line received in this poll interval
                // Check if we've exceeded the deadline
                if (System.currentTimeMillis() >= deadline) {
                    throw new TimeoutException("Engine exceeded time limit");
                }
                // Otherwise, continue polling
                continue;
            }

            // Parse info lines for evaluation
            if (line.startsWith("info ")) {
                // Extract score cp (centipawns)
                if (line.contains(" score cp ")) {
                    try {
                        int scoreIndex = line.indexOf(" score cp ");
                        String afterScore = line.substring(scoreIndex + 10);
                        String[] parts = afterScore.split("\\s+");
                        if (parts.length > 0) {
                            lastScore = Integer.parseInt(parts[0]);
                        }
                    } catch (Exception e) {
                        // Ignore parse errors
                    }
                }
                
                // Extract score mate
                if (line.contains(" score mate ")) {
                    try {
                        int mateIndex = line.indexOf(" score mate ");
                        String afterMate = line.substring(mateIndex + 12);
                        String[] parts = afterMate.split("\\s+");
                        if (parts.length > 0) {
                            int mateIn = Integer.parseInt(parts[0]);
                            // Convert mate distance to score: 10000 - (mate_distance * 100)
                            // Positive mate = good for white, negative = good for black
                            if (mateIn > 0) {
                                lastScore = 10000 - (mateIn * 100);
                            } else {
                                lastScore = -10000 - (mateIn * 100);
                            }
                        }
                    } catch (Exception e) {
                        // Ignore parse errors
                    }
                }
                
                // Extract depth
                if (line.contains(" depth ")) {
                    try {
                        int depthIndex = line.indexOf(" depth ");
                        String afterDepth = line.substring(depthIndex + 7);
                        String[] parts = afterDepth.split("\\s+");
                        if (parts.length > 0) {
                            lastDepth = Integer.parseInt(parts[0]);
                        }
                    } catch (Exception e) {
                        // Ignore parse errors
                    }
                }
                
                // Extract PV (principal variation) - get first 5 moves
                if (line.contains(" pv ")) {
                    try {
                        int pvIndex = line.indexOf(" pv ");
                        String pvString = line.substring(pvIndex + 4);
                        String[] pvMoves = pvString.trim().split("\\s+");
                        // Take first 5 moves max
                        int movesToTake = Math.min(5, pvMoves.length);
                        StringBuilder pvBuilder = new StringBuilder();
                        for (int i = 0; i < movesToTake; i++) {
                            if (i > 0) pvBuilder.append(" ");
                            pvBuilder.append(pvMoves[i]);
                        }
                        lastPv = pvBuilder.toString();
                    } catch (Exception e) {
                        // Ignore parse errors
                    }
                }
                
                // Broadcast thinking update in real-time
                if (lastScore != null || lastDepth != null || lastPv != null) {
                    GameWebSocket.broadcast(WSMessage.engineThinking(
                        gameId, isWhite, lastScore, lastDepth, lastPv
                    ));
                }
            }

            if (line.startsWith("bestmove ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    // Store evaluation in game state for broadcast
                    gameState.setLastEvaluation(isWhite, lastScore, lastDepth, lastPv);
                    return parts[1];
                }
            }
        }
    }

    /**
     * Broadcast game start to WebSocket clients.
     */
    private void broadcastGameStart() {
        try {
            String fen = gameState.getPositionCommand().contains("startpos") ? 
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" : 
                validator.getFen();
            GameWebSocket.broadcast(WSMessage.gameStart(
                gameId, 
                whiteEngineName, 
                blackEngineName, 
                fen,
                gameState.getTimeControl().getWhiteTime(),
                gameState.getTimeControl().getWhiteIncrement()
            ));
        } catch (Exception e) {
            // Don't crash game if broadcast fails
        }
    }

    /**
     * Broadcast move to WebSocket clients.
     */
    private void broadcastMove(String move) {
        try {
            GameWebSocket.broadcast(WSMessage.move(
                gameId,
                move,
                validator.getFen(),
                gameState.getTimeControl().getWhiteTime(),
                gameState.getTimeControl().getBlackTime(),
                gameState.getFullMoveNumber(),
                !gameState.isWhiteToMove(), // Move was just played, so flip
                gameState.getWhiteScore(),
                gameState.getBlackScore(),
                gameState.getWhiteDepth(),
                gameState.getBlackDepth(),
                gameState.getWhitePv(),
                gameState.getBlackPv()
            ));
        } catch (Exception e) {
            // Don't crash game if broadcast fails
        }
    }

    /**
     * Broadcast game end to WebSocket clients.
     */
    private void broadcastGameEnd(GameResult result) {
        try {
            GameWebSocket.broadcast(WSMessage.gameEnd(
                gameId,
                result.getResult(),
                result.getReason(),
                gameState.getMoveCount()
            ));
        } catch (Exception e) {
            // Don't crash game if broadcast fails
        }
    }
}
