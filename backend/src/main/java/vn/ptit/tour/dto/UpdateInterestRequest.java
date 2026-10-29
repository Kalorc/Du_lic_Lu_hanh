package vn.ptit.tour.dto;

import jakarta.validation.constraints.*;

/** Body API 9 (PATCH /internal/interests/{id}). Cac truong null nghia la khong doi. */
public record UpdateInterestRequest(
        @Size(max = 120, message = "name toi da 120 ky tu")
        String name,

        String description,

        Boolean active,

        @Min(value = 0, message = "displayOrder >= 0")
        @Max(value = 32767, message = "displayOrder qua lon")
        Integer displayOrder) {
}
