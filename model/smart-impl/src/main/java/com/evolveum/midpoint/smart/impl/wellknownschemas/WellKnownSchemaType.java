/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas;

/**
 * Enumeration of known schema types that have predefined mappings.
 * These schemas are commonly used in enterprise integration scenarios.
 */
public enum WellKnownSchemaType {

    SCIM_2_0_USER("SCIM 2.0 User", "RFC 7643 User Schema"),
    LDAP_INETORGPERSON("LDAP inetOrgPerson", "RFC 2798 inetOrgPerson Object Class"),
    LDAP_GROUP_OF_NAMES("LDAP groupOfNames", "LDAP groupOfNames/groupOfUniqueNames Object Class"),
    LDAP_ORGANIZATIONAL_UNIT("LDAP organizationalUnit", "LDAP organizationalUnit Object Class"),
    AD_USER("Active Directory User", "Active Directory User Object"),
    AD_GROUP("Active Directory Group", "Active Directory Group Object"),
    AD_ORGANIZATIONAL_UNIT("Active Directory organizationalUnit", "Active Directory organizationalUnit Object");

    private final String displayName;
    private final String description;

    WellKnownSchemaType(String displayName, String description) {
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
