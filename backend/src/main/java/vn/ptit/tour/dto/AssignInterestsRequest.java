package vn.ptit.tour.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Body API 7 (PUT): gan hoac thay the TOAN BO danh sach so thich cua mot phien ban. */
public record AssignInterestsRequest(
        @NotNull(message = "interestCodes khong duoc thieu")
        @Size(max = 10, message = "Toi da 10 ma so thich")
        List<String> interestCodes) {
}
