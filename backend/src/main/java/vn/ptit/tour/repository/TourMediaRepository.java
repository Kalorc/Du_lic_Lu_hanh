package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.ptit.tour.entity.TourMedia;

import java.util.Collection;
import java.util.List;

public interface TourMediaRepository extends JpaRepository<TourMedia, Long> {

    @Query("SELECT m FROM TourMedia m WHERE m.tourVersionId = :versionId ORDER BY m.cover DESC, m.sortOrder")
    List<TourMedia> findByVersion(Long versionId);

    @Query("SELECT m FROM TourMedia m WHERE m.tourVersionId IN :versionIds AND m.cover = true")
    List<TourMedia> findCovers(Collection<Long> versionIds);
}
