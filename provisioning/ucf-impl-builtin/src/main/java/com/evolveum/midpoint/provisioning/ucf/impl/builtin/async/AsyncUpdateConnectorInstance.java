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
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManagedConnectorInstance;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AsyncUpdateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  Connector that is able to obtain and process asynchronous updates.
 *  It can be used to receive messages from JMS or AMQP messaging systems; or maybe from REST calls in the future.
 *
 *  Currently we keep no state besides the configuration. It is because calls to this connector should be really infrequent.
 *  Sources are therefore instantiated on demand, e.g. on test() or startListening() calls.
 *
 *  TODO we should do something reasonable on configuration change: probably we should restart all the listening activities
 *   (we cannot simply cancel them; but restarting seems appropriate; otherwise all Async Update tasks would continue with
 *   the old configuration)
 */
@SuppressWarnings("DefaultAnnotationParam")
@ManagedConnector(type="AsyncUpdateConnector", version="1.0.0")
public class AsyncUpdateConnectorInstance extends AbstractManagedConnectorInstance implements UcfExpressionEvaluatorAware {
	
	@SuppressWarnings("unused")
	private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdateConnectorInstance.class);

	private static final com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory CAPABILITY_OBJECT_FACTORY
			= new com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory();

	private AsyncUpdateConnectorConfiguration configuration;

	private final SourceManager sourceManager = new SourceManager(this);

	/**
	 * The expression evaluator has to come from the higher layers because it needs features not present in UCF impl module.
	 */
	private UcfExpressionEvaluator ucfExpressionEvaluator;

	@Override
	protected void connect(OperationResult result) {
		// no-op
	}

	@Override
	protected void disconnect(OperationResult result) {
		// we do not want to stop currently running listening activities just because the configuration was changed
	}

	@Override
	public void test(OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, AsyncUpdateConnectorInstance.class);
		result.addContext("connector", getConnectorObject().toString());
		Collection<AsyncUpdateSource> sources = sourceManager.createSources(configuration.getAllSources());
		try {
			sources.forEach(s -> s.test(result));
			result.computeStatus();
		} catch (RuntimeException e) {
			result.recordFatalError("Couldn't test async update sources: " + e.getMessage(), e);
		}
	}

	@Override
	public void dispose() {
		// This operation is invoked on system shutdown; for simplicity let's not try to cancel open listening activities
		// as they were probably cancelled on respective Async Update tasks going down; and will be cancelled on system
		// shutdown anyway.
		//
		// This will change if the use of dispose() will change.
	}

	@Override
	public ListeningActivity startListeningForChanges(ChangeListener changeListener, OperationResult parentResult) throws SchemaException {
		TransformationalAsyncUpdateMessageListener messageListener = new TransformationalAsyncUpdateMessageListener(
				changeListener, configuration.getTransformExpression(), ucfExpressionEvaluator, getPrismContext(), getResourceSchema());
		Collection<AsyncUpdateSource> sources = sourceManager.createSources(configuration.getAllSources());
		ConnectorInstanceListeningActivity listeningActivity = new ConnectorInstanceListeningActivity();
		try {
			for (AsyncUpdateSource source : sources) {
				listeningActivity.addActivity(source.startListening(messageListener));
			}
		} catch (RuntimeException e) {
			listeningActivity.stop();
			throw e;
		}
		return listeningActivity;
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

	@ManagedConnectorConfiguration
	public AsyncUpdateConnectorConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(AsyncUpdateConnectorConfiguration configuration) {
		configuration.validate();
		this.configuration = configuration;
	}

	@Override
	public UcfExpressionEvaluator getUcfExpressionEvaluator() {
		return ucfExpressionEvaluator;
	}

	@Override
	public void setUcfExpressionEvaluator(UcfExpressionEvaluator evaluator) {
		this.ucfExpressionEvaluator = evaluator;
	}

	//region Unsupported operations
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
	//endregion
}
