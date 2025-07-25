package payement.worker.rinha.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

@ApplicationScoped
public class ProcessorPaymentClient {

    private final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    PaymentRepository paymentRepository;


    public void processPayment(Payment payment) {

        try {
            var json = OBJECT_MAPPER.writeValueAsString(
                    new PaymentRequestProcessor(
                            payment.getCorrelationId(),
                            payment.getAmount(),
                            payment.getRequestedAt()));
            var request = HttpRequest.newBuilder()

                    .uri(URI.create("http://payment-processor-default:8080/payments"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            paymentRepository.updateBy(Status.PAID, Processor.DEFAULT, payment.getCorrelationId());


        } catch (Exception e) {

            try {

                var json = OBJECT_MAPPER.writeValueAsString(
                        new PaymentRequestProcessor(
                                payment.getCorrelationId(),
                                payment.getAmount(),
                                payment.getRequestedAt()));

                var request = HttpRequest.newBuilder()
                        .uri(URI.create("http://payment-processor-fallback:8080/payments"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                paymentRepository.updateBy(Status.PAID, Processor.FALLBACK, payment.getCorrelationId());

            } catch (Exception e2) {

                System.out.printf("deu ruim até no fallback! msg: %s causa: %s msg2:%s", e2.getMessage(),e.getCause(),e2.getLocalizedMessage());
            }
        }
    }

    @PostConstruct
    void init() {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }
}
