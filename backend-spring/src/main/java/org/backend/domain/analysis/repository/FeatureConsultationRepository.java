package org.backend.domain.analysis.repository;

import org.backend.domain.analysis.entity.FeatureConsultation;
import org.backend.domain.member.dto.ConsultStatProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureConsultationRepository
        extends JpaRepository<FeatureConsultation, Long> {

    @Query(value = """
        SELECT
            AVG(last_30d_consult_count) AS avgValue,
            STDDEV_POP(last_30d_consult_count) AS stdValue
        FROM feature_consultation
        """,
            nativeQuery = true)
    ConsultStatProjection getConsultStats();
}
