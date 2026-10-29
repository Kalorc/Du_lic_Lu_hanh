package vn.ptit.tour.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.tour.dto.*;
import vn.ptit.tour.entity.TourMedia;
import vn.ptit.tour.repository.*;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Thuat toan tra cuu + goi y tour (muc 5 va 6 dac ta).
 *
 * Thu tu xu ly:
 *   1. trim input -> validate -> normalize interestCodes      (SearchParamValidator)
 *   2. chon candidates Tour ACTIVE / current PUBLISHED,
 *      Departure OPEN trong booking window tai asOf,
 *      active offer APPROVED, con cho neu availableOnly
 *   3. ap hard filters (keyword, destination, duration, budget)
 *   4. lay cheapest eligible departure moi Tour                (TourSearchDao)
 *   5. join tags (chi so thich active)
 *   6. compute score                                           (InterestScorer)
 *   7. sort: matchScore DESC NULLS LAST, priceFrom ASC, tourId ASC
 *   8. paginate
 *
 * Cac tour matched = 0 VAN DUOC GIU neu dat hard filters.
 */
@Service
public class TourSearchService {

    public static final String PRICE_BASIS = "ADULT_PER_PERSON";

    private final TourSearchDao searchDao;
    private final TourVersionInterestTagRepository tagRepository;
    private final TourInterestRepository interestRepository;
    private final TourMediaRepository mediaRepository;
    private final TourVersionDestinationRepository versionDestinationRepository;
    private final SearchParamValidator validator;
    private final InterestScorer scorer;

    public TourSearchService(TourSearchDao searchDao,
                             TourVersionInterestTagRepository tagRepository,
                             TourInterestRepository interestRepository,
                             TourMediaRepository mediaRepository,
                             TourVersionDestinationRepository versionDestinationRepository,
                             SearchParamValidator validator,
                             InterestScorer scorer) {
        this.searchDao = searchDao;
        this.tagRepository = tagRepository;
        this.interestRepository = interestRepository;
        this.mediaRepository = mediaRepository;
        this.versionDestinationRepository = versionDestinationRepository;
        this.validator = validator;
        this.scorer = scorer;
    }

    public record SearchOutcome(PageResult<TourSearchItem> page,
                                SearchParams params,
                                OffsetDateTime asOf,
                                String sortMode) {
    }

    @Transactional(readOnly = true)
    public SearchOutcome search(TourSearchRequest request) {
        OffsetDateTime asOf = OffsetDateTime.now();
        SearchParams p = validator.validate(request);

        // Buoc 2-4: hard filters + chuyen khoi hanh re nhat du dieu kien moi tour
        List<CandidateRow> candidates = searchDao.findCandidates(
                p.keyword(), p.destinationId(), p.durationDays(), p.maxBudget(),
                p.availableOnly(), asOf);

        if (candidates.isEmpty()) {
            return new SearchOutcome(
                    new PageResult<>(List.of(), Pagination.of(p.page(), p.size(), 0)),
                    p, asOf, sortMode(p));
        }

        List<Long> versionIds = candidates.stream().map(CandidateRow::tourVersionId).toList();

        // Buoc 5: join tags - CHI tinh cac so thich dang active
        Map<Long, LinkedHashSet<String>> tagsByVersion = new HashMap<>();
        for (Object[] row : tagRepository.findActiveTagsByVersionIds(versionIds)) {
            Long versionId = (Long) row[0];
            String code = ((String) row[1]).toUpperCase();
            tagsByVersion.computeIfAbsent(versionId, k -> new LinkedHashSet<>()).add(code);
        }

        // Ten hien thi cua so thich, dung cho truong "reason"
        Map<String, String> nameByCode = new HashMap<>();
        interestRepository.findAllOrdered()
                .forEach(i -> nameByCode.put(i.getCode().toUpperCase(), i.getName()));

        // Anh dai dien
        Map<Long, String> coverByVersion = new HashMap<>();
        for (TourMedia m : mediaRepository.findCovers(versionIds)) {
            coverByVersion.put(m.getTourVersionId(), m.getMediaUrl());
        }

        // Diem den cua tung phien ban (de hien thi tren the tour)
        Map<Long, List<String>> destByVersion = new HashMap<>();
        for (Long versionId : versionIds) {
            List<String> ids = new ArrayList<>();
            for (Object[] row : versionDestinationRepository.findByVersionId(versionId)) {
                ids.add((String) row[0]);
            }
            destByVersion.put(versionId, ids);
        }

        // Buoc 6: cham diem
        List<TourSearchItem> items = new ArrayList<>(candidates.size());
        for (CandidateRow c : candidates) {
            LinkedHashSet<String> tags = tagsByVersion.getOrDefault(c.tourVersionId(), new LinkedHashSet<>());
            InterestScorer.Score sc = scorer.score(p.interestCodes(), tags);

            items.add(new TourSearchItem(
                    String.valueOf(c.tourId()),
                    String.valueOf(c.tourVersionId()),
                    c.tourCode(),
                    c.slug(),
                    c.title(),
                    c.summary(),
                    c.durationDays(),
                    c.nights(),
                    c.priceFrom().toPlainString(),
                    c.currency(),
                    PRICE_BASIS,
                    String.valueOf(c.departureId()),
                    c.departureStartsAt().toString(),
                    c.availableSeats(),
                    destByVersion.getOrDefault(c.tourVersionId(), List.of()),
                    new ArrayList<>(tags),
                    sc.matched(),
                    sc.matchScore(),
                    buildReason(p.interestCodes(), sc, nameByCode),
                    coverByVersion.get(c.tourVersionId())
            ));
        }

        // Buoc 7: sort - matchScore DESC NULLS LAST, priceFrom ASC, tourId ASC
        Comparator<TourSearchItem> byScoreDescNullsLast =
                Comparator.comparingDouble(i -> i.matchScore() == null ? -1.0d : i.matchScore());
        items.sort(byScoreDescNullsLast.reversed()
                .thenComparingLong((TourSearchItem i) -> Long.parseLong(i.priceFrom()))
                .thenComparingLong((TourSearchItem i) -> Long.parseLong(i.tourId())));

        // Buoc 8: phan trang
        long totalItems = items.size();
        int from = Math.min((p.page() - 1) * p.size(), items.size());
        int to = Math.min(from + p.size(), items.size());
        List<TourSearchItem> pageItems = items.subList(from, to);

        return new SearchOutcome(
                new PageResult<>(new ArrayList<>(pageItems), Pagination.of(p.page(), p.size(), totalItems)),
                p, asOf, sortMode(p));
    }

    private String sortMode(SearchParams p) {
        return p.interestCodes().isEmpty() ? "PRICE_ASC" : "RELEVANCE";
    }

    /**
     * Cau giai thich hien thi cho khach. Khi khach khong chon so thich nao thi
     * KHONG tuyen bo "goi y ca nhan hoa".
     */
    private String buildReason(List<String> selected, InterestScorer.Score sc,
                               Map<String, String> nameByCode) {
        if (selected == null || selected.isEmpty()) {
            return "Sắp xếp theo giá từ thấp đến cao (bạn chưa chọn sở thích)";
        }
        if (sc.matchedCount() == 0) {
            return "Phù hợp 0/" + selected.size() + " sở thích: tour vẫn đạt các điều kiện lọc bạn chọn";
        }
        List<String> names = sc.matched().stream()
                .map(code -> nameByCode.getOrDefault(code, code))
                .toList();
        return "Phù hợp " + sc.matchedCount() + "/" + selected.size()
                + " sở thích: " + String.join(", ", names);
    }
}
