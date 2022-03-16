/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api.connectors;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationQueryable;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.provisioning.ucf.api.UcfExecutionContext;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AbstractWriteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.DeleteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private static final com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory CAPABILITY_OBJECT_FACTORY
    = new com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory();

    // test(), connect() and dispose() are lifecycle operations to be implemented in the subclasses

    // Operations to be implemented in the subclasses. These operations create the tickets.

    protected abstract String createTicketAdd(PrismObject<? extends ShadowType> object, Task task, OperationResult result) throws CommunicationException,
                GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException;

    protected abstract String createTicketModify(ResourceObjectDefinition objectDefinition,
            PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, String resourceOid, Collection<Operation> changes,
            Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, ObjectAlreadyExistsException, ConfigurationException;

    protected abstract String createTicketDelete(ResourceObjectDefinition objectDefinition,
            PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, String resourceOid,
            Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, ConfigurationException;

    @Override
    public AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(
            PrismObject<? extends ShadowType> object,
            UcfExecutionContext ctx, OperationResult parentResult) throws CommunicationException,
            GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {

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

        AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> ret = new AsynchronousOperationReturnValue<>();
        ret.setOperationType(PendingOperationTypeType.MANUAL);
        ret.setOperationResult(result);
        return ret;
    }


    @Override
    public AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyObject(
            ResourceObjectIdentification identification,
            PrismObject<ShadowType> shadow,
            @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options,
            UcfExecutionContext ctx, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, ObjectAlreadyExistsException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(OPERATION_MODIFY);

        InternalMonitor.recordConnectorOperation("modify");
        InternalMonitor.recordConnectorModification("modify");

        String ticketIdentifier;

        try {

            ticketIdentifier = createTicketModify(
                    identification.getResourceObjectDefinition(),
                    shadow, identification.getAllIdentifiers(),
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

        AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> ret = new AsynchronousOperationReturnValue<>();
        ret.setOperationType(PendingOperationTypeType.MANUAL);
        ret.setOperationResult(result);
        return ret;
    }


    @Override
    public AsynchronousOperationResult deleteObject(ResourceObjectDefinition objectDefinition,
            PrismObject<ShadowType> shadow,
            Collection<? extends ResourceAttribute<?>> identifiers,
            UcfExecutionContext ctx, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
            GenericFrameworkException, SchemaException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(OPERATION_DELETE);

        InternalMonitor.recordConnectorOperation("delete");
        InternalMonitor.recordConnectorModification("delete");

        String ticketIdentifier;

        try {

            ticketIdentifier = createTicketDelete(
                    objectDefinition,
                    shadow,
                    identifiers,
                    ctx.getResourceOid(),
                    ctx.getTask(),
                    result);

        } catch (ObjectNotFoundException | CommunicationException | GenericFrameworkException | SchemaException |
                ConfigurationException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        result.recordInProgress();
        result.setAsynchronousOperationReference(ticketIdentifier);

        AsynchronousOperationResult ret = AsynchronousOperationResult.wrap(result);
        ret.setOperationType(PendingOperationTypeType.MANUAL);
        return ret;
    }

    @Override
    public Collection<Object> fetchCapabilities(OperationResult parentResult) {
        Collection<Object> capabilities = new ArrayList<>();

        InternalMonitor.recordConnectorOperation("capabilities");

        // caching-only read capabilities
        ReadCapabilityType readCap = new ReadCapabilityType();
        readCap.setCachingOnly(true);
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createRead(readCap));

        CreateCapabilityType createCap = new CreateCapabilityType();
        setManual(createCap);
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createCreate(createCap));

        UpdateCapabilityType updateCap = new UpdateCapabilityType();
        updateCap.setAddRemoveAttributeValues(true);
        setManual(updateCap);
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createUpdate(updateCap));

        DeleteCapabilityType deleteCap = new DeleteCapabilityType();
        setManual(deleteCap);
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createDelete(deleteCap));

        ActivationCapabilityType activationCap = new ActivationCapabilityType();
        ActivationStatusCapabilityType activationStatusCap = new ActivationStatusCapabilityType();
        activationCap.setStatus(activationStatusCap);
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createActivation(activationCap));

        CredentialsCapabilityType credentialsCap = new CredentialsCapabilityType();
        PasswordCapabilityType passwordCapabilityType = new PasswordCapabilityType();
        credentialsCap.setPassword(passwordCapabilityType);
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createCredentials(credentialsCap));

        return capabilities;
    }

    private void setManual(AbstractWriteCapabilityType cap) {
        cap.setManual(true);
    }

    @Override
    public PrismObject<ShadowType> fetchObject(ResourceObjectIdentification resourceObjectIdentification, AttributesToReturn attributesToReturn,
            UcfExecutionContext ctx, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("fetchObject");
        // Read operations are not supported. We cannot really manually read the content of an off-line resource.
        return null;
    }

    @Override
    public SearchResultMetadata search(
            @NotNull ResourceObjectDefinition objectDefinition, ObjectQuery query,
            @NotNull UcfObjectHandler handler, @Nullable AttributesToReturn attributesToReturn,
            @Nullable PagedSearchCapabilityType pagedSearchConfiguration,
            @Nullable SearchHierarchyConstraints searchHierarchyConstraints,
            @Nullable UcfFetchErrorReportingMethod errorReportingMethod,
            @NotNull UcfExecutionContext ctx, @NotNull OperationResult parentResult) {
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
    public ResourceSchema fetchResourceSchema(OperationResult parentResult) {
        // Schema discovery is not supported. Schema must be defined manually. Or other connector has to provide it.
        InternalMonitor.recordConnectorOperation("schema");
        return null;
    }

    @Override
    public UcfFetchChangesResult fetchChanges(ResourceObjectDefinition objectDefinition, UcfSyncToken lastToken,
                                              AttributesToReturn attrsToReturn, Integer maxChanges, UcfExecutionContext ctx,
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
