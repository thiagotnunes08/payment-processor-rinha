package payement.worker.rinha.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@RegisterForReflection
public record PaymentRequestProcessor(UUID correlationId, BigDecimal amount, Instant requestedAt) {
}
