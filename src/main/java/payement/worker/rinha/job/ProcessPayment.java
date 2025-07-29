package payement.worker.rinha.job;

import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import payement.worker.rinha.client.ProcessorPaymentClient;
import payement.worker.rinha.entities.Payment;
import payement.worker.rinha.entities.Status;
import payement.worker.rinha.repositories.PaymentRepository;

import java.util.List;

public class ProcessPayment {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    ProcessorPaymentClient processorPaymentClient;

    @ConfigProperty(name = "cron.job")
    String cronJob;

    @ConfigProperty(name = "limit.query")
    Integer limitQuery;

    @Scheduled(every = "{cron.job}")
    public void process() {
        List<Payment> payments = paymentRepository.findAllByLimited(Status.PENDING, limitQuery);

        for (Payment payment : payments) {
            Thread.startVirtualThread(() -> {
                try {
                    processorPaymentClient.processPayment(payment).join(); // join pois é async
                } catch (Exception e) {
                    // Log já tratado dentro do client
                }
            });
        }
    }
}
