/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

/** Native resource capabilities and (parsed) native schema. */
record NativeCapabilitiesAndSchema(
        @Nullable CapabilityCollectionType capabilities,
        @Nullable NativeResourceSchema nativeSchema,
        Boolean legacySchema) {

    static @NotNull NativeCapabilitiesAndSchema of(
            @NotNull CapabilityCollectionType capabilities, @NotNull NativeResourceSchema schema) {
        return new NativeCapabilitiesAndSchema(capabilities, schema, isLegacySchema(schema));
    }

    synchronized NativeCapabilitiesAndSchema withUpdatedSchema(@NotNull NativeResourceSchema resourceSchema) {
        return new NativeCapabilitiesAndSchema(capabilities, resourceSchema, isLegacySchema(resourceSchema));
    }

    @NotNull NativeCapabilitiesAndSchema withUpdatedCapabilities(@Nullable CapabilityCollectionType capabilities) {
        return new NativeCapabilitiesAndSchema(capabilities, nativeSchema, legacySchema);
    }

    private static boolean isLegacySchema(NativeResourceSchema resourceSchema) {
        // This is obviously only an approximation. Even in non-legacy connector one can name its class "AccountObjectClass".
        // We can tell with certainty only from the ConnId schema, looking for __ACCOUNT__ and __GROUP__ classes.
        // (We'd need some flag to store the information in the XSD to be precise.)
        return resourceSchema.findObjectClassDefinition(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS) != null;
    }
}
