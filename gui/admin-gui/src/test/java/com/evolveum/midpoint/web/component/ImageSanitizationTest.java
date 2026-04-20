/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.web.component.input.ImageSanitizationException;
import com.evolveum.midpoint.web.component.input.ImageSanitizationUtil;

import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import java.util.Objects;

import static com.evolveum.midpoint.web.component.FileTestConstants.JPG_ARRAY;
import static com.evolveum.midpoint.web.component.FileTestConstants.JPG_METADATA_ARRAY;

import static org.junit.Assert.*;
import static org.testng.Assert.assertSame;

/**
 * @author matisovaa
 *
 */
@ActiveProfiles("test")
public class ImageSanitizationTest {

    @Test
    public void test4299_2ImageSanitization_removeEXIF_notNeeded() throws Exception {
        // asserts if it returns the same reference, so no processing was performed at all
        assertSame(ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, false), JPG_METADATA_ARRAY);
    }

    @Test
    public void test4299_2ImageSanitization_removeEXIF_exception() throws Exception {
        ImageSanitizationException exception = assertThrows(ImageSanitizationException.class, () -> {
            ImageSanitizationUtil.sanitizeImage(JPG_ARRAY, true);
        });
        assertEquals("Failed to read image for removal of exif data.", exception.getMessage());
    }

    @Test
    public void test4299_2ImageSanitization_removeEXIF() throws Exception {
        final byte[] jpgWithoutMetadata = ImageSanitizationUtil.sanitizeImage(JPG_METADATA_ARRAY, true);
        assertNotSame(JPG_METADATA_ARRAY, jpgWithoutMetadata);
        assertTrue(JPG_METADATA_ARRAY.length > Objects.requireNonNull(jpgWithoutMetadata).length);
    }
}
