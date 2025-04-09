package kr.hhplus.be.server.infrastructure.payment;

import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    @Override
    public Optional<Payment> findById(long id) {
        return Optional.empty();
    }

    @Override
    public Payment save(Payment payment) {
        return null;
    }

    @Override
    public List<Payment> findAll() {
        return List.of();
    }
}
