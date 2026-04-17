/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.web.component.input.validator.FileValidatorUtil;

import jakarta.activation.MimeType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static com.evolveum.midpoint.web.component.input.validator.FileMagicNumberConstants.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES;

public final class FileTestConstants {
    public static final byte[] JPG_ARRAY = new byte[] { -1, -40, -1, -32, 0, 16, 74, 70, 73, 70, 0, 1, 1, 1, 0, 72, 0, 72, 0, 0, -1, -30, 12, 88, 73, 67, 67, 95, 80, 82, 79, 70, 73 };
    public static final InputStream JPG_STREAM1 = new ByteArrayInputStream(JPG_ARRAY);
    public static final InputStream JPG_STREAM2 = new ByteArrayInputStream(JPG_ARRAY);
    public static final InputStream PNG_STREAM = new ByteArrayInputStream(new byte[] { -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 7, 65, 0, 0, 1, 2, 8, 2, 0, 0, 0, 7, 21, 56, -25, 0, 0 });
    public static final InputStream XML_STREAM = new ByteArrayInputStream(new byte[] { 60, 114, 111, 108, 101, 32, 120, 109, 108, 110, 115, 61, 34, 104, 116, 116, 112, 58, 47, 47, 109, 105, 100, 112, 111, 105 });
    public static final List<MimeType> MIME_TYPE_LIST = FileValidatorUtil.getMimeTypes(ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES);
}
