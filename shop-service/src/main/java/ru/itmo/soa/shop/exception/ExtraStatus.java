package ru.itmo.soa.shop.exception;

public enum ExtraStatus {

    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
    BAD_GATEWAY(502, "Bad Gateway"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout");

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
