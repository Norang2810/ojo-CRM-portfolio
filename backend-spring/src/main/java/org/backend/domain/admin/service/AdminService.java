package org.backend.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.backend.domain.admin.dto.response.AdminRoleUpdateResponse;
import org.backend.domain.admin.dto.response.AdminStatusUpdateResponse;
import org.backend.domain.admin.dto.response.AdminSummaryDto;
import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminRole;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.auth.repository.RefreshTokenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final AdminRepository adminRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public Page<AdminSummaryDto> getAdmins(Integer page, Integer size, String keyword, AdminStatus status) {
        int p = (page == null) ? 0 : Math.max(page, 0);
        int s = (size == null) ? 20 : Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));

        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasStatus = (status != null);

        Page<Admin> result;
        if (hasKeyword && hasStatus) {
            result = adminRepository.searchByStatusAndKeyword(status, keyword, pageable);
        } else if (hasKeyword) {
            result = adminRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
                    keyword, keyword, pageable
            );
        } else if (hasStatus) {
            result = adminRepository.findByStatus(status, pageable);
        } else {
            result = adminRepository.findAll(pageable);
        }

        return result.map(AdminSummaryDto::from);
    }

    @Transactional
    public AdminRoleUpdateResponse updateRole(Long targetAdminId, AdminRole newRole) {
        if (newRole != AdminRole.CS && newRole != AdminRole.MARKETING) {
            throw new IllegalArgumentException("권한은 CS 또는 MARKETING으로만 변경할 수 있습니다.");
        }

        Admin target = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new IllegalArgumentException("대상 관리자 계정을 찾을 수 없습니다."));

        // 슈퍼 어드민 권한 보호
        if (target.getRole() == AdminRole.ADMIN) {
            throw new IllegalArgumentException("ADMIN 계정의 권한은 변경할 수 없습니다.");
        }

        // 멱등 처리: 이미 같은 권한이면 그대로 반환
        if (target.getRole() == newRole) {
            return AdminRoleUpdateResponse.of(target.getId(), target.getRole());
        }

        target.changeRole(newRole);

        // 권한 즉시 반영을 위해 refresh 토큰 무효화(재로그인/재발급 강제)
        refreshTokenRepository.deleteByAdminId(target.getId());

        return AdminRoleUpdateResponse.of(target.getId(), target.getRole());
    }

    @Transactional
    public AdminStatusUpdateResponse updateStatus(Long targetAdminId, AdminStatus newStatus) {
        Admin target = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new IllegalArgumentException("대상 관리자 계정을 찾을 수 없습니다."));

        // 멱등 처리: 같은 값이면 그냥 반환
        if (target.getStatus() == newStatus) {
            return AdminStatusUpdateResponse.of(target.getId(), target.getStatus());
        }

        target.changeStatus(newStatus);

        // 상태 변경 즉시 반영: RT 삭제
        refreshTokenRepository.deleteByAdminId(target.getId());

        return AdminStatusUpdateResponse.of(target.getId(), target.getStatus());
    }
}