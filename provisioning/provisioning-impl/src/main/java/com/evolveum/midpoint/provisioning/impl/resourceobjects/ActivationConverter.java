/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.prism.PrismProperty.getRealValue;
import static com.evolveum.midpoint.prism.delta.PropertyDeltaCollectionsUtil.findPropertyDelta;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.RepoShadow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;

/**
 * Conversions related to simulated activation (status, validity, lockout).
 *
 * 1. Resource object -> midPoint (simulating/native -> activation)
 * 2. midPoint -> resource object (activation -> simulating/native) on object ADD
 * 3. midPoint -> resource object (activation -> simulating/native) on object MODIFY
 */
class ActivationConverter {

    private static final Trace LOGGER = TraceManager.getTrace(ActivationConverter.class);

    @NotNull private final ProvisioningContext ctx;
    @NotNull private final CommonBeans b = CommonBeans.get();

    ActivationConverter(@NotNull ProvisioningContext ctx) {
        this.ctx = ctx;
    }

    //region Resource object -> midPoint (simulating/native -> activation)
    /**
     * Completes activation for fetched object by determining simulated values if necessary.
     */
    void completeActivation(ResourceObjectShadow resourceObject, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ShadowType resourceObjectBean = resourceObject.getBean();

        ActivationCapabilityType activationCapability = ctx.getCapability(ActivationCapabilityType.class);

        if (!CapabilityUtil.isCapabilityEnabled(activationCapability) && resourceObjectBean.getActivation() == null) {
            LOGGER.trace("No activation capability and also no activation information in the resource object.");
            return;
        }

        ActivationStatusType activationStatus = determineActivationStatus(resourceObject, activationCapability, result);
        LockoutStatusType lockoutStatus = determineLockoutStatus(resourceObject, activationCapability, result);

        if (activationStatus != null || lockoutStatus != null) {
            if (resourceObjectBean.getActivation() == null) {
                resourceObjectBean.setActivation(new ActivationType());
            }
            resourceObjectBean.getActivation().setAdministrativeStatus(activationStatus);
            resourceObjectBean.getActivation().setLockoutStatus(lockoutStatus);
        } else {
            if (resourceObjectBean.getActivation() != null) {
                resourceObjectBean.getActivation().setAdministrativeStatus(null);
                resourceObjectBean.getActivation().setLockoutStatus(null);
            }
        }
    }

    /**
     * Determines activation status for resource object. Uses either native or simulated value.
     */
    private ActivationStatusType determineActivationStatus(
            ResourceObjectShadow resourceObject, ActivationCapabilityType activationCapability, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        ActivationType existingActivation = resourceObject.getBean().getActivation();
        ActivationStatusType nativeValue = existingActivation != null ? existingActivation.getAdministrativeStatus() : null;

        ActivationStatusCapabilityType statusCapability = CapabilityUtil.getEnabledActivationStatusStrict(activationCapability);
        if (statusCapability == null) {
            if (nativeValue != null) {
                LOGGER.trace("The status capability is disabled. Ignoring native value: {}", nativeValue);
            }
            return null; // TODO Reconsider this. Maybe we should return native value.
        }

        if (statusCapability.getAttribute() == null) {
            LOGGER.trace("Simulated activation status is not configured. Using native value: {}", nativeValue);
            return nativeValue;
        }

        Collection<Object> simulatingAttributeValues = getSimulatingAttributeValues(resourceObject, statusCapability.getAttribute());

        TwoStateSimulatedToRealConverter<ActivationStatusType> converter = new TwoStateSimulatedToRealConverter<>(
                statusCapability.getEnableValue(), statusCapability.getDisableValue(),
                ActivationStatusType.ENABLED, ActivationStatusType.DISABLED, "activation status", ctx);

        ActivationStatusType status = converter.convert(simulatingAttributeValues, result);

        LOGGER.trace(
                "Detected simulated activation administrativeStatus attribute {} on {} with value {}, resolved into {}",
                lazy(() -> SchemaDebugUtil.prettyPrint(statusCapability.getAttribute())),
                ctx.getResource(), simulatingAttributeValues, status);

        if (!Boolean.FALSE.equals(statusCapability.isIgnoreAttribute())) {
            removeSimulatingAttribute(resourceObject, statusCapability.getAttribute());
        }

        return status;
    }

    /**
     * Determines lockout status for resource object. Uses either native or simulated value.
     */
    private LockoutStatusType determineLockoutStatus(
            ResourceObjectShadow resourceObject, ActivationCapabilityType activationCapability, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        ActivationType existingActivation = resourceObject.getBean().getActivation();
        LockoutStatusType nativeValue = existingActivation != null ? existingActivation.getLockoutStatus() : null;

        ActivationLockoutStatusCapabilityType lockoutCapability = CapabilityUtil.getEnabledActivationLockoutStrict(activationCapability);
        if (lockoutCapability == null) {
            if (nativeValue != null) {
                LOGGER.trace("The lockout capability is disabled. Ignoring native value: {}", nativeValue);
            }
            return null; // TODO Reconsider this. Maybe we should return native value.
        }

        if (lockoutCapability.getAttribute() == null) {
            LOGGER.trace("Simulated lockout status is not configured. Using native value: {}", nativeValue);
            return nativeValue;
        }

        Collection<Object> simulatingAttributeValues = getSimulatingAttributeValues(resourceObject, lockoutCapability.getAttribute());

        TwoStateSimulatedToRealConverter<LockoutStatusType> converter = new TwoStateSimulatedToRealConverter<>(
                lockoutCapability.getNormalValue(), lockoutCapability.getLockedValue(),
                LockoutStatusType.NORMAL, LockoutStatusType.LOCKED, "lockout status", ctx);

        LockoutStatusType lockout = converter.convert(simulatingAttributeValues, result);

        LOGGER.trace(
                "Detected simulated activation lockoutStatus attribute {} on {} with value {}, resolved into {}",
                lazy(() -> SchemaDebugUtil.prettyPrint(lockoutCapability.getAttribute())),
                ctx.getResource(), simulatingAttributeValues, lockout);

        if (!Boolean.FALSE.equals(lockoutCapability.isIgnoreAttribute())) {
            removeSimulatingAttribute(resourceObject, lockoutCapability.getAttribute());
        }

        return lockout;
    }

    @Nullable
    private Collection<Object> getSimulatingAttributeValues(ResourceObjectShadow resourceObject, QName attributeName) {
        ShadowAttributesContainer attributesContainer = resourceObject.getAttributesContainer();
        ShadowSimpleAttribute<?> simulatingAttribute = attributesContainer.findSimpleAttribute(attributeName);
        return simulatingAttribute != null ?
                simulatingAttribute.getRealValues(Object.class) : null;
    }

    private void removeSimulatingAttribute(ResourceObjectShadow resourceObject, QName attributeName) {
        ShadowAttributesContainer attributesContainer = resourceObject.getAttributesContainer();
        attributesContainer.removeProperty(ItemPath.create(attributeName));
    }
    //endregion

    //region midPoint -> resource object (activation -> simulating/native) on object ADD
    /**
     * Transforms activation information when an object is being added.
     */
    void transformOnAdd(ResourceObjectShadow object, OperationResult result) throws SchemaException, CommunicationException {
        ActivationType activation = object.getBean().getActivation();
        if (activation == null) {
            return;
        }

        ActivationCapabilityType activationCapability = ctx.getCapability(ActivationCapabilityType.class);

        if (activation.getAdministrativeStatus() != null) {
            transformActivationStatusOnAdd(object.getBean(), activationCapability, result);
        }
        if (activation.getLockoutStatus() != null) {
            transformLockoutStatusOnAdd(object.getBean(), activationCapability, result);
        }
    }

    private void transformActivationStatusOnAdd(
            ShadowType resourceObjectBean, ActivationCapabilityType activationCapability, OperationResult result)
            throws SchemaException {

        ActivationStatusCapabilityType statusCapability = CapabilityUtil.getEnabledActivationStatusStrict(activationCapability);
        LOGGER.trace("Activation status capability:\n{}", statusCapability);
        if (statusCapability == null) {
            throw new SchemaException("Attempt to set activation/administrativeStatus on " + ctx.getResource() +
                    " that has neither native nor simulated activation/status capability");
        }

        QName simulatingAttributeName = statusCapability.getAttribute();
        if (simulatingAttributeName == null) {
            LOGGER.trace("Using native activation status capability");
            return;
        }

        boolean converted = TwoStateRealToSimulatedConverter.create(statusCapability, simulatingAttributeName, ctx, b)
                .convertProperty(resourceObjectBean.getActivation().getAdministrativeStatus(), resourceObjectBean, result);

        if (converted) {
            resourceObjectBean.getActivation().setAdministrativeStatus(null);
        }
    }

    private void transformLockoutStatusOnAdd(
            ShadowType resourceObjectBean, ActivationCapabilityType activationCapability, OperationResult result)
            throws SchemaException {

        ActivationLockoutStatusCapabilityType lockoutCapability =
                CapabilityUtil.getEnabledActivationLockoutStrict(activationCapability);
        LOGGER.trace("Lockout status capability:\n{}", lockoutCapability);
        if (lockoutCapability == null) {
            throw new SchemaException("Attempt to set activation/lockoutStatus on " + ctx.getResource() +
                    " that has neither native nor simulated activation/lockoutStatus capability");
        }

        QName simulatingAttributeName = lockoutCapability.getAttribute();
        if (simulatingAttributeName == null) {
            LOGGER.trace("Using native lockout status capability");
            return;
        }

        boolean converted = TwoStateRealToSimulatedConverter.create(lockoutCapability, simulatingAttributeName, ctx, b)
                .convertProperty(resourceObjectBean.getActivation().getLockoutStatus(), resourceObjectBean, result);

        if (converted) {
            resourceObjectBean.getActivation().setLockoutStatus(null);
        }
    }
    //endregion

    //region midPoint -> resource object (activation -> simulating/native) on object MODIFY
    /**
     * Creates activation change operations, based on existing collection of changes.
     */
    @NotNull Collection<Operation> transformOnModify(
            RepoShadow repoShadow, Collection<? extends ItemDelta<?, ?>> modifications, OperationResult result)
            throws SchemaException {

        Collection<Operation> operations = new ArrayList<>();
        ResourceType resource = ctx.getResource();

        ActivationCapabilityType activationCapability = ctx.getCapability(ActivationCapabilityType.class);
        LOGGER.trace("Found activation capability: {}", PrettyPrinter.prettyPrint(activationCapability));

        // using simulating attributes, if defined
        createActivationStatusChange(modifications, repoShadow.getBean(), activationCapability, resource, operations, result);
        createLockoutStatusChange(modifications, repoShadow.getBean(), activationCapability, resource, operations, result);

        // these are converted "as is" (no simulation)
        createValidFromChange(modifications, activationCapability, resource, operations, result);
        createValidToChange(modifications, activationCapability, resource, operations, result);

        return operations;
    }

    private void createActivationStatusChange(
            Collection<? extends ItemDelta<?, ?>> objectChange, ShadowType shadow,
            ActivationCapabilityType activationCapability, ResourceType resource, Collection<Operation> operations,
            OperationResult result) throws SchemaException {
        PropertyDelta<ActivationStatusType> propertyDelta =
                findPropertyDelta(objectChange, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        if (propertyDelta == null) {
            return;
        }

        ActivationStatusCapabilityType statusCapability = CapabilityUtil.getEnabledActivationStatus(activationCapability);
        if (statusCapability == null) {
            SchemaException e = new SchemaException("Attempt to change activation administrativeStatus on " + resource +
                    " which does not have the capability");
            result.setFatalError(e);
            throw e;
        }

        QName simulatingAttributeName = statusCapability.getAttribute();
        if (simulatingAttributeName == null) {
            LOGGER.trace("No simulation attribute, using native value");
            addModification(operations, propertyDelta);
            return;
        }

        ActivationStatusType newStatus = getRealValue(propertyDelta.getPropertyNewMatchingPath());
        LOGGER.trace("Found activation administrativeStatus change to: {}", newStatus);

        PropertyModificationOperation<?> simulatingAttributeModification =
                TwoStateRealToSimulatedConverter.create(statusCapability, simulatingAttributeName, ctx, b)
                        .convertDelta(newStatus, shadow, result);

        if (simulatingAttributeModification != null) {
            operations.add(simulatingAttributeModification);
        } else {
            addModification(operations, propertyDelta);
        }
    }

    private void createLockoutStatusChange(Collection<? extends ItemDelta<?, ?>> objectChange, ShadowType shadow,
            ActivationCapabilityType activationCapability, ResourceType resource, Collection<Operation> operations,
            OperationResult result) throws SchemaException {
        PropertyDelta<LockoutStatusType> propertyDelta =
                findPropertyDelta(objectChange, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
        if (propertyDelta == null) {
            return;
        }

        ActivationLockoutStatusCapabilityType lockoutCapability =
                CapabilityUtil.getEnabledActivationLockoutStrict(activationCapability);
        if (lockoutCapability == null) {
            SchemaException e = new SchemaException("Attempt to change activation lockoutStatus on " + resource +
                    " which does not have the capability");
            result.setFatalError(e);
            throw e;
        }

        QName simulatingAttributeName = lockoutCapability.getAttribute();
        if (simulatingAttributeName == null) {
            LOGGER.trace("No simulation attribute, using native value");
            addModification(operations, propertyDelta);
            return;
        }

        LockoutStatusType newStatus = getRealValue(propertyDelta.getPropertyNewMatchingPath());
        LOGGER.trace("Found activation lockout change to: {}", newStatus);

        PropertyModificationOperation<?> simulatingAttributeModification =
                TwoStateRealToSimulatedConverter.create(lockoutCapability, simulatingAttributeName, ctx, b)
                        .convertDelta(newStatus, shadow, result);

        if (simulatingAttributeModification != null) {
            operations.add(simulatingAttributeModification);
        } else {
            addModification(operations, propertyDelta);
        }
    }

    private void createValidFromChange(Collection<? extends ItemDelta<?, ?>> objectChange,
            ActivationCapabilityType activationCapability, ResourceType resource, Collection<Operation> operations,
            OperationResult result) throws SchemaException {
        PropertyDelta<XMLGregorianCalendar> propertyDelta = findPropertyDelta(objectChange,
                SchemaConstants.PATH_ACTIVATION_VALID_FROM);
        if (propertyDelta == null) {
            return;
        }

        if (CapabilityUtil.getEnabledActivationValidFrom(activationCapability) == null) {
            SchemaException e = new SchemaException("Attempt to change activation validFrom on " + resource +
                    " which does not have the capability");
            result.setFatalError(e);
            throw e;
        }
        LOGGER.trace("Found activation validFrom change to: {}", getRealValue(propertyDelta.getPropertyNewMatchingPath()));
        addModification(operations, propertyDelta);
    }

    private void createValidToChange(Collection<? extends ItemDelta<?, ?>> objectChange,
            ActivationCapabilityType activationCapability, ResourceType resource, Collection<Operation> operations,
            OperationResult result) throws SchemaException {
        PropertyDelta<XMLGregorianCalendar> propertyDelta = findPropertyDelta(objectChange,
                SchemaConstants.PATH_ACTIVATION_VALID_TO);
        if (propertyDelta == null) {
            return;
        }

        if (CapabilityUtil.getEnabledActivationValidTo(activationCapability) == null) {
            SchemaException e = new SchemaException("Attempt to change activation validTo on " + resource +
                    " which does not have the capability");
            result.setFatalError(e);
            throw e;
        }
        LOGGER.trace("Found activation validTo change to: {}", getRealValue(propertyDelta.getPropertyNewMatchingPath()));
        addModification(operations, propertyDelta);
    }

    private void addModification(Collection<Operation> operations, PropertyDelta<?> propertyDelta) {
        operations.add(new PropertyModificationOperation<>(propertyDelta));
    }
    //endregion
}
