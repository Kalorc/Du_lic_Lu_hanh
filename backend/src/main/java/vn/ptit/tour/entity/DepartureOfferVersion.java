package vn.ptit.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 4.9. Phien ban gia Tour. Chi ban gia APPROVED va dang active moi duoc dung
 * khi khach tim tour.
 */
@Entity
@Table(name = "departure_offer_versions")
@Getter @Setter @NoArgsConstructor
public class DepartureOfferVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "departure_id", nullable = false)
    private Long departureId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "approval_status", nullable = false, length = 16)
    private String approvalStatus = "DRAFT";

    @Column(name = "adult_price", nullable = false, precision = 14, scale = 0)
    private BigDecimal adultPrice;

    @Column(name = "child_price", precision = 14, scale = 0)
    private BigDecimal childPrice;

    @Column(name = "infant_price", precision = 14, scale = 0)
    private BigDecimal infantPrice;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "policy_json", columnDefinition = "TEXT")
    private String policyJson;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
