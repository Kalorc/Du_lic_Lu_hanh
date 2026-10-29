package vn.ptit.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/** 4.2. Phien ban noi dung Tour - quan ly noi dung theo vong doi duyet va xuat ban. */
@Entity
@Table(name = "tour_versions")
@Getter @Setter @NoArgsConstructor
public class TourVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tour_version_id")
    private Long tourVersionId;

    @Column(name = "tour_id", nullable = false)
    private Long tourId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_status", nullable = false, length = 16)
    private ContentStatus contentStatus = ContentStatus.DRAFT;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "origin_destination_id", length = 20)
    private String originDestinationId;

    @Column(name = "duration_days", nullable = false)
    private Short durationDays;

    @Column(nullable = false)
    private Short nights = 0;

    @Column(name = "itinerary_json", columnDefinition = "TEXT")
    private String itineraryJson;

    @Column(name = "included_services_json", columnDefinition = "TEXT")
    private String includedServicesJson;

    @Column(name = "excluded_services_json", columnDefinition = "TEXT")
    private String excludedServicesJson;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
