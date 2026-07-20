package org.backend.domain.analysis.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AnalyticsSnapshotManifestRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<ActiveSnapshot> findActive() {
        return jdbcTemplate.query("""
                        SELECT snapshot_version, batch_id, data_as_of
                        FROM analytics_snapshot_manifest
                        WHERE active = TRUE AND status = 'READY'
                        ORDER BY activated_at DESC
                        LIMIT 1
                        """,
                resultSet -> resultSet.next()
                        ? Optional.of(new ActiveSnapshot(
                                resultSet.getString("snapshot_version"),
                                resultSet.getString("batch_id"),
                                resultSet.getTimestamp("data_as_of").toLocalDateTime()
                        ))
                        : Optional.empty()
        );
    }

    public Optional<ActiveSnapshot> findReadyVersion(String snapshotVersion) {
        return jdbcTemplate.query("""
                        SELECT snapshot_version, batch_id, data_as_of
                        FROM analytics_snapshot_manifest
                        WHERE snapshot_version = ? AND status = 'READY'
                        """,
                resultSet -> resultSet.next()
                        ? Optional.of(new ActiveSnapshot(
                                resultSet.getString("snapshot_version"),
                                resultSet.getString("batch_id"),
                                resultSet.getTimestamp("data_as_of").toLocalDateTime()
                        ))
                        : Optional.empty(),
                snapshotVersion
        );
    }

    public record ActiveSnapshot(String version, String batchId, LocalDateTime dataAsOf) {
    }
}
