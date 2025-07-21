package payement.work.job.client;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@DynamicUpdate
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "correlation_id",unique = true)
    private UUID correlationId;
    private BigDecimal amount;
    private Status status;
    private Processor processor;
    @Column(name = "requested_at")
    private Instant requestedAt;

    @Deprecated
    public Payment() {

    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Long getId() {
        return id;
    }
}
