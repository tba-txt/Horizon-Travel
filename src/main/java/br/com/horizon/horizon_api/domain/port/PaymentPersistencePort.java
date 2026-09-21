package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentPersistencePort {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    List<Payment> findAll();
    void delete(Long id);
}
