/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import static com.evolveum.midpoint.common.MimeTypeUtil.*;
import static com.evolveum.midpoint.web.component.FileTestConstants.*;

import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.web.component.input.validator.FileValidatorUtil;

/**
 * Tests of methods for file validation.
 * E.g. if contentType is from allowed list or if inputStream begins with magic number of expected contentType.
 *
 * @author matisovaa
 */
@ActiveProfiles("test")
public class FileValidatorTest {

    @Test
    public void test4299ContentTypeFileValidator_validJPEG() throws Exception {
        assertTrue(FileValidatorUtil.isValidContentType(MIME_IMAGE_JPEG, MIME_TYPE_LIST));
    }

    @Test
    public void test4299ContentTypeFileValidator_validPNG() throws Exception {
        assertTrue(FileValidatorUtil.isValidContentType(MIME_IMAGE_PNG, MIME_TYPE_LIST));
    }

    @Test
    public void test4299ContentTypeFileValidator_invalid() throws Exception {
        assertFalse(FileValidatorUtil.isValidContentType(MIME_APPLICATION_XML, MIME_TYPE_LIST));
    }

    @Test
    public void test4299MagicNumberFileValidator_validJPEG() throws Exception {
        assertTrue(FileValidatorUtil.isValidMagicNumber(MIME_IMAGE_JPEG, JPG_STREAM1));
    }

    @Test
    public void test4299MagicNumberFileValidator_validPNG() throws Exception {
        assertTrue(FileValidatorUtil.isValidMagicNumber(MIME_IMAGE_PNG, PNG_STREAM));
    }

    @Test
    public void test4299MagicNumberFileValidator_invalidXML() throws Exception {
        assertFalse(FileValidatorUtil.isValidMagicNumber(MIME_IMAGE_JPEG, XML_STREAM));
    }

    @Test
    public void test4299MagicNumberFileValidator_invalidJPNG() throws Exception {
        assertFalse(FileValidatorUtil.isValidMagicNumber(MIME_IMAGE_PNG, JPG_STREAM2));
    }
}
