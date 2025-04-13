package kr.hhplus.be.server.domain.coupon;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findById(Long couponId);
    List<Coupon> findAll();
}
