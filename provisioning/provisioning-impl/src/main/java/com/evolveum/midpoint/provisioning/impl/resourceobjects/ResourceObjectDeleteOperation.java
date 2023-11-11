/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.DeleteCapabilityType;

/**
 * Responsibilities:
 *
 * . execute DELETE operation on the resource, including before/after scripts
 * . standard checks (execution persistence, capability presence, protected object)
 * . induced entitlement changes
 */
class ResourceObjectDeleteOperation extends ResourceObjectProvisioningOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectDeleteOperation.class);

    private final ProvisioningContext ctx;
    private final ShadowType shadow;

    private ResourceObjectDeleteOperation(
            ProvisioningContext ctx,
            ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions) {
        super(ctx, scripts, connOptions);
        this.ctx = ctx;
        this.shadow = shadow;
    }

    static AsynchronousOperationResult execute(
            ProvisioningContext ctx,
            ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        return new ResourceObjectDeleteOperation(ctx, shadow, scripts, connOptions)
                .doExecute(result);
    }

    private AsynchronousOperationResult doExecute(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        LOGGER.trace("Deleting resource object {}", shadow);

        ctx.checkExecutionFullyPersistent();
        ctx.checkForCapability(DeleteCapabilityType.class);
        ctx.checkProtectedObjectDeletion(shadow, result);

        executeProvisioningScripts(ProvisioningOperationTypeType.DELETE, BeforeAfterType.BEFORE, result);

        determineAndExecuteEntitlementObjectOperations(result);

        ResourceObjectIdentification<?> identification = getIdentification();

        ConnectorInstance connector = ctx.getConnector(DeleteCapabilityType.class, result);
        AsynchronousOperationResult connectorAsyncOpRet;
        try {

            LOGGER.debug(
                    "PROVISIONING DELETE operation on {}\n DELETE object, object class {}, identified by:\n{}",
                    ctx.getResource(), shadow.getObjectClass(),
                    identification);

            connectorAsyncOpRet =
                    connector.deleteObject(
                            identification, shadow.asPrismObject(), ctx.getUcfExecutionContext(), result);
        } catch (ObjectNotFoundException ex) {
            throw ex.wrap(String.format(
                    "An error occurred while deleting resource object %s with identifiers %s (%s)",
                    shadow, identification, ctx.getExceptionDescription(connector)));
        } catch (CommunicationException ex) {
            throw communicationException(ctx, connector, ex);
        } catch (GenericFrameworkException ex) {
            throw genericConnectorException(ctx, connector, ex);
        } catch (ConfigurationException ex) {
            throw configurationException(ctx, connector, ex);
        } finally {
            b.shadowAuditHelper.auditEvent(AuditEventType.DELETE_OBJECT, shadow, ctx, result);
        }

        LOGGER.trace("Deleted resource object {}", shadow);

        executeProvisioningScripts(ProvisioningOperationTypeType.DELETE, BeforeAfterType.AFTER, result);

        computeResultStatus(result);
        LOGGER.debug("PROVISIONING DELETE result: {}", result.getStatus());

        AsynchronousOperationResult aResult = AsynchronousOperationResult.wrap(result);
        updateQuantum(ctx, connector, aResult, result); // The result is not closed, even if its status is set. So we use it.
        if (connectorAsyncOpRet != null) {
            aResult.setOperationType(connectorAsyncOpRet.getOperationType());
        }
        return aResult;
    }

    private @NotNull ResourceObjectIdentification<?> getIdentification()
            throws SchemaException, ConfigurationException {
        if (ShadowUtil.isAttributesContainerRaw(shadow)) {
            // This could occur if shadow was re-read during op state processing
            ctx.applyAttributesDefinition(shadow);
        }
        return ResourceObjectIdentification.fromIncompleteShadow( // maybe we could require complete shadow here
                ctx.getObjectDefinitionRequired(), shadow);
    }

    private void determineAndExecuteEntitlementObjectOperations(OperationResult result) throws SchemaException {
        try {
            executeEntitlementObjectsOperations(
                    new EntitlementConverter(ctx).transformToObjectOpsOnDelete(shadow, result),
                    result);
        } catch (SchemaException | Error e) {
            throw e; // These we want to propagate.
        } catch (CommunicationException | ObjectNotFoundException | SecurityViolationException | ConfigurationException |
                ObjectAlreadyExistsException | ExpressionEvaluationException | RuntimeException e) {
            // Now just log the errors, but do NOT re-throw the exception (except for some exceptions).
            // We want the original delete to take place, throwing an exception would spoil that.
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}
