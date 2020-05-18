package com.evolveum.midpoint.rest.impl;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@ControllerAdvice
public class RestExceptionHandler {

    private static final Trace LOGGER = TraceManager.getTrace(RestExceptionHandler.class);

    /*
     * TODO only catch all at the moment, later I'l diversify it to various
     * Seems to be working for exceptions, but not for 404.
     * 404 worked with:
     * spring.mvc.throw-exception-if-no-handler-found=true
     * spring.resources.add-mappings=false
     * But this broke images (and perhaps other static content) serving for Wicket app.
     */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> defaultHandler(
            Exception ex, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.warn("Unexpected exception causing HTTP 500", ex);
        return errorResponse(INTERNAL_SERVER_ERROR, request, ex.getMessage());
    }

    private ResponseEntity<?> errorResponse(
            HttpStatus status, HttpServletRequest request, String message) {
        LOGGER.debug("HTTP error status {} with message: {}", status.value(), message);
        return ResponseEntity.status(status)
                .body(createErrorDto(request, status, message));
    }

    public static Map<String, Object> createErrorDto(
            HttpServletRequest request, HttpStatus status, String message) {
        Map<String, Object> errorDto = new LinkedHashMap<>();
        errorDto.put("error", status.getReasonPhrase());
        errorDto.put("message", message);
        errorDto.put("status", status.value());
        errorDto.put("path", request.getRequestURI());
        return errorDto;
    }
}
