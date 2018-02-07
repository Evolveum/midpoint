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

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinitionImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationProperty;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnector;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnectorConfiguration;
import com.evolveum.midpoint.provisioning.ucf.api.UcfUtil;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManagedConnectorInstance;
import com.evolveum.midpoint.repo.api.RepositoryAware;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Connector factory for the connectors built-in to midPoint, such as
 * the "manual connector".
 *
 * @author Radovan Semancik
 *
 */
@Component
public class ConnectorFactoryBuiltinImpl implements ConnectorFactory {

	public static final String SCAN_PACKAGE = "com.evolveum.midpoint";

	private static final String CONFIGURATION_NAMESPACE_PREFIX = SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN + "/bundle/";

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorFactoryBuiltinImpl.class);

	@Autowired(required=true)
	private PrismContext prismContext;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	private Map<String,ConnectorStruct> connectorMap;

	@Override
	public Set<ConnectorType> listConnectors(ConnectorHostType host, OperationResult parentRestul)
			throws CommunicationException {
		if (connectorMap == null) {
			discoverConnectors();
		}
		return connectorMap.entrySet().stream().map(e -> e.getValue().connectorObject).collect(Collectors.toSet());
	}

	private void discoverConnectors() {
		connectorMap = new HashMap<>();
		ClassPathScanningCandidateComponentProvider scanner =
				new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(ManagedConnector.class));
		LOGGER.trace("Scanning package {}", SCAN_PACKAGE);
		for (BeanDefinition bd : scanner.findCandidateComponents(SCAN_PACKAGE)) {
			LOGGER.debug("Found connector class {}", bd);
			String beanClassName = bd.getBeanClassName();
			try {

				Class connectorClass = Class.forName(beanClassName);
				ManagedConnector annotation = (ManagedConnector) connectorClass.getAnnotation(ManagedConnector.class);
				String type = annotation.type();
				LOGGER.debug("Found connector {} class {}", type, connectorClass);
				ConnectorStruct struct = createConnectorStruct(connectorClass, annotation);
				connectorMap.put(type, struct);

			} catch (ClassNotFoundException e) {
				LOGGER.error("Error loading connector class {}: {}", beanClassName, e.getMessage(), e);
			} catch (ObjectNotFoundException | SchemaException e) {
				LOGGER.error("Error discovering the connector {}: {}", beanClassName, e.getMessage(), e);
			}

		}
		LOGGER.trace("Scan done");
	}

	private ConnectorStruct createConnectorStruct(Class connectorClass, ManagedConnector annotation) throws ObjectNotFoundException, SchemaException {
		ConnectorStruct struct = new ConnectorStruct();
		struct.connectorClass = connectorClass;

		ConnectorType connectorType = new ConnectorType();
		String bundleName = connectorClass.getPackage().getName();
		String type = annotation.type();
		if (type == null || type.isEmpty()) {
			type = connectorClass.getSimpleName();
		}
		String version = annotation.version();
		UcfUtil.addConnectorNames(connectorType, "Built-in", bundleName, type, version, null);
		connectorType.setConnectorBundle(bundleName);
		connectorType.setConnectorType(type);
		connectorType.setVersion(version);
		connectorType.setFramework(SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN);
		String namespace = CONFIGURATION_NAMESPACE_PREFIX + bundleName + "/" + type;
		connectorType.setNamespace(namespace);

		struct.connectorObject = connectorType;

		PrismSchema connectorSchema = generateConnectorConfigurationSchema(struct);
		if (connectorSchema != null) {
			LOGGER.trace("Generated connector schema for {}: {} definitions",
					connectorType, connectorSchema.getDefinitions().size());
			UcfUtil.setConnectorSchema(connectorType, connectorSchema);
			struct.connectorConfigurationSchema = connectorSchema;
		} else {
			LOGGER.warn("No connector schema generated for {}", connectorType);
		}

		return struct;
	}

	private ConnectorStruct getConnectorStruct(ConnectorType connectorType) throws ObjectNotFoundException {
		if (connectorMap == null) {
			discoverConnectors();
		}
		String type = connectorType.getConnectorType();
		ConnectorStruct struct = connectorMap.get(type);
		if (struct == null) {
			throw new ObjectNotFoundException("No built-in connector type "+type);
		}
		return struct;
	}

	@Override
	public PrismSchema generateConnectorConfigurationSchema(ConnectorType connectorType)
			throws ObjectNotFoundException {
		ConnectorStruct struct = getConnectorStruct(connectorType);
		return generateConnectorConfigurationSchema(struct);
	}

	private PrismSchema generateConnectorConfigurationSchema(ConnectorStruct struct) {

		Class<? extends ConnectorInstance> connectorClass = struct.connectorClass;

		PropertyDescriptor connectorConfigurationProp = UcfUtil.findAnnotatedProperty(connectorClass, ManagedConnectorConfiguration.class);

		PrismSchema connectorSchema = new PrismSchemaImpl(struct.connectorObject.getNamespace(), prismContext);
		// Create configuration type - the type used by the "configuration" element
		PrismContainerDefinitionImpl<?> configurationContainerDef = ((PrismSchemaImpl) connectorSchema).createPropertyContainerDefinition(
				ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart(),
				SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_TYPE_LOCAL_NAME);

		Class<?> configurationClass = connectorConfigurationProp.getPropertyType();
		BeanWrapper configurationClassBean = new BeanWrapperImpl(configurationClass);
		for (PropertyDescriptor prop: configurationClassBean.getPropertyDescriptors()) {
			if (!UcfUtil.hasAnnotation(prop, ConfigurationProperty.class)) {
				continue;
			}

			ItemDefinition<?> itemDef = createPropertyDefinition(configurationContainerDef, prop);

			LOGGER.trace("Configuration item definition for {}: {}", prop.getName(), itemDef);
		}

		return connectorSchema;
	}

	private ItemDefinition<?> createPropertyDefinition(PrismContainerDefinitionImpl<?> configurationContainerDef,
			PropertyDescriptor prop) {
		String propName = prop.getName();
		Class<?> propertyType = prop.getPropertyType();
		Class<?> baseType = propertyType;
		int minOccurs = 1;
		int maxOccurs = 1;
		if (propertyType.isArray()) {
			maxOccurs = -1;
			baseType = propertyType.getComponentType();
		}
		// TODO: minOccurs: define which properties are optional/mandatory
		// TODO: display names, ordering, help texts
		QName propType = XsdTypeMapper.toXsdType(baseType);
		return configurationContainerDef.createPropertyDefinition(new QName(configurationContainerDef.getName().getNamespaceURI(), propName),
				propType, minOccurs, maxOccurs);
	}


	@Override
	public ConnectorInstance createConnectorInstance(ConnectorType connectorType, String namespace,
			String desc) throws ObjectNotFoundException, SchemaException {
		String type = connectorType.getConnectorType();
		ConnectorStruct struct = connectorMap.get(type);
		Class<? extends ConnectorInstance> connectorClass = struct.connectorClass;
		ConnectorInstance connectorInstance;
		try {
			connectorInstance = connectorClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ObjectNotFoundException("Cannot create instance of connector "+connectorClass+": "+e.getMessage(), e);
		}
		if (connectorInstance instanceof AbstractManagedConnectorInstance) {
			setupAbstractConnectorInstance((AbstractManagedConnectorInstance)connectorInstance, connectorType, namespace,
					desc, struct);
		}
		if (connectorInstance instanceof RepositoryAware) {
			((RepositoryAware)connectorInstance).setRepositoryService(repositoryService);
		}
		return connectorInstance;
	}

	private void setupAbstractConnectorInstance(AbstractManagedConnectorInstance connectorInstance, ConnectorType connectorObject, String namespace,
			String desc, ConnectorStruct struct) {
		connectorInstance.setConnectorObject(connectorObject);
		connectorInstance.setResourceSchemaNamespace(namespace);
		connectorInstance.setPrismContext(prismContext);
		connectorInstance.setConnectorConfigurationSchema(struct.connectorConfigurationSchema);
	}

	@Override
	public void selfTest(OperationResult parentTestResult) {
		// Nothing to do
	}

	@Override
	public boolean supportsFramework(String frameworkIdentifier) {
		return SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN.equals(frameworkIdentifier);
	}

	@Override
	public String getFrameworkVersion() {
		return "1.0.0";
	}

	@Override
	public void shutdown() {
		// Nothing to do
	}

	private class ConnectorStruct {
		Class<? extends ConnectorInstance> connectorClass;
		ConnectorType connectorObject;
		PrismSchema connectorConfigurationSchema;
	}

}
