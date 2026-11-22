package web;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.util.HashMap;
import java.util.Map;

// Read a form, call a service, and return plain text to index.html.
public class WebForms {
    public static void handle(HttpExchange exchange, FoodBankApp app) throws IOException {
        synchronized (app.inventory) {
            WebSession session = WebSession.open(exchange, app);
            try {
                session.checkHost(exchange);
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                Map<String, String> query = parseForm(exchange.getRequestURI().getRawQuery());
                if (method.equals("GET") && path.equals("/api/session")) {
                    reply(exchange, session, 200, session.formCode + "\n" + (session.auth.isLoggedIn() ? session.auth.getCurrentUser().getRole() : ""));
                } else if (method.equals("GET") && path.equals("/api/options")) {
                    reply(exchange, session, 200, Choices.read(app, session.auth, query));
                } else if (method.equals("GET") && path.equals("/api/records")) {
                    reply(exchange, session, 200, Records.read(app, session.auth, query, session.report));
                } else if (method.equals("POST") && path.equals("/api/action")) {
                    Map<String, String> form = readForm(exchange);
                    session.checkForm(exchange, form.get("csrf"));
                    String message = FormActions.submit(app, session, form);
                    if ("login".equals(form.get("action")) || "logout".equals(form.get("action"))) {
                        session.renew();
                    }
                    reply(exchange, session, 200, message);
                } else {
                    reply(exchange, session, 404, "Page not found");
                }
            } catch (SecurityException e) {
                reply(exchange, session, 403, e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException | DateTimeException e) {
                reply(exchange, session, 400, e.getMessage());
            } catch (RuntimeException e) {
                String message = RequestErrors.message(e);
                System.err.println("API request failed: " + message);
                if (e.getStackTrace().length > 0) System.err.println("At " + e.getStackTrace()[0]);
                reply(exchange, session, 500, message);
            } finally {
                exchange.close();
            }
        }
    }

    public static void serveForm(HttpExchange exchange) throws IOException {
        String name = exchange.getRequestURI().getPath().substring("/forms/".length());
        Path file = Path.of("web/public/forms", name);
        if (!name.matches("[a-z-]+\\.html") || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1);
        } else {
            byte[] html = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, html.length);
            exchange.getResponseBody().write(html);
        }
        exchange.close();
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.startsWith("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Submit form fields");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(65_537);
        if (bytes.length > 65_536) throw new IllegalArgumentException("Form is too large");
        return parseForm(new String(bytes, StandardCharsets.UTF_8));
    }

    private static Map<String, String> parseForm(String text) {
        Map<String, String> form = new HashMap<>();
        if (text == null || text.isEmpty()) return form;
        for (String part : text.split("&")) {
            String[] pair = part.split("=", 2);
            form.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
        }
        return form;
    }

    private static void reply(HttpExchange exchange, WebSession session, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Set-Cookie", session.cookie());
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }
}
