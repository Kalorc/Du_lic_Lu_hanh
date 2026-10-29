package vn.ptit.tour.dto;

import java.util.List;

/**
 * Dau vao thuat toan (muc 5.1 dac ta).
 * keyword 0-100 ky tu sau trim, destinationId ma chinh xac, durationDays >= 1,
 * maxBudget > 0 (VND), interestCodes 0-10 ma, availableOnly mac dinh true,
 * page 1-based, size 1-10.
 */
public record TourSearchRequest(String keyword,
                                String destinationId,
                                Integer durationDays,
                                Long maxBudget,
                                List<String> interestCodes,
                                Boolean availableOnly,
                                Integer page,
                                Integer size) {
}
