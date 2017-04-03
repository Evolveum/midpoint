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
package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
public abstract class AbstractConnectorInstance implements ConnectorInstance {

	private ConnectorType connectorObject;
	private PrismSchema connectorConfigurationSchema;
	private String resourceSchemaNamespace;
	private PrismContext prismContext;
	
	private PrismContainerValue<?> connectorConfiguration;
	private ResourceSchema resourceSchema = null;
	private Collection<Object> capabilities = null;
	
	public ConnectorType getConnectorObject() {
		return connectorObject;
	}

	public void setConnectorObject(ConnectorType connectorObject) {
		this.connectorObject = connectorObject;
	}

	public PrismSchema getConnectorConfigurationSchema() {
		return connectorConfigurationSchema;
	}

	public void setConnectorConfigurationSchema(PrismSchema connectorConfigurationSchema) {
		this.connectorConfigurationSchema = connectorConfigurationSchema;
	}

	public PrismContainerValue<?> getConnectorConfiguration() {
		return connectorConfiguration;
	}

	public void setConnectorConfiguration(PrismContainerValue<?> connectorConfiguration) {
		this.connectorConfiguration = connectorConfiguration;
	}

	public String getResourceSchemaNamespace() {
		return resourceSchemaNamespace;
	}

	public void setResourceSchemaNamespace(String resourceSchemaNamespace) {
		this.resourceSchemaNamespace = resourceSchemaNamespace;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}
	
	protected ResourceSchema getResourceSchema() {
		return resourceSchema;
	}

	protected void setResourceSchema(ResourceSchema resourceSchema) {
		this.resourceSchema = resourceSchema;
	}

	protected Collection<Object> getCapabilities() {
		return capabilities;
	}

	protected void setCapabilities(Collection<Object> capabilities) {
		this.capabilities = capabilities;
	}

	@Override
	public void configure(PrismContainerValue<?> configuration, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, SchemaException,
			ConfigurationException {
		
		OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_CONFIGURE);
		result.addParam("configuration", configuration);
		
		boolean immutable = configuration.isImmutable();
		try {
			if (immutable) {
				configuration.setImmutable(false);
			}
			configuration.applyDefinition(getConfigurationContainerDefinition());
		} finally {
			if (immutable) {
				configuration.setImmutable(true);
			}
		}
		
		setConnectorConfiguration(configuration);
		
		// TODO: transform configuration in a subclass
		
		result.recordSuccessIfUnknown();
	}
	
	@Override
	public void initialize(ResourceSchema resourceSchema, Collection<Object> capabilities,
			boolean caseIgnoreAttributeNames, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, ConfigurationException {
		
		OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_INITIALIZE);
		result.addContext("connector", getConnectorObject());
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, this.getClass());
		
		setResourceSchema(resourceSchema);
		setCapabilities(capabilities);
		
		// TODO: initialize connection in the subclass
		
		result.recordSuccessIfUnknown();
	}
	
	protected PrismContainerDefinition<?> getConfigurationContainerDefinition() throws SchemaException {
		QName configContainerQName = new QName(getConnectorObject().getNamespace(),
				ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
		PrismContainerDefinition<?> configContainerDef = getConnectorConfigurationSchema()
				.findContainerDefinitionByElementName(configContainerQName);
		if (configContainerDef == null) {
			throw new SchemaException("No definition of container " + configContainerQName
					+ " in configuration schema for connector " + this);
		}
		return configContainerDef;
	}
}
