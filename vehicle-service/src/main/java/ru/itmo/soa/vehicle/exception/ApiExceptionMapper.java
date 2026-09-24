package ru.itmo.soa.vehicle.exception;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.UnmarshalException;
import ru.itmo.soa.vehicle.model.Error;

import java.time.Instant;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        int status;
        String reason;
        String message;

        Throwable invalidValue = bodyConversionError(exception);
        if (invalidValue instanceof NumberFormatException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            reason = Response.Status.BAD_REQUEST.getReasonPhrase();
            message = "Нечисловое значение в теле запроса: " + invalidValue.getMessage();
        } else if (invalidValue != null) {
            status = ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode();
            reason = ExtraStatus.UNPROCESSABLE_ENTITY.getReasonPhrase();
            message = invalidValue.getMessage();
        } else if (exception instanceof WebApplicationException wae) {
            Response response = wae.getResponse();
            status = response.getStatus();
            reason = response.getStatusInfo().getReasonPhrase();
            message = exception.getMessage() != null ? exception.getMessage() : reason;
        } else if (exception instanceof UnmarshalException || exception instanceof ProcessingException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            reason = Response.Status.BAD_REQUEST.getReasonPhrase();
            message = "Невалидный XML в теле запроса";
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            reason = Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
            message = exception.getMessage() != null ? exception.getMessage() : "Internal server error";
        }

        String rawPath = uriInfo != null ? uriInfo.getPath() : "";
        String path = rawPath.startsWith("/") ? rawPath : "/" + rawPath;
        Error error = new Error()
                .timestamp(Instant.now().toString())
                .status(status)
                .error(reason)
                .message(message)
                .path(path);

        return Response.status(status)
                .type(MediaType.APPLICATION_XML)
                .entity(error)
                .build();
    }

    private static Throwable bodyConversionError(Throwable exception) {
        boolean inUnmarshal = false;
        Throwable current = exception;
        for (int depth = 0; current != null && depth < 20; depth++, current = current.getCause()) {
            if (current instanceof UnmarshalException) {
                inUnmarshal = true;
            } else if (inUnmarshal && current instanceof IllegalArgumentException) {
                return current;
            }
        }
        return null;
    }
}
