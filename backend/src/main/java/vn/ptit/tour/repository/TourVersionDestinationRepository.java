package vn.ptit.tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.ptit.tour.entity.TourVersionDestination;
import vn.ptit.tour.entity.TourVersionDestinationId;

import java.util.List;
import java.util.Collection;

public interface TourVersionDestinationRepository
        extends JpaRepository<TourVersionDestination, TourVersionDestinationId> {

    @Query("""
           SELECT td.id.destinationId, d.name, td.stopOrder, td.primaryStop
           FROM TourVersionDestination td
             JOIN Destination d ON d.destinationId = td.id.destinationId
           WHERE td.id.tourVersionId = :versionId
           ORDER BY td.stopOrder
           """)
    List<Object[]> findByVersionId(Long versionId);

    /** Batch destinations once for the candidate set, before global ranking/pagination. */
    @Query("""
           SELECT td.id.tourVersionId, td.id.destinationId
           FROM TourVersionDestination td
           WHERE td.id.tourVersionId IN :versionIds
           ORDER BY td.id.tourVersionId, td.stopOrder, td.id.destinationId
           """)
    List<Object[]> findByVersionIds(Collection<Long> versionIds);
}
