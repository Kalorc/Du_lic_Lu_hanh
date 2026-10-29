package vn.ptit.tour.service;

import org.springframework.stereotype.Component;
import vn.ptit.tour.dto.TourSearchRequest;
import vn.ptit.tour.exception.ApiException;
import vn.ptit.tour.exception.ErrorCode;
import vn.ptit.tour.repository.DestinationRepository;
import vn.ptit.tour.repository.TourInterestRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Buoc "trim input -> validate -> normalize interestCodes" cua thuat toan (muc 5.1).
 *
 * Gioi han theo dac ta:
 *   keyword 0-100 ky tu sau trim; destinationId la ma chinh xac; durationDays >= 1;
 *   maxBudget > 0 va la so nguyen VND; interestCodes toi da 10 ma; page >= 1; size 1-10.
 */
@Component
public class SearchParamValidator {

    public static final int KEYWORD_MAX = 100;
    public static final int INTEREST_MAX = 10;
    public static final int SIZE_MAX = 10;
    public static final int SIZE_DEFAULT = 10;

    private final TourInterestRepository interestRepository;
    private final DestinationRepository destinationRepository;

    public SearchParamValidator(TourInterestRepository interestRepository,
                                DestinationRepository destinationRepository) {
        this.interestRepository = interestRepository;
        this.destinationRepository = destinationRepository;
    }

    public SearchParams validate(TourSearchRequest req) {

        // --- keyword: trim, gioi han do dai
        String keyword = req.keyword() == null ? null : req.keyword().trim();
        if (keyword != null && keyword.isEmpty()) {
            keyword = null;
        }
        if (keyword != null && keyword.length() > KEYWORD_MAX) {
            throw new ApiException(ErrorCode.INVALID_FILTER,
                    "keyword toi da " + KEYWORD_MAX + " ky tu sau khi trim");
        }

        // --- destinationId: ma chinh xac, phai ton tai va dang active
        String destinationId = req.destinationId() == null ? null : req.destinationId().trim();
        if (destinationId != null && destinationId.isEmpty()) {
            destinationId = null;
        }
        if (destinationId != null) {
            destinationId = destinationId.toUpperCase();
            boolean ok = destinationRepository.findById(destinationId)
                    .map(d -> d.isActive()).orElse(false);
            if (!ok) {
                throw new ApiException(ErrorCode.INVALID_FILTER,
                        "destinationId khong ton tai hoac khong duoc su dung: " + destinationId);
            }
        }

        // --- durationDays >= 1
        Integer durationDays = req.durationDays();
        if (durationDays != null && durationDays < 1) {
            throw new ApiException(ErrorCode.INVALID_FILTER, "durationDays toi thieu la 1");
        }

        // --- maxBudget > 0
        Long maxBudget = req.maxBudget();
        if (maxBudget != null && maxBudget <= 0) {
            throw new ApiException(ErrorCode.INVALID_BUDGET, "maxBudget phai la so nguyen duong (VND)");
        }

        // --- interestCodes: bo trong, bo trung lap, upper-case, toi da 10, phai active
        List<String> codes = new ArrayList<>();
        if (req.interestCodes() != null) {
            Set<String> seen = new LinkedHashSet<>();
            for (String raw : req.interestCodes()) {
                if (raw == null) continue;
                String c = raw.trim().toUpperCase();
                if (!c.isEmpty()) seen.add(c);
            }
            codes.addAll(seen);
        }
        if (codes.size() > INTEREST_MAX) {
            throw new ApiException(ErrorCode.INVALID_FILTER,
                    "interestCodes toi da " + INTEREST_MAX + " ma");
        }
        if (!codes.isEmpty()) {
            Set<String> valid = new LinkedHashSet<>();
            interestRepository.findActiveByCodes(codes).forEach(i -> valid.add(i.getCode().toUpperCase()));
            List<String> unknown = codes.stream().filter(c -> !valid.contains(c)).toList();
            if (!unknown.isEmpty()) {
                throw new ApiException(ErrorCode.INVALID_INTEREST_CODE,
                        "Ma so thich khong ton tai hoac khong active: " + String.join(", ", unknown));
            }
            codes = codes.stream().filter(valid::contains).toList();
        }

        // --- availableOnly: mac dinh true
        boolean availableOnly = req.availableOnly() == null || req.availableOnly();

        // --- phan trang
        int page = req.page() == null ? 1 : req.page();
        int size = req.size() == null ? SIZE_DEFAULT : req.size();
        if (page < 1) {
            throw new ApiException(ErrorCode.INVALID_PAGINATION, "page bat dau tu 1");
        }
        if (size < 1 || size > SIZE_MAX) {
            throw new ApiException(ErrorCode.INVALID_PAGINATION,
                    "size phai nam trong khoang 1-" + SIZE_MAX);
        }

        return new SearchParams(keyword, destinationId, durationDays, maxBudget,
                codes, availableOnly, page, size);
    }
}
