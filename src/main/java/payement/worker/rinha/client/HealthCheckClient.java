package payement.worker.rinha.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import payement.worker.rinha.dto.HealthInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class HealthCheckClient {

    private volatile HealthInfo defaultStatus = new HealthInfo(false, 5000);
    private volatile HealthInfo fallbackStatus = new HealthInfo(false, 5000);

    private final HttpClient client = HttpClient.newHttpClient();

    @Scheduled(every = "5s")
    void updateHealth() {
        defaultStatus = checkHealth("http://payment-processor-default:8080/payments/service-health");
        fallbackStatus = checkHealth("http://payment-processor-fallback:8080/payments/service-health");
//        defaultStatus = checkHealth("http://localhost:8001/payments/service-health");
//        fallbackStatus = checkHealth("http://localhost:8002/payments/service-health");
    }

    private HealthInfo checkHealth(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());

            var json = new ObjectMapper().readTree(response.body());

            boolean isUp = json.get("failing").asText().equalsIgnoreCase("false");
            int latency = json.get("minResponseTime").intValue();

            return new HealthInfo(isUp, latency);

        } catch (Exception e) {
            return new HealthInfo(false, 9999);
        }
    }

    public HealthInfo getDefaultStatus() {
        return defaultStatus;
    }

    public HealthInfo getFallbackStatus() {
        return fallbackStatus;
    }
}
