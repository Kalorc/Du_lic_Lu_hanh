package vn.ptit.tour.dto;

import java.util.List;

public record PageResult<T>(List<T> items, Pagination pagination) {
}
