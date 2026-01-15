package fr.flwrian.WebSocket;

import com.google.gson.JsonObject;

/**
 * Messages sent via WebSocket.
 */
public class WSMessage {
    
    public static JsonObject gameStart(int gameId, String white, String black, String fen, long baseTime, long increment) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "game_start");
        msg.addProperty("gameId", gameId);
        msg.addProperty("white", white);
        msg.addProperty("black", black);
        msg.addProperty("fen", fen);
        msg.addProperty("baseTime", baseTime);
        msg.addProperty("increment", increment);
        msg.addProperty("timestamp", System.currentTimeMillis());
        return msg;
    }

    public static JsonObject move(int gameId, String move, String fen, long whiteTime, long blackTime, 
                                   int moveNumber, boolean isWhiteMove) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "move");
        msg.addProperty("gameId", gameId);
        msg.addProperty("move", move);
        msg.addProperty("fen", fen);
        msg.addProperty("whiteTime", whiteTime);
        msg.addProperty("blackTime", blackTime);
        msg.addProperty("moveNumber", moveNumber);
        msg.addProperty("isWhiteMove", isWhiteMove);
        msg.addProperty("timestamp", System.currentTimeMillis());
        return msg;
    }
    
    public static JsonObject move(int gameId, String move, String fen, long whiteTime, long blackTime, 
                                   int moveNumber, boolean isWhiteMove, 
                                   Integer whiteScore, Integer blackScore,
                                   Integer whiteDepth, Integer blackDepth,
                                   String whitePv, String blackPv) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "move");
        msg.addProperty("gameId", gameId);
        msg.addProperty("move", move);
        msg.addProperty("fen", fen);
        msg.addProperty("whiteTime", whiteTime);
        msg.addProperty("blackTime", blackTime);
        msg.addProperty("moveNumber", moveNumber);
        msg.addProperty("isWhiteMove", isWhiteMove);
        msg.addProperty("timestamp", System.currentTimeMillis());
        
        // Add evaluation data if available
        if (whiteScore != null) msg.addProperty("whiteScore", whiteScore);
        if (blackScore != null) msg.addProperty("blackScore", blackScore);
        if (whiteDepth != null) msg.addProperty("whiteDepth", whiteDepth);
        if (blackDepth != null) msg.addProperty("blackDepth", blackDepth);
        if (whitePv != null) msg.addProperty("whitePv", whitePv);
        if (blackPv != null) msg.addProperty("blackPv", blackPv);
        
        return msg;
    }

    public static JsonObject gameEnd(int gameId, String result, String reason, int totalMoves) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "game_end");
        msg.addProperty("gameId", gameId);
        msg.addProperty("result", result);
        msg.addProperty("reason", reason);
        msg.addProperty("totalMoves", totalMoves);
        msg.addProperty("timestamp", System.currentTimeMillis());
        return msg;
    }
    
    /**
     * Engine thinking update (real-time analysis)
     */
    public static JsonObject engineThinking(int gameId, boolean isWhite, Integer score, 
                                             Integer depth, String pv) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "engine_thinking");
        msg.addProperty("gameId", gameId);
        msg.addProperty("isWhite", isWhite);
        msg.addProperty("timestamp", System.currentTimeMillis());
        
        if (score != null) msg.addProperty("score", score);
        if (depth != null) msg.addProperty("depth", depth);
        if (pv != null) msg.addProperty("pv", pv);
        
        return msg;
    }

    public static JsonObject engineInfo(int gameId, String info) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "engine_info");
        msg.addProperty("gameId", gameId);
        msg.addProperty("info", info);
        msg.addProperty("timestamp", System.currentTimeMillis());
        return msg;
    }

    public static JsonObject matchStats(int totalGames, int completed, double engine1Score, double engine2Score) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "match_stats");
        msg.addProperty("totalGames", totalGames);
        msg.addProperty("completed", completed);
        msg.addProperty("engine1Score", engine1Score);
        msg.addProperty("engine2Score", engine2Score);
        msg.addProperty("timestamp", System.currentTimeMillis());
        return msg;
    }

    public static JsonObject error(int gameId, String error) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "error");
        msg.addProperty("gameId", gameId);
        msg.addProperty("error", error);
        msg.addProperty("timestamp", System.currentTimeMillis());
        return msg;
    }
}
