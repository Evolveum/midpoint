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

import static com.evolveum.midpoint.web.component.input.ImageSanitizationUtil.getFileExtensionFromFileMagicNumber;

import static org.testng.Assert.*;
import static org.junit.Assert.assertThrows;

/**
 * Tests of methods for file sanitization based on ImageUploadProcessingType configuration.
 * E.g. if sanitization removes EXIF data or converts image to expected output format.
 *
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
    public void test4299_40ImageSanitization_preserveFormat_preserveEXIF() throws Exception {
        // asserts if it returns the same reference, so no processing was performed at all
        assertSame(ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingPreserve()), JPG_METADATA_ARRAY);
    }

    @Test
    public void test4299_50ImageSanitization_fixedFormatJPG_removeEXIF() throws Exception {
        final byte[] jpgWithoutMetadata = ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingFixedDefault());
        assertNotSame(jpgWithoutMetadata, JPG_METADATA_ARRAY);
        assertTrue(JPG_METADATA_ARRAY.length > Objects.requireNonNull(jpgWithoutMetadata).length);
        assertEquals(getFileExtensionFromFileMagicNumber(jpgWithoutMetadata), getExtension(MIME_IMAGE_JPEG));
    }

    @Test
    public void test4299_55ImageSanitization_fixedFormatJPG_PNGInput() throws Exception {
        final byte[] jpgWithoutMetadata = ImageSanitizationUtil.sanitizeImage(PNG_ARRAY, getImageUploadProcessingFixedDefault());
        assertNotSame(jpgWithoutMetadata, PNG_ARRAY);
        assertEquals(getFileExtensionFromFileMagicNumber(jpgWithoutMetadata), getExtension(MIME_IMAGE_JPEG));
    }

    @Test
    public void test4299_60ImageSanitization_fixedFormatPNG_removeEXIF() throws Exception {
        final byte[] pngWithoutMetadata = ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingFixedPng());
        assertNotSame(pngWithoutMetadata, JPG_METADATA_ARRAY);
        assertEquals(getFileExtensionFromFileMagicNumber(pngWithoutMetadata), getExtension(MIME_IMAGE_PNG));
    }

    @Test
    public void test4299_65ImageSanitization_fixedFormatPNG_PNGInput() throws Exception {
        final byte[] pngWithoutMetadata = ImageSanitizationUtil.sanitizeImage(PNG_ARRAY, getImageUploadProcessingFixedPng());
        assertNotSame(pngWithoutMetadata, PNG_ARRAY);
        assertTrue(JPG_METADATA_ARRAY.length > Objects.requireNonNull(pngWithoutMetadata).length);
        assertEquals(getFileExtensionFromFileMagicNumber(pngWithoutMetadata), getExtension(MIME_IMAGE_PNG));
    }

    @Test
    public void test4299_70ImageSanitization_fixedFormatPNG_preserveEXIFIgnored() throws Exception {
        final byte[] pngWithoutMetadata = ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingFixedPngStripExifFalse());
        assertNotSame(pngWithoutMetadata, JPG_METADATA_ARRAY);
        assertTrue(JPG_METADATA_ARRAY.length > Objects.requireNonNull(pngWithoutMetadata).length);
        assertEquals(getFileExtensionFromFileMagicNumber(pngWithoutMetadata), getExtension(MIME_IMAGE_PNG));
    }

    @Test
    public void test4299_80ImageSanitization_removeEXIF() throws Exception {
        final byte[] jpgWithoutMetadata = ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, getImageUploadProcessingStripExifTrue());
        assertNotSame(jpgWithoutMetadata, JPG_METADATA_ARRAY);
        assertTrue(JPG_METADATA_ARRAY.length > Objects.requireNonNull(jpgWithoutMetadata).length);
        assertEquals(getFileExtensionFromFileMagicNumber(jpgWithoutMetadata), getExtension(MIME_IMAGE_JPEG));
    }

    @Test
    public void test4299_90ImageSanitization_removeEXIF_exception() throws Exception {
        ImageSanitizationException exception = assertThrows(ImageSanitizationException.class, () -> {
            ImageSanitizationUtil.sanitizeImage(JPG_START_ARRAY, getImageUploadProcessingStripExifTrue());
        });
        assertEquals(exception.getMessage(), "Failed to read image for sanitization.");
    }

    @Test
    public void test4299_100ImageSanitization_validJPEG() throws Exception {
        assertEquals(ImageSanitizationUtil.getFileExtensionFromFileMagicNumber(JPG_START_ARRAY), getExtension(MIME_IMAGE_JPEG));
    }

    @Test
    public void test4299_110ImageSanitization_validPNG() throws Exception {
        assertEquals(ImageSanitizationUtil.getFileExtensionFromFileMagicNumber(PNG_ARRAY), getExtension(MIME_IMAGE_PNG));
    }

    @Test
    public void test4299_120ImageSanitization_invalidXML() throws Exception {
        assertNull(ImageSanitizationUtil.getFileExtensionFromFileMagicNumber(XML_START_ARRAY));
    }
}
