/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;

/**
 * Converts two-state property from "real" to "simulated" form.
 *
 * Used for activation status and lockout status conversion.
 *
 * Unlike {@link TwoStateSimulatedToRealConverter} this one is much more complex.
 * It covers the process up to creation of simulated property and delta instances.
 */
class TwoStateRealToSimulatedConverter<N> {

    private static final Trace LOGGER = TraceManager.getTrace(TwoStateRealToSimulatedConverter.class);

    @NotNull private final List<String> simulatedPositiveValues;
    @NotNull private final List<String> simulatedNegativeValues;
    @NotNull private final N nativePositiveValue;
    @NotNull private final N nativeNegativeValue;
    @NotNull private final QName simulatingAttributeName;
    @NotNull private final String description;
    @NotNull private final ProvisioningContext ctx;
    @NotNull private final CommonBeans beans;

    private TwoStateRealToSimulatedConverter(@NotNull List<String> simulatedPositiveValues, @NotNull List<String> simulatedNegativeValues,
            @NotNull N nativePositiveValue, @NotNull N nativeNegativeValue,
            @NotNull QName simulatingAttributeName, @NotNull String description, @NotNull ProvisioningContext ctx,
            @NotNull CommonBeans beans) {
        this.simulatedPositiveValues = simulatedPositiveValues;
        this.simulatedNegativeValues = simulatedNegativeValues;
        this.nativePositiveValue = nativePositiveValue;
        this.nativeNegativeValue = nativeNegativeValue;
        this.simulatingAttributeName = simulatingAttributeName;
        this.description = description;
        this.ctx = ctx;
        this.beans = beans;
    }

    @NotNull
    static TwoStateRealToSimulatedConverter<ActivationStatusType> create(
            @NotNull ActivationStatusCapabilityType statusCapability, @NotNull QName simulatingAttributeName,
            @NotNull ProvisioningContext ctx, @NotNull CommonBeans beans) {
        return new TwoStateRealToSimulatedConverter<>(
                statusCapability.getEnableValue(), statusCapability.getDisableValue(),
                ActivationStatusType.ENABLED, ActivationStatusType.DISABLED,
                simulatingAttributeName, "activation status", ctx, beans);
    }

    @NotNull
    static TwoStateRealToSimulatedConverter<LockoutStatusType> create(
            @NotNull ActivationLockoutStatusCapabilityType lockoutCapability, @NotNull QName simulatingAttributeName,
            @NotNull ProvisioningContext ctx, @NotNull CommonBeans beans) {
        return new TwoStateRealToSimulatedConverter<>(
                lockoutCapability.getNormalValue(), lockoutCapability.getLockedValue(),
                LockoutStatusType.NORMAL, LockoutStatusType.LOCKED,
                simulatingAttributeName, "lockout status", ctx, beans);
    }

    <S> boolean convertProperty(N nativeValue, ShadowType shadow, OperationResult result)
            throws SchemaException {
        LOGGER.trace("Creating attribute for simulated {}: {}", description, simulatingAttributeName);

        ResourceAttribute<S> simulatingAttribute =
                createEmptySimulatingAttribute(shadow, result);
        if (simulatingAttribute == null) {
            return false; // error already processed
        }

        S simulatingAttributeRealValue = determineSimulatingAttributeRealValue(nativeValue, simulatingAttribute);
        setSimulatingAttribute(simulatingAttribute, simulatingAttributeRealValue, shadow);
        return true;
    }

    private <S> S determineSimulatingAttributeRealValue(N nativeValue,
            ResourceAttribute<S> simulatingAttribute) {

        if (nativeValue == null || nativePositiveValue.equals(nativeValue)) {
            return getPositiveSimulationValue(simulatingAttribute);
        } else {
            return getNegativeSimulationValue(simulatingAttribute);
        }
    }

    /**
     * Sets simulation attribute value and places the attribute in the shadow.
     */
    private <S> void setSimulatingAttribute(ResourceAttribute<S> simulatingAttribute, S simulatingAttributeRealValue,
            ShadowType shadow) throws SchemaException {
        LOGGER.trace("Converted activation status value to {}, attribute {}", simulatingAttributeRealValue, simulatingAttribute);
        PrismContainer<?> attributesContainer = shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<S> existingSimulatedAttr = attributesContainer.findProperty(simulatingAttribute.getElementName());
        if (!isBlank(simulatingAttributeRealValue)) {
            PrismPropertyValue<S> simulatingAttributeValue =
                    beans.prismContext.itemFactory().createPropertyValue(simulatingAttributeRealValue);
            if (existingSimulatedAttr == null) {
                simulatingAttribute.add(simulatingAttributeValue);
                attributesContainer.add(simulatingAttribute);
            } else {
                existingSimulatedAttr.replace(simulatingAttributeValue);
            }
        } else if (existingSimulatedAttr != null) {
            attributesContainer.remove(existingSimulatedAttr);
        }
    }

    <S> PropertyModificationOperation<S> convertDelta(N nativeValue, ShadowType shadow, OperationResult result)
            throws SchemaException {
        PropertyDelta<S> simulatingAttributeDelta;
        ResourceAttribute<S> simulatingAttribute = createEmptySimulatingAttribute(shadow, result);
        if (simulatingAttribute == null) {
            return null;
        }

        ResourceAttributeDefinition<S> def = simulatingAttribute.getDefinition();
        if (nativeValue == null) {
            simulatingAttributeDelta = createActivationPropDelta(def, null);
        } else if (nativePositiveValue.equals(nativeValue)) {
            simulatingAttributeDelta = createActivationPropDelta(def, getPositiveSimulationValue(simulatingAttribute));
        } else if (nativeNegativeValue.equals(nativeValue)) {
            simulatingAttributeDelta = createActivationPropDelta(def, getNegativeSimulationValue(simulatingAttribute));
        } else {
            LOGGER.warn("Value {} for {} is neither positive ({}) nor negative ({}), ignoring the delta for {} on {}",
                    nativeValue, description, nativePositiveValue, nativeNegativeValue, shadow, ctx.getResource());
            simulatingAttributeDelta = null;
        }

        if (simulatingAttributeDelta != null) {
            return new PropertyModificationOperation<>(simulatingAttributeDelta);
        } else {
            return null;
        }
    }

    private <S> PropertyDelta<S> createActivationPropDelta(ResourceAttributeDefinition<S> attrDef, S value) {
        if (isBlank(value)) {
            //noinspection unchecked
            return beans.prismContext.deltaFactory().property()
                    .createModificationReplaceProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, simulatingAttributeName), attrDef);
        } else {
            //noinspection unchecked
            return beans.prismContext.deltaFactory().property()
                    .createModificationReplaceProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, simulatingAttributeName), attrDef, value);
        }
    }

    private <S> ResourceAttribute<S> createEmptySimulatingAttribute(ShadowType shadow,
            OperationResult result) throws SchemaException {
        LOGGER.trace("Name of the simulating attribute for {}: {}", description, simulatingAttributeName);

        ResourceAttributeDefinition<?> attributeDefinition = ctx.findAttributeDefinition(simulatingAttributeName);
        if (attributeDefinition == null) {
            // Warning is appropriate here. Attribute is defined, but that attribute is not known.
            ResourceType resource = ctx.getResource();
            result.recordWarning("Resource " + ObjectTypeUtil.toShortString(resource)
                    + "  attribute for simulated " + description + " capability" + simulatingAttributeName
                    + " in not present in the schema for objectclass " + ctx + ". Processing of "
                    + description + " for " + ObjectTypeUtil.toShortString(shadow) + " was skipped");
            shadow.setFetchResult(result.createBeanReduced());
            return null;
        }

        //noinspection unchecked
        return (ResourceAttribute<S>) attributeDefinition.instantiate(simulatingAttributeName);
    }

    private <S> S getPositiveSimulationValue(ResourceAttribute<S> simulatingAttribute) {
        if (simulatedPositiveValues.isEmpty()) {
            return null;
        } else {
            Class<S> clazz = getAttributeValueClass(simulatingAttribute);
            return JavaTypeConverter.convert(clazz, simulatedPositiveValues.iterator().next());
        }
    }

    private <S> S getNegativeSimulationValue(ResourceAttribute<S> simulatingAttribute) {
        if (simulatedNegativeValues.isEmpty()) {
            return null;
        } else {
            Class<S> clazz = getAttributeValueClass(simulatingAttribute);
            return JavaTypeConverter.convert(clazz, simulatedNegativeValues.iterator().next());
        }
    }

    @NotNull
    private <T> Class<T> getAttributeValueClass(ResourceAttribute<T> attribute) {
        ResourceAttributeDefinition<T> attributeDefinition = attribute.getDefinition();
        Class<?> attributeValueClass = attributeDefinition != null ? attributeDefinition.getTypeClass() : null;
        if (attributeValueClass != null) {
            //noinspection unchecked
            return (Class<T>) attributeValueClass;
        } else {
            LOGGER.warn("No definition for simulated administrative status attribute {} for {}, assuming String",
                    attribute, ctx.getResource());
            //noinspection unchecked
            return (Class<T>) String.class;
        }
    }

    private boolean isBlank(Object realValue) {
        if (realValue == null) {
            return true;
        } else if (realValue instanceof String) {
            return StringUtils.isBlank((String) realValue);
        } else {
            return false;
        }
    }
}
