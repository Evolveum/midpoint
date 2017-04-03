/**
 * Copyright (c) 2017 Evolveum
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

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.AbstractConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationResult;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationReturnValue;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnector;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.SearchHierarchyConstraints;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;

/**
 * @author Radovan Semancik
 *
 */
@ManagedConnector
public class ManualConnector extends AbstractConnectorInstance {

	@Override
	public ConnectorOperationalStatus getOperationalStatus() throws ObjectNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Object> fetchCapabilities(OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, ConfigurationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceSchema fetchResourceSchema(List<QName> generateObjectClasses, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, ConfigurationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends ShadowType> PrismObject<T> fetchObject(Class<T> type,
			ResourceObjectIdentification resourceObjectIdentification, AttributesToReturn attributesToReturn,
			StateReporter reporter, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
			SchemaException, SecurityViolationException, ConfigurationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends ShadowType> SearchResultMetadata search(
			ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
			ResultHandler<T> handler, AttributesToReturn attributesToReturn,
			PagedSearchCapabilityType pagedSearchConfigurationType,
			SearchHierarchyConstraints searchHierarchyConstraints, StateReporter reporter,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			SchemaException, SecurityViolationException, ObjectNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int count(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
			PagedSearchCapabilityType pagedSearchConfigurationType, StateReporter reporter,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			SchemaException, UnsupportedOperationException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ConnectorOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(
			PrismObject<? extends ShadowType> object, Collection<Operation> additionalOperations,
			StateReporter reporter, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConnectorOperationReturnValue<Collection<PropertyModificationOperation>> modifyObject(
			ObjectClassComplexTypeDefinition objectClass,
			Collection<? extends ResourceAttribute<?>> identifiers, Collection<Operation> changes,
			StateReporter reporter, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
			SchemaException, SecurityViolationException, ObjectAlreadyExistsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConnectorOperationResult deleteObject(ObjectClassComplexTypeDefinition objectClass,
			Collection<Operation> additionalOperations,
			Collection<? extends ResourceAttribute<?>> identifiers, StateReporter reporter,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, StateReporter reporter,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrismProperty<?> deserializeToken(Object serializedToken) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> PrismProperty<T> fetchCurrentToken(ObjectClassComplexTypeDefinition objectClass,
			StateReporter reporter, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Change> fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken,
			AttributesToReturn attrsToReturn, StateReporter reporter, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, SchemaException,
			ConfigurationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void test(OperationResult parentResult) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

}
