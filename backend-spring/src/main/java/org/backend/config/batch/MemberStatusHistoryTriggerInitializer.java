package org.backend.config.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberStatusHistoryTriggerInitializer implements ApplicationRunner {

    private static final String TRIGGER_NAME = "trg_member_status_history";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (exists(schema)) return;
        try {
            jdbcTemplate.execute("""
                    CREATE TRIGGER trg_member_status_history
                    AFTER UPDATE ON member
                    FOR EACH ROW
                    INSERT INTO member_status_history (
                        member_id, previous_status, status, changed_at, source
                    )
                    SELECT NEW.member_id, OLD.status, NEW.status, UTC_TIMESTAMP(6), 'DATABASE_TRIGGER'
                    WHERE NOT (OLD.status <=> NEW.status)
                    """);
            log.info("Created member status history trigger");
        } catch (DataAccessException concurrentCreate) {
            if (!exists(schema)) throw concurrentCreate;
        }
    }

    private boolean exists(String schema) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.TRIGGERS
                        WHERE TRIGGER_SCHEMA = ? AND TRIGGER_NAME = ?
                        """,
                Integer.class,
                schema,
                TRIGGER_NAME
        );
        return count != null && count > 0;
    }
}
