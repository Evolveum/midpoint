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

import static com.evolveum.midpoint.web.component.input.validator.FileMagicNumberConstants.CONTENT_TYPES_TO_MAGIC_NUMBERS;

/**
 * @author matisovaa
 *
 */
public final class FileValidatorUtil {

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
        final String magicNumberForContentType = CONTENT_TYPES_TO_MAGIC_NUMBERS.get(contentType);
        final String magicNumberOfFile = HexFormat.of().formatHex(inputStream.readNBytes(magicNumberForContentType.length() / 2));
        return Objects.equals(magicNumberForContentType, magicNumberOfFile);
    }
}
