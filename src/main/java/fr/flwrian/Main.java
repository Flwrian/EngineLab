package fr.flwrian;

/**
 * Main entry point - redirects to ConfigMain.
 * EngineLab now uses config.yml for all configuration.
 * 
 * Usage: java -jar enginelab.jar [config.yml]
 * 
 * See config.yml for configuration details.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("⚠️  EngineLab now uses config.yml for configuration.");
        System.out.println("📖 See config.yml for details.");
        System.out.println("");
        System.out.println("Redirecting to ConfigMain...");
        System.out.println("─────────────────────────────────────");
        System.out.println("");
        
        ConfigMain.main(args);
    }
}