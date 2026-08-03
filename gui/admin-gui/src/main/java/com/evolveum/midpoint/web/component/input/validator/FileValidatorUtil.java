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
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.web.component.input.validator.FileMagicNumberConstants.CONTENT_TYPES_TO_MAGIC_NUMBERS;

/**
 * Contains methods for file validation.
 * E.g. compares file contentType with list of allowed ones or checks if given file starts with magic number of expected contentType.
 *
 * @author matisovaa
 *
 */
public final class FileValidatorUtil {

    /**
     * Converts list of String mime type names (e.g. "image/jpeg") to list of MimeType objects.
     *
     * @param stringMimeTypes list of String mime type names (e.g. "image/jpeg")
     * @return list of MimeType objects creates from input list of String mime type names (e.g. "image/jpeg")
     */
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

    /**
     * Validates if given content type name is in the list of allowed MimeTypes.
     *
     * @param contentType to check if it is allowed
     * @param allowedTypes the list of allowed MimeTypes
     * @return true if given contentType is in the list of allowed MimeTypes, false otherwise
     * @throws MimeTypeParseException if it is not possible to convert given contentType to MimeType
     */
    public static boolean isValidContentType(final String contentType, final List<MimeType> allowedTypes) throws MimeTypeParseException {
        final MimeType fileMime = new MimeType(contentType);

        for (MimeType allowed : allowedTypes) {
            if (allowed.match(fileMime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates if given inputStream begins with magic number of given contentType.
     *
     * @param contentType expected contentType of data in given inputStream
     * @param inputStream stream of data to check contentType based on its magic number
     * @return true if given inputStream begins with magic number of given contentType, false otherwise
     * @throws IOException if there is problem to read bytes from given inputStream
     */
    public static boolean isValidMagicNumber(final String contentType, final InputStream inputStream) throws IOException {
        final String magicNumberForContentType = CONTENT_TYPES_TO_MAGIC_NUMBERS.get(contentType);
        final String magicNumberOfFile = HexFormat.of().formatHex(inputStream.readNBytes(magicNumberForContentType.length() / 2));
        return Objects.equals(magicNumberForContentType, magicNumberOfFile);
    }
}
