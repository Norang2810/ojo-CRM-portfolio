package org.backend.domain.admin.repository;

import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<Admin> findByStatus(AdminStatus status, Pageable pageable);

    Page<Admin> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
            String emailKeyword, String nameKeyword, Pageable pageable);

    //상태 + 키워드(이메일/이름) 검색을 괄호로 고정
    @Query("""
        select a from Admin a
        where a.status = :status
          and (
               lower(a.email) like lower(concat('%', :keyword, '%'))
            or lower(a.name)  like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<Admin> searchByStatusAndKeyword(@Param("status") AdminStatus status,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);
}