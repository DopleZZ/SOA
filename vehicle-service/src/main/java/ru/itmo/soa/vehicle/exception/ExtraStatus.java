package ru.itmo.soa.vehicle.exception;

import jakarta.ws.rs.core.Response;

public enum ExtraStatus implements Response.StatusType {

    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity");

    private final int code;
    private final String reason;

    ExtraStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    @Override
    public int getStatusCode() {
        return code;
    }

    @Override
    public Response.Status.Family getFamily() {
        return Response.Status.Family.familyOf(code);
    }

    @Override
    public String getReasonPhrase() {
        return reason;
    }
}
