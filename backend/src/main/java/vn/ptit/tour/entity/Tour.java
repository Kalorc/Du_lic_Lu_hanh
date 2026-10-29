package vn.ptit.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 4.1. Tour - dinh danh on dinh cua mot san pham Tour.
 * Noi dung chi tiet nam o {@link TourVersion}.
 */
@Entity
@Table(name = "tours")
@Getter @Setter @NoArgsConstructor
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tour_id")
    private Long tourId;

    @Column(name = "tour_code", nullable = false, unique = true, length = 24)
    private String tourCode;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    /** ACTIVE hoac ARCHIVED. ARCHIVED khong xuat hien trong ket qua tim kiem cong khai. */
    @Column(name = "lifecycle_status", nullable = false, length = 16)
    private String lifecycleStatus = "ACTIVE";

    @Column(name = "current_published_version_id")
    private Long currentPublishedVersionId;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    /** Kiem soat sua dong thoi (optimistic locking). */
    @Version
    @Column(nullable = false)
    private Integer revision = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
