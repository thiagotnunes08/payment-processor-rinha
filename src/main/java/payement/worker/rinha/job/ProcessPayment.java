package payement.worker.rinha.job;

import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import payement.worker.rinha.client.ProcessorPaymentClient;
import payement.worker.rinha.entities.Status;
import payement.worker.rinha.repositories.PaymentRepository;

import java.util.concurrent.Callable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ProcessPayment {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    ProcessorPaymentClient processorPaymentClient;

    @ConfigProperty(name = "cron.job")
    String cronJob;

    @ConfigProperty(name = "limit.query")
    Integer limitQuery;

    @ConfigProperty(name = "thread.size")
    Integer threadSize;

    ExecutorService executor = Executors.newFixedThreadPool(2);


    @Scheduled(every = "{cron.job}")
    @Transactional
    public void process() {

        try {
            var paymentsPending = paymentRepository
                    .findAllByLimited(Status.PENDING, limitQuery);


            var tarefas = paymentsPending
                    .stream()
                    .map(p -> (Callable<Void>) () -> {
                        processorPaymentClient.processPayment(p);
                        return null;
                    }).toList();

            executor.invokeAll(tarefas);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }
}
