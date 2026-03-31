/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import static com.evolveum.midpoint.common.MimeTypeUtil.*;

import static org.testng.Assert.*;

import com.evolveum.midpoint.web.component.input.validator.ContentTypeFileValidator;
import com.evolveum.midpoint.web.component.input.validator.FileValidatorUtil;

/**
 * @author matisovaa
 *
 */
@ActiveProfiles("test")
public class FileValidatorTest {

    @Test
    public void test4299ContentTypeFileValidator_validJPEG() throws Exception {
        final ContentTypeFileValidator contentTypeFileValidator =
                new ContentTypeFileValidator(FileValidatorUtil.getMimeTypes(FileValidatorUtil.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES));
        assertTrue(contentTypeFileValidator.isValid(MIME_IMAGE_JPEG));
    }

    @Test
    public void test4299ContentTypeFileValidator_validPNG() throws Exception {
        final ContentTypeFileValidator contentTypeFileValidator =
                new ContentTypeFileValidator(FileValidatorUtil.getMimeTypes(FileValidatorUtil.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES));
        assertTrue(contentTypeFileValidator.isValid(MIME_IMAGE_PNG));
    }

    @Test
    public void test4299ContentTypeFileValidator_invalid() throws Exception {
        final ContentTypeFileValidator contentTypeFileValidator =
                new ContentTypeFileValidator(FileValidatorUtil.getMimeTypes(FileValidatorUtil.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES));
        assertFalse(contentTypeFileValidator.isValid(MIME_APPLICATION_XML));
    }
}
