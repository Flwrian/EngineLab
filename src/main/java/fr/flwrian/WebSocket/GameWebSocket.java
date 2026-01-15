package fr.flwrian.WebSocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import fr.flwrian.Stats.StatsManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket endpoint for live game streaming.
 */
@WebSocket
public class GameWebSocket {
    private static final CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<>();
    private static final Gson gson = new Gson();
    private static StatsManager statsManager;
    
    // Store current game states for late-joining clients
    private static final ConcurrentHashMap<Integer, JsonObject> gameStates = new ConcurrentHashMap<>();
    
    // Store move history for each game
    private static final ConcurrentHashMap<Integer, List<String>> gameMoves = new ConcurrentHashMap<>();
    
    /**
     * Set the stats manager for broadcasting leaderboard updates
     */
    public static void setStatsManager(StatsManager manager) {
        statsManager = manager;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.add(session);
        System.out.println("WebSocket connected: " + session.getRemoteAddress());
        
        // Send welcome message
        JsonObject welcome = new JsonObject();
        welcome.addProperty("type", "connected");
        welcome.addProperty("message", "Connected to EngineLab live stream");
        sendToSession(session, welcome);
        
        // Send current game states to new client
        for (Map.Entry<Integer, JsonObject> entry : gameStates.entrySet()) {
            sendToSession(session, entry.getValue());
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
        System.out.println("WebSocket disconnected: " + session.getRemoteAddress());
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            JsonObject request = gson.fromJson(message, JsonObject.class);
            if (request.has("type")) {
                String type = request.get("type").getAsString();
                
                // Handle leaderboard request
                if ("get_leaderboard".equals(type) && statsManager != null) {
                    JsonObject response = new JsonObject();
                    response.addProperty("type", "leaderboard");
                    response.add("stats", gson.toJsonTree(statsManager.getStats()));
                    sendToSession(session, response);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }
    
    /**
     * Broadcast leaderboard update to all clients
     */
    public static void broadcastLeaderboard() {
        if (statsManager != null) {
            JsonObject message = new JsonObject();
            message.addProperty("type", "leaderboard");
            message.add("stats", gson.toJsonTree(statsManager.getStats()));
            broadcast(message);
        }
    }

    /**
     * Broadcast message to all connected clients.
     */
    public static void broadcast(JsonObject message) {
        // Store/update game state for late-joining clients (only for game messages)
        if (message.has("gameId")) {
            String type = message.get("type").getAsString();
            int gameId = message.get("gameId").getAsInt();
            
            if ("game_start".equals(type)) {
            // Store initial game state
            JsonObject state = new JsonObject();
            state.addProperty("type", "game_start");
            state.addProperty("gameId", gameId);
            state.addProperty("white", message.has("white") ? message.get("white").getAsString() : "Engine");
            state.addProperty("black", message.has("black") ? message.get("black").getAsString() : "Engine");
            state.addProperty("fen", message.has("fen") ? message.get("fen").getAsString() : "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            state.addProperty("whiteTime", message.has("whiteTime") ? message.get("whiteTime").getAsLong() : 0);
            state.addProperty("blackTime", message.has("blackTime") ? message.get("blackTime").getAsLong() : 0);
            
            // Initialize empty move history
            gameMoves.put(gameId, new ArrayList<>());
            
            gameStates.put(gameId, state);
        } else if ("move".equals(type)) {
            // Store the move in history
            if (message.has("move")) {
                List<String> moves = gameMoves.get(gameId);
                if (moves != null) {
                    moves.add(message.get("move").getAsString());
                }
            }
            
            // Update existing game state with latest position and times
            JsonObject state = gameStates.get(gameId);
            if (state != null) {
                state.addProperty("fen", message.get("fen").getAsString());
                state.addProperty("whiteTime", message.has("whiteTime") ? message.get("whiteTime").getAsLong() : 0);
                state.addProperty("blackTime", message.has("blackTime") ? message.get("blackTime").getAsLong() : 0);
                
                // Add move history to state for late joiners
                List<String> moves = gameMoves.get(gameId);
                if (moves != null && !moves.isEmpty()) {
                    JsonArray movesArray = new JsonArray();
                    for (String move : moves) {
                        movesArray.add(move);
                    }
                    state.add("moves", movesArray);
                }
                
                // Update evaluation scores if present
                if (message.has("whiteScore")) {
                    state.addProperty("whiteScore", message.get("whiteScore").getAsInt());
                }
                if (message.has("blackScore")) {
                    state.addProperty("blackScore", message.get("blackScore").getAsInt());
                }
                if (message.has("whiteDepth")) {
                    state.addProperty("whiteDepth", message.get("whiteDepth").getAsInt());
                }
                if (message.has("blackDepth")) {
                    state.addProperty("blackDepth", message.get("blackDepth").getAsInt());
                }
                if (message.has("whitePv")) {
                    state.addProperty("whitePv", message.get("whitePv").getAsString());
                }
                if (message.has("blackPv")) {
                    state.addProperty("blackPv", message.get("blackPv").getAsString());
                }
            }
        } else if ("game_end".equals(type)) {
            // Remove finished game from state
            gameStates.remove(gameId);
            gameMoves.remove(gameId);
        }
        } // Close the if (message.has("gameId"))
        
        String json = gson.toJson(message);
        for (Session session : sessions) {
            sendToSession(session, json);
        }
    }

    private static void sendToSession(Session session, JsonObject message) {
        sendToSession(session, gson.toJson(message));
    }

    private static void sendToSession(Session session, String json) {
        if (session.isOpen()) {
            try {
                session.getRemote().sendString(json);
            } catch (Exception e) {
                System.err.println("Error sending to session: " + e.getMessage());
            }
        }
    }

    public static int getConnectedClients() {
        return sessions.size();
    }
}
