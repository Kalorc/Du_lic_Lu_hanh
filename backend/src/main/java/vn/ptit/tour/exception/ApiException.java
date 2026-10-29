package vn.ptit.tour.exception;

import java.util.Map;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, String> details;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage(), null);
    }

    public ApiException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public ApiException(ErrorCode code, String message, Map<String, String> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public ErrorCode getCode() { return code; }
    public Map<String, String> getDetails() { return details; }
}
