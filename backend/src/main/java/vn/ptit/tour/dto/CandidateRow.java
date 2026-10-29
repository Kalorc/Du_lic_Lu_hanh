package vn.ptit.tour.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Dong ung vien sau buoc hard filter + chon chuyen khoi hanh re nhat du dieu kien
 * cua tung Tour. Chua cham diem so thich.
 */
public record CandidateRow(Long tourId,
                           Long tourVersionId,
                           String tourCode,
                           String slug,
                           String title,
                           String summary,
                           Integer durationDays,
                           Integer nights,
                           Long departureId,
                           OffsetDateTime departureStartsAt,
                           Integer availableSeats,
                           BigDecimal priceFrom,
                           String currency) {
}
