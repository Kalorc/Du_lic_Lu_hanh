package vn.ptit.tour.dto;

import java.util.List;

/** Mot dong ket qua tra cuu / goi y tour. */
public record TourSearchItem(String tourId,
                             String tourVersionId,
                             String tourCode,
                             String slug,
                             String title,
                             String summary,
                             Integer durationDays,
                             Integer nights,
                             String priceFrom,
                             String currency,
                             String priceBasis,
                             String representativeDepartureId,
                             String representativeDepartureStartsAt,
                             Integer availableSeats,
                             List<String> destinationIds,
                             List<String> interestCodes,
                             List<String> matchedInterests,
                             Double matchScore,
                             String reason,
                             String coverImageUrl) {
}
