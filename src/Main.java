import java.time.ZoneId;
import java.util.TimeZone;
import Services.AIEngine;
import database.DatabaseWriter;
import web.FoodBankApp;

public class Main {
    public static void main(String[] args) throws Exception {
        String timezone = System.getenv().getOrDefault("AI_TIMEZONE", "America/Vancouver");
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(timezone)));
        DatabaseWriter.enabled = true;

        FoodBankApp app = new FoodBankApp();
        app.loadDatabase();


        AIEngine aiEngine = new AIEngine();
        aiEngine.startScheduler(app.stock, app.inventory, app.alerts, app.trend);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            aiEngine.stopScheduler();
        }));
    }
}
