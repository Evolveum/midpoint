/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api.connectors;

import java.util.Collection;
import java.util.List;

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
public abstract class AbstractManualConnectorInstance extends AbstractManagedConnectorInstance implements AsynchronousOperationQueryable {

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
    public UcfAddReturnValue addObject(
            PrismObject<? extends ShadowType> object,
            UcfExecutionContext ctx, OperationResult parentResult) throws CommunicationException,
            GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        OperationResult result = parentResult.createSubresult(OPERATION_ADD);

        String ticketIdentifier;

        InternalMonitor.recordConnectorOperation("add");
        InternalMonitor.recordConnectorModification("add");

        try {

            ticketIdentifier = createTicketAdd(object, ctx.getTask(), result);

        } catch (CommunicationException | GenericFrameworkException | SchemaException |
                ObjectAlreadyExistsException | ConfigurationException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        result.recordInProgress();
        result.setAsynchronousOperationReference(ticketIdentifier);

        return UcfAddReturnValue.of(
                List.of(), result, PendingOperationTypeType.MANUAL);
    }

    @Override
    public @NotNull UcfModifyReturnValue modifyObject(
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            PrismObject<ShadowType> shadow,
            @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options,
            UcfExecutionContext ctx, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, ObjectAlreadyExistsException, ConfigurationException {

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        OperationResult result = parentResult.createSubresult(OPERATION_MODIFY);

        InternalMonitor.recordConnectorOperation("modify");
        InternalMonitor.recordConnectorModification("modify");

        String ticketIdentifier;

        try {

            ticketIdentifier = createTicketModify(
                    identification.getResourceObjectDefinition(),
                    shadow,
                    identification.getAllIdentifiersAsAttributes(),
                    ctx.getResourceOid(),
                    changes,
                    ctx.getTask(),
                    result);

        } catch (ObjectNotFoundException | CommunicationException | GenericFrameworkException | SchemaException |
                ObjectAlreadyExistsException | ConfigurationException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        result.recordInProgress();
        result.setAsynchronousOperationReference(ticketIdentifier);

        return UcfModifyReturnValue.of(
                List.of(), result, PendingOperationTypeType.MANUAL);
    }


    @Override
    public UcfDeleteReturnValue deleteObject(
            @NotNull ResourceObjectIdentification<?> identification,
            PrismObject<ShadowType> shadow,
            UcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException,
            GenericFrameworkException, SchemaException, ConfigurationException {

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        OperationResult result = parentResult.createSubresult(OPERATION_DELETE);

        InternalMonitor.recordConnectorOperation("delete");
        InternalMonitor.recordConnectorModification("delete");

        String ticketIdentifier;

        try {

            ticketIdentifier = createTicketDelete(
                    identification.getResourceObjectDefinition(),
                    shadow,
                    identification.getAllIdentifiersAsAttributes(),
                    ctx.getResourceOid(),
                    ctx.getTask(),
                    result);

        } catch (ObjectNotFoundException | CommunicationException | GenericFrameworkException | SchemaException |
                ConfigurationException | RuntimeException | Error e) {
            result.recordException(e);
            throw e;
        }

        result.recordInProgress();
        result.setAsynchronousOperationReference(ticketIdentifier);

        return UcfDeleteReturnValue.of(result, PendingOperationTypeType.MANUAL);
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
            ResourceObjectIdentification.WithPrimary resourceObjectIdentification, ShadowItemsToReturn shadowItemsToReturn,
            UcfExecutionContext ctx, OperationResult parentResult) {
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
            @NotNull UcfExecutionContext ctx,
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
    public NativeResourceSchema fetchResourceSchema(OperationResult parentResult) {
        // Schema discovery is not supported. Schema must be defined manually. Or other connector has to provide it.
        InternalMonitor.recordConnectorOperation("schema");
        return null;
    }

    @Override
    public UcfFetchChangesResult fetchChanges(ResourceObjectDefinition objectDefinition, UcfSyncToken lastToken,
            ShadowItemsToReturn attrsToReturn, Integer maxChanges, UcfExecutionContext ctx,
            @NotNull UcfLiveSyncChangeListener changeHandler, OperationResult parentResult) {
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
