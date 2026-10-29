package vn.ptit.tour.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Bao bi phan hoi chung: {"success":..., "data":..., "meta":..., "error":...}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, Map<String, Object> meta, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, meta, null);
    }

    public static ApiResponse<Object> fail(ApiError error) {
        return new ApiResponse<>(false, null, null, error);
    }
}
