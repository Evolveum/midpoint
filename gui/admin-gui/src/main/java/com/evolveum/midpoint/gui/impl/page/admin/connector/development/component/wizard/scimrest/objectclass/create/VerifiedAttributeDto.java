/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.RetrievedAttributeDto;

/**
 * One attribute of a created object with its sent value.
 */
public class VerifiedAttributeDto extends RetrievedAttributeDto {

    @Serial private static final long serialVersionUID = 1L;

    public static final String F_EXPECTED_VALUE = "expectedValue";

    private final String expectedValue;
    private final AttributeVerificationStatus status;

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
