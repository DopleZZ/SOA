package ru.itmo.soa.shop.exception;

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

    public static ApiException badGateway(String message) {
        return new ApiException(ExtraStatus.BAD_GATEWAY.getStatusCode(),
                ExtraStatus.BAD_GATEWAY.getReasonPhrase(), message);
    }

    public static ApiException serviceUnavailable(String message) {
        return new ApiException(503, "Service Unavailable", message);
    }

    public static ApiException gatewayTimeout(String message) {
        return new ApiException(ExtraStatus.GATEWAY_TIMEOUT.getStatusCode(),
                ExtraStatus.GATEWAY_TIMEOUT.getReasonPhrase(), message);
    }
}
