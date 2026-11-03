/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serializable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Row of the clean up table describing one recorded test object.
 */
public class CleanupObjectDto implements Serializable {

    public static final String F_OBJECT_CLASS = "objectClass";
    public static final String F_PRIMARY_IDENTIFIER = "primaryIdentifier";
    public static final String F_NAME = "name";

    private final String oid;
    private final String objectClass;
    private final String primaryIdentifier;
    private final String name;

    /**
     * Creates the row for a test object.
     *
     * @param shadow shadow of the recorded test object
     * @param objectClass displayed object class
     * @param primaryIdentifier displayed primary identifier values
     * @param name displayed name of the object
     */
    public CleanupObjectDto(ShadowType shadow, String objectClass, String primaryIdentifier, String name) {
        this.oid = shadow.getOid();
        this.objectClass = objectClass;
        this.primaryIdentifier = primaryIdentifier;
        this.name = name;
    }

    public String getOid() {
        return oid;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public String getPrimaryIdentifier() {
        return primaryIdentifier;
    }

    public String getName() {
        return name;
    }
}
