/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * Validates the URL of a connector-development documentation source.
 */
public class DocumentationUriValidator implements IValidator<String> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("http", "https");

    /**
     * Reports an error when the URL does not meet the requirements checked by {@link #checkUri(String)}.
     * An empty value is not reported.
     */
    @Override
    public void validate(IValidatable<String> validatable) {
        DocumentationUriViolation violation = checkUri(validatable.getValue());
        if (violation != null) {
            validatable.error(new ValidationError(this).addKey(violation.getMessageKey()));
        }
    }

    private static DocumentationUriViolation checkUri(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        URI uri;
        try {
            uri = new URI(value.trim());
        } catch (URISyntaxException e) {
            return DocumentationUriViolation.INVALID_FORMAT;
        }

        if (!uri.isAbsolute() || uri.isOpaque()) {
            return DocumentationUriViolation.INVALID_FORMAT;
        }
        if (!SUPPORTED_PROTOCOLS.contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
            return DocumentationUriViolation.UNSUPPORTED_PROTOCOL;
        }
        if (StringUtils.isEmpty(uri.getHost())) {
            return DocumentationUriViolation.INVALID_FORMAT;
        }
        return null;
    }
}
