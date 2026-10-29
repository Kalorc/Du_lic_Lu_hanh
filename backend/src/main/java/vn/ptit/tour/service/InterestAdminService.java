package vn.ptit.tour.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.tour.dto.*;
import vn.ptit.tour.entity.ContentStatus;
import vn.ptit.tour.entity.TourInterest;
import vn.ptit.tour.entity.TourVersion;
import vn.ptit.tour.entity.TourVersionInterestTag;
import vn.ptit.tour.exception.ApiException;
import vn.ptit.tour.exception.ErrorCode;
import vn.ptit.tour.repository.TourInterestRepository;
import vn.ptit.tour.repository.TourVersionInterestTagRepository;
import vn.ptit.tour.repository.TourVersionRepository;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * API noi bo 6-9: xem/gan so thich cho phien ban Tour va quan ly danh muc so thich.
 * Chi vai tro PRODUCT (va MANAGER o API chi doc) duoc goi - kiem soat o tang Security.
 */
@Service
public class InterestAdminService {

    private final TourInterestRepository interestRepository;
    private final TourVersionRepository versionRepository;
    private final TourVersionInterestTagRepository tagRepository;

    public InterestAdminService(TourInterestRepository interestRepository,
                                TourVersionRepository versionRepository,
                                TourVersionInterestTagRepository tagRepository) {
        this.interestRepository = interestRepository;
        this.versionRepository = versionRepository;
        this.tagRepository = tagRepository;
    }

    /** API 6: GET /internal/tour-versions/{versionId}/interests */
    @Transactional(readOnly = true)
    public VersionInterestsDto getVersionInterests(Long versionId) {
        TourVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ApiException(ErrorCode.VERSION_NOT_FOUND));

        List<AssignedInterestDto> interests = new ArrayList<>();
        for (Object[] row : tagRepository.findAllTagsByVersionId(versionId)) {
            interests.add(new AssignedInterestDto(
                    String.valueOf(row[0]),
                    (String) row[1],
                    (String) row[2],
                    (Boolean) row[3],
                    row[4] == null ? null : String.valueOf(row[4]),
                    row[5] == null ? null : row[5].toString()));
        }

        return new VersionInterestsDto(
                String.valueOf(version.getTourVersionId()),
                String.valueOf(version.getTourId()),
                version.getTitle(),
                version.getContentStatus().name(),
                version.getContentStatus() == ContentStatus.DRAFT,
                interests);
    }

    /**
     * API 7: PUT /internal/tour-versions/{versionId}/interests
     * Gan hoac THAY THE TOAN BO danh sach so thich. Chi phien ban DRAFT moi duoc sua
     * (neu khac DRAFT -> 409 VERSION_LOCKED).
     */
    @Transactional
    public VersionInterestsDto replaceVersionInterests(Long versionId,
                                                      AssignInterestsRequest request,
                                                      Long actorUserId) {
        TourVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ApiException(ErrorCode.VERSION_NOT_FOUND));

        if (version.getContentStatus() != ContentStatus.DRAFT) {
            throw new ApiException(ErrorCode.VERSION_LOCKED,
                    "Phien ban dang o trang thai " + version.getContentStatus()
                            + ", chi sua duoc khi la DRAFT");
        }

        // Normalize: trim, upper, bo trong. Trung lap trong body -> 422 DUPLICATE_INTEREST_CODE
        List<String> raw = request.interestCodes() == null ? List.of() : request.interestCodes();
        List<String> cleaned = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String c : raw) {
            if (c == null || c.trim().isEmpty()) continue;
            String code = c.trim().toUpperCase();
            if (!seen.add(code)) {
                throw new ApiException(ErrorCode.DUPLICATE_INTEREST_CODE,
                        "Ma so thich bi trung trong body: " + code);
            }
            cleaned.add(code);
        }
        if (cleaned.size() > SearchParamValidator.INTEREST_MAX) {
            throw new ApiException(ErrorCode.INVALID_FILTER,
                    "Toi da " + SearchParamValidator.INTEREST_MAX + " ma so thich");
        }

        // Danh sach rong la hop le: nghia la bo toan bo so thich cua phien ban.
        // Khong goi findActiveByCodes voi tap rong vi se sinh SQL "IN ()".
        Map<String, Long> idByCode = new LinkedHashMap<>();
        if (!cleaned.isEmpty()) {
            interestRepository.findActiveByCodes(cleaned)
                    .forEach(i -> idByCode.put(i.getCode().toUpperCase(), i.getInterestId()));

            List<String> unknown = cleaned.stream().filter(c -> !idByCode.containsKey(c)).toList();
            if (!unknown.isEmpty()) {
                throw new ApiException(ErrorCode.INVALID_INTEREST_CODE,
                        "So thich khong ton tai hoac khong active: " + String.join(", ", unknown));
            }
        }

        tagRepository.deleteByVersionId(versionId);
        tagRepository.flush();

        List<TourVersionInterestTag> tags = cleaned.stream()
                .map(code -> new TourVersionInterestTag(versionId, idByCode.get(code), actorUserId))
                .toList();
        tagRepository.saveAll(tags);
        tagRepository.flush();

        return getVersionInterests(versionId);
    }

    /** API 8: POST /internal/interests */
    @Transactional
    public InterestDto createInterest(CreateInterestRequest request) {
        String code = request.code().trim().toUpperCase();
        if (interestRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(ErrorCode.DUPLICATE_INTEREST_CODE,
                    "Ma so thich da ton tai: " + code);
        }

        TourInterest entity = new TourInterest();
        entity.setCode(code);
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setActive(request.active() == null || request.active());
        entity.setDisplayOrder(request.displayOrder() == null
                ? (short) 99 : request.displayOrder().shortValue());
        entity.setUpdatedAt(OffsetDateTime.now());

        TourInterest saved = interestRepository.save(entity);
        return toDto(saved);
    }

    /** API 9: PATCH /internal/interests/{interestId} */
    @Transactional
    public InterestDto updateInterest(Long interestId, UpdateInterestRequest request) {
        TourInterest entity = interestRepository.findById(interestId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTEREST_NOT_FOUND,
                        "So thich khong ton tai: " + interestId));

        if (request.name() != null && !request.name().isBlank()) {
            entity.setName(request.name().trim());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        if (request.displayOrder() != null) {
            entity.setDisplayOrder(request.displayOrder().shortValue());
        }
        entity.setUpdatedAt(OffsetDateTime.now());

        return toDto(interestRepository.save(entity));
    }

    /** Danh muc day du (ke ca so thich da tat) cho man hinh quan tri. */
    @Transactional(readOnly = true)
    public List<InterestDto> listAllInterests() {
        return interestRepository.findAllOrdered().stream().map(this::toDto).toList();
    }

    private InterestDto toDto(TourInterest i) {
        return new InterestDto(i.getInterestId(), i.getCode(), i.getName(),
                i.getDescription(), i.isActive(), i.getDisplayOrder());
    }
}
