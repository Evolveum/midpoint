/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_JPEG;
import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_PNG;
import static com.evolveum.midpoint.web.component.FileTestConstants.XML_START_ARRAY;
import static com.evolveum.midpoint.web.component.FileTestConstants.jpegBytes;
import static com.evolveum.midpoint.web.component.FileTestConstants.pngBytes;
import static org.testng.Assert.assertThrows;

import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.web.component.input.ImageSanitizationException;
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
        assertRejected(() ->
                FileValidatorUtil.validateUploadContent(jpegBytes(), MIME_IMAGE_PNG, List.of(MIME_IMAGE_JPEG)));
    }

    @Test
    public void testDetectedTypeIsNotAllowed() {
        assertRejected(() ->
                FileValidatorUtil.validateUploadContent(pngBytes(), MIME_IMAGE_PNG, List.of(MIME_IMAGE_JPEG)));
    }

    @Test
    public void testWildcardImageAllowsJpegAndPng() throws Exception {
        FileValidatorUtil.validateUploadContent(jpegBytes(), MIME_IMAGE_JPEG, List.of("image/*"));
        FileValidatorUtil.validateUploadContent(pngBytes(), MIME_IMAGE_PNG, List.of("image/*"));
    }

    @Test
    public void testMalformedDeclaredMimeTypeIsRejected() {
        assertRejected(() ->
                FileValidatorUtil.validateUploadContent(jpegBytes(), "image", List.of(MIME_IMAGE_JPEG)));
    }

    @Test
    public void testMalformedConfiguredAllowedMimeTypeIsRejected() {
        assertRejected(() ->
                FileValidatorUtil.validateUploadContent(jpegBytes(), MIME_IMAGE_JPEG, List.of("image")));
    }

    @Test
    public void testUnsupportedBytesAreRejected() {
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
        assertRejected(() ->
                FileValidatorUtil.validateUploadContent(
                        jpegBytes(),
                        MIME_IMAGE_JPEG,
                        List.of()));
    }

    private void assertRejected(ThrowingRunnable runnable) {
        assertThrows(ImageSanitizationException.class, runnable::run);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
