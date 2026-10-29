package vn.ptit.tour.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

/** Than loi thong nhat theo bang ma loi cua dac ta. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code,
                       String message,
                       Map<String, String> details,
                       String requestId,
                       OffsetDateTime timestamp) {
}
