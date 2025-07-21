package payement.work.job.client;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

import static io.quarkus.hibernate.orm.panache.Panache.getEntityManager;

@ApplicationScoped
public class PaymentRepository {

    public void updateBy(Status status, Processor processor,Long id) {
        getEntityManager()
                .createQuery("""
        update Payment p 
        set p.status = :status, p.processor = :processor 
        where p.id = :id
    """)
                .setParameter("status", status)
                .setParameter("processor", processor)
                .setParameter("id", id)
                .executeUpdate();
    }
    public List<Payment> findAllByLimited(Status status, int limit) {
        String query = """
        SELECT * 
        FROM payment 
        WHERE status = :status 
        FOR UPDATE SKIP LOCKED
        LIMIT :limit
    """;

        return getEntityManager()
                .createNativeQuery(query, Payment.class)
                .setParameter("status", status) // ou status.toString(), depende do tipo
//                .setParameter("limit", limit)
                .getResultList();
    }

}
