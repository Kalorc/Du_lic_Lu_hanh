package vn.ptit.tour.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.ptit.tour.dto.ApiResponse;
import vn.ptit.tour.dto.DestinationDto;
import vn.ptit.tour.dto.InterestDto;
import vn.ptit.tour.service.CatalogService;

import java.util.List;

/**
 * API 1: GET /api/v1/interests     - danh sach so thich dang hoat dong cho bo loc.
 * API 2: GET /api/v1/destinations  - danh sach diem den de chon tinh/thanh, khu vuc.
 * Quyen: tat ca nguoi dung, ke ca chua dang nhap.
 */
@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/interests")
    public ResponseEntity<ApiResponse<List<InterestDto>>> interests() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.listActiveInterests()));
    }

    @GetMapping("/destinations")
    public ResponseEntity<ApiResponse<List<DestinationDto>>> destinations(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.listDestinations(type)));
    }
}
