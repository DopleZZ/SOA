package ru.itmo.soa.vehicle.exception;

public class ApiException extends RuntimeException {

    private final int status;
    private final String reason;

    public ApiException(int status, String reason, String message) {
        super(message);
        this.status = status;
        this.reason = reason;
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, "Bad Request", message);
    }

    public static ApiException unprocessableEntity(String message) {
        return new ApiException(ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(),
                ExtraStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(), message);
    }

    public static ApiException gone(String message) {
        return new ApiException(410, "Gone", message);
    }

    public static ApiException unsupportedMediaType(String message) {
        return new ApiException(415, "Unsupported Media Type", message);
    }
}
