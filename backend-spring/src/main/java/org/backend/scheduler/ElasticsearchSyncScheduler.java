package org.backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.domain.advice.document.AdviceDocument;
import org.backend.domain.advice.entity.Advice;
import org.backend.domain.advice.repository.AdviceRepository;
import org.backend.domain.advice.repository.AdviceSearchRepository;
import org.backend.domain.member.document.MemberSearchDocument;
import org.backend.domain.member.entity.Member;
import org.backend.domain.member.repository.MemberRepository;
import org.backend.domain.member.repository.MemberSearchRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchSyncScheduler {

    private final MemberRepository memberRepository;
    private final MemberSearchRepository memberSearchRepository;

    private final AdviceRepository adviceRepository;
    private final AdviceSearchRepository adviceSearchRepository;

    /**
     * 매시간 0분에 MySQL의 데이터를 Elasticsearch로 전체 동기화합니다.
     * (실무에서는 updatedAt 등 조건기반 혹은 Logstash 이용 권장,
     * 현 구현은 데이터량이 매우 많지 않다고 가정하고 findAll() 사용)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void syncDataToElasticsearch() {
        log.info("Starting data synchronization from MySQL to Elasticsearch...");

        syncMembers();
        syncAdvices();

        log.info("Data synchronization completed successfully.");
    }

    private void syncMembers() {
        log.info("Syncing Members...");
        List<Member> members = memberRepository.findAll();

        List<MemberSearchDocument> memberDocuments = members.stream()
                .map(m -> new MemberSearchDocument(
                        m.getId(),
                        m.getName(),
                        m.getPhone(),
                        m.getEmail(),
                        m.getCreatedAt(),
                        m.getStatus()))
                .collect(Collectors.toList());

        memberSearchRepository.saveAll(memberDocuments);
        log.info("Successfully synced {} members.", memberDocuments.size());
    }

    @Transactional(readOnly = true)
    private void syncAdvices() {
        log.info("Syncing Advices...");
        List<Advice> advices = adviceRepository.findAll();

        List<AdviceDocument> adviceDocuments = advices.stream()
                .map(a -> AdviceDocument.builder()
                        .id(a.getId())
                        .memberId(a.getMember() != null ? a.getMember().getId() : null)
                        .adminName(a.getAdmin() != null ? a.getAdmin().getName() : null)
                        .category(a.getCategory() != null ? a.getCategory().getCategoryName() : null)
                        .adviceContent(a.getAdviceContent())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        adviceSearchRepository.saveAll(adviceDocuments);
        log.info("Successfully synced {} advices.", adviceDocuments.size());
    }
}
