package org.backend.domain.batch.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangedMemberRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void storesTargetsWithEndExclusiveWindowAndPrimaryKeyDedupe() {
        when(jdbcTemplate.queryForObject(
                anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(1);
        ChangedMemberRepository repository = new ChangedMemberRepository(jdbcTemplate);

        int count = repository.findAndStoreChangedMembers(
                "batch-1",
                LocalDateTime.of(2026, 7, 18, 0, 0),
                LocalDateTime.of(2026, 7, 18, 1, 0)
        );

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).update(sql.capture(), any(MapSqlParameterSource.class));
        String targetSql = sql.getAllValues().get(0);
        assertThat(targetSql).contains("INSERT IGNORE INTO analytics_batch_target");
        assertThat(targetSql).contains(">= :from");
        assertThat(targetSql).contains("< :to");
        assertThat(targetSql).contains("GROUP BY changed.member_id");
        assertThat(targetSql).contains("failed.retry_count < 3");
        assertThat(targetSql).contains("NOT EXISTS");
        assertThat(count).isEqualTo(1);
    }
}
