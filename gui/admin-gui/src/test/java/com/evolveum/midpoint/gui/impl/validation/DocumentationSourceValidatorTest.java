/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.validation;

import static org.testng.Assert.assertTrue;

import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.ValidationError;
import org.testng.annotations.Test;

import com.evolveum.midpoint.smart.api.conndev.DocumentationContentTypes;

public class DocumentationSourceValidatorTest {

    @Test
    public void testValidUrl() {
        Validatable<String> validatable = new Validatable<>("https://example.com/api/docs");

        new DocumentationUriValidator().validate(validatable);

        assertTrue(validatable.isValid());
    }

    @Test
    public void testBlankUrlIsNotReported() {
        Validatable<String> validatable = new Validatable<>("  ");

        new DocumentationUriValidator().validate(validatable);

        assertTrue(validatable.isValid());
    }

    @Test
    public void testUrlWithoutSchemeIsInvalid() {
        Validatable<String> validatable = new Validatable<>("dwed");

        new DocumentationUriValidator().validate(validatable);

        ValidationError error = (ValidationError) validatable.getErrors().get(0);
        assertTrue(error.getKeys().contains(DocumentationUriViolation.INVALID_FORMAT.getMessageKey()));
    }

    @Test
    public void testUrlWithoutHostIsInvalid() {
        Validatable<String> validatable = new Validatable<>("https:///docs");

        new DocumentationUriValidator().validate(validatable);

        ValidationError error = (ValidationError) validatable.getErrors().get(0);
        assertTrue(error.getKeys().contains(DocumentationUriViolation.INVALID_FORMAT.getMessageKey()));
    }

    @Test
    public void testFtpUrlIsUnsupported() {
        Validatable<String> validatable = new Validatable<>("ftp://example.com/docs");

        new DocumentationUriValidator().validate(validatable);

        ValidationError error = (ValidationError) validatable.getErrors().get(0);
        assertTrue(error.getKeys().contains(DocumentationUriViolation.UNSUPPORTED_PROTOCOL.getMessageKey()));
    }

    @Test
    public void testSupportedContentType() {
        Validatable<String> validatable = new Validatable<>("application/json");

        new DocumentationContentTypeValidator().validate(validatable);

        assertTrue(validatable.isValid());
    }

    @Test
    public void testAnyTextContentTypeIsSupported() {
        Validatable<String> validatable = new Validatable<>("text/x-custom");

        new DocumentationContentTypeValidator().validate(validatable);

        assertTrue(validatable.isValid());
    }

    @Test
    public void testContentTypeWithoutSubtypeIsInvalid() {
        Validatable<String> validatable = new Validatable<>("xdsx");

        new DocumentationContentTypeValidator().validate(validatable);

        ValidationError error = (ValidationError) validatable.getErrors().get(0);
        assertTrue(error.getKeys().contains(DocumentationContentTypeViolation.INVALID_FORMAT.getMessageKey()));
    }

    @Test
    public void testImageContentTypeIsUnsupported() {
        Validatable<String> validatable = new Validatable<>("image/png");

        new DocumentationContentTypeValidator().validate(validatable);

        ValidationError error = (ValidationError) validatable.getErrors().get(0);
        assertTrue(error.getKeys().contains(DocumentationContentTypeViolation.UNSUPPORTED.getMessageKey()));
    }

    @Test
    public void testOfferedContentTypesAreValid() {
        for (String contentType : DocumentationContentTypes.getSupportedContentTypes()) {
            Validatable<String> validatable = new Validatable<>(contentType);

            new DocumentationContentTypeValidator().validate(validatable);

            assertTrue(validatable.isValid(), "Offered content type '" + contentType + "' must be valid");
        }
    }
}
