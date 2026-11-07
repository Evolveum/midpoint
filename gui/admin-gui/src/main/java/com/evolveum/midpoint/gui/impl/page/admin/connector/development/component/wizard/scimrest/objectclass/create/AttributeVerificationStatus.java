/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create;

/**
 * Result of comparing a created attribute with the sent value.
 */
public enum AttributeVerificationStatus {

    MATCH,

    MISMATCH,

    GENERATED
}
