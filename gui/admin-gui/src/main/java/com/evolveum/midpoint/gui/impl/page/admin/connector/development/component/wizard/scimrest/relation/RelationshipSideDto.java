/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.Strings;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

/**
 * One side of a relationship, the object class and its attribute referring to the other side.
 */
public class RelationshipSideDto implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Details of the object class of the side.
     */
    private final RelationshipObjectClassDetails objectClass;

    /**
     * Details of the attribute of the object class referring to the other side.
     */
    private final RelationshipAttributeDetails attribute;

    private RelationshipSideDto(RelationshipObjectClassDetails objectClass, RelationshipAttributeDetails attribute) {
        this.objectClass = objectClass;
        this.attribute = attribute;
    }

    /**
     * Creates the side from the names of its object class and attribute.
     *
     * @param objectClass object class name
     * @param attribute attribute name
     * @param configuredObjectClasses object classes configured in the connector
     * @return relationship side
     */
    public static RelationshipSideDto create(
            String objectClass, String attribute, List<ConnDevObjectClassInfoType> configuredObjectClasses) {
        ConnDevObjectClassInfoType configuredObjectClass = findConfiguredObjectClass(objectClass, configuredObjectClasses);
        return new RelationshipSideDto(
                RelationshipObjectClassDetails.create(objectClass, configuredObjectClass),
                RelationshipAttributeDetails.create(attribute, configuredObjectClass));
    }

    public RelationshipObjectClassDetails getObjectClass() {
        return objectClass;
    }

    public RelationshipAttributeDetails getAttribute() {
        return attribute;
    }

    private static ConnDevObjectClassInfoType findConfiguredObjectClass(
            String name, List<ConnDevObjectClassInfoType> configuredObjectClasses) {
        return configuredObjectClasses.stream()
                .filter(configuredObjectClass -> Strings.CS.equals(configuredObjectClass.getName(), name))
                .findFirst()
                .orElse(null);
    }
}
