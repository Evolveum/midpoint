/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowAssociationParticipantRole;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedByteArrayType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorObjectReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Map;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.typeNameInRi;
import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;

class ConnIdTypeMapper {

    /**
     * Converts ConnId type (used in connector objects and configuration properties)
     * to a midPoint type (used in XSD schemas and beans).
     *
     * The "subtype" parameter is currently applicable only to connector object attributes.
     */
    static XsdTypeInformation connIdTypeToXsdTypeInfo(Class<?> type, String subtype, boolean isConfidential)
            throws SchemaException {
        boolean multivalue;
        Class<?> componentType;
        // For multi-valued properties we are only interested in the component type.
        // We consider arrays to be multi-valued ... unless it is byte[] or char[]
        if (type.isArray() && !type.equals(byte[].class) && !type.equals(char[].class)) {
            multivalue = true;
            componentType = type.getComponentType();
        } else {
            multivalue = false;
            componentType = type;
        }

        if (Map.class.isAssignableFrom(type)) {
            // ConnId type is "Map". We need more precise definition on midPoint side.
            schemaCheck(subtype != null,
                    "Map type cannot be converted without knowing the subtype");
            if (SchemaConstants.ICF_SUBTYPES_POLYSTRING_URI.equals(subtype)) {
                return new XsdTypeInformation(PolyStringType.COMPLEX_TYPE, multivalue, null);
            } else {
                throw new SchemaException("Unsupported subtype '%s' for a Map".formatted(subtype));
            }
        } else if (GuardedString.class.equals(componentType)
                || String.class.equals(componentType) && isConfidential) {
            // GuardedString is a special case. It is a ICF-specific type implementing Potemkin-like security.
            return new XsdTypeInformation(ProtectedStringType.COMPLEX_TYPE, multivalue, null);
        } else if (GuardedByteArray.class.equals(componentType)
                || Byte.class.equals(componentType) && isConfidential) {
            // GuardedByteArray is a special case. It is a ICF-specific type implementing Potemkin-like security.
            return new XsdTypeInformation(ProtectedByteArrayType.COMPLEX_TYPE, multivalue, null);
        } else if (ConnectorObjectReference.class.equals(componentType)) {
            schemaCheck(subtype != null, "ConnectorObjectReference has no subtype");
            ShadowAssociationParticipantRole role;
            if (subtype.endsWith("#1")) {
                role = ShadowAssociationParticipantRole.SUBJECT;
            } else if (subtype.endsWith("#2")) {
                role = ShadowAssociationParticipantRole.OBJECT;
            } else {
                throw new SchemaException("Participant role cannot be determined from subtype: " + subtype);
            }
            return new XsdTypeInformation(
                    typeNameInRi(subtype.substring(0, subtype.length() - 2)),
                    multivalue,
                    role);
        } else {
            return new XsdTypeInformation(
                    XsdTypeMapper.toXsdType(componentType), multivalue, null);
        }
    }

    /**
     * Information about XSD type, obtained from ConnId type (technically, Java class).
     *
     * @param associationParticipantRole - used only for {@link ConnectorObjectReference}
     */
    record XsdTypeInformation(
            @NotNull QName xsdTypeName,
            boolean multivalued,
            @Nullable ShadowAssociationParticipantRole associationParticipantRole) {

        public int getMaxOccurs() {
            return multivalued ? -1 : 1;
        }
    }
}
