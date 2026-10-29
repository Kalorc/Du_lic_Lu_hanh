package vn.ptit.tour.dto;

import java.util.List;

/** Chi tiet tour cong khai (API 5). */
public record TourDetailDto(String tourId,
                            String tourVersionId,
                            String tourCode,
                            String slug,
                            String title,
                            String summary,
                            String description,
                            Integer durationDays,
                            Integer nights,
                            String originDestinationId,
                            List<DestinationStopDto> destinations,
                            List<InterestRefDto> interests,
                            Object itinerary,
                            Object includedServices,
                            Object excludedServices,
                            List<MediaDto> media,
                            String coverImageUrl,
                            String priceFrom,
                            String currency,
                            String priceBasis,
                            List<DepartureDto> departures) {
}
