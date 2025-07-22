package payement.worker.rinha.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequestProcessor(UUID correlationId, BigDecimal amount, Instant requestedAt) {
}
