package vn.ptit.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/** 4.4. Diem den cua phien ban Tour. Khoa chinh ghep (tour_version_id, destination_id). */
@Entity
@Table(name = "tour_version_destinations")
@Getter @Setter @NoArgsConstructor
public class TourVersionDestination {

    @EmbeddedId
    private TourVersionDestinationId id;

    @Column(name = "stop_order", nullable = false)
    private Short stopOrder = 1;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryStop = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
