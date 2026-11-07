/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

/**
 * Represents possible sources of the tested object for modifying operations.
 */
public enum TestObjectSource {

    /**
     * Object recorded as a test object of the connector development.
     */
    PREVIOUSLY_CREATED,

    /**
     * Object that already exists in the external system, found by its primary identifier.
     */
    EXISTING_OBJECT,

    /**
     * Object created for this test from the values entered by the user.
     */
    NEW_OBJECT
}
