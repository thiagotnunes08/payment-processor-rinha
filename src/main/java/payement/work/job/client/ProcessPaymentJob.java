package payement.work.job.client;

import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ProcessPaymentJob {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    ProcessorPaymentClient processorPaymentClient;

    @ConfigProperty(name = "cron.job")
    String cronJob;

    @ConfigProperty(name = "limit.query")
    Integer limitQuery;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();


    @Scheduled(every = "{cron.job}")
//    @Transactional
    public void process() {

        paymentRepository
                .findAllByLimited(Status.PENDING,limitQuery)
                .forEach(payment -> processorPaymentClient.processPayment(payment));
    }

//    @Scheduled(every = "5s")
//    public void verifyDisponibility() {
//        processorPaymentClient.verify();
//    }
}
