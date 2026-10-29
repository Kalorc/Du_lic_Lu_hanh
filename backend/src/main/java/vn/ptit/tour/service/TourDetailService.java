package vn.ptit.tour.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.tour.dto.*;
import vn.ptit.tour.entity.*;
import vn.ptit.tour.exception.ApiException;
import vn.ptit.tour.exception.ErrorCode;
import vn.ptit.tour.repository.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** API 5: chi tiet mot tour cong khai. */
@Service
public class TourDetailService {

    private final TourRepository tourRepository;
    private final TourVersionRepository versionRepository;
    private final TourVersionDestinationRepository versionDestinationRepository;
    private final TourVersionInterestTagRepository tagRepository;
    private final TourMediaRepository mediaRepository;
    private final TourDepartureRepository departureRepository;
    private final DepartureOfferVersionRepository offerRepository;
    private final TourSearchDao searchDao;
    private final ObjectMapper objectMapper;

    public TourDetailService(TourRepository tourRepository,
                             TourVersionRepository versionRepository,
                             TourVersionDestinationRepository versionDestinationRepository,
                             TourVersionInterestTagRepository tagRepository,
                             TourMediaRepository mediaRepository,
                             TourDepartureRepository departureRepository,
                             DepartureOfferVersionRepository offerRepository,
                             TourSearchDao searchDao,
                             ObjectMapper objectMapper) {
        this.tourRepository = tourRepository;
        this.versionRepository = versionRepository;
        this.versionDestinationRepository = versionDestinationRepository;
        this.tagRepository = tagRepository;
        this.mediaRepository = mediaRepository;
        this.departureRepository = departureRepository;
        this.offerRepository = offerRepository;
        this.searchDao = searchDao;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TourDetailDto getPublicDetail(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ApiException(ErrorCode.TOUR_NOT_FOUND));

        // Tour ARCHIVED hoac chua co phien ban cong bo thi khong duoc xem cong khai
        if (!"ACTIVE".equals(tour.getLifecycleStatus()) || tour.getCurrentPublishedVersionId() == null) {
            throw new ApiException(ErrorCode.TOUR_NOT_FOUND);
        }

        TourVersion version = versionRepository.findById(tour.getCurrentPublishedVersionId())
                .orElseThrow(() -> new ApiException(ErrorCode.VERSION_NOT_FOUND));

        if (version.getContentStatus() != ContentStatus.PUBLISHED) {
            throw new ApiException(ErrorCode.TOUR_NOT_FOUND);
        }

        List<DestinationStopDto> destinations = new ArrayList<>();
        for (Object[] row : versionDestinationRepository.findByVersionId(version.getTourVersionId())) {
            destinations.add(new DestinationStopDto((String) row[0], (String) row[1],
                    ((Number) row[2]).intValue(), (Boolean) row[3]));
        }

        List<InterestRefDto> interests = new ArrayList<>();
        for (Object[] row : tagRepository.findAllTagsByVersionId(version.getTourVersionId())) {
            boolean active = (Boolean) row[3];
            if (!active) continue;   // cong khai chi hien so thich dang active
            interests.add(new InterestRefDto(String.valueOf(row[0]), (String) row[1],
                    (String) row[2], true));
        }

        List<MediaDto> media = mediaRepository.findByVersion(version.getTourVersionId()).stream()
                .map(m -> new MediaDto(String.valueOf(m.getMediaId()), m.getMediaType(), m.getMediaUrl(),
                        m.getAltText(), m.getCaption(), (int) m.getSortOrder(), m.isCover()))
                .toList();
        String coverUrl = media.stream().filter(MediaDto::cover).map(MediaDto::mediaUrl)
                .findFirst().orElse(null);

        // Gia "tu": chuyen khoi hanh re nhat dang du dieu kien ban
        Optional<CandidateRow> cheapest = searchDao
                .findCandidateForTour(tourId, true, OffsetDateTime.now()).stream().findFirst();
        String priceFrom = cheapest.map(c -> c.priceFrom().toPlainString()).orElse(null);
        String currency = cheapest.map(CandidateRow::currency).orElse("VND");

        OffsetDateTime now = OffsetDateTime.now();
        List<DepartureDto> departures = new ArrayList<>();
        for (TourDeparture d : departureRepository.findByTourAndVersion(tourId, version.getTourVersionId())) {
            DepartureOfferVersion offer = d.getActiveOfferId() == null ? null
                    : offerRepository.findById(d.getActiveOfferId()).orElse(null);
            boolean approvedPrice = offer != null && "APPROVED".equals(offer.getApprovalStatus());
            boolean bookable = "OPEN".equals(d.getSalesStatus())
                    && approvedPrice
                    && !now.isBefore(d.getBookingOpensAt())
                    && !now.isAfter(d.getBookingClosesAt())
                    && d.getAvailableSeats() > 0;

            departures.add(new DepartureDto(
                    String.valueOf(d.getDepartureId()),
                    d.getDepartureCode(),
                    d.getStartsAt().toString(),
                    d.getEndsAt().toString(),
                    d.getBookingOpensAt().toString(),
                    d.getBookingClosesAt().toString(),
                    d.getSalesStatus(),
                    d.getAvailableSeats(),
                    approvedPrice ? offer.getAdultPrice().toPlainString() : null,
                    approvedPrice && offer.getChildPrice() != null
                            ? offer.getChildPrice().toPlainString() : null,
                    offer != null ? offer.getCurrency() : "VND",
                    bookable));
        }

        return new TourDetailDto(
                String.valueOf(tour.getTourId()),
                String.valueOf(version.getTourVersionId()),
                tour.getTourCode(),
                tour.getSlug(),
                version.getTitle(),
                version.getSummary(),
                version.getDescription(),
                (int) version.getDurationDays(),
                (int) version.getNights(),
                version.getOriginDestinationId(),
                destinations,
                interests,
                readJson(version.getItineraryJson()),
                readJson(version.getIncludedServicesJson()),
                readJson(version.getExcludedServicesJson()),
                media,
                coverUrl,
                priceFrom,
                currency,
                TourSearchService.PRICE_BASIS,
                departures);
    }

    private Object readJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return raw;
        }
    }
}
