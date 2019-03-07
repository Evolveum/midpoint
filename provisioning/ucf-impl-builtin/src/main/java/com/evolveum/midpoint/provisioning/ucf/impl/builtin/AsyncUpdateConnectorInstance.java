/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManagedConnectorInstance;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import javax.xml.namespace.QName;
import java.util.*;

/**
 *  Connector that is able to obtain and process asynchronous updates.
 *  It can be used to receive messages from JMS or AMQP messaging systems; or maybe from REST calls in the future.
 */
@ManagedConnector(type="AsyncUpdateConnector", version="1.0.0")
public class AsyncUpdateConnectorInstance extends AbstractManagedConnectorInstance {
	
	private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdateConnectorInstance.class);

	private static final com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory CAPABILITY_OBJECT_FACTORY
			= new com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory();

	private AsyncUpdateConnectorConfiguration configuration;

	private boolean connected = false;

	private ChangeListener changeListener;

	@ManagedConnectorConfiguration
	public AsyncUpdateConnectorConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(AsyncUpdateConnectorConfiguration configuration) {
		this.configuration = configuration;
	}

	public boolean isConnected() {
		return connected;
	}

	@Override
	protected void connect(OperationResult result) {
		if (connected && InternalsConfig.isSanityChecks()) {
			throw new IllegalStateException("Double connect in "+this);
		}
		connected = true;
		// Nothing else to do
	}
	@Override
	public void test(OperationResult parentResult) {
		OperationResult connectionResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
		connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, AsyncUpdateConnectorInstance.class);
		connectionResult.addContext("connector", getConnectorObject().toString());

		if (!connected && InternalsConfig.isSanityChecks()) {
			throw new IllegalStateException("Attempt to test non-connected connector instance "+this);
		}

		// TODO try to connect to the message source
		
		connectionResult.recordSuccess();
	}

	@Override
	public void dispose() {
		// Nothing to dispose
		connected = false;
		stopListening();
	}

	@Override
	public ConnectorOperationalStatus getOperationalStatus() {
		ConnectorOperationalStatus status = new ConnectorOperationalStatus();
		status.setConnectorClassName(this.getClass().getName());
		return status;
	}

	@Override
	public Collection<Object> fetchCapabilities(OperationResult parentResult) {
		InternalMonitor.recordConnectorOperation("capabilities");

		Collection<Object> capabilities = new ArrayList<>();
		capabilities.add(CAPABILITY_OBJECT_FACTORY.createAsyncUpdate(new AsyncUpdateCapabilityType()));
		capabilities.add(CAPABILITY_OBJECT_FACTORY.createRead(new ReadCapabilityType().cachingOnly(true)));
		return capabilities;

		// TODO activation, credentials?
	}

	@Override
	public ResourceSchema fetchResourceSchema(List<QName> generateObjectClasses, OperationResult parentResult) {
		// Schema discovery is not supported. Schema must be defined manually. Or other connector has to provide it.
		InternalMonitor.recordConnectorOperation("schema");
		return null;
	}

	@Override
	public PrismObject<ShadowType> fetchObject(ResourceObjectIdentification resourceObjectIdentification,
			AttributesToReturn attributesToReturn, StateReporter reporter, OperationResult parentResult) {
		InternalMonitor.recordConnectorOperation("fetchObject");
		return null;
	}

	@Override
	public SearchResultMetadata search(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
			ShadowResultHandler handler, AttributesToReturn attributesToReturn,
			PagedSearchCapabilityType pagedSearchConfigurationType, SearchHierarchyConstraints searchHierarchyConstraints,
			StateReporter reporter, OperationResult parentResult) {
		InternalMonitor.recordConnectorOperation("search");
		return null;
	}

	@Override
	public int count(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
			PagedSearchCapabilityType pagedSearchConfigurationType, StateReporter reporter, OperationResult parentResult) {
		InternalMonitor.recordConnectorOperation("count");
		return 0;
	}

	@Override
	public AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(PrismObject<? extends ShadowType> object,
			Collection<Operation> additionalOperations, StateReporter reporter, OperationResult parentResult) {
		InternalMonitor.recordConnectorOperation("addObject");
		return null;
	}

	@Override
	public AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyObject(
			ResourceObjectIdentification identification, PrismObject<ShadowType> shadow, Collection<Operation> changes,
			ConnectorOperationOptions options, StateReporter reporter, OperationResult parentResult) {
		InternalMonitor.recordConnectorOperation("modifyObject");
		return null;
	}

	@Override
	public AsynchronousOperationResult deleteObject(ObjectClassComplexTypeDefinition objectClass,
			Collection<Operation> additionalOperations, PrismObject<ShadowType> shadow,
			Collection<? extends ResourceAttribute<?>> identifiers, StateReporter reporter, OperationResult parentResult) {
		InternalMonitor.recordConnectorOperation("deleteObject");
		return null;
	}

	@Override
	public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, StateReporter reporter,
			OperationResult parentResult) {
		InternalMonitor.recordConnectorOperation("executeScript");
		return null;
	}

	@Override
	public PrismProperty<?> deserializeToken(Object serializedToken) {
		return null;
	}

	@Override
	public <T> PrismProperty<T> fetchCurrentToken(ObjectClassComplexTypeDefinition objectClass, StateReporter reporter,
			OperationResult parentResult) {
		return null;
	}

	@Override
	public List<Change> fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken,
			AttributesToReturn attrsToReturn, StateReporter reporter, OperationResult parentResult) {
		return null;
	}

	@Override
	public void startListeningForChanges(ChangeListener changeListener, OperationResult parentResult) {
		this.changeListener = changeListener;
		// TODO really start
	}

	@Override
	public void stopListeningForChanges(OperationResult parentResult) {
		stopListening();
	}

	private void stopListening() {
		if (changeListener != null) {
			changeListener = null;
			// todo really stop
		}
	}

	private String getShadowIdentifier(Collection<? extends ResourceAttribute<?>> identifiers){
		try {
			Object[] shadowIdentifiers = identifiers.toArray();

			return ((ResourceAttribute)shadowIdentifiers[0]).getValue().getValue().toString();
		} catch (NullPointerException e){
			return "";
		}
	}
}
