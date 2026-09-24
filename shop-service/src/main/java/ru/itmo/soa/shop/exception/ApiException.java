package ru.itmo.soa.shop.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class ApiException extends WebApplicationException {

    public ApiException(Response.StatusType status, String message) {
        super(message, Response.status(status).build());
    }

    public static ApiException badRequest(String message) {
        return new ApiException(Response.Status.BAD_REQUEST, message);
    }

    public static ApiException unprocessableEntity(String message) {
        return new ApiException(ExtraStatus.UNPROCESSABLE_ENTITY, message);
    }

    public static ApiException badGateway(String message) {
        return new ApiException(ExtraStatus.BAD_GATEWAY, message);
    }

    public static ApiException serviceUnavailable(String message) {
        return new ApiException(Response.Status.SERVICE_UNAVAILABLE, message);
    }

    public static ApiException gatewayTimeout(String message) {
        return new ApiException(ExtraStatus.GATEWAY_TIMEOUT, message);
    }
}
