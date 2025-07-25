package payement.worker.rinha.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import payement.worker.rinha.entities.Payment;
import payement.worker.rinha.entities.Processor;
import payement.worker.rinha.entities.Status;

import java.util.List;
import java.util.UUID;

import static io.quarkus.hibernate.orm.panache.Panache.getEntityManager;

@ApplicationScoped
public class PaymentRepository {

    public void updateBy(Status status, Processor processor, UUID correlationId) {
        getEntityManager()
                .createQuery("""
        update Payment p 
        set p.status = :status, p.processor = :processor 
        where p.correlationId = :correlationId
    """)
                .setParameter("status", status)
                .setParameter("processor", processor)
                .setParameter("correlationId", correlationId)
                .executeUpdate();
    }

    public List<Payment> findAllByLimited(Status status, int limit) {
        String query = """
       select p from Payment p where p.status = :status
    """;

        return getEntityManager()
                .createQuery(query, Payment.class)
                .setParameter("status", status)
                .setMaxResults(limit)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("jakarta.persistence.lock.timeout", -2)
                .getResultList();
    }
}
