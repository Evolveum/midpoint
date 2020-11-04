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
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.SearchHierarchyConstraints;
import com.evolveum.midpoint.schema.result.AsynchronousOperationQueryable;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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

    private static final Trace LOGGER = TraceManager.getTrace(AbstractManualConnectorInstance.class);

    // test(), connect() and dispose() are lifecycle operations to be implemented in the subclasses

    // Operations to be implemented in the subclasses. These operations create the tickets.

    protected abstract String createTicketAdd(PrismObject<? extends ShadowType> object, OperationResult result) throws CommunicationException,
                GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException;

    protected abstract String createTicketModify(ObjectClassComplexTypeDefinition objectClass,
            PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, String resourceOid, Collection<Operation> changes,
            OperationResult result) throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            ObjectAlreadyExistsException, ConfigurationException;

    protected abstract String createTicketDelete(ObjectClassComplexTypeDefinition objectClass,
            PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, String resourceOid, OperationResult result)
                    throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
                        ConfigurationException;

    @Override
    public AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(
            PrismObject<? extends ShadowType> object,
            StateReporter reporter, OperationResult parentResult) throws CommunicationException,
            GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(OPERATION_ADD);

        String ticketIdentifier;

        InternalMonitor.recordConnectorOperation("add");
        InternalMonitor.recordConnectorModification("add");

        try {

            ticketIdentifier = createTicketAdd(object, result);

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
            Collection<Operation> changes,
            ConnectorOperationOptions options,
            StateReporter reporter, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(OPERATION_MODIFY);

        InternalMonitor.recordConnectorOperation("modify");
        InternalMonitor.recordConnectorModification("modify");

        String ticketIdentifier = null;

        try {

            ticketIdentifier = createTicketModify(identification.getObjectClassDefinition(), shadow, identification.getAllIdentifiers(), reporter.getResourceOid(), changes, result);

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
    public AsynchronousOperationResult deleteObject(ObjectClassComplexTypeDefinition objectClass,
            PrismObject<ShadowType> shadow,
            Collection<? extends ResourceAttribute<?>> identifiers, StateReporter reporter,
            OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
            GenericFrameworkException, SchemaException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(OPERATION_DELETE);

        InternalMonitor.recordConnectorOperation("delete");
        InternalMonitor.recordConnectorModification("delete");

        String ticketIdentifier;

        try {

            ticketIdentifier = createTicketDelete(objectClass, shadow, identifiers, reporter.getResourceOid(), result);

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
    public Collection<Object> fetchCapabilities(OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException {
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
            StateReporter reporter, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, SecurityViolationException, ConfigurationException {
        InternalMonitor.recordConnectorOperation("fetchObject");
        // Read operations are not supported. We cannot really manually read the content of an off-line resource.
        return null;
    }

    @Override
    public SearchResultMetadata search(
            ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
            ShadowResultHandler handler, AttributesToReturn attributesToReturn,
            PagedSearchCapabilityType pagedSearchConfigurationType,
            SearchHierarchyConstraints searchHierarchyConstraints, StateReporter reporter,
            OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
            SchemaException, SecurityViolationException, ObjectNotFoundException {
        InternalMonitor.recordConnectorOperation("search");
        // Read operations are not supported. We cannot really manually read the content of an off-line resource.
        return null;
    }

    @Override
    public int count(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
            PagedSearchCapabilityType pagedSearchConfigurationType, StateReporter reporter,
            OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
            SchemaException, UnsupportedOperationException {
        InternalMonitor.recordConnectorOperation("count");
        // Read operations are not supported. We cannot really manually read the content of an off-line resource.
        return 0;
    }


    @Override
    public ConnectorOperationalStatus getOperationalStatus() throws ObjectNotFoundException {
        ConnectorOperationalStatus opstatus = new ConnectorOperationalStatus();
        opstatus.setConnectorClassName(this.getClass().getName());
        return opstatus;
    }

    @Override
    public ResourceSchema fetchResourceSchema(OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException {
        // Schema discovery is not supported. Schema must be defined manually. Or other connector has to provide it.
        InternalMonitor.recordConnectorOperation("schema");
        return null;
    }

    @Override
    public <T> PrismProperty<T> fetchCurrentToken(ObjectClassComplexTypeDefinition objectClass,
            StateReporter reporter, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException {
        // not supported
        return null;
    }

    @Override
    public void fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken,
            AttributesToReturn attrsToReturn, Integer maxChanges, StateReporter reporter,
            LiveSyncChangeListener changeHandler, OperationResult parentResult) {
        // not supported
    }

    @Override
    public PrismProperty<?> deserializeToken(Object serializedToken) {
        // not supported
        return null;
    }

    @Override
    public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, StateReporter reporter,
            OperationResult parentResult) throws CommunicationException, GenericFrameworkException {
        // not supported
        return null;
    }

}
