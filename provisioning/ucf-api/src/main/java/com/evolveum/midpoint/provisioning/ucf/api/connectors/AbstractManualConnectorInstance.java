/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api.connectors;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationQueryable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

/**
 * Common abstract superclass for all manual connectors. There are connectors that do not
 * talk to the resource directly. They rather rely on a human to manually execute the
 * modification task. These connectors are efficiently write-only.
 *
 * @author Radovan Semancik
 */
@ManagedConnector
@Experimental
public abstract class AbstractManualConnectorInstance
        extends AbstractManagedConnectorInstance
        implements AsynchronousOperationQueryable {

    private static final String OPERATION_ADD = AbstractManualConnectorInstance.class.getName() + ".addObject";
    private static final String OPERATION_MODIFY = AbstractManualConnectorInstance.class.getName() + ".modifyObject";
    private static final String OPERATION_DELETE = AbstractManualConnectorInstance.class.getName() + ".deleteObject";

    // test(), connect() and dispose() are lifecycle operations to be implemented in the subclasses

    // Operations to be implemented in the subclasses. These operations create the tickets.

    protected abstract String createTicketAdd(PrismObject<? extends ShadowType> object, Task task, OperationResult result) throws CommunicationException,
                GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException;

    protected abstract String createTicketModify(ResourceObjectDefinition objectDefinition,
            PrismObject<ShadowType> shadow, Collection<? extends ShadowSimpleAttribute<?>> identifiers, String resourceOid, Collection<Operation> changes,
            Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, ObjectAlreadyExistsException, ConfigurationException;

    protected abstract String createTicketDelete(ResourceObjectDefinition objectDefinition,
            PrismObject<ShadowType> shadow, Collection<? extends ShadowSimpleAttribute<?>> identifiers, String resourceOid,
            Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, ConfigurationException;

    @Override
    public @NotNull UcfAddReturnValue addObject(
            @NotNull PrismObject<? extends ShadowType> object,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ObjectAlreadyExistsException,
            ConfigurationException {

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        OperationResult result = parentResult.createSubresult(OPERATION_ADD);
        try {

            InternalMonitor.recordConnectorOperation("add");
            InternalMonitor.recordConnectorModification("add");

            var ticketIdentifier = createTicketAdd(object, ctx.getTask(), result);

            result.recordInProgress();
            result.setAsynchronousOperationReference(ticketIdentifier);

        } catch (CommunicationException | GenericFrameworkException | SchemaException |
                ObjectAlreadyExistsException | ConfigurationException | RuntimeException | Error e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }

        return UcfAddReturnValue.fromResult(result, PendingOperationTypeType.MANUAL);
    }

    @Override
    public @NotNull UcfModifyReturnValue modifyObject(
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            PrismObject<ShadowType> shadow,
            @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, ObjectAlreadyExistsException, ConfigurationException {

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        OperationResult result = parentResult.createSubresult(OPERATION_MODIFY);
        try {

            InternalMonitor.recordConnectorOperation("modify");
            InternalMonitor.recordConnectorModification("modify");

            var ticketIdentifier = createTicketModify(
                    identification.getResourceObjectDefinition(),
                    shadow,
                    identification.getAllIdentifiersAsAttributes(),
                    ctx.getResourceOid(),
                    changes,
                    ctx.getTask(),
                    result);

            result.recordInProgress();
            result.setAsynchronousOperationReference(ticketIdentifier);

        } catch (ObjectNotFoundException | CommunicationException | GenericFrameworkException | SchemaException |
                ObjectAlreadyExistsException | ConfigurationException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        } finally {
            result.close();
        }

        return UcfModifyReturnValue.fromResult(result, PendingOperationTypeType.MANUAL);
    }


    @Override
    public @NotNull UcfDeleteResult deleteObject(
            @NotNull ResourceObjectIdentification<?> identification,
            PrismObject<ShadowType> shadow,
            @NotNull UcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException,
            GenericFrameworkException, SchemaException, ConfigurationException {

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        OperationResult result = parentResult.createSubresult(OPERATION_DELETE);
        try {

            InternalMonitor.recordConnectorOperation("delete");
            InternalMonitor.recordConnectorModification("delete");

            var ticketIdentifier = createTicketDelete(
                    identification.getResourceObjectDefinition(),
                    shadow,
                    identification.getAllIdentifiersAsAttributes(),
                    ctx.getResourceOid(),
                    ctx.getTask(),
                    result);

            result.recordInProgress();
            result.setAsynchronousOperationReference(ticketIdentifier);

        } catch (ObjectNotFoundException | CommunicationException | GenericFrameworkException | SchemaException |
                ConfigurationException | RuntimeException | Error e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }

        return UcfDeleteResult.fromResult(result, PendingOperationTypeType.MANUAL);
    }

    @Override
    public @NotNull CapabilityCollectionType fetchCapabilities(OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("capabilities");
        return new CapabilityCollectionType()
                .read(new ReadCapabilityType()
                        .cachingOnly(true))
                .create(new CreateCapabilityType()
                        .manual(true))
                .update(new UpdateCapabilityType()
                        .manual(true)
                        .addRemoveAttributeValues(true))
                .delete(new DeleteCapabilityType()
                        .manual(true))
                .activation(new ActivationCapabilityType()
                        .status(new ActivationStatusCapabilityType()))
                .credentials(new CredentialsCapabilityType()
                        .password(new PasswordCapabilityType()));
    }

    @Override
    public UcfResourceObject fetchObject(
            @NotNull ResourceObjectIdentification.WithPrimary resourceObjectIdentification,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("fetchObject");
        // Read operations are not supported. We cannot really manually read the content of an off-line resource.
        return null;
    }

    @Override
    public SearchResultMetadata search(
            @NotNull ResourceObjectDefinition objectDefinition,
            @Nullable ObjectQuery query,
            @NotNull UcfObjectHandler handler,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @Nullable PagedSearchCapabilityType pagedSearchConfiguration,
            @Nullable SearchHierarchyConstraints searchHierarchyConstraints,
            @Nullable UcfFetchErrorReportingMethod errorReportingMethod,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("search");
        // Read operations are not supported. We cannot really manually read the content of an off-line resource.
        return null;
    }

    @Override
    public int count(ResourceObjectDefinition objectDefinition, ObjectQuery query,
            PagedSearchCapabilityType pagedSearchConfigurationType,
            UcfExecutionContext ctx, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("count");
        // Read operations are not supported. We cannot really manually read the content of an off-line resource.
        return 0;
    }

    @Override
    public ConnectorOperationalStatus getOperationalStatus() {
        ConnectorOperationalStatus opStatus = new ConnectorOperationalStatus();
        opStatus.setConnectorClassName(this.getClass().getName());
        return opStatus;
    }

    @Override
    public NativeResourceSchema fetchResourceSchema(@NotNull OperationResult parentResult) {
        // Schema discovery is not supported. Schema must be defined manually. Or other connector has to provide it.
        InternalMonitor.recordConnectorOperation("schema");
        return null;
    }

    @Override
    public UcfFetchChangesResult fetchChanges(
            @Nullable ResourceObjectDefinition objectDefinition,
            @Nullable UcfSyncToken lastToken,
            @Nullable ShadowItemsToReturn attrsToReturn,
            @Nullable Integer maxChanges,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull UcfLiveSyncChangeListener changeHandler,
            @NotNull OperationResult parentResult) {
        // not supported
        return null;
    }

    @Override
    public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation,
            UcfExecutionContext ctx, OperationResult parentResult) {
        // not supported
        return null;
    }
}
