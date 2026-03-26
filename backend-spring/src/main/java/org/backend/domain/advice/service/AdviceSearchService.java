package org.backend.domain.advice.service;

import lombok.RequiredArgsConstructor;
import org.backend.domain.advice.document.AdviceDocument;
import org.backend.domain.advice.dto.AdviceSearchResponse;
import org.backend.domain.advice.repository.AdviceSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdviceSearchService {

    private final AdviceSearchRepository adviceSearchRepository;


    public Page<AdviceSearchResponse> searchByKeyword(String keyword, Pageable pageable){

        if(keyword == null || keyword.trim().isEmpty()){
            return Page.empty(pageable);
        }

        Page<AdviceDocument> documentPage = adviceSearchRepository.findByAdviceContent(keyword, pageable);

        return documentPage.map(AdviceSearchResponse::from);

    }

}
