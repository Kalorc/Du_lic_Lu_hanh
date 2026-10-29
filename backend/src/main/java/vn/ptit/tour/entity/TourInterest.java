package vn.ptit.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/** 4.5. Danh muc so thich. Khong luu diem phu hop o day - diem la ket qua tinh toan. */
@Entity
@Table(name = "tour_interests")
@Getter @Setter @NoArgsConstructor
public class TourInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_id")
    private Long interestId;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
