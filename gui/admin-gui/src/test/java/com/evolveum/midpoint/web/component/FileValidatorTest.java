/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import static com.evolveum.midpoint.common.MimeTypeUtil.*;

import com.evolveum.midpoint.web.component.input.validator.ContentTypeFileValidator;
import com.evolveum.midpoint.web.component.input.validator.FileValidatorUtil;

import static org.testng.Assert.assertEquals;

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
        final String deniedContentType = contentTypeFileValidator.validate(MIME_IMAGE_JPEG);
        assertEquals(deniedContentType, "");
    }

    @Test
    public void test4299ContentTypeFileValidator_validPNG() throws Exception {
        final ContentTypeFileValidator contentTypeFileValidator =
                new ContentTypeFileValidator(FileValidatorUtil.getMimeTypes(FileValidatorUtil.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES));
        final String deniedContentType = contentTypeFileValidator.validate(MIME_IMAGE_PNG);
        assertEquals(deniedContentType, "");
    }

    @Test
    public void test4299ContentTypeFileValidator_invalid() throws Exception {
        final ContentTypeFileValidator contentTypeFileValidator =
                new ContentTypeFileValidator(FileValidatorUtil.getMimeTypes(FileValidatorUtil.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES));
        final String deniedContentType = contentTypeFileValidator.validate(MIME_APPLICATION_XML);
        assertEquals(deniedContentType, MIME_APPLICATION_XML);
    }
}
