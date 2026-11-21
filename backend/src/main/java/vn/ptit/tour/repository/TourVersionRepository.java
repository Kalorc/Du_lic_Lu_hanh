package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import vn.ptit.tour.entity.TourVersion;
import java.util.Optional;

public interface TourVersionRepository extends JpaRepository<TourVersion, Long> {
    /** Serialize complete-set replacements, including versions with no existing tags. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT version FROM TourVersion version WHERE version.tourVersionId = :versionId")
    Optional<TourVersion> findForTagUpdate(Long versionId);
}
