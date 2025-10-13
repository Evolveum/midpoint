/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

/**
 * This enum contains ICF operational attributes that are used in ICF but are not defined there.
 * The operational attributes are in form __SOME_NAME__.
 *
 * NOTE: This attributes also needs to be defined in the resource-schema XSD!
 *
 */
public enum SecretIcfOperationalAttributes {

    DESCRIPTION("__DESCRIPTION__"),
    GROUPS("__GROUPS__"),
    LAST_LOGIN_DATE("__LAST_LOGIN_DATE__");

    private final String name;

    SecretIcfOperationalAttributes(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
