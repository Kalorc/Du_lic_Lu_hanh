package vn.ptit.tour.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.ptit.tour.entity.AppUser;
import vn.ptit.tour.entity.Role;
import vn.ptit.tour.repository.AppUserRepository;

/**
 * Tao tai khoan demo khi APP_DEMO_PASSWORD duoc cau hinh cho moi truong cuc bo.
 * GUEST khong can tai khoan - cac API cong khai cho phep truy cap an danh.
 */
@Configuration
@Profile({"demo", "test"})
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public ApplicationRunner seedUsers(AppUserRepository repo, PasswordEncoder encoder,
                                      @Value("${app.demo.password:}") String demoPassword) {
        return args -> {
            if (demoPassword.isBlank()) {
                log.info("Khong tao tai khoan demo: APP_DEMO_PASSWORD chua duoc cau hinh");
                return;
            }
            createIfMissing(repo, encoder, "khach01", "Nguyen Van Khach", Role.CUSTOMER, demoPassword);
            createIfMissing(repo, encoder, "product01", "Tran Thi San Pham", Role.PRODUCT, demoPassword);
            createIfMissing(repo, encoder, "manager01", "Le Van Quan Ly", Role.MANAGER, demoPassword);
            createIfMissing(repo, encoder, "admin01", "Pham Quan Tri", Role.ADMIN, demoPassword);
            log.info("Tai khoan demo da duoc khoi tao");
        };
    }

    private void createIfMissing(AppUserRepository repo, PasswordEncoder encoder,
                                 String username, String fullName, Role role, String demoPassword) {
        if (repo.existsByUsername(username)) return;
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(demoPassword));
        u.setFullName(fullName);
        u.setRole(role);
        u.setActive(true);
        repo.save(u);
    }
}
