/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.*;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.provisioning.api.GenericConnectorException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ShadowAttribute;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.ResourceOperationStatus;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

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

    /**
     * This is the object we obtained with the goal of adding to the resource. Here we also store the newly created attributes,
     * provided by the UCF (such as `icfs:uid`). Not into {@link #workingObject}!
     */
    @NotNull private final ResourceObjectShadow originalObject;

    /**
     * {@link #originalObject} cloned. The reason for cloning is that will modify the object sometimes
     * (e.g. for simulated capabilities or entitlements). We do not want the changes to propagate back
     * to the calling code.
     */
    @NotNull private final ResourceObjectShadow workingObject;

    private final boolean skipExplicitUniquenessCheck;

    private final EntitlementConverter entitlementConverter = new EntitlementConverter(ctx);
    private final ActivationConverter activationConverter = new ActivationConverter(ctx);

    private ResourceObjectAddOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectShadow object,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            boolean skipExplicitUniquenessCheck) {
        super(ctx, scripts, connOptions);
        this.originalObject = object;
        this.workingObject = object.clone();
        this.skipExplicitUniquenessCheck = skipExplicitUniquenessCheck;
    }

    public static @NotNull ResourceObjectAddReturnValue execute(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectShadow objectToAdd,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            boolean skipExplicitUniquenessCheck,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        return new ResourceObjectAddOperation(ctx, objectToAdd, scripts, connOptions, skipExplicitUniquenessCheck)
                .doExecute(result);
    }

    private @NotNull ResourceObjectAddReturnValue doExecute(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        LOGGER.trace("Adding resource object {}", workingObject);

        ctx.checkExecutionFullyPersistent();
        ctx.checkProtectedObjectAddition(workingObject);
        ctx.checkForCapability(CreateCapabilityType.class);

        if (!skipExplicitUniquenessCheck) {
            checkForAddConflictsForMultiConnectors(result);
        }

        executeProvisioningScripts(ProvisioningOperationTypeType.ADD, BeforeAfterType.BEFORE, result);

        var connector = ctx.getConnector(CreateCapabilityType.class, result);
        ResourceOperationStatus ucfOpStatus;
        try {
            LOGGER.debug("PROVISIONING ADD operation on resource {}\nADD object:\n{}\n",
                    ctx.getResource(), workingObject.debugDumpLazily(1));

            entitlementConverter.transformToSubjectOpsOnAdd(workingObject);
            activationConverter.transformOnAdd(workingObject, result);

            var ucfAddReturnValue = connector.addObject(workingObject.getPrismObject(), ctx.getUcfExecutionContext(), result);
            ucfOpStatus = ucfAddReturnValue.getOpStatus();

            var knownCreatedObjectAttributes = ucfAddReturnValue.getKnownCreatedObjectAttributes();

            LOGGER.debug("PROVISIONING ADD successful, returned attributes:\n{}",
                    SchemaDebugUtil.prettyPrintLazily(knownCreatedObjectAttributes));

            storeInto(workingObject, knownCreatedObjectAttributes); // For auditing and entitlements (object ops) processing.
            storeInto(originalObject, knownCreatedObjectAttributes); // This is returned to the caller.

            if (!ucfOpStatus.isInProgress()) {
                var volatileAttributes = fetchVolatileAttributes(result);

                LOGGER.debug("Fetched volatile attributes:\n{}", SchemaDebugUtil.prettyPrintLazily(volatileAttributes));

                storeInto(workingObject, volatileAttributes); // For auditing and entitlements (object ops) processing.
                storeInto(originalObject, volatileAttributes); // This is returned to the caller.
            }

        } catch (CommunicationException ex) {
            throw communicationException(ctx, connector, ex);
        } catch (GenericFrameworkException ex) {
            throw genericConnectorException(ctx, connector, ex);
        } catch (ObjectAlreadyExistsException ex) {
            throw objectAlreadyExistsException("", ctx, connector, ex);
        } finally {
            // Auditing as much as we can. (Raw data sent to the resource + returned attrs + fetched volatile ones.)
            b.shadowAuditHelper.auditEvent(AuditEventType.ADD_OBJECT, workingObject.getBean(), ctx, result);
        }

        // Working or original object? It should be almost the same, as (most probably) we need just the identifiers there.
        executeEntitlementObjectsOperations(
                entitlementConverter.transformToObjectOpsOnAdd(workingObject, result),
                result);

        executeProvisioningScripts(ProvisioningOperationTypeType.ADD, BeforeAfterType.AFTER, result);

        computeResultStatusAndAsyncOpReference(result);

        // This should NOT be the working object: we need the original in order to retry the operation.
        return ResourceObjectAddReturnValue.fromResult(originalObject, result, ucfOpStatus);
    }

    private Collection<ShadowAttribute<?, ?, ?, ?>> fetchVolatileAttributes(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException {

        var volatileAttributesDefinitions = ctx.getObjectDefinitionRequired().getAttributesVolatileOnAddOperation();
        if (volatileAttributesDefinitions.isEmpty()) {
            return List.of();
        }

        var volatileAttributesNames = volatileAttributesDefinitions.stream()
                .map(ShadowAttributeDefinition::getItemName)
                .collect(Collectors.toSet());

        if (!ctx.hasRealReadCapability()) {
            LOGGER.debug("Volatile attributes present but there's no read capability: not attempting to fetch them: {}",
                    volatileAttributesNames);
            return List.of();
        }

        var identification = originalObject.getIdentification();
        if (!(identification instanceof ResourceObjectIdentification.WithPrimary primaryIdentification)) {
            LOGGER.debug("Volatile attributes present but no primary identification: not attempting to fetch them: {}",
                    volatileAttributesNames);
            return List.of();
        }

        // We assume that we want to get all the volatile attributes. So, let's explicitly ask for all of them.
        // We hope we won't reach some resource limit here.
        var itemsToReturn = new ShadowItemsToReturn();
        itemsToReturn.setReturnDefaultAttributes(false);
        itemsToReturn.setItemsToReturn(volatileAttributesDefinitions);

        var connector = ctx.getConnector(ReadCapabilityType.class, result);
        UcfResourceObject ucfObject;
        try {
            ucfObject = connector.fetchObject(primaryIdentification, itemsToReturn, ctx.getUcfExecutionContext(), result);
            // Note on the error handling below: We consider any errors here just as serious as "regular" errors,
            // because if this fails, we may miss crucial information that could be needed for further processing.
            // When recovering, midPoint will probably try to re-create the object, and handle the eventual conflict
            // in the regular way.
        } catch (GenericFrameworkException e) {
            throw new GenericConnectorException(volatileAttributesFetchFailureMessage(primaryIdentification, e), e);
        } catch (CommonException e) {
            MiscUtil.throwAsSame(e, volatileAttributesFetchFailureMessage(primaryIdentification, e));
            throw e; // just to make compiler happy
        }

        var caseIgnore = ctx.isCaseIgnoreAttributeNames();
        return ucfObject.getAttributes().stream()
                .filter(attr ->
                        QNameUtil.contains(volatileAttributesNames, attr.getElementName(), caseIgnore))
                .toList();
    }

    private static String volatileAttributesFetchFailureMessage(ResourceObjectIdentification<?> identification, Exception e) {
        return "Couldn't fetch volatile attributes for " + identification + ": " + e.getMessage();
    }

    /**
     * Special case for multi-connectors (e.g. semi-manual connectors). There is a possibility that the object
     * which we want to add is already present in the backing store. In case of manual provisioning the resource
     * itself will not indicate "already exist" error. We have to explicitly check for that.
     */
    private void checkForAddConflictsForMultiConnectors(OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        LOGGER.trace("Checking for add conflicts for {}", workingObject.shortDumpLazily());
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
            ResourceObjectIdentification<?> identification = workingObject.getIdentification();
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
            LOGGER.trace("No add conflicts for {}", workingObject);
        } else {
            LOGGER.debug("Detected add conflict for {}, conflicting shadow: {}",
                    workingObject.shortDumpLazily(), existingObject.shortDumpLazily());
            LOGGER.trace("Conflicting shadow:\n{}", existingObject.debugDumpLazily(1));
            throw new ObjectAlreadyExistsException(
                    String.format("Object %s already exists in the snapshot of %s as %s",
                            workingObject.shortDump(),
                            ctx.getResource(),
                            existingObject.shortDump()));
        }
    }

    /**
     * Attributes returned by the connector update the original shadow: they are either added (if not present before),
     * or they replace their previous versions.
     */
    private void storeInto(ResourceObjectShadow object, Collection<ShadowAttribute<?, ?, ?, ?>> knownCreatedObjectAttributes)
            throws SchemaException {
        ShadowAttributesContainer targetAttrContainer = object.getAttributesContainer();
        for (var createdAttribute : emptyIfNull(knownCreatedObjectAttributes)) {

            targetAttrContainer.removeAttribute(createdAttribute.getElementName());

            // This clone is necessary (1) because the method above does not unset the parent in the PrismValue being removed
            // (this should be fixed), but also (2) because we put the attribute into more objects.
            var clone = createdAttribute.clone();
            clone.applyDefinitionFrom(object.getObjectDefinition());

            targetAttrContainer.addAttribute(clone);
        }
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}
