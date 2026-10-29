package vn.ptit.tour.dto;

import java.math.BigDecimal;

public record DestinationDto(String destinationId,
                             String name,
                             String slug,
                             String destinationType,
                             String parentDestinationId,
                             String provinceName,
                             String region,
                             String countryCode,
                             BigDecimal latitude,
                             BigDecimal longitude) {
}
