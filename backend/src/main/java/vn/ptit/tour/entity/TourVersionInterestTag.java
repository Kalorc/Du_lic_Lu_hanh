package vn.ptit.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 4.6. Gan so thich cho phien ban Tour - bang trung gian quan trong nhat cua thuat toan.
 * Dung tour_version_id thay vi tour_id: khi noi dung Tour thay doi, danh muc so thich
 * cua phien ban da ban van duoc giu nguyen.
 */
@Entity
@Table(name = "tour_version_interest_tags")
@Getter @Setter @NoArgsConstructor
public class TourVersionInterestTag {

    @EmbeddedId
    private TourVersionInterestTagId id;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public TourVersionInterestTag(Long tourVersionId, Long interestId, Long assignedByUserId) {
        this.id = new TourVersionInterestTagId(tourVersionId, interestId);
        this.assignedByUserId = assignedByUserId;
    }
}
