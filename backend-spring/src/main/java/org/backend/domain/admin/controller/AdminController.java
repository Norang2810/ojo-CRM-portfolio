package org.backend.domain.admin.controller;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.domain.admin.dto.request.AdminRoleUpdateRequest;
import org.backend.domain.admin.dto.request.AdminStatusUpdateRequest;
import org.backend.domain.admin.dto.response.AdminRoleUpdateResponse;
import org.backend.domain.admin.dto.response.AdminStatusUpdateResponse;
import org.backend.domain.admin.dto.response.AdminSummaryDto;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admins")
public class AdminController {

    private final AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')") // ADMIN 전용
    @GetMapping
    public ResponseEntity<CommonResponse<Page<AdminSummaryDto>>> getAdmins(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AdminStatus status
    ) {
        Page<AdminSummaryDto> result = adminService.getAdmins(page, size, keyword, status);
        return ResponseEntity.ok(
                CommonResponse.success(result, "관리자 목록 조회 성공")
        );
    }

    // : 관리자 권한 변경(ADMIN 전용 PreAuthorize에서 막음)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{adminId}/role")
    public ResponseEntity<CommonResponse<AdminRoleUpdateResponse>> updateAdminRole(
            @PathVariable Long adminId,
            @RequestBody AdminRoleUpdateRequest request
    ) {
        AdminRoleUpdateResponse result = adminService.updateRole(adminId, request.role());
        return ResponseEntity.ok(CommonResponse.success(result, "관리자 권한 변경 성공"));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{adminId}/status")
    public ResponseEntity<CommonResponse<AdminStatusUpdateResponse>> updateAdminStatus(
            @PathVariable Long adminId,
            @RequestBody AdminStatusUpdateRequest request
    ) {
        AdminStatusUpdateResponse result = adminService.updateStatus(adminId, request.status());
        return ResponseEntity.ok(CommonResponse.success(result, "관리자 계정 상태 변경 성공"));
    }
}