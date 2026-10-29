package vn.ptit.tour.dto;

import java.util.List;

/** Ket qua API noi bo 6 / 7: so thich da gan cho mot phien ban noi dung Tour. */
public record VersionInterestsDto(String tourVersionId,
                                  String tourId,
                                  String title,
                                  String contentStatus,
                                  boolean editable,
                                  List<AssignedInterestDto> interests) {
}
