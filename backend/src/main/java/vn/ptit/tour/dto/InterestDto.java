package vn.ptit.tour.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record InterestDto(@JsonSerialize(using = ToStringSerializer.class) Long interestId,
                          String code,
                          String name,
                          String description,
                          boolean active,
                          Short displayOrder) {
}
