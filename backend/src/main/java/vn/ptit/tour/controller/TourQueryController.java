package vn.ptit.tour.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.ptit.tour.dto.*;
import vn.ptit.tour.service.TourDetailService;
import vn.ptit.tour.service.TourSearchService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API 3: GET /api/v1/tours                 - tim kiem va loc danh sach tour.
 * API 4: GET /api/v1/tour-recommendations  - goi y va xep hang tour theo so thich.
 * API 5: GET /api/v1/tours/{tourId}        - chi tiet mot tour cong khai.
 *
 * Ca ba API deu mo cho GUEST. API 3 va API 4 dung chung mot thuat toan;
 * khac biet la API 4 duoc goi kem interestCodes nen ket qua duoc xep hang theo
 * muc do trung so thich.
 */
@RestController
@RequestMapping("/api/v1")
public class TourQueryController {

    private final TourSearchService searchService;
    private final TourDetailService detailService;

    public TourQueryController(TourSearchService searchService, TourDetailService detailService) {
        this.searchService = searchService;
        this.detailService = detailService;
    }

    @GetMapping("/tours")
    public ResponseEntity<ApiResponse<PageResult<TourSearchItem>>> searchTours(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String destinationId,
            @RequestParam(required = false) Integer durationDays,
            @RequestParam(required = false) Long maxBudget,
            @RequestParam(required = false) List<String> interestCodes,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        return respond(new TourSearchRequest(keyword, destinationId, durationDays, maxBudget,
                interestCodes, availableOnly, page, size));
    }

    @GetMapping("/tour-recommendations")
    public ResponseEntity<ApiResponse<PageResult<TourSearchItem>>> recommendTours(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String destinationId,
            @RequestParam(required = false) Integer durationDays,
            @RequestParam(required = false) Long maxBudget,
            @RequestParam(required = false) List<String> interestCodes,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        return respond(new TourSearchRequest(keyword, destinationId, durationDays, maxBudget,
                interestCodes, availableOnly, page, size));
    }

    @GetMapping("/tours/{tourId}")
    public ResponseEntity<ApiResponse<TourDetailDto>> tourDetail(@PathVariable Long tourId) {
        return ResponseEntity.ok(ApiResponse.ok(detailService.getPublicDetail(tourId)));
    }

    private ResponseEntity<ApiResponse<PageResult<TourSearchItem>>> respond(TourSearchRequest request) {
        TourSearchService.SearchOutcome outcome = searchService.search(request);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("asOf", outcome.asOf().toString());
        meta.put("sort", outcome.sortMode());
        meta.put("priceBasis", TourSearchService.PRICE_BASIS);
        meta.put("availableOnly", outcome.params().availableOnly());
        meta.put("appliedInterestCodes", outcome.params().interestCodes());
        meta.put("personalized", !outcome.params().interestCodes().isEmpty());
        meta.put("scoreMeaning",
                "matchScore la ty le trung so thich (0-100), khong phai xac suat hay ty le hai long du bao");

        return ResponseEntity.ok(ApiResponse.ok(outcome.page(), meta));
    }
}
