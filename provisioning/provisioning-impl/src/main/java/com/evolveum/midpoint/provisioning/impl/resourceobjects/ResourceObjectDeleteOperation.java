/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.*;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfDeleteResult;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.DeleteCapabilityType;

import org.jetbrains.annotations.NotNull;

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
    private final RepoShadow shadow;

    private ResourceObjectDeleteOperation(
            ProvisioningContext ctx,
            RepoShadow shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions) {
        super(ctx, scripts, connOptions);
        this.ctx = ctx;
        this.shadow = shadow;
    }

    static @NotNull ResourceObjectDeleteResult execute(
            ProvisioningContext ctx,
            RepoShadow shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        return new ResourceObjectDeleteOperation(ctx, shadow, scripts, connOptions)
                .doExecute(result);
    }

    private @NotNull ResourceObjectDeleteResult doExecute(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        LOGGER.trace("Deleting resource object {}", shadow);

        ctx.checkExecutionFullyPersistent();
        ctx.checkForCapability(DeleteCapabilityType.class);
        ctx.checkProtectedObjectDeletion(shadow);

        executeProvisioningScripts(ProvisioningOperationTypeType.DELETE, BeforeAfterType.BEFORE, result);

        determineAndExecuteEntitlementObjectOperations(result);

        ResourceObjectIdentification<?> identification = shadow.getIdentificationRequired();

        ConnectorInstance connector = ctx.getConnector(DeleteCapabilityType.class, result);
        UcfDeleteResult ucfDeleteResult;
        try {

            LOGGER.debug(
                    "PROVISIONING DELETE operation on {}\n DELETE object, object class {}, identified by:\n{}",
                    ctx.getResource(), shadow.getObjectClassName(),
                    identification);

            ucfDeleteResult =
                    connector.deleteObject(
                            identification, shadow.getPrismObject(), ctx.getUcfExecutionContext(), result);
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
            b.shadowAuditHelper.auditEvent(AuditEventType.DELETE_OBJECT, shadow.getBean(), ctx, result);
        }

        LOGGER.trace("Deleted resource object {}", shadow);

        executeProvisioningScripts(ProvisioningOperationTypeType.DELETE, BeforeAfterType.AFTER, result);

        computeResultStatusAndAsyncOpReference(result);

        LOGGER.debug("PROVISIONING DELETE result: {}", result.getStatus());

        var rv = ResourceObjectDeleteResult.fromResult(result, ucfDeleteResult.getOperationType());
        updateQuantum(ctx, connector, rv, result); // The result is not closed, even if its status is set. So we use it.
        return rv;
    }

    private void determineAndExecuteEntitlementObjectOperations(OperationResult result) throws SchemaException {
        try {
            executeEntitlementObjectsOperations(
                    new EntitlementConverter(ctx).transformToObjectOpsOnSubjectDelete(shadow, result),
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
