package fr.flwrian.WebSocket;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Servlet to serve static resources (JS, CSS) from the classpath.
 */
public class StaticResourceServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // Remove leading slash
        String resourcePath = path.substring(1);
        
        // Set content type based on extension
        if (resourcePath.endsWith(".js")) {
            resp.setContentType("application/javascript");
        } else if (resourcePath.endsWith(".css")) {
            resp.setContentType("text/css");
        } else if (resourcePath.endsWith(".svg")) {
            resp.setContentType("image/svg+xml");
        } else {
            resp.setContentType("application/octet-stream");
        }
        
        // Load resource from classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + resourcePath);
                return;
            }
            
            // Copy to response
            try (OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
