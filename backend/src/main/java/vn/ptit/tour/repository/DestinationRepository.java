package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.ptit.tour.entity.Destination;

import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, String> {

    @Query("SELECT d FROM Destination d WHERE d.active = true ORDER BY d.destinationType, d.name")
    List<Destination> findAllActive();

    @Query("""
           SELECT d FROM Destination d
           WHERE d.active = true AND d.destinationType = :type
           ORDER BY d.name
           """)
    List<Destination> findActiveByType(String type);
}
