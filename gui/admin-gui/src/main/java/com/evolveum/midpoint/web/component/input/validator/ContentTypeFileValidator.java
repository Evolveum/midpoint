/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input.validator;

import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;

import java.util.List;

/**
 * @author matisovaa
 *
 */
public class ContentTypeFileValidator {
    private final List<MimeType> allowedTypes;

    public ContentTypeFileValidator(final List<MimeType> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    public boolean isValid(final String contentType) throws MimeTypeParseException {
        final MimeType fileMime = new MimeType(contentType);

        for (MimeType allowed : allowedTypes) {
            if (allowed.match(fileMime)) {
                return true;
            }
        }

        return false;
    }
}
