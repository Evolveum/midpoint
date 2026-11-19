/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.io.Serial;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

/**
 * Details of the attribute of one relationship side referring to the other side, including whether it is configured in the connector.
 */
public class RelationshipAttributeDetails implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Name of the attribute.
     */
    private final String name;

    /**
     * Whether the object class of the side is configured in the connector and has the attribute.
     */
    private final boolean configured;

    private RelationshipAttributeDetails(String name, boolean configured) {
        this.name = name;
        this.configured = configured;
    }

    /**
     * Creates the attribute details and checks whether the configured object class has the attribute.
     *
     * @param name attribute name
     * @param configuredObjectClass object class of the side configured in the connector, or null
     * @return attribute details
     */
    public static RelationshipAttributeDetails create(String name, ConnDevObjectClassInfoType configuredObjectClass) {
        return new RelationshipAttributeDetails(name, isAttributeConfigured(name, configuredObjectClass));
    }

    public String getName() {
        return name;
    }

    public boolean isConfigured() {
        return configured;
    }

    private static boolean isAttributeConfigured(String name, ConnDevObjectClassInfoType configuredObjectClass) {
        if (configuredObjectClass == null) {
            return false;
        }
        if (StringUtils.isEmpty(name) || configuredObjectClass.getAttribute().isEmpty()) {
            return true;
        }
        return configuredObjectClass.getAttribute().stream()
                .anyMatch(attribute -> Strings.CS.equals(attribute.getName(), name));
    }
}
