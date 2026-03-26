package org.backend.domain.advice.repository;

import org.backend.domain.advice.document.AdviceDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AdviceSearchRepository extends ElasticsearchRepository<AdviceDocument, Long> {
    Page<AdviceDocument> findByAdviceContent(String keyword, Pageable pageable);

}
