package org.backend.domain.member.repository;

import org.backend.domain.member.dto.CustomerFilterRequest;
import org.backend.domain.member.dto.CustomerFilterResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberRepositoryCustom {
    Page<CustomerFilterResponse> findFilteredCustomers(CustomerFilterRequest request, Pageable pageable);
}
