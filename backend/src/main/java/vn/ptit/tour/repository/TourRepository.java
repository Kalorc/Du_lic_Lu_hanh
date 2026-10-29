package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.tour.entity.Tour;

public interface TourRepository extends JpaRepository<Tour, Long> {
}
