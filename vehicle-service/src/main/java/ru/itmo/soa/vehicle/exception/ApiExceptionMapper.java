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

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        int status;
        String reason;
        String message;

        if (exception instanceof WebApplicationException wae) {
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
        Error error = new Error(status, reason, message, path);

        return Response.status(status)
                .type(MediaType.APPLICATION_XML)
                .entity(error)
                .build();
    }
}
