package payement.worker.rinha.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.Executors;

@ApplicationScoped
public class ProcessorPaymentClient {

    private final HttpClient CLIENT = HttpClient.newBuilder()
            .executor(Executors.newFixedThreadPool(50))
            .connectTimeout(Duration.ofMillis(500))
            .build();

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Inject
    PaymentRepository paymentRepository;

    private final Logger logger = LoggerFactory.getLogger(ProcessorPaymentClient.class);

    public void processPayment(Payment payment) {
        long start = System.nanoTime();

        var dto = new PaymentRequestProcessor(
                payment.getCorrelationId(),
                payment.getAmount(),
                payment.getRequestedAt());

        try {
            var json = OBJECT_MAPPER.writeValueAsString(dto);

            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://payment-processor-default:8080/payments"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            paymentRepository.updateBy(Status.PAID, Processor.DEFAULT, payment.getCorrelationId());

        } catch (Exception primary) {
            try {
                var fallbackJson = OBJECT_MAPPER.writeValueAsString(dto);

                var fallbackRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://payment-processor-fallback:8080/payments"))
                        .timeout(Duration.ofSeconds(2))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(fallbackJson))
                        .build();

                CLIENT.send(fallbackRequest, HttpResponse.BodyHandlers.discarding());
                paymentRepository.updateBy(Status.PAID, Processor.FALLBACK, payment.getCorrelationId());

            } catch (Exception ignored) {
                // rinha style: ignora mesmo

            }
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        logger.info("payment {} executado em {} ms", payment.getCorrelationId(),duration);
    }
}
