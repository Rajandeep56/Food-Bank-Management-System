package web;

import com.sun.net.httpserver.HttpExchange;
import java.util.ArrayList;
import java.util.UUID;
import Services.AuthService;
import model.Report;

// Stores the logged-in user and report for one browser.
class WebSession {
    private static final ArrayList<WebSession> sessions = new ArrayList<>();
    private static final long TIMEOUT = 30 * 60 * 1000; // 30 minutes

    String id;
    String formCode;
    final AuthService auth;
    Report report;
    private long lastSeen = System.currentTimeMillis();

    private WebSession(FoodBankApp app) {
        auth = new AuthService(app.users);
        renew();
    }

    static WebSession open(HttpExchange exchange, FoodBankApp app) {
        removeOldSessions();
        String savedId = getCookieId(exchange);

        // Find the browser in our list.
        for (int i = 0; i < sessions.size(); i++) {
            WebSession session = sessions.get(i);
            if (session.id.equals(savedId)) {
                session.lastSeen = System.currentTimeMillis();
                return session;
            }
        }

        // A new browser needs a new session.
        WebSession session = new WebSession(app);
        sessions.add(session);
        return session;
    }

    private static String getCookieId(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) {
            String[] parts = cookie.split(";");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                if (part.startsWith("FOODBANK_SESSION=")) {
                    String savedId = part.substring("FOODBANK_SESSION=".length());
                    return savedId.replace("\"", "");
                }
            }
        }
        return null;
    }

    private static void removeOldSessions() {
        long now = System.currentTimeMillis();
        int i = 0;
        while (i < sessions.size()) {
            WebSession session = sessions.get(i);
            if (now - session.lastSeen > TIMEOUT) {
                sessions.remove(i);
            } else {
                i++;
            }
        }
    }

    void checkHost(HttpExchange exchange) {
        String host = exchange.getRequestHeaders().getFirst("Host");
        int port = exchange.getLocalAddress().getPort();
        boolean localAddress = ("127.0.0.1:" + port).equals(host);
        boolean localName = ("localhost:" + port).equals(host);
        if (!localAddress && !localName) {
            throw new SecurityException("Invalid host");
        }
    }

    void checkForm(HttpExchange exchange, String submittedCode) {
        if (!formCode.equals(submittedCode)) {
            throw new SecurityException("Session expired. Refresh the page and try again.");
        }
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null) {
            return;
        }
        int port = exchange.getLocalAddress().getPort();
        String[] allowedPages = {
            "http://127.0.0.1:" + port, "http://localhost:" + port
        };
        for (int i = 0; i < allowedPages.length; i++) {
            if (origin.equals(allowedPages[i])) {
                return;
            }
        }
        throw new SecurityException("Invalid request origin");
    }

    void renew() {
        // Give the browser new random codes after logging in or out.
        id = UUID.randomUUID().toString();
        formCode = UUID.randomUUID().toString();
        report = null;
    }

    String cookie() {
        return "FOODBANK_SESSION=" + id + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=1800";
    }
}
