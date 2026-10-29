package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.ptit.tour.entity.TourDeparture;

import java.util.List;

public interface TourDepartureRepository extends JpaRepository<TourDeparture, Long> {

    @Query("""
           SELECT d FROM TourDeparture d
           WHERE d.tourId = :tourId AND d.tourVersionId = :versionId
           ORDER BY d.startsAt
           """)
    List<TourDeparture> findByTourAndVersion(Long tourId, Long versionId);
}
