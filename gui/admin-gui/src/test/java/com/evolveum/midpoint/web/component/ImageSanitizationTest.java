/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.web.component.input.ImageSanitizationException;
import com.evolveum.midpoint.web.component.input.ImageSanitizationUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageUploadProcessingType;

import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import java.util.Objects;

import static com.evolveum.midpoint.common.MimeTypeUtil.*;
import static com.evolveum.midpoint.web.component.FileTestConstants.*;

import static com.evolveum.midpoint.web.component.input.ImageSanitizationUtil.getContentTypeFromFileMagicNumber;

import static org.junit.Assert.*;
import static org.testng.Assert.assertSame;

/**
 * @author matisovaa
 *
 */
@ActiveProfiles("test")
public class ImageSanitizationTest {

    @Test
    public void test4299_10ImageSanitization_noConfig() throws Exception {
        // asserts if it returns the same reference, so no processing was performed at all
        assertSame(ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, null), JPG_METADATA_ARRAY);
    }

    @Test
    public void test4299_20ImageSanitization_emptyConfig() throws Exception {
        // asserts if it returns the same reference, so no processing was performed at all
        assertSame(ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, new ImageUploadProcessingType()), JPG_METADATA_ARRAY);
    }

    @Test
    public void test4299_30ImageSanitization_onlyFormatInConfig() throws Exception {
        // asserts if it returns the same reference, so no processing was performed at all
        assertSame(ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingOnlyFormat()), JPG_METADATA_ARRAY);
    }

    @Test
    public void test4299_40ImageSanitization_preserveFormat_preserveExif() throws Exception {
        // asserts if it returns the same reference, so no processing was performed at all
        assertSame(ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingPreserve()), JPG_METADATA_ARRAY);
    }

    @Test
    public void test4299_50ImageSanitization_fixedFormatJPG_removeEXIF() throws Exception {
        final byte[] jpgWithoutMetadata = ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingFixedDefault());
        assertNotSame(JPG_METADATA_ARRAY, jpgWithoutMetadata);
        assertTrue(JPG_METADATA_ARRAY.length > Objects.requireNonNull(jpgWithoutMetadata).length);
        assertEquals(getContentTypeFromFileMagicNumber(jpgWithoutMetadata), getExtensionRaw(MIME_IMAGE_JPEG));
    }

    @Test
    public void test4299_60ImageSanitization_fixedFormatPNG_removeEXIF() throws Exception {
        final byte[] pngWithoutMetadata = ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingFixedPng());
        assertNotSame(JPG_METADATA_ARRAY, pngWithoutMetadata);
        assertTrue(JPG_METADATA_ARRAY.length > Objects.requireNonNull(pngWithoutMetadata).length);
        assertEquals(getContentTypeFromFileMagicNumber(pngWithoutMetadata), getExtensionRaw(MIME_IMAGE_PNG));
    }

    @Test
    public void test4299_70ImageSanitization_fixedFormatPNG_preserveEXIFIgnored() throws Exception {
        final byte[] pngWithoutMetadata = ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingFixedPngStripExifFalse());
        assertNotSame(JPG_METADATA_ARRAY, pngWithoutMetadata);
        assertTrue(JPG_METADATA_ARRAY.length > Objects.requireNonNull(pngWithoutMetadata).length);
        assertEquals(getContentTypeFromFileMagicNumber(pngWithoutMetadata), getExtensionRaw(MIME_IMAGE_PNG));
    }

    @Test
    public void test4299_80ImageSanitization_removeEXIF() throws Exception {
        final byte[] jpgWithoutMetadata = ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingStripExifTrue());
        assertNotSame(JPG_METADATA_ARRAY, jpgWithoutMetadata);
        assertTrue(JPG_METADATA_ARRAY.length > Objects.requireNonNull(jpgWithoutMetadata).length);
        assertEquals(getContentTypeFromFileMagicNumber(jpgWithoutMetadata), getExtensionRaw(MIME_IMAGE_JPEG));
    }

    @Test
    public void test4299_90ImageSanitization_removeEXIF_exception() throws Exception {
        ImageSanitizationException exception = assertThrows(ImageSanitizationException.class, () -> {
            ImageSanitizationUtil.sanitizeImage(JPG_ARRAY, getImageUploadProcessingStripExifTrue());
        });
        assertEquals("Failed to read image for sanitization.", exception.getMessage());
    }
}
