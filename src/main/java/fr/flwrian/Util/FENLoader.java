package fr.flwrian.Util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for loading FEN positions from files.
 */
public class FENLoader {
    
    /**
     * Load FEN positions from a file.
     * Each line should contain a FEN string (comments starting with # are ignored).
     * 
     * Example format:
     * rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1 # 1.e4
     * rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1 # 1.d4
     * 
     * @param filename Path to the FEN file
     * @return List of FEN strings
     * @throws IOException if file cannot be read
     */
    public static List<String> loadFromFile(String filename) throws IOException {
        List<String> fens = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Remove inline comments
                int commentIndex = line.indexOf('#');
                if (commentIndex > 0) {
                    line = line.substring(0, commentIndex).trim();
                }
                
                // Basic FEN validation (should have at least 6 parts)
                String[] parts = line.split("\\s+");
                if (parts.length < 6) {
                    System.err.println("Warning: Invalid FEN at line " + lineNumber + ": " + line);
                    continue;
                }
                
                fens.add(line);
            }
        }
        
        return fens;
    }
    
    /**
     * Create a list of common starting positions (popular openings).
     * 
     * @return List of FEN strings representing popular opening positions
     */
    public static List<String> getCommonOpenings() {
        List<String> openings = new ArrayList<>();
        
        // Starting position
        openings.add("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        
        // 1.e4
        openings.add("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        
        // 1.d4
        openings.add("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1");
        
        // 1.c4 (English)
        openings.add("rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq c3 0 1");
        
        // 1.Nf3
        openings.add("rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1");
        
        // 1.e4 e5 (Open game)
        openings.add("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2");
        
        // 1.e4 c5 (Sicilian)
        openings.add("rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2");
        
        // 1.d4 d5 (Closed game)
        openings.add("rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6 0 2");
        
        // 1.d4 Nf6 (Indian defenses)
        openings.add("rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 1 2");
        
        return openings;
    }
    
    /**
     * Validate a FEN string (basic validation).
     * 
     * @param fen FEN string to validate
     * @return true if FEN appears valid
     */
    public static boolean isValidFEN(String fen) {
        if (fen == null || fen.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = fen.trim().split("\\s+");
        
        // Must have 6 parts: position, turn, castling, en passant, halfmove, fullmove
        if (parts.length < 6) {
            return false;
        }
        
        // Check position has 8 ranks
        String[] ranks = parts[0].split("/");
        if (ranks.length != 8) {
            return false;
        }
        
        // Check turn is 'w' or 'b'
        if (!parts[1].equals("w") && !parts[1].equals("b")) {
            return false;
        }
        
        return true;
    }
}
