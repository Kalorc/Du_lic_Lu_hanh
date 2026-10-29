package vn.ptit.tour.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.ptit.tour.dto.*;
import vn.ptit.tour.security.CurrentUser;
import vn.ptit.tour.service.InterestAdminService;

import java.util.List;

/**
 * API noi bo (yeu cau xac thuc JWT):
 *   6. GET   /api/v1/internal/tour-versions/{versionId}/interests  - PRODUCT, MANAGER
 *   7. PUT   /api/v1/internal/tour-versions/{versionId}/interests  - PRODUCT
 *   8. POST  /api/v1/internal/interests                            - PRODUCT
 *   9. PATCH /api/v1/internal/interests/{interestId}               - PRODUCT
 */
@RestController
@RequestMapping("/api/v1/internal")
public class InternalInterestController {

    private final InterestAdminService service;

    public InternalInterestController(InterestAdminService service) {
        this.service = service;
    }

    @GetMapping("/tour-versions/{versionId}/interests")
    public ResponseEntity<ApiResponse<VersionInterestsDto>> getVersionInterests(
            @PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getVersionInterests(versionId)));
    }

    @PutMapping("/tour-versions/{versionId}/interests")
    public ResponseEntity<ApiResponse<VersionInterestsDto>> replaceVersionInterests(
            @PathVariable Long versionId,
            @Valid @RequestBody AssignInterestsRequest request) {
        VersionInterestsDto result =
                service.replaceVersionInterests(versionId, request, CurrentUser.userId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** Danh muc day du (ke ca so thich da tat) phuc vu man hinh quan tri. */
    @GetMapping("/interests")
    public ResponseEntity<ApiResponse<List<InterestDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.listAllInterests()));
    }

    @PostMapping("/interests")
    public ResponseEntity<ApiResponse<InterestDto>> create(
            @Valid @RequestBody CreateInterestRequest request) {
        InterestDto created = service.createInterest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PatchMapping("/interests/{interestId}")
    public ResponseEntity<ApiResponse<InterestDto>> update(
            @PathVariable Long interestId,
            @Valid @RequestBody UpdateInterestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateInterest(interestId, request)));
    }
}
