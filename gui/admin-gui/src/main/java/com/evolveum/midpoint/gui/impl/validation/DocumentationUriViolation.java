/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

/**
 * Requirement violated by the URL of a connector-development documentation source.
 */
public enum DocumentationUriViolation {

    INVALID_FORMAT("DocumentationUriValidator.invalidFormat"),
    UNSUPPORTED_PROTOCOL("DocumentationUriValidator.unsupportedProtocol");

    private final String messageKey;

    DocumentationUriViolation(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
