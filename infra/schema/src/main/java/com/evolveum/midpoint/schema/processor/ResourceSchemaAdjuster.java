/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;

/**
 * TODO TODO TODO Temporary implementation!
 *
 * This is a migration of this component that used to modify the native schema after being fetched, by setting "ignored"
 * flag for simulated activation attribute. This should be a better solution: We won't modify the schema. We will weave
 * the "ignored" flag into the refined schema definition during parsing.
 */
class ResourceSchemaAdjuster {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceSchemaAdjuster.class);

    @NotNull private final ResourceType resource;

    /** Not final because we clone it to mutable form (when needed). */
    @NotNull private NativeResourceSchema nativeResourceSchema;

    ResourceSchemaAdjuster(
            @NotNull ResourceType resource,
            @NotNull NativeResourceSchema nativeResourceSchema) {
        this.resource = resource;
        this.nativeResourceSchema = nativeResourceSchema;
    }

    Set<QName> getIgnoredAttributes() {
        CapabilitiesType capabilities = resource.getCapabilities();
        if (capabilities == null || capabilities.getConfigured() == null) {
            return Set.of();
        }
        // TODO what if activation as a whole is disabled?
        ActivationCapabilityType activationCapability = capabilities.getConfigured().getActivation();
        if (CapabilityUtil.getEnabledActivationStatus(activationCapability) != null) {
            QName attributeName = activationCapability.getStatus().getAttribute();
            Boolean ignore = activationCapability.getStatus().isIgnoreAttribute();
            if (attributeName != null && !Boolean.FALSE.equals(ignore)) {
                if (nativeResourceSchema.isImmutable()) {
                    // Maybe we may postpone this cloning until absolutely necessary ... but that would complicate things.
                    // This is adequate.
                    nativeResourceSchema = nativeResourceSchema.clone();
                }
                return Set.of(attributeName);
            }
        }
        return Set.of();
    }
}
