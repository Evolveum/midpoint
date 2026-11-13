/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

/**
 * Result of comparing a retrieved attribute with the sent value.
 */
public enum AttributeVerificationStatus {

    /**
     * Retrieved value equals the sent value.
     */
    MATCH("bg-success-subtle border border-success-subtle text-success-emphasis"),

    /**
     * Retrieved value differs from the sent value.
     */
    MISMATCH("bg-warning-subtle border border-warning-subtle text-warning-emphasis"),

    /**
     * Value was not sent but the target system returned it.
     */
    GENERATED("bg-light border text-dark");

    private final String badgeCssClass;

    AttributeVerificationStatus(String badgeCssClass) {
        this.badgeCssClass = badgeCssClass;
    }

    /**
     * Returns the CSS classes for displaying this status as a badge.
     *
     * @return badge color classes
     */
    public String getBadgeCssClass() {
        return badgeCssClass;
    }
}
