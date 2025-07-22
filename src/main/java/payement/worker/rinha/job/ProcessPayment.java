package payement.worker.rinha.job;

import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import payement.worker.rinha.client.ProcessorPaymentClient;
import payement.worker.rinha.entities.Status;
import payement.worker.rinha.repositories.PaymentRepository;


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
    @Transactional
    public void process() {

        paymentRepository
                .findAllByLimited(Status.PENDING, limitQuery)
                .forEach(processorPaymentClient::processPayment);

    }
}
