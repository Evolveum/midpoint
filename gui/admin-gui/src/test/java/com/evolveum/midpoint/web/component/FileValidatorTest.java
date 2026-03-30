/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemHeaders;
import org.apache.commons.fileupload2.core.FileItemHeadersProvider;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static com.evolveum.midpoint.common.MimeTypeUtil.*;

import com.evolveum.midpoint.web.component.input.validator.ContentTypeFileValidator;
import com.evolveum.midpoint.web.component.input.validator.FileValidatorFactory;

import static org.testng.Assert.assertEquals;

/**
 * @author matisovaa
 *
 */
@ActiveProfiles("test")
public class FileValidatorTest {

    @Test
    public void test4299ContentTypeFileValidator_validJPEG() throws Exception {
        final FileUpload fu = new FileUpload(this.getFileItem(MIME_IMAGE_JPEG));
        final ContentTypeFileValidator contentTypeFileValidator =
                new ContentTypeFileValidator(FileValidatorFactory.getMimeTypes(FileValidatorFactory.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES));
        final String deniedContentType = contentTypeFileValidator.validate(fu);
        assertEquals(deniedContentType, "");
    }

    @Test
    public void test4299ContentTypeFileValidator_validPNG() throws Exception {
        final FileUpload fu = new FileUpload(this.getFileItem(MIME_IMAGE_PNG));
        final ContentTypeFileValidator contentTypeFileValidator =
                new ContentTypeFileValidator(FileValidatorFactory.getMimeTypes(FileValidatorFactory.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES));
        final String deniedContentType = contentTypeFileValidator.validate(fu);
        assertEquals(deniedContentType, "");
    }

    @Test
    public void test4299ContentTypeFileValidator_invalid() throws Exception {
        final FileUpload fu = new FileUpload(this.getFileItem(MIME_APPLICATION_XML));
        final ContentTypeFileValidator contentTypeFileValidator =
                new ContentTypeFileValidator(FileValidatorFactory.getMimeTypes(FileValidatorFactory.ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES));
        final String deniedContentType = contentTypeFileValidator.validate(fu);
        assertEquals(deniedContentType, MIME_APPLICATION_XML);
    }

    private FileItem getFileItem(final String contentType) {
        return new FileItem() {
            @Override
            public FileItem delete() throws IOException {
                return null;
            }

            @Override
            public byte[] get() throws IOException {
                return new byte[0];
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public String getFieldName() {
                return "";
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return null;
            }

            @Override
            public String getName() {
                return "";
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return null;
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public String getString() throws IOException {
                return "";
            }

            @Override
            public String getString(Charset charset) throws IOException {
                return "";
            }

            @Override
            public boolean isFormField() {
                return false;
            }

            @Override
            public boolean isInMemory() {
                return false;
            }

            @Override
            public FileItem setFieldName(String s) {
                return null;
            }

            @Override
            public FileItem setFormField(boolean b) {
                return null;
            }

            @Override
            public FileItem write(Path path) throws IOException {
                return null;
            }

            @Override
            public FileItemHeaders getHeaders() {
                return null;
            }

            @Override
            public FileItemHeadersProvider setHeaders(FileItemHeaders fileItemHeaders) {
                return null;
            }
        };
    }
}
