package org.backend.domain.member.repository;

import org.backend.domain.member.document.MemberSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MemberSearchRepository
        extends ElasticsearchRepository<MemberSearchDocument, Long> {

    @Query("""
    {
      "bool": {
        "should": [
          { "wildcard": { "name": "*?0*" } },
          { "wildcard": { "email": "*?0*" } },
          { "wildcard": { "phone": "*?0*" } }
        ],
        "minimum_should_match": 1
      }
    }
    """)
    Page<MemberSearchDocument> searchByKeyword(String keyword, Pageable pageable);
}