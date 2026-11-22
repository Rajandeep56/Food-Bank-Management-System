import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.TimeZone;
import Services.AIEngine;
import database.DatabaseWriter;
import web.FoodBankApp;
import web.WebForms;

public class Main {
    public static void main(String[] args) throws Exception {
        String timezone = System.getenv().getOrDefault("AI_TIMEZONE", "America/Vancouver");
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(timezone)));
        DatabaseWriter.enabled = true;
        FoodBankApp app = new FoodBankApp();
        app.loadDatabase();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/", exchange -> WebForms.handle(exchange, app));
        server.createContext("/forms/", exchange -> WebForms.serveForm(exchange));
        server.createContext("/", exchange -> {
            byte[] page = Files.readAllBytes(Path.of("web/index.html"));
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, page.length);
            exchange.getResponseBody().write(page);
            exchange.close();
        });
        server.start();
        System.out.println("Java services running on port " + port);

        AIEngine aiEngine = new AIEngine();
        aiEngine.startScheduler(app.stock, app.inventory, app.alerts, app.trend);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            aiEngine.stopScheduler();
        }));
    }
}
