package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.tour.entity.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
