package fr.flwrian.WebSocket;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * WebSocket server for live game streaming with SSL/TLS support.
 */
public class WebSocketServer {
    private final Server server;
    private final int port;
    private final boolean sslEnabled;
    private final int sslPort;

    public WebSocketServer(int port) {
        this(port, false, 8443, null, null, null);
    }

    public WebSocketServer(int port, boolean sslEnabled, int sslPort, 
                          String keyStorePath, String keyStorePassword, String keyStoreType) {
        this.port = port;
        this.sslEnabled = sslEnabled;
        this.sslPort = sslPort;
        this.server = new Server();
        
        // HTTP connector
        ServerConnector httpConnector = new ServerConnector(server);
        httpConnector.setPort(port);
        server.addConnector(httpConnector);
        
        // HTTPS/WSS connector (if SSL enabled)
        if (sslEnabled && keyStorePath != null && keyStorePassword != null) {
            try {
                if (!Files.exists(Paths.get(keyStorePath))) {
                    System.err.println("WARNING: SSL keystore not found: " + keyStorePath);
                    System.err.println("   Falling back to HTTP only");
                } else {
                    ServerConnector httpsConnector = createSslConnector(keyStorePath, keyStorePassword, keyStoreType);
                    server.addConnector(httpsConnector);
                    System.out.println("SSL/TLS enabled on port " + sslPort);
                }
            } catch (Exception e) {
                System.err.println("WARNING: Failed to configure SSL: " + e.getMessage());
                System.err.println("   Falling back to HTTP only");
            }
        }

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // WebSocket endpoint
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.setMaxTextMessageSize(65536);
            wsContainer.setIdleTimeout(Duration.ofMinutes(10));
            wsContainer.addMapping("/ws", (req, resp) -> new GameWebSocket());
        });

        // HTTP endpoint for testing
        context.addServlet(new ServletHolder(new StatusServlet()), "/status");
        context.addServlet(new ServletHolder(new StaticResourceServlet()), "/static/*");
        context.addServlet(new ServletHolder(new LiveServlet()), "/live");
        context.addServlet(new ServletHolder(new LeaderboardServlet()), "/leaderboard");
        context.addServlet(new ServletHolder(new IndexServlet()), "/");
    }

    /**
     * Create SSL/TLS connector for HTTPS and WSS.
     */
    private ServerConnector createSslConnector(String keyStorePath, String keyStorePassword, String keyStoreType) {
        // SSL Context Factory
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(keyStorePath);
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.setKeyStoreType(keyStoreType != null ? keyStoreType : "PKCS12");
        
        // Secure protocols
        sslContextFactory.setIncludeProtocols("TLSv1.2", "TLSv1.3");
        sslContextFactory.setExcludeProtocols("SSLv3", "TLSv1", "TLSv1.1");
        
        // Strong ciphers only
        sslContextFactory.setIncludeCipherSuites(
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256"
        );
        
        // HTTP Configuration for HTTPS
        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme("https");
        httpsConfig.setSecurePort(sslPort);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        
        // SSL Connector
        ServerConnector sslConnector = new ServerConnector(
            server,
            new SslConnectionFactory(sslContextFactory, "http/1.1"),
            new HttpConnectionFactory(httpsConfig)
        );
        sslConnector.setPort(sslPort);
        
        return sslConnector;
    }

    public void start() throws Exception {
        server.start();
        System.out.println("WebSocket server started on port " + port);
        System.out.println("WebSocket endpoint: ws://localhost:" + port + "/ws");
        System.out.println("HTTP status: http://localhost:" + port + "/status");
        
        if (sslEnabled) {
            System.out.println("Secure WebSocket endpoint: wss://localhost:" + sslPort + "/ws");
            System.out.println("HTTPS status: https://localhost:" + sslPort + "/status");
        }
    }

    public void stop() throws Exception {
        System.out.println("Closing WebSocket connections gracefully...");
        
        // Send a disconnect message to all clients before stopping
        try {
            com.google.gson.JsonObject disconnectMsg = new com.google.gson.JsonObject();
            disconnectMsg.addProperty("type", "server_shutdown");
            disconnectMsg.addProperty("message", "Server is shutting down. You can refresh to reconnect.");
            GameWebSocket.broadcast(disconnectMsg);
            
            // Give clients a moment to receive the message
            Thread.sleep(500);
        } catch (Exception e) {
            System.err.println("Could not send disconnect message: " + e.getMessage());
        }
        
        server.stop();
        System.out.println("WebSocket server stopped");
    }

    public void join() throws InterruptedException {
        server.join();
    }

    /**
     * Simple status servlet.
     */
    private static class StatusServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"status\":\"ok\",\"clients\":" + 
                GameWebSocket.getConnectedClients() + "}");
        }
    }

    /**
     * Live viewer servlet.
     */
    private static class LiveServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            resp.setContentType("text/html");
            resp.setStatus(HttpServletResponse.SC_OK);
            
            // Read the live HTML file from resources
            try (var is = getClass().getClassLoader().getResourceAsStream("live.html")) {
                if (is != null) {
                    resp.getWriter().println(new String(is.readAllBytes()));
                } else {
                    resp.getWriter().println("<html><body><h1>Live viewer not found</h1></body></html>");
                }
            }
        }
    }

    /**
     * Leaderboard servlet.
     */
    private static class LeaderboardServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            resp.setContentType("text/html");
            resp.setStatus(HttpServletResponse.SC_OK);
            
            // Read the leaderboard HTML file from resources
            try (var is = getClass().getClassLoader().getResourceAsStream("leaderboard.html")) {
                if (is != null) {
                    resp.getWriter().println(new String(is.readAllBytes()));
                } else {
                    resp.getWriter().println("<html><body><h1>Leaderboard not found</h1></body></html>");
                }
            }
        }
    }

    /**
     * Simple HTML page with WebSocket client.
     */
    private static class IndexServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            resp.setContentType("text/html");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println(getHTMLPage());
        }

        private String getHTMLPage() {
            return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>EngineLab Live</title>\n" +
                "    <style>\n" +
                "        body { font-family: monospace; background: #1a1a1a; color: #00ff00; padding: 20px; }\n" +
                "        .game { border: 1px solid #00ff00; padding: 10px; margin: 10px 0; }\n" +
                "        .move { color: #ffff00; }\n" +
                "        .result { color: #ff0000; font-weight: bold; }\n" +
                "        #status { position: fixed; top: 10px; right: 10px; padding: 10px; border: 1px solid #00ff00; }\n" +
                "        .connected { color: #00ff00; }\n" +
                "        .disconnected { color: #ff0000; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>EngineLab Live Stream</h1>\n" +
                "    <div id=\"status\" class=\"disconnected\">Disconnected</div>\n" +
                "    <div id=\"games\"></div>\n" +
                "    <div id=\"log\"></div>\n" +
                "    <script>\n" +
                "        const games = {};\n" +
                "        const ws = new WebSocket('ws://' + location.host + '/ws');\n" +
                "        \n" +
                "        ws.onopen = () => {\n" +
                "            document.getElementById('status').textContent = 'Connected';\n" +
                "            document.getElementById('status').className = 'connected';\n" +
                "        };\n" +
                "        \n" +
                "        ws.onclose = () => {\n" +
                "            document.getElementById('status').textContent = 'Disconnected';\n" +
                "            document.getElementById('status').className = 'disconnected';\n" +
                "        };\n" +
                "        \n" +
                "        ws.onmessage = (event) => {\n" +
                "            const msg = JSON.parse(event.data);\n" +
                "            console.log(msg);\n" +
                "            \n" +
                "            if (msg.type === 'game_start') {\n" +
                "                games[msg.gameId] = { moves: [], fen: msg.fen };\n" +
                "                updateGames();\n" +
                "            } else if (msg.type === 'move') {\n" +
                "                if (games[msg.gameId]) {\n" +
                "                    games[msg.gameId].moves.push(msg.move);\n" +
                "                    games[msg.gameId].fen = msg.fen;\n" +
                "                    games[msg.gameId].whiteTime = msg.whiteTime;\n" +
                "                    games[msg.gameId].blackTime = msg.blackTime;\n" +
                "                    updateGames();\n" +
                "                }\n" +
                "            } else if (msg.type === 'game_end') {\n" +
                "                if (games[msg.gameId]) {\n" +
                "                    games[msg.gameId].result = msg.result;\n" +
                "                    games[msg.gameId].reason = msg.reason;\n" +
                "                    updateGames();\n" +
                "                }\n" +
                "            }\n" +
                "        };\n" +
                "        \n" +
                "        function updateGames() {\n" +
                "            const gamesDiv = document.getElementById('games');\n" +
                "            gamesDiv.innerHTML = '';\n" +
                "            \n" +
                "            for (const [id, game] of Object.entries(games)) {\n" +
                "                const gameDiv = document.createElement('div');\n" +
                "                gameDiv.className = 'game';\n" +
                "                \n" +
                "                let html = '<h3>Game ' + id + '</h3>';\n" +
                "                if (game.whiteTime !== undefined) {\n" +
                "                    html += '<div>White: ' + (game.whiteTime / 1000).toFixed(1) + 's | Black: ' + (game.blackTime / 1000).toFixed(1) + 's</div>';\n" +
                "                }\n" +
                "                html += '<div class=\"move\">Moves: ' + game.moves.join(' ') + '</div>';\n" +
                "                if (game.result) {\n" +
                "                    html += '<div class=\"result\">Result: ' + game.result + ' (' + game.reason + ')</div>';\n" +
                "                }\n" +
                "                \n" +
                "                gameDiv.innerHTML = html;\n" +
                "                gamesDiv.appendChild(gameDiv);\n" +
                "            }\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
        }
    }
}
