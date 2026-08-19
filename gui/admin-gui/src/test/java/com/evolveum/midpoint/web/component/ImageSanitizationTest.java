/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import static com.evolveum.midpoint.web.component.FileTestConstants.XML_START_ARRAY;
import static com.evolveum.midpoint.web.component.FileTestConstants.jpegBytes;
import static com.evolveum.midpoint.web.component.FileTestConstants.jpegBytesWithFakeExif;
import static com.evolveum.midpoint.web.component.FileTestConstants.pngBytes;
import static com.evolveum.midpoint.web.component.FileTestConstants.pngBytesWithTransparency;
import static com.evolveum.midpoint.web.component.input.ImageSanitizationUtil.getFileExtensionFromFileMagicNumber;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import com.google.common.primitives.Bytes;
import org.testng.annotations.Test;

import com.evolveum.midpoint.web.component.input.ImageSanitizationException;
import com.evolveum.midpoint.web.component.input.ImageSanitizationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageFormatType;

/**
 * Tests optional image processing used for uploaded binary values.
 */
public class ImageSanitizationTest {

    @Test
    public void testRecognizesJpegMagicNumber() throws Exception {
        assertEquals(getFileExtensionFromFileMagicNumber(jpegBytes()), "jpg");
    }

    @Test
    public void testRecognizesPngMagicNumber() throws Exception {
        assertEquals(getFileExtensionFromFileMagicNumber(pngBytes()), "png");
    }

    @Test
    public void testUnrecognizedMagicNumberReturnsNull() {
        assertNull(getFileExtensionFromFileMagicNumber(XML_START_ARRAY));
    }

    @Test
    public void testNoConversionAndNoMetadataStrippingReturnsOriginalBytes() throws Exception {
        byte[] original = jpegBytes();

        byte[] sanitized = ImageSanitizationUtil.sanitizeImage(original, null, false);

        assertSame(sanitized, original);
    }

    @Test
    public void testJpegToPngConversion() throws Exception {
        byte[] sanitized = ImageSanitizationUtil.sanitizeImage(jpegBytes(), ImageFormatType.PNG, false);

        assertEquals(getFileExtensionFromFileMagicNumber(sanitized), "png");
    }

    @Test
    public void testPngToJpegConversionHandlesTransparency() throws Exception {
        byte[] sanitized = ImageSanitizationUtil.sanitizeImage(
                pngBytesWithTransparency(), ImageFormatType.JPG, false);

        assertEquals(getFileExtensionFromFileMagicNumber(sanitized), "jpg");
    }

    @Test
    public void testMetadataStrippingWithoutConversionRewritesOriginalFormat() throws Exception {
        byte[] original = jpegBytesWithFakeExif();

        byte[] sanitized = ImageSanitizationUtil.sanitizeImage(original, null, true);

        assertTrue(containsAscii(original));
        assertFalse(containsAscii(sanitized));
        assertNotSame(sanitized, original);
        assertEquals(getFileExtensionFromFileMagicNumber(sanitized), "jpg");
    }

    @Test
    public void testConversionAlsoRewritesImageAndDropsOriginalMetadata() throws Exception {
        byte[] original = jpegBytesWithFakeExif();

        byte[] sanitized = ImageSanitizationUtil.sanitizeImage(original, ImageFormatType.PNG, true);

        assertTrue(containsAscii(original));
        assertFalse(containsAscii(sanitized));
        assertNotSame(sanitized, original);
        assertEquals(getFileExtensionFromFileMagicNumber(sanitized), "png");
    }

    @Test
    public void testNullInputReturnsNull() throws Exception {
        assertNull(ImageSanitizationUtil.sanitizeImage(null, ImageFormatType.PNG, true));
    }

    @Test
    public void testInvalidImageInputIsRejectedWhenProcessingRequested() {
        assertThrows(ImageSanitizationException.class,
                () -> ImageSanitizationUtil.sanitizeImage(XML_START_ARRAY, ImageFormatType.PNG, false));
    }

    @Test
    public void testUnrecognizedFormatIsRejectedWhenMetadataStrippingPreservesOriginalFormat() {
        assertThrows(ImageSanitizationException.class,
                () -> ImageSanitizationUtil.sanitizeImage(XML_START_ARRAY, null, true));
    }

    private static boolean containsAscii(byte[] bytes) {
        return Bytes.indexOf(
                bytes,
                "Exif".getBytes(StandardCharsets.US_ASCII)) >= 0;
    }
}
