package vn.ptit.tour.exception;

import org.springframework.http.HttpStatus;

/** Bang ma loi theo muc 8 cua dac ta. */
public enum ErrorCode {

    INVALID_FILTER(HttpStatus.BAD_REQUEST, "Tham so loc khong hop le"),
    INVALID_BUDGET(HttpStatus.BAD_REQUEST, "Ngan sach khong hop le"),
    INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "Tham so phan trang khong hop le"),

    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Thieu hoac sai token"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Khong du quyen truy cap"),

    TOUR_NOT_FOUND(HttpStatus.NOT_FOUND, "Tour khong ton tai hoac khong duoc cong khai"),
    VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Phien ban noi dung Tour khong ton tai"),
    INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "So thich khong ton tai"),

    VERSION_LOCKED(HttpStatus.CONFLICT, "Chi duoc sua phien ban o trang thai DRAFT"),
    TAG_UPDATE_CONFLICT(HttpStatus.CONFLICT, "Xung dot khi cap nhat so thich cua phien ban"),

    INVALID_INTEREST_CODE(HttpStatus.UNPROCESSABLE_ENTITY, "Ma so thich khong ton tai hoac khong active"),
    DUPLICATE_INTEREST_CODE(HttpStatus.UNPROCESSABLE_ENTITY, "Ma so thich bi trung"),

    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Vuot gioi han so lan goi"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Loi khong mong doi");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }
}
