package ru.itmo.soa.vehicle.exception;

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

    public static ApiException gone(String message) {
        return new ApiException(Response.Status.GONE, message);
    }

    public static ApiException unsupportedMediaType(String message) {
        return new ApiException(Response.Status.UNSUPPORTED_MEDIA_TYPE, message);
    }
}
