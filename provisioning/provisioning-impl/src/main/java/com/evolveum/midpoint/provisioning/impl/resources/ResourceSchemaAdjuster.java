/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.CapabilityUtil.getCapability;
import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.getObjectClassName;
import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.isDefaultForObjectClass;

/**
 * Adjusts the schema after being fetched from the resource.
 *
 * Currently, the "source" attribute for activation capability is hidden (by ignoring it).
 *
 * TODO consider moving this functionality to the resource parsing stage
 */
class ResourceSchemaAdjuster {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceSchemaAdjuster.class);

    /** Must be expanded. */
    @NotNull private final ResourceType resource;

    /** Not final because we clone it to mutable form (when needed). */
    @NotNull private ResourceSchema rawResourceSchema;

    /**
     * BEWARE! The resource must be expanded, as we access the definition beans.
     */
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
     * TODO Is this the appropriate place to do this? An alternative would be when refinements are parsed, and complete
     *  schema is being constructed. Then, we should not modify the raw schema, but should set the "ignore" flag in
     *  the refined schema only. But, for now, we do it here.
     *
     * @return Adjusted schema. If the original was immutable, and a modification was needed, a clone is returned.
     */
    ResourceSchema adjustSchema() {
        setActivationSimulationAttributesIgnored();
        return rawResourceSchema;
    }

    /**
     * Sets the attributes used for activation simulation to be ignored in the schema (if needed).
     *
     * The attribute used for enable/disable simulation should be ignored in the schema
     * otherwise strange things may happen, such as changing the same attribute both from
     * activation/enable and from the attribute using its native name.
     *
     * Contrary to the way how this was done since 4.8.4, we just go through all object classes,
     * and consider each object class individually.
     */
    private void setActivationSimulationAttributesIgnored() {

        // If we have immutable schema, we first check if there is anything to do - and if so, we clone the schema.
        // We do not do the cloning in the main loop below, because we'd need to have the actual object class definition
        // cloned as well. (And it's not trivial to find it in the clone.)
        if (rawResourceSchema.isImmutable()) {
            if (!hasSimulationAttributesToIgnore()) {
                return; // nothing is needed, so the second iteration is not necessary
            }
            rawResourceSchema = rawResourceSchema.clone();
        }

        // Here we do the actual work.
        for (var objectClassDefinition : rawResourceSchema.getObjectClassDefinitions()) {

            var attributeName = getSimulationAttributeToIgnoreIfPresent(objectClassDefinition);
            if (attributeName == null) {
                continue;
            }

            var attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
            if (attributeDefinition != null) {
                objectClassDefinition.toMutable().replaceDefinition(
                        attributeDefinition.getItemName(),
                        attributeDefinition.spawnModifyingRaw(def -> def.setProcessing(ItemProcessing.IGNORE)));
            } else {
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

    private boolean hasSimulationAttributesToIgnore() {
        for (var objectClassDefinition : rawResourceSchema.getObjectClassDefinitions()) {
            if (getSimulationAttributeToIgnoreIfPresent(objectClassDefinition) != null) {
                return true;
            }
        }
        return false;
    }

    private QName getSimulationAttributeToIgnoreIfPresent(ResourceObjectClassDefinition classDefinition) {
        var specificCapabilities = findObjectClassSpecificCapabilities(classDefinition.getObjectClassName());
        var activationCapability = getCapability(resource, specificCapabilities, ActivationCapabilityType.class);
        if (activationCapability == null) {
            return null;
        }
        var statusCapability = CapabilityUtil.getEnabledActivationStatus(activationCapability);
        if (statusCapability == null) {
            return null;
        }
        var attributeName = statusCapability.getAttribute();
        var ignore = statusCapability.isIgnoreAttribute();
        return attributeName != null && !Boolean.FALSE.equals(ignore) ? attributeName : null;
    }

    /** We have no parsed type definitions here, so we must look at the beans. */
    private CapabilityCollectionType findObjectClassSpecificCapabilities(@NotNull QName className) {
        var schemaHandling = resource.getSchemaHandling();
        if (schemaHandling == null) {
            return null;
        }
        for (var objectTypeDefBean : schemaHandling.getObjectType()) {
            if (QNameUtil.match(getObjectClassName(objectTypeDefBean), className)
                    && isDefaultForObjectClass(objectTypeDefBean)) {
                return objectTypeDefBean.getConfiguredCapabilities();
            }
        }
        return null;
    }
}
