package vn.ptit.tour.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class TourVersionDestinationId implements Serializable {

    @Column(name = "tour_version_id")
    private Long tourVersionId;

    @Column(name = "destination_id", length = 20)
    private String destinationId;
}
