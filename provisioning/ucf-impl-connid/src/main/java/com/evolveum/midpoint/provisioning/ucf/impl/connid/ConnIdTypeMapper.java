/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.typeNameInRi;
import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.xml.namespace.QName;

import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorObjectReference;
import org.identityconnectors.framework.common.objects.EmbeddedObject;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedByteArrayType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

class ConnIdTypeMapper {

    /**
     * Converts ConnId type (used in connector objects and configuration properties)
     * to a midPoint type (used in XSD schemas and beans).
     *
     * The "subtype" parameter is currently applicable only to connector object attributes.
     */
    static @NotNull QName connIdTypeToXsdTypeName(
            Class<?> type, String subtype, boolean isConfidential, Supplier<String> referenceTypeNameSupplier)
            throws SchemaException {

        if (Map.class.isAssignableFrom(type)) {
            // ConnId type is "Map". We need more precise definition on midPoint side.
            schemaCheck(subtype != null,
                    "Map type cannot be converted without knowing the subtype");
            if (SchemaConstants.ICF_SUBTYPES_POLYSTRING_URI.equals(subtype)) {
                return PolyStringType.COMPLEX_TYPE;
            } else {
                throw new SchemaException("Unsupported subtype '%s' for a Map".formatted(subtype));
            }
        } else if (GuardedString.class.equals(type) || String.class.equals(type) && isConfidential) {
            // GuardedString is a special case. It is a ICF-specific type implementing Potemkin-like security.
            return ProtectedStringType.COMPLEX_TYPE;
        } else if (GuardedByteArray.class.equals(type) || Byte.class.equals(type) && isConfidential) {
            // GuardedByteArray is a special case. It is a ICF-specific type implementing Potemkin-like security.
            return ProtectedByteArrayType.COMPLEX_TYPE;
        } else if (ConnectorObjectReference.class.equals(type) || EmbeddedObject.class.equals(type)) {
            return typeNameInRi(Objects.requireNonNullElseGet(subtype, referenceTypeNameSupplier));
        } else {
            return XsdTypeMapper.toXsdType(type);
        }
    }
}
