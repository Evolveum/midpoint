/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Enumeration of various types of reference entities (subtypes of {@link QReference}).
 * Each value contains information about concrete Q-type (implying the concrete sub-table)
 * and what is mapped to that kind of reference (reference owner + item that stores it).
 *
 * Implementation notes:
 *
 * * Order of values is irrelevant.
 * * Constant names must match the custom enum type ReferenceType in the database schema.
 *
 * This class has a bit of bad gravity, as it depends on various schema types.
 * TODO: this can perhaps be a job of QObjectReferenceMapping and other ref mappings...
 */
public enum MReferenceType {

    // OBJECT REFERENCES

    ARCHETYPE(AssignmentHolderType.class, AssignmentHolderType.F_ARCHETYPE_REF),

    DELEGATED(AssignmentHolderType.class, AssignmentHolderType.F_DELEGATED_REF),

    INCLUDE(ObjectTemplateType.class, ObjectTemplateType.F_INCLUDE_REF),

    PROJECTION(FocusType.class, FocusType.F_LINK_REF),

    OBJECT_CREATE_APPROVER(ObjectType.class, MetadataType.F_CREATE_APPROVER_REF),

    OBJECT_MODIFY_APPROVER(ObjectType.class, MetadataType.F_MODIFY_APPROVER_REF),

    OBJECT_PARENT_ORG(ObjectType.class, ObjectType.F_PARENT_ORG_REF),

    PERSONA(FocusType.class, FocusType.F_PERSONA_REF),

    RESOURCE_BUSINESS_CONFIGURATION_APPROVER(
            ResourceType.class, ResourceBusinessConfigurationType.F_APPROVER_REF),

    ROLE_MEMBERSHIP(AssignmentHolderType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF),

    // OTHER REFERENCES

    ASSIGNMENT_CREATE_APPROVER(AssignmentType.class, MetadataType.F_CREATE_APPROVER_REF),

    ASSIGNMENT_MODIFY_APPROVER(AssignmentType.class, MetadataType.F_MODIFY_APPROVER_REF)

    // TODO acc.cert.wi refs
    // TODO case.wi refs
    ;

    private final Class<? extends Containerable> schemaType;
    private final QName itemName;

    MReferenceType(
            @NotNull Class<? extends Containerable> schemaType,
            @NotNull QName itemName) {
        this.schemaType = schemaType;
        this.itemName = itemName;
    }

    public Class<? extends Containerable> schemaType() {
        return schemaType;
    }

    public QName itemName() {
        return itemName;
    }

    // TODO: in old repo it's used by ObjectReferenceMapper.map, will we need it or will Q*Mapping definitions take care of it?
    public static MReferenceType getOwnerByQName(
            Class<? extends Containerable> typeClass, QName itemName) {
        Objects.requireNonNull(typeClass, "Schema type class must not be null");
        Objects.requireNonNull(itemName, "QName must not be null");

        for (MReferenceType referenceType : values()) {
            if (QNameUtil.match(itemName, referenceType.itemName)
                    && referenceType.schemaType.isAssignableFrom(typeClass)) {
                return referenceType;
            }
        }

        throw new IllegalArgumentException("Can't find reference type for item '" + itemName
                + "' in schema type " + typeClass.getName());
    }
}
