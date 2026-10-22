/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import java.io.Serial;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.smart.api.conndev.DocumentationContentTypes;

/**
 * Validates the content type of a connector-development documentation source.
 */
public class DocumentationContentTypeValidator implements IValidator<String> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern CONTENT_TYPE_FORMAT = Pattern.compile(
            "[a-z0-9][a-z0-9!#$&^_.+-]*/[a-z0-9][a-z0-9!#$&^_.+-]*", Pattern.CASE_INSENSITIVE);

    /**
     * Reports an error when the content type does not meet the requirements.
     * An empty value is not reported.
     */
    @Override
    public void validate(IValidatable<String> validatable) {
        DocumentationContentTypeViolation violation = checkContentType(validatable.getValue());
        if (violation != null) {
            validatable.error(new ValidationError(this).addKey(violation.getMessageKey()));
        }
    }

    private static DocumentationContentTypeViolation checkContentType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (!isValidFormat(value)) {
            return DocumentationContentTypeViolation.INVALID_FORMAT;
        }
        if (!DocumentationContentTypes.isSupportedContentType(value)) {
            return DocumentationContentTypeViolation.UNSUPPORTED;
        }
        return null;
    }

    private static boolean isValidFormat(String contentType) {
        String mediaType = contentType.split(";", 2)[0].trim();
        return CONTENT_TYPE_FORMAT.matcher(mediaType).matches();
    }
}
