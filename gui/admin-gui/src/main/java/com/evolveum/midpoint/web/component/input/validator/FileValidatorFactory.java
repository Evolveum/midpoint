/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input.validator;

import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_JPEG;
import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_PNG;

/**
 * @author matisovaa
 *
 */
public class FileValidatorFactory {
    public static final List<String> ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES = Arrays.asList(MIME_IMAGE_JPEG, MIME_IMAGE_PNG);

    public static List<MimeType> getMimeTypes(final List<String> stringMimeTypes) {
        return stringMimeTypes.stream()
                .map(s -> {
                    try {
                        return new MimeType(s);
                    } catch (MimeTypeParseException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
