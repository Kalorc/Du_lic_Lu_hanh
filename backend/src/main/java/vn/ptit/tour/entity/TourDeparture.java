package vn.ptit.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 4.8. Chuyen khoi hanh. Thuoc phan he Tour & Departure nhung module goi y phai doc
 * de xac dinh Tour co dang ban va con cho hay khong.
 */
@Entity
@Table(name = "tour_departures")
@Getter @Setter @NoArgsConstructor
public class TourDeparture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "departure_id")
    private Long departureId;

    @Column(name = "departure_code", nullable = false, unique = true, length = 28)
    private String departureCode;

    @Column(name = "tour_id", nullable = false)
    private Long tourId;

    @Column(name = "tour_version_id", nullable = false)
    private Long tourVersionId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "booking_opens_at", nullable = false)
    private OffsetDateTime bookingOpensAt;

    @Column(name = "booking_closes_at", nullable = false)
    private OffsetDateTime bookingClosesAt;

    /** CLOSED, OPEN, CANCELLED, COMPLETED. */
    @Column(name = "sales_status", nullable = false, length = 16)
    private String salesStatus = "CLOSED";

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "sellable_capacity", nullable = false)
    private Integer sellableCapacity;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats = 0;

    @Column(name = "hold_seats", nullable = false)
    private Integer holdSeats = 0;

    @Column(name = "confirmed_seats", nullable = false)
    private Integer confirmedSeats = 0;

    @Column(name = "active_offer_id")
    private Long activeOfferId;

    @Column(nullable = false)
    private Integer revision = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
