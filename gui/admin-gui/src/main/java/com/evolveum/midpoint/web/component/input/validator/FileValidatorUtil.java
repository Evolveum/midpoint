/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input.validator;

import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_JPEG;
import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_PNG;

/**
 * @author matisovaa
 *
 */
public final class FileValidatorUtil {
    public static final List<String> ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES = Arrays.asList(MIME_IMAGE_JPEG, MIME_IMAGE_PNG);
    public static final Map<String, String> CONTENT_TYPES_TO_MAGIC_NUMBERS = Map.of(
            MIME_IMAGE_JPEG, "ffd8ff",
            MIME_IMAGE_PNG, "89504e470d0a1a0a"
    );

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

    public static boolean isValidContentType(final String contentType, final List<MimeType> allowedTypes) throws MimeTypeParseException {
        final MimeType fileMime = new MimeType(contentType);

        for (MimeType allowed : allowedTypes) {
            if (allowed.match(fileMime)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidMagicNumber(final String contentType, final InputStream inputStream) throws IOException {
        final String magicNumberForContentType = FileValidatorUtil.CONTENT_TYPES_TO_MAGIC_NUMBERS.get(contentType);
        final String magicNumberOfFile = HexFormat.of().formatHex(inputStream.readNBytes(magicNumberForContentType.length() / 2));
        return Objects.equals(magicNumberForContentType, magicNumberOfFile);
    }
}
