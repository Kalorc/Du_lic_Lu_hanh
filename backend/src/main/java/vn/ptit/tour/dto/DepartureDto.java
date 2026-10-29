package vn.ptit.tour.dto;

public record DepartureDto(String departureId,
                           String departureCode,
                           String startsAt,
                           String endsAt,
                           String bookingOpensAt,
                           String bookingClosesAt,
                           String salesStatus,
                           Integer availableSeats,
                           String adultPrice,
                           String childPrice,
                           String currency,
                           boolean bookable) {
}
