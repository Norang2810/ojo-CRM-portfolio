package org.backend.domain.advice.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.backend.domain.advice.dto.HourlyConsultationDto;
import org.backend.domain.advice.entity.QAdvice;

import java.util.List;

@RequiredArgsConstructor
public class AdviceRepositoryImpl implements AdviceRepositoryCustom {

        private final JPAQueryFactory queryFactory;

        @Override
        public List<HourlyConsultationDto> getHourlyStatistics() {
                QAdvice advice = QAdvice.advice;

                NumberExpression<Integer> hourExpression = advice.startAt.hour();

                return queryFactory
                                .select(Projections.constructor(HourlyConsultationDto.class,
                                                hourExpression,
                                                new CaseBuilder()
                                                                .when(advice.direction.equalsIgnoreCase("INBOUND"))
                                                                .then(1L)
                                                                .otherwise(0L).sum(),
                                                new CaseBuilder()
                                                                .when(advice.direction.equalsIgnoreCase("OUTBOUND"))
                                                                .then(1L)
                                                                .otherwise(0L).sum(),
                                                advice.count()))
                                .from(advice)
                                .where(advice.startAt.isNotNull())
                                .groupBy(hourExpression)
                                .orderBy(hourExpression.asc())
                                .fetch();
        }
}
