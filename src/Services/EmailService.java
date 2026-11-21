package Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class EmailService {

    public void sendEmail(String to, String subject,
                          String message) throws Exception {
        sendEmail(to, subject, message, null);
    }

    public void sendEmail(String to, String subject, String message, String idempotencyKey) throws Exception {
        validateRecipient(to);

        String apiKey = System.getenv("RESEND_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API key missing");
        }

        String body = """
            {
                "from": "Food Bank <onboarding@resend.dev>",
                "to": [%s],
                "subject": %s,
                "text": %s
            }
            """.formatted(json(to), json(subject), json(message));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create("https://api.resend.com/emails"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(15))
            .POST(HttpRequest.BodyPublishers.ofString(body));
        if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);
        HttpRequest request = builder.build();

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 ||
            response.statusCode() >= 300) {
            throw new Exception(
                "Email provider returned HTTP " + response.statusCode()
            );
        }
    }

    public static void validateRecipient(String to) {
        if (to == null || !to.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Valid recipient email is required");
        }
    }

    private String json(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Email content cannot be null");
        }
        StringBuilder result = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '\\' || c == '"') result.append('\\').append(c);
            else if (c < 0x20) result.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
            else result.append(c);
        }
        return result.append('"').toString();
    }
}
