/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_APPLICATION_PDF;
import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_JPEG;
import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_PNG;
import static com.evolveum.midpoint.web.component.FileTestConstants.UNKNOWN_BINARY;
import static com.evolveum.midpoint.web.component.FileTestConstants.XML_START_ARRAY;
import static com.evolveum.midpoint.web.component.FileTestConstants.jpegBytes;
import static com.evolveum.midpoint.web.component.FileTestConstants.minimalPdfBytes;
import static com.evolveum.midpoint.web.component.FileTestConstants.pngBytes;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.web.component.input.validator.FileUploadContentValidationException;
import com.evolveum.midpoint.web.component.input.validator.FileUploadContentValidationException.Reason;
import com.evolveum.midpoint.web.component.input.validator.FileValidatorUtil;

/**
 * Tests upload content validation based on magic bytes, declared MIME type,
 * and configured allowed MIME types.
 */
public class FileValidatorTest {

    @Test
    public void testValidJpegDeclaredAsJpeg() throws Exception {
        FileValidatorUtil.validateUploadContent(jpegBytes(), MIME_IMAGE_JPEG, List.of(MIME_IMAGE_JPEG));
    }

    @Test
    public void testValidPngDeclaredAsPng() throws Exception {
        FileValidatorUtil.validateUploadContent(pngBytes(), MIME_IMAGE_PNG, List.of(MIME_IMAGE_PNG));
    }

    @Test
    public void testDeclaredMimeTypeDoesNotMatchDetectedMagicBytes() {
        assertRejected(Reason.CONTENT_TYPE_MISMATCH, () ->
                FileValidatorUtil.validateUploadContent(jpegBytes(), MIME_IMAGE_PNG, List.of(MIME_IMAGE_JPEG)));
    }

    @Test
    public void testDetectedTypeIsNotAllowed() {
        assertRejected(Reason.NOT_ALLOWED, () ->
                FileValidatorUtil.validateUploadContent(pngBytes(), MIME_IMAGE_PNG, List.of(MIME_IMAGE_JPEG)));
    }

    @Test
    public void testValidPdfDeclaredAsPdf() throws Exception {
        FileValidatorUtil.validateUploadContent(minimalPdfBytes(), MIME_APPLICATION_PDF, List.of(MIME_APPLICATION_PDF));
    }

    @Test
    public void testPdfRejectedByImageWildcardAllowedType() {
        assertRejected(Reason.NOT_ALLOWED, () ->
                FileValidatorUtil.validateUploadContent(minimalPdfBytes(), MIME_APPLICATION_PDF, List.of("image/*")));
    }

    @Test
    public void testPdfDeclaredAsJpegIsRejectedAsMismatch() {
        assertRejected(Reason.CONTENT_TYPE_MISMATCH, () ->
                FileValidatorUtil.validateUploadContent(minimalPdfBytes(), MIME_IMAGE_JPEG, List.of(MIME_APPLICATION_PDF)));
    }

    @Test
    public void testWildcardImageAllowsJpegAndPng() throws Exception {
        FileValidatorUtil.validateUploadContent(jpegBytes(), MIME_IMAGE_JPEG, List.of("image/*"));
        FileValidatorUtil.validateUploadContent(pngBytes(), MIME_IMAGE_PNG, List.of("image/*"));
    }

    @Test
    public void testMalformedDeclaredMimeTypeIsRejected() {
        assertRejected(Reason.MALFORMED_MIME_TYPE, () ->
                FileValidatorUtil.validateUploadContent(jpegBytes(), "image", List.of(MIME_IMAGE_JPEG)));
    }

    @Test
    public void testMalformedConfiguredAllowedMimeTypeIsRejected() {
        assertRejected(Reason.MALFORMED_MIME_TYPE, () ->
                FileValidatorUtil.validateUploadContent(jpegBytes(), MIME_IMAGE_JPEG, List.of("image")));
    }

    @Test
    public void testXmlDeclaredContentDoesNotMatchDetectedType() {
        assertRejected(() ->
                FileValidatorUtil.validateUploadContent(XML_START_ARRAY, "text/xml", List.of("text/xml")));
    }

    @Test
    public void testNullDeclaredMimeTypeIsAcceptedWhenDetectedTypeIsAllowed() throws Exception {
        FileValidatorUtil.validateUploadContent(jpegBytes(), null, List.of(MIME_IMAGE_JPEG));
    }

    @Test
    public void testBlankDeclaredMimeTypeIsAcceptedWhenDetectedTypeIsAllowed() throws Exception {
        FileValidatorUtil.validateUploadContent(jpegBytes(), " ", List.of(MIME_IMAGE_JPEG));
    }

    @Test
    public void testDeclaredMimeTypeParametersAreIgnored() throws Exception {
        FileValidatorUtil.validateUploadContent(jpegBytes(), "IMAGE/JPEG; charset=binary", List.of(MIME_IMAGE_JPEG));
    }

    @Test
    public void testEmptyAllowedTypesRejectsDetectedContent() {
        assertRejected(Reason.NOT_ALLOWED, () ->
                FileValidatorUtil.validateUploadContent(
                        jpegBytes(),
                        MIME_IMAGE_JPEG,
                        List.of()));
    }

    @Test
    public void testUnknownBinaryContentIsRejectedAsUnrecognized() {
        assertRejected(Reason.UNRECOGNIZED_CONTENT, () ->
                FileValidatorUtil.validateUploadContent(
                        UNKNOWN_BINARY,
                        MIME_APPLICATION_PDF,
                        List.of(MIME_APPLICATION_PDF)));
    }

    private void assertRejected(ThrowingRunnable runnable) {
        expectThrows(FileUploadContentValidationException.class, runnable::run);
    }

    private void assertRejected(Reason expectedReason, ThrowingRunnable runnable) {
        FileUploadContentValidationException ex =
                expectThrows(FileUploadContentValidationException.class, runnable::run);
        assertEquals(ex.getReason(), expectedReason);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
