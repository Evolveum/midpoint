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
 */
public enum ReferenceType {

    ARCHETYPE(QReferenceMapping.INSTANCE_ARCHETYPE,
            AssignmentHolderType.class, AssignmentHolderType.F_ARCHETYPE_REF),

    CREATE_APPROVER(QReferenceMapping.INSTANCE_CREATE_APPROVER,
            ObjectType.class, MetadataType.F_CREATE_APPROVER_REF),

    DELEGATED(QReferenceMapping.INSTANCE_DELEGATED,
            FocusType.class, FocusType.F_DELEGATED_REF),

    // TODO map in QObjectTemplate when it exists
    INCLUDE(QReferenceMapping.INSTANCE_INCLUDE,
            ObjectTemplateType.class, ObjectTemplateType.F_INCLUDE_REF),

    MODIFY_APPROVER(QReferenceMapping.INSTANCE_MODIFY_APPROVER,
            ObjectType.class, MetadataType.F_MODIFY_APPROVER_REF),

    OBJECT_PARENT_ORG(QReferenceMapping.INSTANCE_OBJECT_PARENT_ORG,
            ObjectType.class, ObjectType.F_PARENT_ORG_REF),

    PERSONA(QReferenceMapping.INSTANCE_PERSONA,
            FocusType.class, FocusType.F_PERSONA_REF),

    // TODO map in QResource when it exists
    RESOURCE_BUSINESS_CONFIGURATION_APPROVER(
            QReferenceMapping.INSTANCE_RESOURCE_BUSINESS_CONFIGURATION_APPROVER,
            ResourceType.class, ResourceBusinessConfigurationType.F_APPROVER_REF),

    ROLE_MEMBER(QReferenceMapping.INSTANCE_ROLE_MEMBER,
            AssignmentHolderType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF),

    USER_ACCOUNT(QReferenceMapping.INSTANCE_USER_ACCOUNT,
            FocusType.class, FocusType.F_LINK_REF);

    private final QReferenceMapping qReferenceMapping;
    private final Class<? extends ObjectType> schemaType;
    private final QName itemName;

    ReferenceType(
            @NotNull QReferenceMapping qReferenceMapping,
            @NotNull Class<? extends ObjectType> schemaType,
            @NotNull QName itemName) {
        this.qReferenceMapping = qReferenceMapping;
        this.schemaType = schemaType;
        this.itemName = itemName;
    }

    public QReferenceMapping qReferenceMapping() {
        return qReferenceMapping;
    }

    public Class<? extends ObjectType> schemaType() {
        return schemaType;
    }

    public QName itemName() {
        return itemName;
    }

    public static ReferenceType getOwnerByQName(
            Class<? extends ObjectType> typeClass, QName itemName) {
        Objects.requireNonNull(typeClass, "Schema type class must not be null");
        Objects.requireNonNull(itemName, "QName must not be null");

        for (ReferenceType referenceType : values()) {
            if (QNameUtil.match(itemName, referenceType.itemName)
                    && referenceType.schemaType.isAssignableFrom(typeClass)) {
                return referenceType;
            }
        }

        throw new IllegalArgumentException("Can't find reference type for item '" + itemName
                + "' in schema type " + typeClass.getName());
    }
}
