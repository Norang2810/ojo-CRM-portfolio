package org.backend.domain.batch.service;

import lombok.RequiredArgsConstructor;
import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BatchResetService {
    private final JdbcTemplate jdbcTemplate;

    // 5년 지난 데이터 삭제
    public int deleteOldData(){
        try{
            LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
            String fiveYearsAgoStr = fiveYearsAgo.toLocalDate().toString(); // "2026-02-26" 형태
            String fiveYearsAgoMonth = fiveYearsAgoStr.substring(0, 7).replace("-", ""); // "202602" 형탵

            int delCount = 0;
//          delCount += jdbcTemplate.update("DELETE FROM advice WHERE created_at < ?", fiveYearsAgo);
//          delCount += jdbcTemplate.update("DELETE FROM memo WHERE created_at < ?", fiveYearsAgo);

            delCount += jdbcTemplate.update("DELETE FROM feature_consultation WHERE feature_base_date < ?", fiveYearsAgoStr);
            delCount += jdbcTemplate.update("DELETE FROM feature_lifecycle WHERE feature_base_date < ?", fiveYearsAgoStr);
            delCount += jdbcTemplate.update("DELETE FROM feature_monetary WHERE feature_base_date < ?", fiveYearsAgoStr);
            delCount += jdbcTemplate.update("DELETE FROM feature_usage WHERE feature_base_date < ?", fiveYearsAgoStr);

            delCount += jdbcTemplate.update("DELETE FROM rfm WHERE updated_at < ?", fiveYearsAgoStr);
            delCount += jdbcTemplate.update("DELETE FROM rfm_kpi WHERE base_month < ?", fiveYearsAgoMonth);
            delCount += jdbcTemplate.update("DELETE FROM snapshot_billing WHERE base_month < ?", fiveYearsAgoMonth);
//          delCount += jdbcTemplate.update("DELETE FROM analysis WHERE created_at < ?", fiveYearsAgo);

//          delCount += jdbcTemplate.update("DELETE FROM access_logs WHERE created_at < ?", fiveYearsAgo);
            return delCount;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.DATA_DELETE_FAILED);
        }
    }

    // 선택한 달 혹은 전체 초기화 - 원하는 테이블 추가 가능
    public void resetBatchData(List<String> baseMonths, boolean isAll){
        try {
            if(isAll){
                jdbcTemplate.update("DELETE FROM rfm");
                jdbcTemplate.update("DELETE FROM rfm_kpi");
                jdbcTemplate.update("DELETE FROM snapshot_billing");
                return;
            }

            if(baseMonths != null && !baseMonths.isEmpty()){
                String inSql = String.join(",", baseMonths.stream().map(m -> "'" + m + "'").toList());
                jdbcTemplate.update("DELETE FROM rfm_kpi WHERE base_month IN (" + inSql + ")");
                jdbcTemplate.update("DELETE FROM snapshot_billing WHERE base_month IN (" + inSql + ")");
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.DATA_DELETE_FAILED);
        }

    }
}
