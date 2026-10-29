package vn.ptit.tour.service;

import java.util.List;

/** Dau vao da duoc trim + validate + normalize. */
public record SearchParams(String keyword,
                           String destinationId,
                           Integer durationDays,
                           Long maxBudget,
                           List<String> interestCodes,
                           boolean availableOnly,
                           int page,
                           int size) {
}
