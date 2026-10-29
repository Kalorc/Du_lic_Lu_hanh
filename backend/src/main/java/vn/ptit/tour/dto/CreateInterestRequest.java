package vn.ptit.tour.dto;

import jakarta.validation.constraints.*;

/** Body API 8 (POST /internal/interests). */
public record CreateInterestRequest(
        @NotBlank(message = "code khong duoc trong")
        @Size(max = 40, message = "code toi da 40 ky tu")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$",
                 message = "code chi gom chu, so va dau gach duoi, bat dau bang chu")
        String code,

        @NotBlank(message = "name khong duoc trong")
        @Size(max = 120, message = "name toi da 120 ky tu")
        String name,

        String description,

        Boolean active,

        @Min(value = 0, message = "displayOrder >= 0")
        @Max(value = 32767, message = "displayOrder qua lon")
        Integer displayOrder) {
}
