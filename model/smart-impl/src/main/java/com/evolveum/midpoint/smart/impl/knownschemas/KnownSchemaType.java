/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.knownschemas;

/**
 * Enumeration of known schema types that have predefined mappings.
 * These schemas are commonly used in enterprise integration scenarios.
 */
public enum KnownSchemaType {

    SCIM_2_0_USER("SCIM 2.0 User", "RFC 7643 User Schema"),
    LDAP_INETORGPERSON("LDAP inetOrgPerson", "RFC 2798 inetOrgPerson Object Class"),
    AD_USER("Active Directory User", "Active Directory User Object");

    private final String displayName;
    private final String description;

    KnownSchemaType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
