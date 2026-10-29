package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.tour.entity.TourVersion;

public interface TourVersionRepository extends JpaRepository<TourVersion, Long> {
}
