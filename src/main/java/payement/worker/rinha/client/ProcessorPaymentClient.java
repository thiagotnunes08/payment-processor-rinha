package payement.worker.rinha.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Inject
    PaymentRepository paymentRepository;


    @Transactional
    public  void processPayment(Payment payment) {

        try {

            OBJECT_MAPPER.registerModule(new JavaTimeModule());

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

            paymentRepository.updateBy(Status.PAID, Processor.DEFAULT,payment.getId());


        } catch (Exception e) {

            try {

                var json = OBJECT_MAPPER.writeValueAsString(
                        new PaymentRequestProcessor(
                                payment.getCorrelationId(),
                                payment.getAmount(),
                                payment.getRequestedAt()));

                var request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8002/payments"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                paymentRepository.updateBy(Status.PAID,Processor.FALLBACK,payment.getId());

                System.out.println("atualizado pagamento id " + payment.getId());

            } catch (Exception e2) {

                System.out.println("deu ruim até no fallback!"+e2.getCause());
            }
        }
    }
}
