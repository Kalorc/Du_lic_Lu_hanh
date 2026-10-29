package vn.ptit.tour.dto;

public record InterestDto(Long interestId,
                          String code,
                          String name,
                          String description,
                          boolean active,
                          Short displayOrder) {
}
