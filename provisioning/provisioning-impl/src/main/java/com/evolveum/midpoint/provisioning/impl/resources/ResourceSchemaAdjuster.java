/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * Adjusts the schema after being fetched from the resource.
 *
 * Currently, the "source" attribute for activation capability is hidden (by ignoring it).
 */
class ResourceSchemaAdjuster {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceSchemaAdjuster.class);

    @NotNull private final ResourceType resource;

    /** Not final because we clone it to mutable form (when needed). */
    @NotNull private ResourceSchema rawResourceSchema;

    ResourceSchemaAdjuster(
            @NotNull ResourceType resource,
            @NotNull ResourceSchema rawResourceSchema) {
        assert rawResourceSchema.isRaw();

        this.resource = resource;
        this.rawResourceSchema = rawResourceSchema;
    }

    /**
     * Adjust scheme with respect to capabilities. E.g. disable attributes that
     * are used for special purpose (such as account activation simulation).
     *
     * TODO treat also objectclass-specific capabilities here
     *
     * @return Adjusted schema. If the original was immutable, and a modification was needed, a clone is returned.
     */
    ResourceSchema adjustSchema() {
        CapabilitiesType capabilities = resource.getCapabilities();
        if (capabilities == null || capabilities.getConfigured() == null) {
            return rawResourceSchema;
        }
        // TODO what if activation as a whole is disabled?
        ActivationCapabilityType activationCapability = capabilities.getConfigured().getActivation();
        if (CapabilityUtil.getEnabledActivationStatus(activationCapability) != null) {
            QName attributeName = activationCapability.getStatus().getAttribute();
            Boolean ignore = activationCapability.getStatus().isIgnoreAttribute();
            if (attributeName != null && !Boolean.FALSE.equals(ignore)) {
                if (rawResourceSchema.isImmutable()) {
                    // Maybe we may postpone this cloning until absolutely necessary ... but that would complicate things.
                    // This is adequate.
                    rawResourceSchema = rawResourceSchema.clone();
                }
                setAttributeIgnored(attributeName);
            }
        }
        return rawResourceSchema;
    }

    /**
     * Sets the attribute with a given name as ignored - in all object classes.
     *
     * The attribute used for enable/disable simulation should be ignored in the schema
     * otherwise strange things may happen, such as changing the same attribute both from
     * activation/enable and from the attribute using its native name.
     *
     * TODO Is it OK that we update the attribute in all the object classes?
     */
    private void setAttributeIgnored(QName attributeName) {
        for (ResourceObjectClassDefinition objectClassDefinition : rawResourceSchema.getObjectClassDefinitions()) {
            ResourceAttributeDefinition<?> attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
            if (attributeDefinition != null) {
                objectClassDefinition.toMutable().replaceDefinition(
                        attributeDefinition.getItemName(),
                        attributeDefinition.spawnModifyingRaw(def -> def.setProcessing(ItemProcessing.IGNORE)));
            } else {
                // TODO is the following description OK even if we consider multiple object classes?
                //  For example, the attribute may be present in inetOrgPerson but may be missing in
                //  organizationalUnit.
                //
                // Simulated activation attribute points to something that is not in the schema
                // technically, this is an error. But it looks to be quite common in connectors.
                // The enable/disable is using operational attributes that are not exposed in the
                // schema, but they work if passed to the connector.
                // Therefore we don't want to break anything. We could log an warning here, but the
                // warning would be quite frequent. Maybe a better place to warn user would be import
                // of the object.
                LOGGER.debug("Simulated activation attribute {} for objectclass {} in {}  does not exist in "
                        + "the resource schema. This may work well, but it is not clean. Connector exposing "
                        + "such schema should be fixed.", attributeName, objectClassDefinition.getTypeName(), resource);
            }
        }
    }
}
