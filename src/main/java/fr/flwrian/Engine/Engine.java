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

    public Engine(String path) throws Exception {
        this.enginePath = path;
        
        // Vérifications détaillées du fichier engine
        File engineFile = new File(path);
        System.out.println("🔍 [Engine] Initialisation de l'engine: " + path);
        System.out.println("  ├─ Chemin absolu: " + engineFile.getAbsolutePath());
        System.out.println("  ├─ Existe: " + engineFile.exists());
        System.out.println("  ├─ Est un fichier: " + engineFile.isFile());
        System.out.println("  ├─ Peut être lu: " + engineFile.canRead());
        System.out.println("  ├─ Peut être exécuté: " + engineFile.canExecute());
        System.out.println("  └─ Taille: " + engineFile.length() + " octets");
        
        if (!engineFile.exists()) {
            throw new Exception("❌ Engine introuvable: " + path);
        }
        
        if (!engineFile.canExecute()) {
            System.err.println("⚠️ Engine n'est pas exécutable: " + path);
            System.err.println("   Tentative de rendre exécutable...");
            engineFile.setExecutable(true);
        }
        
        System.out.println("🚀 [Engine] Démarrage du processus...");
        process = new ProcessBuilder(path).start();
        
        if (!process.isAlive()) {
            throw new Exception("❌ Le processus engine n'a pas démarré correctement");
        }
        
        System.out.println("✓ [Engine] Processus démarré (PID: " + process.pid() + ")");
        in = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        // Thread pour capturer la sortie standard de l'engine
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("📤 [Engine] " + enginePath + " -> " + line);
                    outQueue.put(line);
                }
            } catch (Exception e) {
                System.err.println("❌ [Engine] Erreur lecture sortie: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
        
        // Thread pour capturer les erreurs de l'engine
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.err.println("⚠️ [Engine ERROR] " + enginePath + " -> " + line);
                }
            } catch (Exception e) {
                System.err.println("❌ [Engine] Erreur lecture stderr: " + e.getMessage());
            }
        }).start();

        System.out.println("📨 [Engine] Envoi de la commande 'uci'...");
        send("uci");
        System.out.println("⏳ [Engine] Attente de 'uciok'...");
        waitFor("uciok");
        System.out.println("✓ [Engine] 'uciok' reçu");
        
        System.out.println("📨 [Engine] Envoi de la commande 'isready'...");
        send("isready");
        System.out.println("⏳ [Engine] Attente de 'readyok'...");
        waitFor("readyok");
        System.out.println("✓ [Engine] 'readyok' reçu - Engine prêt!");
    }

    public void send(String cmd) throws Exception {
        System.out.println("📥 [Engine] " + enginePath + " <- " + cmd);
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
}
