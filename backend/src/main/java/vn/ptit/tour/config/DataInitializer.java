package vn.ptit.tour.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.ptit.tour.entity.AppUser;
import vn.ptit.tour.entity.Role;
import vn.ptit.tour.repository.AppUserRepository;

/**
 * Tao san tai khoan demo cho tung vai tro. Mat khau mac dinh: 123456.
 * GUEST khong can tai khoan - cac API cong khai cho phep truy cap an danh.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public ApplicationRunner seedUsers(AppUserRepository repo, PasswordEncoder encoder) {
        return args -> {
            createIfMissing(repo, encoder, "khach01", "Nguyen Van Khach", Role.CUSTOMER);
            createIfMissing(repo, encoder, "product01", "Tran Thi San Pham", Role.PRODUCT);
            createIfMissing(repo, encoder, "manager01", "Le Van Quan Ly", Role.MANAGER);
            createIfMissing(repo, encoder, "admin01", "Pham Quan Tri", Role.ADMIN);
            log.info("Tai khoan demo san sang (mat khau: 123456): khach01 / product01 / manager01 / admin01");
        };
    }

    private void createIfMissing(AppUserRepository repo, PasswordEncoder encoder,
                                 String username, String fullName, Role role) {
        if (repo.existsByUsername(username)) return;
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode("123456"));
        u.setFullName(fullName);
        u.setRole(role);
        u.setActive(true);
        repo.save(u);
    }
}
