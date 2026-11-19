/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.io.Serial;
import java.io.Serializable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

/**
 * Details of the object class of one relationship side, including whether it is configured in the connector.
 */
public class RelationshipObjectClassDetails implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Name of the object class.
     */
    private final String name;

    /**
     * Whether the object class is configured in the connector.
     */
    private final boolean configured;

    private RelationshipObjectClassDetails(String name, boolean configured) {
        this.name = name;
        this.configured = configured;
    }

    /**
     * Creates the object class details and checks whether the object class is configured in the connector.
     *
     * @param name object class name
     * @param configuredObjectClass object class configured in the connector with this name, or null
     * @return object class details
     */
    public static RelationshipObjectClassDetails create(String name, ConnDevObjectClassInfoType configuredObjectClass) {
        return new RelationshipObjectClassDetails(name, configuredObjectClass != null);
    }

    public String getName() {
        return name;
    }

    public boolean isConfigured() {
        return configured;
    }
}
