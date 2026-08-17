/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input.validator;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;

import org.apache.tika.Tika;

import com.evolveum.midpoint.web.component.input.validator.FileUploadContentValidationException.Reason;

/**
 * Contains utility methods for validating uploaded file content.
 *
 * Validation compares the content type detected from the file bytes
 * with both the declared content type and the configured allowed MIME types.
 *
 * @author matisovaa
 */
public final class FileValidatorUtil {

    private static final Tika TIKA = new Tika();
    private static final String GENERIC_BINARY_CONTENT_TYPE = "application/octet-stream";

    /**
     * Validates uploaded file content against its declared content type and the
     * configured allowed MIME types.
     *
     * The actual content type is detected from the file bytes. Configured allowed
     * types support MIME wildcards, for example {@code image/*}.
     *
     * @param bytes uploaded file content
     * @param declaredContentType content type declared by the upload request;
     *                            may be {@code null} or blank
     * @param allowedContentTypes MIME types allowed by the effective upload policy
     * @throws FileUploadContentValidationException if the content type cannot be
     *         recognized, a MIME type is malformed, the detected type is not allowed,
     *         or the declared type does not match the detected type
     */
    public static void validateUploadContent(
            byte[] bytes,
            String declaredContentType,
            List<String> allowedContentTypes)
            throws FileUploadContentValidationException {

        String detectedContentType = detectContentType(bytes);
        if (detectedContentType == null) {
            throw new FileUploadContentValidationException(
                    Reason.UNRECOGNIZED_CONTENT,
                    "Uploaded content type is not recognized.");
        }

        if (!isAllowedContentType(detectedContentType, allowedContentTypes)) {
            throw new FileUploadContentValidationException(
                    Reason.NOT_ALLOWED,
                    "Uploaded content type " + detectedContentType + " is not allowed.");
        }

        String normalizedDeclaredContentType = normalizeMimeType(declaredContentType);
        if (normalizedDeclaredContentType != null
                && !Objects.equals(normalizedDeclaredContentType, detectedContentType)) {
            throw new FileUploadContentValidationException(
                    Reason.CONTENT_TYPE_MISMATCH,
                    "Declared content type " + normalizedDeclaredContentType
                            + " does not match uploaded content type "
                            + detectedContentType + ".");
        }
    }

    /**
     * Determines whether the detected content type matches one of the configured
     * allowed MIME types.
     *
     * @param detectedContentType content type detected from the uploaded data
     * @param allowedContentTypes configured allowed MIME types
     * @return {@code true} if one of the allowed types matches the detected type
     * @throws FileUploadContentValidationException if an allowed MIME type is malformed
     */
    private static boolean isAllowedContentType(
            String detectedContentType,
            List<String> allowedContentTypes)
            throws FileUploadContentValidationException {

        MimeType detectedMimeType = parseMimeType(detectedContentType);

        for (String allowedContentType : allowedContentTypes) {
            if (allowedContentType == null || allowedContentType.isBlank()) {
                continue;
            }

            MimeType allowedMimeType = parseMimeType(allowedContentType);
            if (allowedMimeType.match(detectedMimeType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normalizes a MIME type to its lowercase {@code type/subtype} form,
     * omitting any parameters.
     *
     * @param mimeType MIME type to normalize; may be {@code null} or blank
     * @return normalized MIME type, or {@code null} for a missing value
     * @throws FileUploadContentValidationException if the value is not a valid MIME type
     */
    private static String normalizeMimeType(String mimeType)
            throws FileUploadContentValidationException {
        if (mimeType == null || mimeType.isBlank()) {
            return null;
        }

        MimeType parsed = parseMimeType(mimeType);
        return parsed.getPrimaryType().toLowerCase(Locale.ROOT)
                + "/"
                + parsed.getSubType().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses a MIME type and converts parsing errors to the exception used by
     * the upload validation flow.
     *
     * @param mimeType MIME type to parse
     * @return parsed MIME type
     * @throws FileUploadContentValidationException if the MIME type is malformed
     */
    private static MimeType parseMimeType(String mimeType)
            throws FileUploadContentValidationException {
        try {
            return new MimeType(mimeType);
        } catch (MimeTypeParseException e) {
            throw new FileUploadContentValidationException(
                    Reason.MALFORMED_MIME_TYPE,
                    "Malformed MIME type: " + mimeType, e);
        }
    }

    /**
     * Detects the MIME type of uploaded content from its bytes.
     *
     * The method uses Apache Tika core detection without relying on a file name.
     *
     * @param bytes uploaded file content
     * @return detected MIME type, or {@code null} if the content type is not recognized
     */
    private static String detectContentType(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        String detectedContentType = TIKA.detect(bytes);
        return GENERIC_BINARY_CONTENT_TYPE.equals(detectedContentType) ? null : detectedContentType;
    }
}
