/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.ShadowAttribute;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.*;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

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

    /** This is the object we obtained with the goal of adding to the resource. */
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

    public static ResourceObjectAddReturnValue execute(
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

    private ResourceObjectAddReturnValue doExecute(OperationResult result)
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

        ConnectorInstance connector = ctx.getConnector(CreateCapabilityType.class, result);
        UcfAddReturnValue ucfAddReturnValue;
        try {
            LOGGER.debug("PROVISIONING ADD operation on resource {}\nADD object:\n{}\n",
                    ctx.getResource(), workingObject.debugDumpLazily(1));

            entitlementConverter.transformToSubjectOpsOnAdd(workingObject);
            activationConverter.transformOnAdd(workingObject, result);

            ucfAddReturnValue = connector.addObject(workingObject.getPrismObject(), ctx.getUcfExecutionContext(), result);
            var knownCreatedObjectAttributes = ucfAddReturnValue.getKnownCreatedObjectAttributes();

            LOGGER.debug("PROVISIONING ADD successful, returned attributes:\n{}",
                    SchemaDebugUtil.prettyPrintLazily(knownCreatedObjectAttributes));

            storeIntoOriginalObject(knownCreatedObjectAttributes);
        } catch (CommunicationException ex) {
            throw communicationException(ctx, connector, ex);
        } catch (GenericFrameworkException ex) {
            throw genericConnectorException(ctx, connector, ex);
        } catch (ObjectAlreadyExistsException ex) {
            throw objectAlreadyExistsException("", ctx, connector, ex);
        } finally {
            b.shadowAuditHelper.auditEvent(AuditEventType.ADD_OBJECT, originalObject.getBean(), ctx, result);
        }

        // TODO should we execute the entitlement operations using the working or original object?
        //  Working object would be fine if we could be sure it contains all the generated attributes.
        //  (If they are needed for group membership operations.)
        executeEntitlementObjectsOperations(
                entitlementConverter.transformToObjectOpsOnAdd(workingObject, result),
                result);

        executeProvisioningScripts(ProvisioningOperationTypeType.ADD, BeforeAfterType.AFTER, result);

        computeResultStatus(result);

        // This should NOT be the working object: we need the original in order to retry the operation.
        return ResourceObjectAddReturnValue.of(originalObject, result, ucfAddReturnValue.getOperationType());
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Detected add conflict for {}, conflicting shadow: {}",
                        workingObject.shortDump(), existingObject.shortDump());
            }
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
     *
     * We must apply these on the original shadow, not the cloned one!
     * They need to be propagated outside ADD operation.
     */
    private void storeIntoOriginalObject(Collection<ShadowAttribute<?, ?, ?, ?>> knownCreatedObjectAttributes)
            throws SchemaException {
        ShadowAttributesContainer targetAttrContainer = originalObject.getAttributesContainer();
        for (var addedAttribute : emptyIfNull(knownCreatedObjectAttributes)) {

            targetAttrContainer.removeProperty(addedAttribute.getElementName());

            // must be cloned because the method above does not unset the parent in the PrismValue being removed (should be fixed)
            var clone = addedAttribute.clone();
            clone.applyDefinitionFrom(originalObject.getObjectDefinition());

            targetAttrContainer.addAttribute(clone);
        }
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}
