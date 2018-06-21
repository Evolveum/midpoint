/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.other;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

/**
 * This is just helper enumeration for different types of reference entities
 * used in many relationships.
 *
 * @author lazyman
 */
public enum RReferenceOwner {

    OBJECT_PARENT_ORG(ObjectType.class, ObjectType.F_PARENT_ORG_REF),      // 0

    USER_ACCOUNT(FocusType.class, FocusType.F_LINK_REF),                   // 1

    RESOURCE_BUSINESS_CONFIGURATON_APPROVER(ResourceType.class, ResourceBusinessConfigurationType.F_APPROVER_REF),    // 2

    ROLE_APPROVER(AbstractRoleType.class, AbstractRoleType.F_APPROVER_REF),              // 3

    /**
     * @deprecated
     */
    @Deprecated
    SYSTEM_CONFIGURATION_ORG_ROOT(SystemConfigurationType.class, null),                 // 4

    CREATE_APPROVER(ObjectType.class, MetadataType.F_CREATE_APPROVER_REF), // 5

    MODIFY_APPROVER(ObjectType.class, MetadataType.F_MODIFY_APPROVER_REF), // 6

    INCLUDE(ObjectTemplateType.class, ObjectTemplateType.F_INCLUDE_REF),           // 7

    ROLE_MEMBER(FocusType.class, FocusType.F_ROLE_MEMBERSHIP_REF),        // 8

    DELEGATED(FocusType.class, FocusType.F_DELEGATED_REF),                // 9

    PERSONA(FocusType.class, FocusType.F_PERSONA_REF);                    // 10

    private Class<? extends ObjectType> typeClass;
    private QName elementName;

    RReferenceOwner(Class<? extends ObjectType> typeClass, QName elementName) {
        this.typeClass = typeClass;
        this.elementName = elementName;
    }

    public Class<? extends ObjectType> getTypeClass() {
        return typeClass;
    }

    public QName getElementName() {
        return elementName;
    }

    public static RReferenceOwner getOwnerByQName(Class<? extends ObjectType> typeClass, QName qname) {
        Validate.notNull(typeClass, "Jaxb type class must not be null");
        Validate.notNull(qname, "QName must not be null");

        for (RReferenceOwner owner : values()) {
            if (qname.equals(owner.getElementName()) && owner.getTypeClass().isAssignableFrom(typeClass)) {
                return owner;
            }
        }

        throw new IllegalArgumentException("Can't find owner for qname '" + qname + "'");
    }
}
