package vn.ptit.tour.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.tour.dto.DestinationDto;
import vn.ptit.tour.dto.InterestDto;
import vn.ptit.tour.entity.Destination;
import vn.ptit.tour.repository.DestinationRepository;
import vn.ptit.tour.repository.TourInterestRepository;

import java.util.List;

/** API 1 + 2: danh muc so thich va danh muc diem den cho bo loc. */
@Service
public class CatalogService {

    private final TourInterestRepository interestRepository;
    private final DestinationRepository destinationRepository;

    public CatalogService(TourInterestRepository interestRepository,
                          DestinationRepository destinationRepository) {
        this.interestRepository = interestRepository;
        this.destinationRepository = destinationRepository;
    }

    @Transactional(readOnly = true)
    public List<InterestDto> listActiveInterests() {
        return interestRepository.findAllActive().stream()
                .map(i -> new InterestDto(i.getInterestId(), i.getCode(), i.getName(),
                        i.getDescription(), i.isActive(), i.getDisplayOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DestinationDto> listDestinations(String type) {
        List<Destination> rows = (type == null || type.isBlank())
                ? destinationRepository.findAllActive()
                : destinationRepository.findActiveByType(type.trim().toUpperCase());
        return rows.stream()
                .map(d -> new DestinationDto(d.getDestinationId(), d.getName(), d.getSlug(),
                        d.getDestinationType(), d.getParentDestinationId(), d.getProvinceName(),
                        d.getRegion(), d.getCountryCode(), d.getLatitude(), d.getLongitude()))
                .toList();
    }
}
