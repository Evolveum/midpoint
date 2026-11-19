/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRelationInfoType;

/**
 * Relationship between the subject and object class with the state of both sides.
 */
public class RelationshipDto implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Relationship between the subject and object class.
     */
    private final ConnDevRelationInfoType relation;

    /**
     * Subject side of the relationship.
     */
    private final RelationshipSideDto subject;

    /**
     * Object side of the relationship.
     */
    private final RelationshipSideDto object;

    private RelationshipDto(ConnDevRelationInfoType relation, RelationshipSideDto subject, RelationshipSideDto object) {
        this.relation = relation;
        this.subject = subject;
        this.object = object;
    }

    /**
     * Creates the relationship with its subject and object sides.
     *
     * @param relation relationship between the subject and object class
     * @param configuredObjectClasses object classes configured in the connector
     * @return relationship
     */
    public static RelationshipDto create(
            ConnDevRelationInfoType relation, List<ConnDevObjectClassInfoType> configuredObjectClasses) {
        return new RelationshipDto(
                relation,
                RelationshipSideDto.create(relation.getSubject(), relation.getSubjectAttribute(), configuredObjectClasses),
                RelationshipSideDto.create(relation.getObject(), relation.getObjectAttribute(), configuredObjectClasses));
    }

    public ConnDevRelationInfoType getRelation() {
        return relation;
    }

    public RelationshipSideDto getSubject() {
        return subject;
    }

    public RelationshipSideDto getObject() {
        return object;
    }

    /**
     * Returns the name shown to the user.
     *
     * @return display name, or the name when the display name is empty
     */
    public String getDisplayName() {
        return StringUtils.defaultIfEmpty(relation.getDisplayName(), relation.getName());
    }

    /**
     * Checks whether the object classes of both sides are configured in the connector.
     *
     * @return true when both object classes are configured
     */
    public boolean hasConfiguredObjectClasses() {
        return subject.getObjectClass().isConfigured() && object.getObjectClass().isConfigured();
    }

    /**
     * Checks whether the attributes of both sides are configured in the connector.
     *
     * @return true when both attributes are configured
     */
    public boolean hasConfiguredAttributes() {
        return subject.getAttribute().isConfigured() && object.getAttribute().isConfigured();
    }
}
