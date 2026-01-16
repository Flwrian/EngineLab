package fr.flwrian.Engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Engine {
    private final Process process;
    private final BufferedWriter in;
    private final BlockingQueue<String> outQueue = new LinkedBlockingQueue<>();
    private final String enginePath;
    private static boolean logCommunication = false;

    /**
     * Enable or disable UCI communication logging for all engines.
     */
    public static void setLogCommunication(boolean enabled) {
        logCommunication = enabled;
    }

    public Engine(String path) throws Exception {
        this.enginePath = path;
        
        // Detailed engine file verification
        File engineFile = new File(path);
        // System.out.println("[Engine] Initializing engine: " + path);
        // System.out.println("  ├─ Absolute path: " + engineFile.getAbsolutePath());
        // System.out.println("  ├─ Exists: " + engineFile.exists());
        // System.out.println("  ├─ Is file: " + engineFile.isFile());
        // System.out.println("  ├─ Can read: " + engineFile.canRead());
        // System.out.println("  ├─ Can execute: " + engineFile.canExecute());
        // System.out.println("  └─ Size: " + engineFile.length() + " bytes");
        
        if (!engineFile.exists()) {
            throw new Exception("Engine not found: " + path);
        }
        
        if (!engineFile.canExecute()) {
            System.err.println("Engine is not executable: " + path);
            System.err.println("   Attempting to make executable...");
            engineFile.setExecutable(true);
        }
        
        // System.out.println("[Engine] Starting process...");
        process = new ProcessBuilder(path).start();
        
        if (!process.isAlive()) {
            throw new Exception("Engine process failed to start properly");
        }
        
        // System.out.println("[Engine] Process started (PID: " + process.pid() + ")");
        in = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        // Thread to capture engine standard output
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (logCommunication) {
                        System.out.println("[UCI ->] " + line);
                    }
                    outQueue.put(line);
                }
            } catch (Exception e) {
                System.err.println("[Engine] Error reading output: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
        
        // Thread to capture engine errors
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.err.println("[Engine ERROR] " + enginePath + " -> " + line);
                }
            } catch (Exception e) {
                System.err.println("[Engine] Error reading stderr: " + e.getMessage());
            }
        }).start();

        // System.out.println("[Engine] Sending 'uci' command...");
        send("uci");
        // System.out.println("[Engine] Waiting for 'uciok'...");
        waitFor("uciok");
        // System.out.println("[Engine] 'uciok' received");
        
        // System.out.println("[Engine] Sending 'isready' command...");
        send("isready");
        // System.out.println("[Engine] Waiting for 'readyok'...");
        waitFor("readyok");
        // System.out.println("[Engine] 'readyok' received - Engine ready!");
    }

    public void send(String cmd) throws Exception {
        if (logCommunication) {
            System.out.println("[UCI <-] " + cmd);
        }
        in.write(cmd + "\n");
        in.flush();
    }

    public String takeLine() throws InterruptedException {
        return outQueue.take();
    }

    /**
     * Poll for a line with timeout.
     * @param timeout timeout value
     * @param unit timeout unit
     * @return line or null if timeout
     */
    public String pollLine(long timeout, TimeUnit unit) throws InterruptedException {
        return outQueue.poll(timeout, unit);
    }

    public void waitFor(String token) throws InterruptedException {
        while (true) {
            String l = takeLine();
            if (l.contains(token)) return;
        }
    }

    public void newGame() throws Exception {
        send("ucinewgame");
        send("isready");
        waitFor("readyok");
    }

    /**
     * Reset engine state: clear output queue and reinitialize for new game.
     * Should be called between games to ensure clean state.
     */
    public void reset() throws Exception {
        // Clear any pending output from previous game
        outQueue.clear();
        
        // Reinitialize engine
        send("ucinewgame");
        send("isready");
        waitFor("readyok");
    }

    public boolean isAlive() {
        return process.isAlive();
    }
    
    /**
     * Close the engine process properly.
     */
    public void close() {
        try {
            // System.out.println("[Engine] Closing engine: " + enginePath);
            send("quit");
            
            // Give the engine 2 seconds to close gracefully
            boolean exited = process.waitFor(2, TimeUnit.SECONDS);
            
            if (!exited) {
                System.err.println("[Engine] Engine did not exit gracefully, forcing termination");
                process.destroyForcibly();
            }
            
            // System.out.println("[Engine] Engine closed: " + enginePath);
        } catch (Exception e) {
            System.err.println("[Engine] Error closing engine: " + e.getMessage());
            process.destroyForcibly();
        }
    }
}
