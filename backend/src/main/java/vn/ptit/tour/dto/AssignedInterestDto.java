package vn.ptit.tour.dto;

public record AssignedInterestDto(String interestId,
                                  String code,
                                  String name,
                                  boolean active,
                                  String assignedByUserId,
                                  String createdAt) {
}
