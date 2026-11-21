package Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class EmailService {

    public void sendEmail(String to, String subject,
                          String message) throws Exception {

        String apiKey = System.getenv("RESEND_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API key missing");
        }

        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing");
        }


        String body = """
            {
                "from": "Food Bank <onboarding@resend.dev>",
                "to": [%s],
                "subject": %s,
                "text": %s
            }
            """.formatted(json(to), json(subject), json(message));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.resend.com/emails"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(15))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 ||
            response.statusCode() >= 300) {
            throw new Exception(
                "Email failed: " + response.statusCode()
    + " - " + response.body()
            );
        }
    }

    private String json(String value) {
        return "\"" + value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\"";
    }
}