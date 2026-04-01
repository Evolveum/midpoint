/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.web.component.input.validator.FileValidatorUtil;

import jakarta.activation.MimeType;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static com.evolveum.midpoint.common.MimeTypeUtil.*;

import static org.testng.Assert.*;

/**
 * @author matisovaa
 *
 */
@ActiveProfiles("test")
public class FileValidatorTest {

    private static final byte[] JPG_ARRAY = new byte[] { -1, -40, -1, -32, 0, 16, 74, 70, 73, 70, 0, 1, 1, 1, 0, 72, 0, 72, 0, 0, -1, -30, 12, 88, 73, 67, 67, 95, 80, 82, 79, 70, 73 };
    private static final InputStream JPG_STREAM1 = new ByteArrayInputStream(JPG_ARRAY);
    private static final InputStream JPG_STREAM2 = new ByteArrayInputStream(JPG_ARRAY);
    private static final InputStream PNG_STREAM = new ByteArrayInputStream(new byte[] { -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 7, 65, 0, 0, 1, 2, 8, 2, 0, 0, 0, 7, 21, 56, -25, 0, 0 });
    private static final InputStream XML_STREAM = new ByteArrayInputStream(new byte[] { 60, 114, 111, 108, 101, 32, 120, 109, 108, 110, 115, 61, 34, 104, 116, 116, 112, 58, 47, 47, 109, 105, 100, 112, 111, 105 });
    private static final List<MimeType> MIME_TYPE_LIST = FileValidatorUtil.getMimeTypes(FileValidatorUtil.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES);

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
