/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AttributeVerificationStatus;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.VerifiedAttributeDto;

/**
 * One attribute of an updated object with its value before the update.
 */
public class UpdatedAttributeDto extends VerifiedAttributeDto {

    @Serial private static final long serialVersionUID = 1L;

    public static final String F_PREVIOUS_VALUE = "previousValue";

    private final String previousValue;

    /**
     * Creates an updated attribute.
     *
     * @param name attribute name
     * @param previousValue value before the update
     * @param expectedValue value sent by the test
     * @param value value retrieved from the external system
     * @param status result of comparing the sent and the retrieved value
     */
    public UpdatedAttributeDto(String name, String previousValue, String expectedValue, String value,
            AttributeVerificationStatus status) {
        super(name, expectedValue, value, status);
        this.previousValue = previousValue;
    }

    public String getPreviousValue() {
        return previousValue;
    }
}
