/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import java.nio.file.AccessDeniedException;
import javax.naming.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.server.ResponseStatusException;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@ControllerAdvice
public class RestExceptionHandler {

    private static final Trace LOGGER = TraceManager.getTrace(RestExceptionHandler.class);

    @ExceptionHandler({
            // no midPoint specific exceptions here
            IllegalArgumentException.class,

            // exceptions from Spring's ResponseEntityExceptionHandler
            ServletRequestBindingException.class,
            TypeMismatchException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<?> badRequestHandler(
            Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, request, ex);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<?> badRequestHandlerShowingMostSpecificCause(
            Exception ex, HttpServletRequest request) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
        /* useful for generic JSON parsing, but not needed with our custom JSON parser/formatter
        if (cause instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException upex = (UnrecognizedPropertyException) cause;
            message = "Invalid property [" + upex.getPropertyName()
                    + "], known properties: " + upex.getKnownPropertyIds();
        }
        */
        return errorResponse(HttpStatus.BAD_REQUEST, request, cause);
    }

    // Normally not used, auth filter before the controller will not let it here.
    // Left here as a fallback if something in the chain changes.
    @ExceptionHandler({ AuthenticationException.class })
    public ResponseEntity<?> unauthorizedHandler(Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.UNAUTHORIZED, request, ex);
    }

    // Not used currently, left here as a fallback if something in the chain changes.
    @ExceptionHandler({ AccessDeniedException.class })
    public ResponseEntity<?> forbiddenHandler(Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.FORBIDDEN, request, ex);
    }

    @ExceptionHandler({ HttpRequestMethodNotSupportedException.class })
    public ResponseEntity<?> methodNotAllowedHandler(
            Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.METHOD_NOT_ALLOWED, request, ex);
    }

    @ExceptionHandler({ HttpMediaTypeNotAcceptableException.class })
    public ResponseEntity<?> notAcceptableHandler(
            Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_ACCEPTABLE, request, ex);
    }

    @ExceptionHandler({ HttpMediaTypeNotSupportedException.class })
    public ResponseEntity<?> unsupportedMediaTypeHandler(
            Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, request, ex);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<?> serviceUnavailableHandler(
            Exception ex, HttpServletRequest request) {
        return errorResponse(SERVICE_UNAVAILABLE, request, ex);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {
        return errorResponse(ex.getStatus(), request, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> defaultHandler(
            Exception ex, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.warn("Unexpected exception causing HTTP 500", ex);
        return errorResponse(INTERNAL_SERVER_ERROR, request, ex);
    }

    private ResponseEntity<?> errorResponse(
            HttpStatus status, HttpServletRequest request, Throwable ex) {
        String message = ex.getMessage();
        LOGGER.debug("HTTP error status {} with message: {}", status.value(), message);

        OperationResult result = new OperationResult(request.getRequestURI())
                .addParam("status", status.value())
                .addParam("error", status.getReasonPhrase());
        result.recordFatalError(ex);

        return ResponseEntity.status(status).body(result.createOperationResultType());
    }
}
