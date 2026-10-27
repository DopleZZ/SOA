package ru.itmo.soa.vehicle.exception;

public enum ExtraStatus {

    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity");

    private final int statusCode;
    private final String reasonPhrase;

    ExtraStatus(int statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }
}
