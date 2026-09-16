/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input.validator;

import com.evolveum.midpoint.web.component.input.ImageSanitizationException;

/**
 * Content-validation failure with a structured reason for UI message selection.
 */
public class FileUploadContentValidationException extends ImageSanitizationException {

    public enum Reason {
        NOT_ALLOWED,
        CONTENT_TYPE_MISMATCH,
        UNRECOGNIZED_CONTENT,
        MALFORMED_MIME_TYPE
    }

    private final Reason reason;

    public FileUploadContentValidationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public FileUploadContentValidationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
