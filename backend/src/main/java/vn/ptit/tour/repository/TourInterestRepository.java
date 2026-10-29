package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.ptit.tour.entity.TourInterest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TourInterestRepository extends JpaRepository<TourInterest, Long> {

    Optional<TourInterest> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT i FROM TourInterest i WHERE i.active = true ORDER BY i.displayOrder, i.name")
    List<TourInterest> findAllActive();

    @Query("SELECT i FROM TourInterest i ORDER BY i.displayOrder, i.name")
    List<TourInterest> findAllOrdered();

    /** Chi lay cac so thich dang active - dung de validate interestCodes dau vao. */
    @Query("SELECT i FROM TourInterest i WHERE i.active = true AND UPPER(i.code) IN :codes")
    List<TourInterest> findActiveByCodes(Collection<String> codes);
}
