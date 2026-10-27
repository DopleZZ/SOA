package ru.itmo.soa.shop.exception;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.itmo.soa.shop.model.Error;
import ru.itmo.soa.shop.xml.ErrorXml;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.UnmarshalException;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> apiException(ApiException exception, HttpServletRequest request) {
        return errorResponse(exception.getStatus(), exception.getReason(), exception.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> unreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return errorResponse(400, "Bad Request", "Невалидный XML в теле запроса", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> notAcceptable(HttpMediaTypeNotAcceptableException exception, HttpServletRequest request) {
        return errorResponse(406, "Not Acceptable", exception.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> methodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return errorResponse(405, "Method Not Allowed", exception.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> unsupportedMedia(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return errorResponse(415, "Unsupported Media Type", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> typeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        Class<?> requiredType = exception.getRequiredType();
        if (requiredType != null && requiredType.isEnum()) {
            return errorResponse(422, "Unprocessable Entity",
                    "Значение " + exception.getName() + " вне допустимого перечня: " + exception.getValue(), request);
        }
        if (requiredType != null && (requiredType == double.class || Number.class.isAssignableFrom(requiredType))) {
            return errorResponse(400, "Bad Request",
                    "Нечисловое значение параметра " + exception.getName() + ": " + exception.getValue(), request);
        }
        return errorResponse(400, "Bad Request", exception.getMessage(), request);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<String> unexpected(Throwable exception, HttpServletRequest request) {
        String message = exception.getMessage() != null ? exception.getMessage() : "Internal server error";
        return errorResponse(500, "Internal Server Error", message, request);
    }

    private static ResponseEntity<String> errorResponse(int status, String reason, String message,
                                                        HttpServletRequest request) {
        String rawPath = request.getRequestURI();
        String path = rawPath != null ? rawPath : "/";
        Error error = new Error()
                .timestamp(Instant.now().toString())
                .status(status)
                .error(reason)
                .message(message)
                .path(path);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_XML)
                .body(ErrorXml.write(error));
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
