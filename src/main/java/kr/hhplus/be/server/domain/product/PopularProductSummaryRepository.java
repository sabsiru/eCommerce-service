package kr.hhplus.be.server.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PopularProductSummaryRepository extends JpaRepository<PopularProductSummary, Long> {
}