/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;

/**
 * One attribute of a retrieved object with its sent value.
 */
public class VerifiedAttributeDto extends RetrievedAttributeDto {

    @Serial private static final long serialVersionUID = 1L;

    public static final String F_EXPECTED_VALUE = "expectedValue";

    private final String expectedValue;
    private final AttributeVerificationStatus status;

    /**
     * Creates a verified attribute.
     *
     * @param name attribute name
     * @param expectedValue value sent by the test
     * @param value value retrieved from the external system
     * @param status result of comparing both values
     */
    public VerifiedAttributeDto(String name, String expectedValue, String value, AttributeVerificationStatus status) {
        super(name, value);
        this.expectedValue = expectedValue;
        this.status = status;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public AttributeVerificationStatus getStatus() {
        return status;
    }
}
