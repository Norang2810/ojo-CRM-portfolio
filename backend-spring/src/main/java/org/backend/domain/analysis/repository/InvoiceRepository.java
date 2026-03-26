package org.backend.domain.analysis.repository;

import org.backend.domain.analysis.entity.Invoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // 고객별 청구서 조회
    @EntityGraph(attributePaths = {
            "member",
            "invoiceDetails",
            "invoiceDetails.product"
    })
    Optional<Invoice> findByMemberIdAndBaseMonth(Long memberId, String baseMonth);
}
