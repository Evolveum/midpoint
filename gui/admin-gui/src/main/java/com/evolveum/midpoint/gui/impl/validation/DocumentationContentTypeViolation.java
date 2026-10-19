/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

/**
 * Requirement violated by the content type of a connector-development documentation source.
 */
public enum DocumentationContentTypeViolation {

    INVALID_FORMAT("DocumentationContentTypeValidator.invalidFormat"),
    UNSUPPORTED("DocumentationContentTypeValidator.unsupported");

    private final String messageKey;

    DocumentationContentTypeViolation(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
