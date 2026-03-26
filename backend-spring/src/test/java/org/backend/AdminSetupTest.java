package org.backend;

import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminRole;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.elasticsearch.repositories.enabled=false"
})
public class AdminSetupTest {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void createAdminUser() {
        String email = "admin@test.com";

        if (adminRepository.findByEmail(email).isEmpty()) {
            Admin admin = Admin.builder()
                    .name("관리자")
                    .email(email)
                    .password(passwordEncoder.encode("1234"))
                    .phone("010-1234-5678")
                    .google(false)
                    .role(AdminRole.ADMIN)
                    .status(AdminStatus.ACTIVE)
                    .build();

            adminRepository.save(admin);
            System.out.println("========== ADMIN CREATED ==========");
            System.out.println("Email: " + email);
            System.out.println("Password: 1234");
            System.out.println("===================================");
        } else {
            System.out.println("Admin user already exists!");
            // Update password just in case
            Admin admin = adminRepository.findByEmail(email).get();
            admin.changePassword(passwordEncoder.encode("1234"));
            admin.changeStatus(AdminStatus.ACTIVE);
            adminRepository.save(admin);
            System.out.println("Admin updated with password 1234 and ACTIVE status.");
        }
    }
}
