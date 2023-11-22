/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.*;

/**
 * Responsibilities:
 *
 * . execute the ADD operation on the resource, including before/after scripts
 * . standard checks (execution persistence, capability presence, protected object)
 * . induced entitlement changes
 * . shadow uniqueness check, if requested
 */
class ResourceObjectAddOperation extends ResourceObjectProvisioningOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectAddOperation.class);

    @NotNull private final ShadowType shadow;

    /**
     * We will modify the shadow sometimes (e.g. for simulated capabilities or entitlements).
     * But we do not want the changes to propagate back to the calling code. Hence the clone.
     */
    @NotNull private final ShadowType shadowClone;

    private final boolean skipExplicitUniquenessCheck;

    private final EntitlementConverter entitlementConverter = new EntitlementConverter(ctx);
    private final ActivationConverter activationConverter = new ActivationConverter(ctx);

    private ResourceObjectAddOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            boolean skipExplicitUniquenessCheck) {
        super(ctx, scripts, connOptions);
        this.shadow = shadow;
        this.shadowClone = shadow.clone();
        this.skipExplicitUniquenessCheck = skipExplicitUniquenessCheck;
    }

    public static AsynchronousOperationReturnValue<ShadowType> execute(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            boolean skipExplicitUniquenessCheck,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        return new ResourceObjectAddOperation(ctx, shadow, scripts, connOptions, skipExplicitUniquenessCheck)
                .doExecute(result);
    }

    private AsynchronousOperationReturnValue<ShadowType> doExecute(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        LOGGER.trace("Adding resource object {}", shadow);

        ctx.checkExecutionFullyPersistent();
        ctx.checkProtectedObjectAddition(shadow, result);
        ctx.checkForCapability(CreateCapabilityType.class);

        if (!skipExplicitUniquenessCheck) {
            checkForAddConflictsForMultiConnectors(result);
        }

        executeProvisioningScripts(ProvisioningOperationTypeType.ADD, BeforeAfterType.BEFORE, result);

        ConnectorInstance connector = ctx.getConnector(CreateCapabilityType.class, result);
        AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> connectorAsyncOpRet;
        try {
            LOGGER.debug("PROVISIONING ADD operation on resource {}\n ADD object:\n{}\n",
                    ctx.getResource(), shadowClone.debugDumpLazily());

            entitlementConverter.transformToSubjectOpsOnAdd(shadowClone);
            activationConverter.transformOnAdd(shadowClone, result);

            connectorAsyncOpRet = connector.addObject(shadowClone.asPrismObject(), ctx.getUcfExecutionContext(), result);
            Collection<ResourceAttribute<?>> resourceAttributesAfterAdd = connectorAsyncOpRet.getReturnValue();

            LOGGER.debug("PROVISIONING ADD successful, returned attributes:\n{}",
                    SchemaDebugUtil.prettyPrintLazily(resourceAttributesAfterAdd));

            applyAfterOperationAttributes(resourceAttributesAfterAdd);
        } catch (CommunicationException ex) {
            throw communicationException(ctx, connector, ex);
        } catch (GenericFrameworkException ex) {
            throw genericConnectorException(ctx, connector, ex);
        } catch (ObjectAlreadyExistsException ex) {
            throw objectAlreadyExistsException("", ctx, connector, ex);
        } finally {
            b.shadowAuditHelper.auditEvent(AuditEventType.ADD_OBJECT, shadow, ctx, result);
        }

        executeEntitlementObjectsOperations(
                entitlementConverter.transformToObjectOpsOnAdd(shadowClone, result),
                result);

        LOGGER.trace("Added resource object {}", shadowClone);

        executeProvisioningScripts(ProvisioningOperationTypeType.ADD, BeforeAfterType.AFTER, result);

        computeResultStatus(result);

        // This should NOT be the cloned shadow: we need the original in order to retry the operation.
        AsynchronousOperationReturnValue<ShadowType> asyncOpRet = AsynchronousOperationReturnValue.wrap(shadow, result);
        asyncOpRet.setOperationType(connectorAsyncOpRet.getOperationType());
        return asyncOpRet;
    }

    /**
     * Special case for multi-connectors (e.g. semi-manual connectors). There is a possibility that the object
     * which we want to add is already present in the backing store. In case of manual provisioning the resource
     * itself will not indicate "already exist" error. We have to explicitly check for that.
     */
    private void checkForAddConflictsForMultiConnectors(OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        LOGGER.trace("Checking for add conflicts for {}", ShadowUtil.shortDumpShadowLazily(shadowClone));
        UcfResourceObject existingObject;
        ConnectorInstance readConnector = null;
        try {
            ConnectorInstance createConnector = ctx.getConnector(CreateCapabilityType.class, result);
            readConnector = ctx.getConnector(ReadCapabilityType.class, result);
            if (readConnector == createConnector) {
                // Same connector for reading and creating. We assume that the connector can check uniqueness itself.
                // No need to check explicitly. We will gladly skip the check, as the check may be additional overhead
                // that we normally do not need or want.
                return;
            }
            ResourceObjectIdentification<?> identification =
                    ResourceObjectIdentification.fromIncompleteShadow(ctx.getObjectDefinitionRequired(), shadowClone);

            if (identification instanceof ResourceObjectIdentification.WithPrimary primaryIdentification) {
                existingObject = readConnector.fetchObject(
                        primaryIdentification, null, ctx.getUcfExecutionContext(), result);
            } else {
                LOGGER.trace("No primary identifier present, skipping add conflict check for {}", identification);
                existingObject = null;
            }

        } catch (ObjectNotFoundException e) {
            // This is OK
            result.muteLastSubresultError();
            return;
        } catch (CommunicationException ex) {
            throw communicationException(ctx, readConnector, ex);
        } catch (GenericFrameworkException ex) {
            throw genericConnectorException(ctx, readConnector, ex);
        }
        if (existingObject == null) {
            LOGGER.trace("No add conflicts for {}", shadowClone);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Detected add conflict for {}, conflicting shadow: {}",
                        ShadowUtil.shortDumpShadow(shadowClone), existingObject.shortDump());
            }
            LOGGER.trace("Conflicting shadow:\n{}", existingObject.debugDumpLazily(1));
            throw new ObjectAlreadyExistsException(
                    String.format("Object %s already exists in the snapshot of %s as %s",
                            ShadowUtil.shortDumpShadow(shadowClone),
                            ctx.getResource(),
                            existingObject.shortDump()));
        }
    }

    /**
     * Attributes returned by the connector update the original shadow: they are either added (if not present before),
     * or they replace their previous versions.
     *
     * We must apply these on the original shadow, not the cloned one!
     * They need to be propagated outside ADD operation.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void applyAfterOperationAttributes(Collection<ResourceAttribute<?>> resourceAttributesAfterAdd) throws SchemaException {
        if (resourceAttributesAfterAdd == null) {
            return;
        }
        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
        for (ResourceAttribute attributeAfter : resourceAttributesAfterAdd) {
            ResourceAttribute attributeBefore = attributesContainer.findAttribute(attributeAfter.getElementName());
            if (attributeBefore != null) {
                attributesContainer.remove(attributeBefore);
            }
            if (!attributesContainer.contains(attributeAfter)) {
                attributesContainer.add(attributeAfter.clone());
            }
        }
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}
