package payement.worker.rinha.job;

import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import payement.worker.rinha.client.ProcessorPaymentClient;
import payement.worker.rinha.entities.Payment;
import payement.worker.rinha.entities.Status;
import payement.worker.rinha.repositories.PaymentRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


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

        var payments = paymentRepository
                .findAllByLimited(Status.PENDING, limitQuery);


        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<? extends Future<?>> futures = payments.stream()
                    .map(payment -> executor.submit(() -> processorPaymentClient.processPayment(payment)))
                    .toList();

            for (Future<?> future : futures) {
                future.get(); // aguarda e propaga exceções se quiser
            }

        } catch (InterruptedException | ExecutionException e) {
            // Log ou tratamento
            e.printStackTrace();
        }

    }
}
