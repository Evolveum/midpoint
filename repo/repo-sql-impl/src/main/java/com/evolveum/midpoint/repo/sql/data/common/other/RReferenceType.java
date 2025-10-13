/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.other;

import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * This is just helper enumeration for different types of reference entities
 * used in many relationships.
 *
 * @author lazyman
 */
public enum RReferenceType {

    OBJECT_PARENT_ORG(ObjectType.class, ObjectType.F_PARENT_ORG_REF),      // 0

    USER_ACCOUNT(FocusType.class, FocusType.F_LINK_REF),                   // 1

    RESOURCE_BUSINESS_CONFIGURATION_APPROVER(ResourceType.class, ResourceBusinessConfigurationType.F_APPROVER_REF),    // 2

    @Deprecated // REMOVED from schema in 4.0
            ROLE_APPROVER(AbstractRoleType.class, null /* was: AbstractRoleType.F_APPROVER_REF */),              // 3

    @Deprecated
    SYSTEM_CONFIGURATION_ORG_ROOT(SystemConfigurationType.class, null),                 // 4

    CREATE_APPROVER(ObjectType.class, MetadataType.F_CREATE_APPROVER_REF), // 5

    MODIFY_APPROVER(ObjectType.class, MetadataType.F_MODIFY_APPROVER_REF), // 6

    INCLUDE(ObjectTemplateType.class, ObjectTemplateType.F_INCLUDE_REF),           // 7

    ROLE_MEMBER(AssignmentHolderType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF),        // 8

    DELEGATED(FocusType.class, FocusType.F_DELEGATED_REF),                // 9

    PERSONA(FocusType.class, FocusType.F_PERSONA_REF),                    // 10

    ARCHETYPE(AssignmentHolderType.class, AssignmentHolderType.F_ARCHETYPE_REF);                    // 11

    private final Class<? extends ObjectType> typeClass;
    private final QName elementName;

    RReferenceType(Class<? extends ObjectType> typeClass, QName elementName) {
        this.typeClass = typeClass;
        this.elementName = elementName;
    }

    public Class<? extends ObjectType> getTypeClass() {
        return typeClass;
    }

    public QName getElementName() {
        return elementName;
    }

    public static RReferenceType getOwnerByQName(Class<? extends ObjectType> typeClass, QName qname) {
        Objects.requireNonNull(typeClass, "Jaxb type class must not be null");
        Objects.requireNonNull(qname, "QName must not be null");

        for (RReferenceType owner : values()) {
            if (QNameUtil.match(qname, owner.getElementName()) && owner.getTypeClass().isAssignableFrom(typeClass)) {
                return owner;
            }
        }

        throw new IllegalArgumentException("Can't find owner for qname '" + qname + "'");
    }
}
