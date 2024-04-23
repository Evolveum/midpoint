/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

/** Native resource capabilities and (parsed) native schema. */
record NativeCapabilitiesAndSchema(
        @NotNull CapabilityCollectionType capabilities,
        @Nullable NativeResourceSchema nativeSchema,
        Boolean legacySchema) {
}
