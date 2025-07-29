package payement.worker.rinha.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import payement.worker.rinha.dto.HealthInfo;
import payement.worker.rinha.dto.PaymentRequestProcessor;
import payement.worker.rinha.entities.Payment;
import payement.worker.rinha.entities.Processor;
import payement.worker.rinha.entities.Status;
import payement.worker.rinha.repositories.PaymentRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ProcessorPaymentClient {

    @ConfigProperty(name = "payment.default.latency-limit", defaultValue = "100")
    long DEFAULT_LATENCY_LIMIT_MS;

    private final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(500))
            .build();

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    HealthCheckClient healthCheckClient;

    private final Logger logger = LoggerFactory.getLogger(ProcessorPaymentClient.class);

    public CompletableFuture<Void> processPayment(Payment payment) {
        long start = System.nanoTime();

        PaymentRequestProcessor dto = new PaymentRequestProcessor(
                payment.getCorrelationId(),
                payment.getAmount(),
                payment.getRequestedAt());

        try {
            String json = OBJECT_MAPPER.writeValueAsString(dto);

            var defaultHealth = healthCheckClient.getDefaultStatus();
            var fallbackHealth = healthCheckClient.getFallbackStatus();

            // Decide para onde vai
            String targetUrl = chooseTarget(defaultHealth, fallbackHealth);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl + "/payments"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

//            Processor used = targetUrl.contains("8002") ? Processor.FALLBACK : Processor.DEFAULT;
            var used = targetUrl.contains("fallback") ? Processor.FALLBACK : Processor.DEFAULT;

            return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenAccept(response -> {

                        if (response.statusCode() == 200) {
                            paymentRepository.updateBy(Status.PAID, used, payment.getCorrelationId());
                            long duration = (System.nanoTime() - start) / 1_000_000;
                            logger.info("payment {} executado em {} ms [{}]", payment.getCorrelationId(), duration, used);
                        }
                    })
                    .exceptionally(e -> {
                        logger.error("ERRO ao processar {}: {}", payment.getCorrelationId(), e.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            logger.error("Falha ao criar requisição para {}: {}", payment.getCorrelationId(), e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    private String chooseTarget(HealthInfo def, HealthInfo fb) {
        if (def.isUp() && def.minResponseTime() <= DEFAULT_LATENCY_LIMIT_MS) {
            // Está saudável e rápido → usa default
//            return "http://localhost:8001";
            return "http://payment-processor-default:8080";
        }
        if (fb.isUp()) {
            // Default está down ou lento → fallback
//            return "http://localhost:8002";
            return "http://payment-processor-fallback:8080";
        }
        // Se o fallback também estiver off → tenta default mesmo assim
//        return "http://localhost:8001";
        return "http://payment-processor-default:8080";

    }
}