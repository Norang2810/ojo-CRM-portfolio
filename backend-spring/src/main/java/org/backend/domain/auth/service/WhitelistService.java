package org.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.backend.domain.auth.entity.AuthWhitelistEmail;
import org.backend.domain.auth.repository.AuthWhitelistEmailRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhitelistService {

    private final AuthWhitelistEmailRepository whitelistEmailRepository;

    // 옵션: 도메인 제한 (비어있으면 도메인 체크 스킵)
    @Value("${auth.whitelist.domain:}")
    private String allowedDomain;

    /**
     * 최초 로그인 시 화이트리스트에 없으면 자동 등록하고 통과
     */
    @Transactional
    public void validateAndAutoRegister(String email) {
        // 1) 도메인 제한
        if (allowedDomain != null && !allowedDomain.isBlank()) {
            if (!email.toLowerCase().endsWith("@" + allowedDomain.toLowerCase())) {
                throw new IllegalArgumentException("허용되지 않은 이메일 도메인입니다.");
            }
        }

        // 2) 화이트리스트 자동 등록
        if (!whitelistEmailRepository.existsByEmail(email)) {
            whitelistEmailRepository.save(AuthWhitelistEmail.create(email));
        }
    }
}