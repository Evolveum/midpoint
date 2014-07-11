/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.provisioning.ucf.impl;

import static com.evolveum.midpoint.provisioning.ucf.impl.IcfUtil.processIcfException;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ResultsHandlerConfiguration;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.DeleteApiOp;
import org.identityconnectors.framework.api.operations.GetApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.api.operations.SearchApiOp;
import org.identityconnectors.framework.api.operations.SyncApiOp;
import org.identityconnectors.framework.api.operations.TestApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PasswordChangeOperation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.query.FilterInterpreter;
import com.evolveum.midpoint.provisioning.ucf.util.UcfUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ActivationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationValidityCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.DeleteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType.Host;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.TestConnectionCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedByteArrayType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Implementation of ConnectorInstance for ICF connectors.
 * <p/>
 * This class implements the ConnectorInstance interface. The methods are
 * converting the data from the "midPoint semantics" as seen by the
 * ConnectorInstance interface to the "ICF semantics" as seen by the ICF
 * framework.
 * 
 * @author Radovan Semancik
 */
public class ConnectorInstanceIcfImpl implements ConnectorInstance {

	private static final com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory capabilityObjectFactory 
		= new com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory();

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorInstanceIcfImpl.class);

	ConnectorInfo cinfo;
	ConnectorType connectorType;
	ConnectorFacade icfConnectorFacade;
	String resourceSchemaNamespace;
	Protector protector;
	PrismContext prismContext;
	private IcfNameMapper icfNameMapper;

	private ResourceSchema resourceSchema = null;
	private Collection<Object> capabilities = null;
	private PrismSchema connectorSchema;
	private String description;

	public ConnectorInstanceIcfImpl(ConnectorInfo connectorInfo, ConnectorType connectorType,
			String schemaNamespace, PrismSchema connectorSchema, Protector protector,
			PrismContext prismContext) {
		this.cinfo = connectorInfo;
		this.connectorType = connectorType;
		this.resourceSchemaNamespace = schemaNamespace;
		this.connectorSchema = connectorSchema;
		this.protector = protector;
		this.prismContext = prismContext;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSchemaNamespace() {
		return resourceSchemaNamespace;
	}

	public IcfNameMapper getIcfNameMapper() {
		return icfNameMapper;
	}

	public void setIcfNameMapper(IcfNameMapper icfNameMapper) {
		this.icfNameMapper = icfNameMapper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance#configure
	 * (com.evolveum.midpoint.xml.ns._public.common.common_2.Configuration)
	 */
	@Override
	public void configure(PrismContainerValue<?> configuration, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".configure");
		result.addParam("configuration", configuration);

		try {
			// Get default configuration for the connector. This is important,
			// as it contains types of connector configuration properties.

			// Make sure that the proper configuration schema is applied. This
			// will cause that all the "raw" elements are parsed
			configuration.applyDefinition(getConfigurationContainerDefinition());

			APIConfiguration apiConfig = cinfo.createDefaultAPIConfiguration();

			// Transform XML configuration from the resource to the ICF
			// connector
			// configuration
			try {
				transformConnectorConfiguration(apiConfig, configuration);
			} catch (SchemaException e) {
				result.recordFatalError(e.getMessage(), e);
				throw e;
			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Configuring connector {}", connectorType);
				for (String propName : apiConfig.getConfigurationProperties().getPropertyNames()) {
					LOGGER.trace("P: {} = {}", propName,
							apiConfig.getConfigurationProperties().getProperty(propName).getValue());
				}
			}

			// Create new connector instance using the transformed configuration
			icfConnectorFacade = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

			result.recordSuccess();
		} catch (Throwable ex) {
			Throwable midpointEx = processIcfException(ex, this, result);
			result.computeStatus("Removing attribute values failed");
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof CommunicationException) {
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof ConfigurationException) {
				throw (ConfigurationException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else if (midpointEx instanceof Error) {
				throw (Error) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}

		}

	}

	private PrismContainerDefinition<?> getConfigurationContainerDefinition() throws SchemaException {
		if (connectorSchema == null) {
			generateConnectorSchema();
		}
		QName configContainerQName = new QName(connectorType.getNamespace(),
				ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
		PrismContainerDefinition<?> configContainerDef = connectorSchema
				.findContainerDefinitionByElementName(configContainerQName);
		if (configContainerDef == null) {
			throw new SchemaException("No definition of container " + configContainerQName
					+ " in configuration schema for connector " + this);
		}
		return configContainerDef;
	}

	public PrismSchema generateConnectorSchema() {

		LOGGER.trace("Generating configuration schema for {}", this);
		APIConfiguration defaultAPIConfiguration = cinfo.createDefaultAPIConfiguration();
		ConfigurationProperties icfConfigurationProperties = defaultAPIConfiguration
				.getConfigurationProperties();

		if (icfConfigurationProperties == null || icfConfigurationProperties.getPropertyNames() == null
				|| icfConfigurationProperties.getPropertyNames().isEmpty()) {
			LOGGER.debug("No configuration schema for {}", this);
			return null;
		}

		PrismSchema mpSchema = new PrismSchema(connectorType.getNamespace(), prismContext);

		// Create configuration type - the type used by the "configuration"
		// element
		PrismContainerDefinition<?> configurationContainerDef = mpSchema.createPropertyContainerDefinition(
				ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart(),
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_TYPE_LOCAL_NAME);

		// element with "ConfigurationPropertiesType" - the dynamic part of
		// configuration schema
		ComplexTypeDefinition configPropertiesTypeDef = mpSchema.createComplexTypeDefinition(new QName(
				connectorType.getNamespace(),
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME));

		// Create definition of "configurationProperties" type
		// (CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME)
		for (String icfPropertyName : icfConfigurationProperties.getPropertyNames()) {
			ConfigurationProperty icfProperty = icfConfigurationProperties.getProperty(icfPropertyName);

			QName propXsdType = icfTypeToXsdType(icfProperty.getType(), icfProperty.isConfidential());
			LOGGER.trace("{}: Mapping ICF config schema property {} from {} to {}", new Object[] { this,
					icfPropertyName, icfProperty.getType(), propXsdType });
			PrismPropertyDefinition propertyDefinifion = configPropertiesTypeDef.createPropertyDefinition(
					icfPropertyName, propXsdType);
			propertyDefinifion.setDisplayName(icfProperty.getDisplayName(null));
			propertyDefinifion.setHelp(icfProperty.getHelpMessage(null));
			if (isMultivaluedType(icfProperty.getType())) {
				propertyDefinifion.setMaxOccurs(-1);
			} else {
				propertyDefinifion.setMaxOccurs(1);
			}
			if (icfProperty.isRequired()) {
				propertyDefinifion.setMinOccurs(1);
			} else {
				propertyDefinifion.setMinOccurs(0);
			}

		}

		// Create common ICF configuration property containers as a references
		// to a static schema
		configurationContainerDef.createContainerDefinition(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_ELEMENT,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_TYPE, 0, 1);
		configurationContainerDef.createPropertyDefinition(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_ELEMENT,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_TYPE, 0, 1);
		configurationContainerDef.createContainerDefinition(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_ELEMENT,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_TYPE, 0, 1);
        configurationContainerDef.createContainerDefinition(
                ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT,
                ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_TYPE, 0, 1);

		// No need to create definition of "configuration" element.
		// midPoint will look for this element, but it will be generated as part
		// of the PropertyContainer serialization to schema

		configurationContainerDef.createContainerDefinition(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME,
				configPropertiesTypeDef, 1, 1);

		LOGGER.debug("Generated configuration schema for {}: {} definitions", this, mpSchema.getDefinitions()
				.size());
		connectorSchema = mpSchema;
		return mpSchema;
	}

	private QName icfTypeToXsdType(Class<?> type, boolean isConfidential) {
		// For arrays we are only interested in the component type
		if (isMultivaluedType(type)) {
			type = type.getComponentType();
		}
		QName propXsdType = null;
		if (GuardedString.class.equals(type) || 
				(String.class.equals(type) && isConfidential)) {
			// GuardedString is a special case. It is a ICF-specific
			// type
			// implementing Potemkin-like security. Use a temporary
			// "nonsense" type for now, so this will fail in tests and
			// will be fixed later
//			propXsdType = SchemaConstants.T_PROTECTED_STRING_TYPE;
			propXsdType = ProtectedStringType.COMPLEX_TYPE;
		} else if (GuardedByteArray.class.equals(type) || 
				(Byte.class.equals(type) && isConfidential)) {
			// GuardedString is a special case. It is a ICF-specific
			// type
			// implementing Potemkin-like security. Use a temporary
			// "nonsense" type for now, so this will fail in tests and
			// will be fixed later
//			propXsdType = SchemaConstants.T_PROTECTED_BYTE_ARRAY_TYPE;
			propXsdType = ProtectedByteArrayType.COMPLEX_TYPE;
		} else {
			propXsdType = XsdTypeMapper.toXsdType(type);
		}
		return propXsdType;
	}

	private boolean isMultivaluedType(Class<?> type) {
		// We consider arrays to be multi-valued
		// ... unless it is byte[] or char[]
		return type.isArray() && !type.equals(byte[].class) && !type.equals(char[].class);
	}

	@Override
	public void initialize(ResourceSchema resourceSchema, Collection<Object> capabilities, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ConfigurationException {

		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".initialize");
		result.addContext("connector", connectorType);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorFactoryIcfImpl.class);

		if (icfConnectorFacade == null) {
			result.recordFatalError("Attempt to use unconfigured connector");
			throw new IllegalStateException("Attempt to use unconfigured connector "
					+ ObjectTypeUtil.toShortString(connectorType));
		}

		this.resourceSchema = resourceSchema;
		this.capabilities = capabilities;

		if (resourceSchema == null || capabilities == null) {
			try {
				retrieveResourceSchema(null, result);
			} catch (CommunicationException ex) {
				// This is in fact fatal. There is not schema. Not even the pre-cached one. 
				// The connector will not be able to work.
				result.recordFatalError(ex);
				throw ex;
			} catch (ConfigurationException ex) {
				result.recordFatalError(ex);
				throw ex;
			} catch (GenericFrameworkException ex) {
				result.recordFatalError(ex);
				throw ex;
			}
		}

		result.recordSuccess();
	}

	@Override
	public ResourceSchema fetchResourceSchema(List<QName> generateObjectClasses, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ConfigurationException {

		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".fetchResourceSchema");
		result.addContext("connector", connectorType);

		try {
			retrieveResourceSchema(generateObjectClasses, result);
		} catch (CommunicationException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (ConfigurationException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (GenericFrameworkException ex) {
			result.recordFatalError(ex);
			throw ex;
		}
		
		if (resourceSchema == null) {
			result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Connector does not support schema");
		} else {
			result.recordSuccess();
		}

		return resourceSchema;
	}
	
	@Override
	public Collection<Object> fetchCapabilities(OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ConfigurationException {

		// Result type for this operation
		OperationResult result = parentResult.createMinorSubresult(ConnectorInstance.class.getName()
				+ ".fetchCapabilities");
		result.addContext("connector", connectorType);

		try {
			retrieveResourceSchema(null, result);
		} catch (CommunicationException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (ConfigurationException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (GenericFrameworkException ex) {
			result.recordFatalError(ex);
			throw ex;
		}

		result.recordSuccess();

		return capabilities;
	}
	
	private void retrieveResourceSchema(List<QName> generateObjectClasses, OperationResult parentResult) throws CommunicationException, ConfigurationException, GenericFrameworkException {
		// Connector operation cannot create result for itself, so we need to
		// create result for it
		OperationResult icfResult = parentResult.createSubresult(ConnectorFacade.class.getName() + ".schema");
		icfResult.addContext("connector", icfConnectorFacade.getClass());

		org.identityconnectors.framework.common.objects.Schema icfSchema = null;
		try {

			// Fetch the schema from the connector (which actually gets that
			// from the resource).
			icfSchema = icfConnectorFacade.schema();

			icfResult.recordSuccess();
		} catch (UnsupportedOperationException ex) {
			// The connector does no support schema() operation.
			icfResult.recordStatus(OperationResultStatus.HANDLED_ERROR, ex.getMessage());
			resourceSchema = null;
			return;
		} catch (Throwable ex) {
			// conditions.
			// Therefore this kind of heavy artillery is necessary.
			// ICF interface does not specify exceptions or other error
			// TODO maybe we can try to catch at least some specific exceptions
			Throwable midpointEx = processIcfException(ex, this, icfResult);

			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof CommunicationException) {
				icfResult.recordFatalError(midpointEx.getMessage(), midpointEx);
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof ConfigurationException) {
				icfResult.recordFatalError(midpointEx.getMessage(), midpointEx);
				throw (ConfigurationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				icfResult.recordFatalError(midpointEx.getMessage(), midpointEx);
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				icfResult.recordFatalError(midpointEx.getMessage(), midpointEx);
				throw (RuntimeException) midpointEx;
			} else if (midpointEx instanceof Error) {
				icfResult.recordFatalError(midpointEx.getMessage(), midpointEx);
				throw (Error) midpointEx;
			} else {
				icfResult.recordFatalError(midpointEx.getMessage(), midpointEx);
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}

		parseResourceSchema(icfSchema, generateObjectClasses);
	}

	private void parseResourceSchema(org.identityconnectors.framework.common.objects.Schema icfSchema, List<QName> generateObjectClasses) {

		AttributeInfo passwordAttributeInfo = null;
		AttributeInfo enableAttributeInfo = null;
		AttributeInfo enableDateAttributeInfo = null;
		AttributeInfo disableDateAttributeInfo = null;
		AttributeInfo lockoutAttributeInfo = null;

		// New instance of midPoint schema object
		resourceSchema = new ResourceSchema(getSchemaNamespace(), prismContext);

		// Let's convert every objectclass in the ICF schema ...
		Set<ObjectClassInfo> objectClassInfoSet = icfSchema.getObjectClassInfo();
		for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {

			// "Flat" ICF object class names needs to be mapped to QNames
			QName objectClassXsdName = icfNameMapper.objectClassToQname(objectClassInfo.getType(), getSchemaNamespace());

			if (!shouldBeGenerated(generateObjectClasses, objectClassXsdName)){
				continue;
			}
			
			// ResourceObjectDefinition is a midPpoint way how to represent an
			// object class.
			// The important thing here is the last "type" parameter
			// (objectClassXsdName). The rest is more-or-less cosmetics.
			ObjectClassComplexTypeDefinition roDefinition = resourceSchema
					.createObjectClassDefinition(objectClassXsdName);

			// The __ACCOUNT__ objectclass in ICF is a default account
			// objectclass. So mark it appropriately.
			if (ObjectClass.ACCOUNT_NAME.equals(objectClassInfo.getType())) {
				roDefinition.setKind(ShadowKindType.ACCOUNT);
				roDefinition.setDefaultInAKind(true);
			}

			// Every object has UID in ICF, therefore add it right now
			ResourceAttributeDefinition uidDefinition = roDefinition.createAttributeDefinition(
					ConnectorFactoryIcfImpl.ICFS_UID, DOMUtil.XSD_STRING);
			// DO NOT make it mandatory. It must not be present on create hence it cannot be mandatory.
			uidDefinition.setMinOccurs(0);
			uidDefinition.setMaxOccurs(1);
			// Make it read-only
			uidDefinition.setReadOnly();
			// Set a default display name
			uidDefinition.setDisplayName("ICF UID");
			// Uid is a primary identifier of every object (this is the ICF way)
			((Collection<ResourceAttributeDefinition>)roDefinition.getIdentifiers()).add(uidDefinition);

			// Let's iterate over all attributes in this object class ...
			Set<AttributeInfo> attributeInfoSet = objectClassInfo.getAttributeInfo();
			for (AttributeInfo attributeInfo : attributeInfoSet) {

				if (OperationalAttributes.PASSWORD_NAME.equals(attributeInfo.getName())) {
					// This attribute will not go into the schema
					// instead a "password" capability is used
					passwordAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}

				if (OperationalAttributes.ENABLE_NAME.equals(attributeInfo.getName())) {
					enableAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}
				
				if (OperationalAttributes.ENABLE_DATE_NAME.equals(attributeInfo.getName())) {
					enableDateAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}
				
				if (OperationalAttributes.DISABLE_DATE_NAME.equals(attributeInfo.getName())) {
					disableDateAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}
				
				if (OperationalAttributes.LOCK_OUT_NAME.equals(attributeInfo.getName())) {
					lockoutAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}

				QName attrXsdName = icfNameMapper.convertAttributeNameToQName(attributeInfo.getName(), getSchemaNamespace());
				QName attrXsdType = icfTypeToXsdType(attributeInfo.getType(), false);
				
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Attr conversion ICF: {}({}) -> XSD: {}({})", 
							new Object[]{attributeInfo.getName(), attributeInfo.getType().getSimpleName(),
								PrettyPrinter.prettyPrint(attrXsdName), PrettyPrinter.prettyPrint(attrXsdType)});
				}

				// Create ResourceObjectAttributeDefinition, which is midPoint
				// way how to express attribute schema.
				ResourceAttributeDefinition attrDef = roDefinition.createAttributeDefinition(
						attrXsdName, attrXsdType);

				
				if (attrXsdName.equals(ConnectorFactoryIcfImpl.ICFS_NAME)) {
					// Set a better display name for __NAME__. The "name" is s very
					// overloaded term, so let's try to make things
					// a bit clearer
					attrDef.setDisplayName("ICF NAME");
					((Collection<ResourceAttributeDefinition>)roDefinition.getSecondaryIdentifiers()).add(attrDef);
				}

				// Now we are going to process flags such as optional and
				// multi-valued
				Set<Flags> flagsSet = attributeInfo.getFlags();
				// System.out.println(flagsSet);

				attrDef.setMinOccurs(0);
				attrDef.setMaxOccurs(1);
				boolean canCreate = true;
				boolean canUpdate = true;
				boolean canRead = true;

				for (Flags flags : flagsSet) {
					if (flags == Flags.REQUIRED) {
						attrDef.setMinOccurs(1);
					}
					if (flags == Flags.MULTIVALUED) {
						attrDef.setMaxOccurs(-1);
					}
					if (flags == Flags.NOT_CREATABLE) {
						canCreate = false;
					}
					if (flags == Flags.NOT_READABLE) {
						canRead = false;
					}
					if (flags == Flags.NOT_UPDATEABLE) {
						canUpdate = false;
					}
					if (flags == Flags.NOT_RETURNED_BY_DEFAULT) {
						attrDef.setReturnedByDefault(false);
					}
				}

				attrDef.setCanAdd(canCreate);
				attrDef.setCanModify(canUpdate);
				attrDef.setCanRead(canRead);

			}

			// Add schema annotations
			roDefinition.setNativeObjectClass(objectClassInfo.getType());
			roDefinition.setDisplayNameAttribute(ConnectorFactoryIcfImpl.ICFS_NAME);
			roDefinition.setNamingAttribute(ConnectorFactoryIcfImpl.ICFS_NAME);

		}

		capabilities = new ArrayList<Object>();
		
		ActivationCapabilityType capAct = null;

		if (enableAttributeInfo != null) {
			if (capAct == null) {
				capAct = new ActivationCapabilityType();
			}
			ActivationStatusCapabilityType capActStatus = new ActivationStatusCapabilityType();
			capAct.setStatus(capActStatus);
			if (!enableAttributeInfo.isReturnedByDefault()) {
				capActStatus.setReturnedByDefault(false);
			}
		}
		
		if (enableDateAttributeInfo != null) {
			if (capAct == null) {
				capAct = new ActivationCapabilityType();
			}
			ActivationValidityCapabilityType capValidFrom = new ActivationValidityCapabilityType();
			capAct.setValidFrom(capValidFrom);
			if (!enableDateAttributeInfo.isReturnedByDefault()) {
				capValidFrom.setReturnedByDefault(false);
			}
		}

		if (disableDateAttributeInfo != null) {
			if (capAct == null) {
				capAct = new ActivationCapabilityType();
			}
			ActivationValidityCapabilityType capValidTo = new ActivationValidityCapabilityType();
			capAct.setValidTo(capValidTo);
			if (!disableDateAttributeInfo.isReturnedByDefault()) {
				capValidTo.setReturnedByDefault(false);
			}
		}
		
		if (lockoutAttributeInfo != null) {
			if (capAct == null) {
				capAct = new ActivationCapabilityType();
			}
			ActivationLockoutStatusCapabilityType capActStatus = new ActivationLockoutStatusCapabilityType();
			capAct.setLockoutStatus(capActStatus);
			if (!lockoutAttributeInfo.isReturnedByDefault()) {
				capActStatus.setReturnedByDefault(false);
			}
		}

		if (capAct != null) {
			capabilities.add(capabilityObjectFactory.createActivation(capAct));
		}

		if (passwordAttributeInfo != null) {
			CredentialsCapabilityType capCred = new CredentialsCapabilityType();
			PasswordCapabilityType capPass = new PasswordCapabilityType();
			if (!passwordAttributeInfo.isReturnedByDefault()) {
				capPass.setReturnedByDefault(false);
			}
			capCred.setPassword(capPass);
			capabilities.add(capabilityObjectFactory.createCredentials(capCred));
		}

		// Create capabilities from supported connector operations

		Set<Class<? extends APIOperation>> supportedOperations = icfConnectorFacade.getSupportedOperations();
		
		LOGGER.trace("Connector supported operations: {}", supportedOperations);

		if (supportedOperations.contains(SyncApiOp.class)) {
			LiveSyncCapabilityType capSync = new LiveSyncCapabilityType();
			capabilities.add(capabilityObjectFactory.createLiveSync(capSync));
		}

		if (supportedOperations.contains(TestApiOp.class)) {
			TestConnectionCapabilityType capTest = new TestConnectionCapabilityType();
			capabilities.add(capabilityObjectFactory.createTestConnection(capTest));
		}
		
		if (supportedOperations.contains(CreateApiOp.class)){
			CreateCapabilityType capCreate = new CreateCapabilityType();
			capabilities.add(capabilityObjectFactory.createCreate(capCreate));
		}
		
		if (supportedOperations.contains(GetApiOp.class) || supportedOperations.contains(SearchApiOp.class)){
			ReadCapabilityType capRead = new ReadCapabilityType();
			capabilities.add(capabilityObjectFactory.createRead(capRead));
		}
		
		if (supportedOperations.contains(UpdateApiOp.class)){
			UpdateCapabilityType capUpdate = new UpdateCapabilityType();
			capabilities.add(capabilityObjectFactory.createUpdate(capUpdate));
		}
		
		if (supportedOperations.contains(DeleteApiOp.class)){
			DeleteCapabilityType capDelete = new DeleteCapabilityType();
			capabilities.add(capabilityObjectFactory.createDelete(capDelete));
		}

		if (supportedOperations.contains(ScriptOnResourceApiOp.class)
				|| supportedOperations.contains(ScriptOnConnectorApiOp.class)) {
			ScriptCapabilityType capScript = new ScriptCapabilityType();
			if (supportedOperations.contains(ScriptOnResourceApiOp.class)) {
				Host host = new Host();
				host.setType(ProvisioningScriptHostType.RESOURCE);
				capScript.getHost().add(host);
				// language is unknown here
			}
			if (supportedOperations.contains(ScriptOnConnectorApiOp.class)) {
				Host host = new Host();
				host.setType(ProvisioningScriptHostType.CONNECTOR);
				capScript.getHost().add(host);
				// language is unknown here
			}
			capabilities.add(capabilityObjectFactory.createScript(capScript));
		}

	}

	private boolean shouldBeGenerated(List<QName> generateObjectClasses,
			QName objectClassXsdName) {
		if (generateObjectClasses == null || generateObjectClasses.isEmpty()){
			return true;
		}
		
		for (QName objClassToGenerate : generateObjectClasses){
			if (objClassToGenerate.equals(objectClassXsdName)){
				return true;
			}
		}
		
		return false;
	}

	@Override
	public <T extends ShadowType> PrismObject<T> fetchObject(Class<T> type,
			ObjectClassComplexTypeDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute<?>> identifiers, AttributesToReturn attributesToReturn, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
			SchemaException, SecurityViolationException, ConfigurationException {

		// Result type for this operation
		OperationResult result = parentResult.createMinorSubresult(ConnectorInstance.class.getName()
				+ ".fetchObject");
		result.addParam("resourceObjectDefinition", objectClassDefinition);
		result.addCollectionOfSerializablesAsParam("identifiers", identifiers);
		result.addContext("connector", connectorType);

		if (icfConnectorFacade == null) {
			result.recordFatalError("Attempt to use unconfigured connector");
			throw new IllegalStateException("Attempt to use unconfigured connector "
					+ ObjectTypeUtil.toShortString(connectorType));
		}

		// Get UID from the set of identifiers
		Uid uid = getUid(identifiers);
		if (uid == null) {
			result.recordFatalError("Required attribute UID not found in identification set while attempting to fetch object identified by "
					+ identifiers + " from " + ObjectTypeUtil.toShortString(connectorType));
			throw new IllegalArgumentException(
					"Required attribute UID not found in identification set while attempting to fetch object identified by "
							+ identifiers + " from " + ObjectTypeUtil.toShortString(connectorType));
		}

		ObjectClass icfObjectClass = icfNameMapper.objectClassToIcf(objectClassDefinition, getSchemaNamespace(), connectorType);
		if (icfObjectClass == null) {
			result.recordFatalError("Unable to determine object class from QName "
					+ objectClassDefinition.getTypeName()
					+ " while attempting to fetch object identified by " + identifiers + " from "
					+ ObjectTypeUtil.toShortString(connectorType));
			throw new IllegalArgumentException("Unable to determine object class from QName "
					+ objectClassDefinition.getTypeName()
					+ " while attempting to fetch object identified by " + identifiers + " from "
					+ ObjectTypeUtil.toShortString(connectorType));
		}
		
		OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
		String[] attributesToGet = convertToIcfAttrsToGet(objectClassDefinition, attributesToReturn);
		if (attributesToGet != null) {
			optionsBuilder.setAttributesToGet(attributesToGet);
		}
		OperationOptions options = optionsBuilder.build();
		
		ConnectorObject co = null;
		try {

			// Invoke the ICF connector
			co = fetchConnectorObject(icfObjectClass, uid, options,
					result);

		} catch (CommunicationException ex) {
			result.recordFatalError(ex);
			// This is fatal. No point in continuing. Just re-throw the
			// exception.
			throw ex;
		} catch (GenericFrameworkException ex) {
			result.recordFatalError(ex);
			// This is fatal. No point in continuing. Just re-throw the
			// exception.
			throw ex;
		} catch (ConfigurationException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Object not found");
			throw new ObjectNotFoundException("Object identified by " + identifiers + " was not found by "
					+ connectorType);
		} catch (SchemaException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (RuntimeException ex) {
			result.recordFatalError(ex);
			throw ex;
		}

		if (co == null) {
			result.recordFatalError("Object not found");
			throw new ObjectNotFoundException("Object identified by " + identifiers + " was not found by "
					+ connectorType);
		}

		PrismObjectDefinition<T> shadowDefinition = toShadowDefinition(objectClassDefinition);
		PrismObject<T> shadow = convertToResourceObject(co, shadowDefinition, false);

		result.recordSuccess();
		return shadow;

	}

	private <T extends ShadowType> PrismObjectDefinition<T> toShadowDefinition(
			ObjectClassComplexTypeDefinition objectClassDefinition) {
		ResourceAttributeContainerDefinition resourceAttributeContainerDefinition = objectClassDefinition
				.toResourceAttributeContainerDefinition(ShadowType.F_ATTRIBUTES);
		return resourceAttributeContainerDefinition.toShadowDefinition();
	}

	/**
	 * Returns null if nothing is found.
	 */
	private ConnectorObject fetchConnectorObject(ObjectClass icfObjectClass, Uid uid,
			OperationOptions options, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SecurityViolationException, SchemaException, ConfigurationException {

		// Connector operation cannot create result for itself, so we need to
		// create result for it
		OperationResult icfResult = parentResult.createMinorSubresult(ConnectorFacade.class.getName()
				+ ".getObject");
		icfResult.addParam("objectClass", icfObjectClass.toString());
		icfResult.addParam("uid", uid.getUidValue());
		icfResult.addArbitraryObjectAsParam("options", options);
		icfResult.addContext("connector", icfConnectorFacade.getClass());
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Fetching connector object ObjectClass={}, UID={}, options={}",
					new Object[]{icfObjectClass, uid, UcfUtil.dumpOptions(options)});
		}
		
		ConnectorObject co = null;
		try {

			// Invoke the ICF connector
			co = icfConnectorFacade.getObject(icfObjectClass, uid, options);

			icfResult.recordSuccess();
		} catch (Throwable ex) {
			String desc = this.getHumanReadableName() + " while getting object identified by ICF UID '"+uid.getUidValue()+"'";
			Throwable midpointEx = processIcfException(ex, desc, icfResult);
			icfResult.computeStatus("Add object failed");

			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof CommunicationException) {
				icfResult.muteError();
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof ConfigurationException) {
				throw (ConfigurationException) midpointEx;
			} else if (midpointEx instanceof SecurityViolationException){
				throw (SecurityViolationException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException)midpointEx;
			} else if (midpointEx instanceof Error) {
				throw (Error)midpointEx;
			} else {
				throw new SystemException(midpointEx.getClass().getName()+": "+midpointEx.getMessage(), midpointEx);
			}
		
		}

		return co;
	}

	private String[] convertToIcfAttrsToGet(ObjectClassComplexTypeDefinition objectClassDefinition, AttributesToReturn attributesToReturn) throws SchemaException {
		if (attributesToReturn == null) {
			return null;
		}
		Collection<? extends ResourceAttributeDefinition> attrs = attributesToReturn.getAttributesToReturn();
		if (attributesToReturn.isReturnDefaultAttributes() && !attributesToReturn.isReturnPasswordExplicit()
				&& (attrs == null || attrs.isEmpty())) {
			return null;
		}
		List<String> icfAttrsToGet = new ArrayList<String>(); 
		if (attributesToReturn.isReturnDefaultAttributes()) {
			// Add all the attributes that are defined as "returned by default" by the schema
			for (ResourceAttributeDefinition attributeDef: objectClassDefinition.getAttributeDefinitions()) {
				if (attributeDef.isReturnedByDefault()) {
					String attrName = icfNameMapper.convertAttributeNameToIcf(attributeDef.getName(), getSchemaNamespace());
					icfAttrsToGet.add(attrName);
				}
			}
		}
		if (attributesToReturn.isReturnPasswordExplicit() 
				|| (attributesToReturn.isReturnDefaultAttributes() && passwordReturnedByDefault())) {
			icfAttrsToGet.add(OperationalAttributes.PASSWORD_NAME);
		}
		if (attributesToReturn.isReturnAdministrativeStatusExplicit() 
				|| (attributesToReturn.isReturnDefaultAttributes() && enabledReturnedByDefault())) {
			icfAttrsToGet.add(OperationalAttributes.ENABLE_NAME);
		}
		if (attributesToReturn.isReturnLockoutStatusExplicit()
				|| (attributesToReturn.isReturnDefaultAttributes() && lockoutReturnedByDefault())) {
			icfAttrsToGet.add(OperationalAttributes.LOCK_OUT_NAME);
		}
		if (attrs != null) {
			for (ResourceAttributeDefinition attrDef: attrs) {
				String attrName = icfNameMapper.convertAttributeNameToIcf(attrDef.getName(), getSchemaNamespace());
				if (!icfAttrsToGet.contains(attrName)) {
					icfAttrsToGet.add(attrName);
				}
			}
		}
		return icfAttrsToGet.toArray(new String[0]);
	}

	private boolean passwordReturnedByDefault() {
		CredentialsCapabilityType capability = CapabilityUtil.getCapability(capabilities, CredentialsCapabilityType.class);
		return CapabilityUtil.isPasswordReturnedByDefault(capability);
	}
	
	private boolean enabledReturnedByDefault() {
		ActivationCapabilityType capability = CapabilityUtil.getCapability(capabilities, ActivationCapabilityType.class);
		return CapabilityUtil.isActivationStatusReturnedByDefault(capability);
	}

	private boolean lockoutReturnedByDefault() {
		ActivationCapabilityType capability = CapabilityUtil.getCapability(capabilities, ActivationCapabilityType.class);
		return CapabilityUtil.isActivationLockoutStatusReturnedByDefault(capability);
	}

	@Override
	public Collection<ResourceAttribute<?>> addObject(PrismObject<? extends ShadowType> object,
			Collection<Operation> additionalOperations, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		validateShadow(object, "add", false);
		ShadowType objectType = object.asObjectable();

		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(object);
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".addObject");
		result.addParam("resourceObject", object);
		result.addParam("additionalOperations", DebugUtil.debugDump(additionalOperations));         // because of serialization issues

		// getting icf object class from resource object class
		ObjectClass objectClass = icfNameMapper.objectClassToIcf(object, getSchemaNamespace(), connectorType);

		if (objectClass == null) {
			result.recordFatalError("Couldn't get icf object class from " + object);
			throw new IllegalArgumentException("Couldn't get icf object class from " + object);
		}

		// setting ifc attributes from resource object attributes
		Set<Attribute> attributes = null;
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("midPoint object before conversion:\n{}", attributesContainer.debugDump());
			}
			attributes = convertFromResourceObject(attributesContainer, result);

			if (objectType.getCredentials() != null && objectType.getCredentials().getPassword() != null) {
				PasswordType password = objectType.getCredentials().getPassword();
				ProtectedStringType protectedString = password.getValue();
				GuardedString guardedPassword = toGuardedString(protectedString, "new password");
				attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME,
						guardedPassword));
			}
			
			if (ActivationUtil.hasAdministrativeActivation(objectType)){
				attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, ActivationUtil.isAdministrativeEnabled(objectType)));
			}
			
			if (ActivationUtil.hasValidFrom(objectType)){
				attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, XmlTypeConverter.toMillis(objectType.getActivation().getValidFrom())));
			}

			if (ActivationUtil.hasValidTo(objectType)){
				attributes.add(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, XmlTypeConverter.toMillis(objectType.getActivation().getValidTo())));
			}
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("ICF attributes after conversion:\n{}", IcfUtil.dump(attributes));
			}
		} catch (SchemaException ex) {
			result.recordFatalError(
					"Error while converting resource object attributes. Reason: " + ex.getMessage(), ex);
			throw new SchemaException("Error while converting resource object attributes. Reason: "
					+ ex.getMessage(), ex);
		}
		if (attributes == null) {
			result.recordFatalError("Couldn't set attributes for icf.");
			throw new IllegalStateException("Couldn't set attributes for icf.");
		}

		checkAndExecuteAdditionalOperation(additionalOperations, BeforeAfterType.BEFORE, result);

		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".create");
		icfResult.addArbitraryObjectAsParam("objectClass", objectClass);
		icfResult.addArbitraryCollectionAsParam("attributes", attributes);
		icfResult.addParam("options", null);
		icfResult.addContext("connector", icfConnectorFacade.getClass());

		Uid uid = null;
		try {

			// CALL THE ICF FRAMEWORK
			uid = icfConnectorFacade.create(objectClass, attributes, new OperationOptionsBuilder().build());

		} catch (Throwable ex) {
			Throwable midpointEx = processIcfException(ex, this, icfResult);
			result.computeStatus("Add object failed");

			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof ObjectAlreadyExistsException) {
				throw (ObjectAlreadyExistsException) midpointEx;
			} else if (midpointEx instanceof CommunicationException) {
//				icfResult.muteError();
//				result.muteError();
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof ConfigurationException) {
				throw (ConfigurationException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else if (midpointEx instanceof Error) {
				throw (Error) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}
		
		checkAndExecuteAdditionalOperation(additionalOperations, BeforeAfterType.AFTER, result);

		if (uid == null || uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
			icfResult.recordFatalError("ICF did not returned UID after create");
			result.computeStatus("Add object failed");
			throw new GenericFrameworkException("ICF did not returned UID after create");
		}

		ResourceAttributeDefinition uidDefinition = getUidDefinition(attributesContainer.getDefinition());
		if (uidDefinition == null) {
			throw new IllegalArgumentException("No definition for ICF UID attribute found in definition "
					+ attributesContainer.getDefinition());
		}
		ResourceAttribute<?> attribute = createUidAttribute(uid, uidDefinition);
		attributesContainer.getValue().addReplaceExisting(attribute);
		icfResult.recordSuccess();

		result.recordSuccess();
		return attributesContainer.getAttributes();
	}

	private void validateShadow(PrismObject<? extends ShadowType> shadow, String operation,
			boolean requireUid) {
		if (shadow == null) {
			throw new IllegalArgumentException("Cannot " + operation + " null " + shadow);
		}
		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			throw new IllegalArgumentException("Cannot " + operation + " shadow without attributes container");
		}
		ResourceAttributeContainer resourceAttributesContainer = ShadowUtil
				.getAttributesContainer(shadow);
		if (resourceAttributesContainer == null) {
			throw new IllegalArgumentException("Cannot " + operation
					+ " shadow without attributes container of type ResourceAttributeContainer, got "
					+ attributesContainer.getClass());
		}
		if (requireUid) {
			Collection<ResourceAttribute<?>> identifiers = resourceAttributesContainer.getIdentifiers();
			if (identifiers == null || identifiers.isEmpty()) {
				throw new IllegalArgumentException("Cannot " + operation + " shadow without identifiers");
			}
		}
	}

	@Override
	public Set<PropertyModificationOperation> modifyObject(ObjectClassComplexTypeDefinition objectClass,
			Collection<? extends ResourceAttribute<?>> identifiers, Collection<Operation> changes,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".modifyObject");
		result.addParam("objectClass", objectClass);
		result.addCollectionOfSerializablesAsParam("identifiers", identifiers);
		result.addArbitraryCollectionAsParam("changes", changes);
		
		if (changes.isEmpty()){
			LOGGER.info("No modifications for connector object specified. Skipping processing.");
			result.recordSuccess();
			return new HashSet<PropertyModificationOperation>();
		}

		ObjectClass objClass = icfNameMapper.objectClassToIcf(objectClass, getSchemaNamespace(), connectorType);
		
		Uid uid = getUid(identifiers);
		String originalUid = uid.getUidValue();

		Collection<ResourceAttribute<?>> addValues = new HashSet<ResourceAttribute<?>>();
		Collection<ResourceAttribute<?>> updateValues = new HashSet<ResourceAttribute<?>>();
		Collection<ResourceAttribute<?>> valuesToRemove = new HashSet<ResourceAttribute<?>>();

		Set<Operation> additionalOperations = new HashSet<Operation>();
		PasswordChangeOperation passwordChangeOperation = null;
		Collection<PropertyDelta<?>> activationDeltas = new HashSet<PropertyDelta<?>>();
		PropertyDelta<ProtectedStringType> passwordDelta = null;

		for (Operation operation : changes) {
			if (operation instanceof PropertyModificationOperation) {
				PropertyModificationOperation change = (PropertyModificationOperation) operation;
				PropertyDelta<?> delta = change.getPropertyDelta();

				if (delta.getParentPath().equals(new ItemPath(ShadowType.F_ATTRIBUTES))) {
					if (delta.getDefinition() == null || !(delta.getDefinition() instanceof ResourceAttributeDefinition)) {
						ResourceAttributeDefinition def = objectClass
								.findAttributeDefinition(delta.getElementName());
						if (def == null) {
							String message = "No definition for attribute "+delta.getElementName()+" used in modification delta";
							result.recordFatalError(message);
							throw new SchemaException(message);
						}
						try {
							delta.applyDefinition(def);
						} catch (SchemaException e) {
							result.recordFatalError(e.getMessage(), e);
							throw e;
						}
					}
					// Change in (ordinary) attributes. Transform to the ICF
					// attributes.
					if (delta.isAdd()) {
						ResourceAttribute<?> addAttribute = (ResourceAttribute<?>) delta.instantiateEmptyProperty();
						addAttribute.addValues((Collection)PrismValue.cloneCollection(delta.getValuesToAdd()));
						if (addAttribute.getDefinition().isMultiValue()) {
							addValues.add(addAttribute);
						} else {
							// Force "update" for single-valued attributes instead of "add". This is saving one
							// read in some cases. It should also make no substantial difference in such case.
							// But it is working around some connector bugs.
							updateValues.add(addAttribute);
						}
					}
					if (delta.isDelete()) {
						ResourceAttribute<?> deleteAttribute = (ResourceAttribute<?>) delta.instantiateEmptyProperty();
						if (deleteAttribute.getDefinition().isMultiValue()) {
							deleteAttribute.addValues((Collection)PrismValue.cloneCollection(delta.getValuesToDelete()));
							valuesToRemove.add(deleteAttribute);
						} else {
							// Force "update" for single-valued attributes instead of "add". This is saving one
							// read in some cases. 
							// Update attribute to no values. This will efficiently clean up the attribute.
							// It should also make no substantial difference in such case. 
							// But it is working around some connector bugs.
							updateValues.add(deleteAttribute);
						}
					}
					if (delta.isReplace()) {
						ResourceAttribute<?> updateAttribute = (ResourceAttribute<?>) delta
								.instantiateEmptyProperty();
						updateAttribute.addValues((Collection)PrismValue.cloneCollection(delta.getValuesToReplace()));
						updateValues.add(updateAttribute);
					}
				} else if (delta.getParentPath().equals(new ItemPath(ShadowType.F_ACTIVATION))) {
					activationDeltas.add(delta);
				} else if (delta.getParentPath().equals(
						new ItemPath(new ItemPath(ShadowType.F_CREDENTIALS),
								CredentialsType.F_PASSWORD))) {
					passwordDelta = (PropertyDelta<ProtectedStringType>) delta;
				} else {
					throw new SchemaException("Change of unknown attribute " + delta.getElementName());
				}

			} else if (operation instanceof PasswordChangeOperation) {
				passwordChangeOperation = (PasswordChangeOperation) operation;
				// TODO: check for multiple occurrences and fail

			} else if (operation instanceof ExecuteProvisioningScriptOperation) {
				ExecuteProvisioningScriptOperation scriptOperation = (ExecuteProvisioningScriptOperation) operation;
				additionalOperations.add(scriptOperation);

			} else {
				throw new IllegalArgumentException("Unknown operation type " + operation.getClass().getName()
						+ ": " + operation);
			}

		}

		// Needs three complete try-catch blocks because we need to create
		// icfResult for each operation
		// and handle the faults individually

		checkAndExecuteAdditionalOperation(additionalOperations, BeforeAfterType.BEFORE, result);

		OperationResult icfResult = null;
		try {
			if (addValues != null && !addValues.isEmpty()) {
				Set<Attribute> attributes = null;
				try {
					attributes = convertFromResourceObject(addValues, result);
				} catch (SchemaException ex) {
					result.recordFatalError("Error while converting resource object attributes. Reason: "
							+ ex.getMessage(), ex);
					throw new SchemaException("Error while converting resource object attributes. Reason: "
							+ ex.getMessage(), ex);
				}
				OperationOptions options = new OperationOptionsBuilder().build();
				icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".addAttributeValues");
				icfResult.addParam("objectClass", objectClass);
				icfResult.addParam("uid", uid.getUidValue());
				icfResult.addArbitraryCollectionAsParam("attributes", attributes);
				icfResult.addArbitraryObjectAsParam("options", options);
				icfResult.addContext("connector", icfConnectorFacade.getClass());

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
							"Invoking ICF addAttributeValues(), objectclass={}, uid={}, attributes=\n{}",
							new Object[] { objClass, uid, dumpAttributes(attributes) });
				}

				uid = icfConnectorFacade.addAttributeValues(objClass, uid, attributes, options);

				icfResult.recordSuccess();
			}
		} catch (Throwable ex) {
			String desc = this.getHumanReadableName() + " while adding attribute values to object identified by ICF UID '"+uid.getUidValue()+"'";
			Throwable midpointEx = processIcfException(ex, desc, icfResult);
			result.computeStatus("Adding attribute values failed");
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException) midpointEx;
			} else if (midpointEx instanceof CommunicationException) {
				//in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
				result.muteError();
				icfResult.muteError();
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof AlreadyExistsException) {
				throw (AlreadyExistsException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else if (midpointEx instanceof SecurityViolationException){
				throw (SecurityViolationException) midpointEx;
			} else if (midpointEx instanceof Error){
				throw (Error) midpointEx;
			}else{
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}

		if (updateValues != null && !updateValues.isEmpty() || activationDeltas != null
				|| passwordDelta != null) {

			Set<Attribute> updateAttributes = null;

			try {
				updateAttributes = convertFromResourceObject(updateValues, result);
			} catch (SchemaException ex) {
				result.recordFatalError(
						"Error while converting resource object attributes. Reason: " + ex.getMessage(), ex);
				throw new SchemaException("Error while converting resource object attributes. Reason: "
						+ ex.getMessage(), ex);
			}

			if (activationDeltas != null) {
				// Activation change means modification of attributes
				convertFromActivation(updateAttributes, activationDeltas);
			}

			if (passwordDelta != null) {
				// Activation change means modification of attributes
				convertFromPassword(updateAttributes, passwordDelta);
			}

			OperationOptions options = new OperationOptionsBuilder().build();
			icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".update");
			icfResult.addParam("objectClass", objectClass);
			icfResult.addParam("uid", uid.getUidValue());
			icfResult.addArbitraryCollectionAsParam("attributes", updateAttributes);
			icfResult.addArbitraryObjectAsParam("options", options);
			icfResult.addContext("connector", icfConnectorFacade.getClass());

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Invoking ICF update(), objectclass={}, uid={}, attributes=\n{}", new Object[] {
						objClass, uid, dumpAttributes(updateAttributes) });
			}

			try {
				// Call ICF
				uid = icfConnectorFacade.update(objClass, uid, updateAttributes, options);

				icfResult.recordSuccess();
			} catch (Throwable ex) {
				String desc = this.getHumanReadableName() + " while updating object identified by ICF UID '"+uid.getUidValue()+"'";
				Throwable midpointEx = processIcfException(ex, desc, icfResult);
				result.computeStatus("Update failed");
				// Do some kind of acrobatics to do proper throwing of checked
				// exception
				if (midpointEx instanceof ObjectNotFoundException) {
					throw (ObjectNotFoundException) midpointEx;
				} else if (midpointEx instanceof CommunicationException) {
					//in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
					result.muteError();
					icfResult.muteError();
					throw (CommunicationException) midpointEx;
				} else if (midpointEx instanceof GenericFrameworkException) {
					throw (GenericFrameworkException) midpointEx;
				} else if (midpointEx instanceof SchemaException) {
					throw (SchemaException) midpointEx;
				} else if (midpointEx instanceof ObjectAlreadyExistsException) {
					throw (ObjectAlreadyExistsException) midpointEx;
				} else if (midpointEx instanceof RuntimeException) {
					throw (RuntimeException) midpointEx;
                } else if (midpointEx instanceof SecurityViolationException) {
                    throw (SecurityViolationException) midpointEx;
				} else if (midpointEx instanceof Error) {
					throw (Error) midpointEx;
				} else {
					throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
				}
			}
		}

		try {
			if (valuesToRemove != null && !valuesToRemove.isEmpty()) {
				Set<Attribute> attributes = null;
				try {
					attributes = convertFromResourceObject(valuesToRemove, result);
				} catch (SchemaException ex) {
					result.recordFatalError("Error while converting resource object attributes. Reason: "
							+ ex.getMessage(), ex);
					throw new SchemaException("Error while converting resource object attributes. Reason: "
							+ ex.getMessage(), ex);
				}
				OperationOptions options = new OperationOptionsBuilder().build();
				icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".update");
				icfResult.addParam("objectClass", objectClass);
				icfResult.addParam("uid", uid.getUidValue());
				icfResult.addArbitraryCollectionAsParam("attributes", attributes);
				icfResult.addArbitraryObjectAsParam("options", options);
				icfResult.addContext("connector", icfConnectorFacade.getClass());

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
							"Invoking ICF removeAttributeValues(), objectclass={}, uid={}, attributes=\n{}",
							new Object[] { objClass, uid, dumpAttributes(attributes) });
				}

				uid = icfConnectorFacade.removeAttributeValues(objClass, uid, attributes, options);
				icfResult.recordSuccess();
			}
		} catch (Throwable ex) {
			String desc = this.getHumanReadableName() + " while removing attribute values from object identified by ICF UID '"+uid.getUidValue()+"'";
			Throwable midpointEx = processIcfException(ex, desc, icfResult);
			result.computeStatus("Removing attribute values failed");
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException) midpointEx;
			} else if (midpointEx instanceof CommunicationException) {
				//in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
				result.muteError();
				icfResult.muteError();
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof ObjectAlreadyExistsException) {
				throw (ObjectAlreadyExistsException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
            } else if (midpointEx instanceof SecurityViolationException) {
                throw (SecurityViolationException) midpointEx;
			} else if (midpointEx instanceof Error) {
				throw (Error) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}
		checkAndExecuteAdditionalOperation(additionalOperations, BeforeAfterType.AFTER, result);
		
		result.computeStatus();

		Set<PropertyModificationOperation> sideEffectChanges = new HashSet<PropertyModificationOperation>();
		if (!originalUid.equals(uid.getUidValue())) {
			// UID was changed during the operation, this is most likely a
			// rename
			PropertyDelta<String> uidDelta = createUidDelta(uid, getUidDefinition(identifiers));
			PropertyModificationOperation uidMod = new PropertyModificationOperation(uidDelta);
			sideEffectChanges.add(uidMod);
		}
		return sideEffectChanges;
	}

	private PropertyDelta<String> createUidDelta(Uid uid, ResourceAttributeDefinition uidDefinition) {
		QName attributeName = icfNameMapper.convertAttributeNameToQName(uid.getName(), getSchemaNamespace());
		PropertyDelta<String> uidDelta = new PropertyDelta<String>(new ItemPath(ShadowType.F_ATTRIBUTES, attributeName),
				uidDefinition, prismContext);
		uidDelta.setValueToReplace(new PrismPropertyValue<String>(uid.getUidValue()));
		return uidDelta;
	}

	private String dumpAttributes(Set<Attribute> attributes) {
		if (attributes == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		for (Attribute attr : attributes) {
			for (Object value : attr.getValue()) {
				sb.append(attr.getName());
				sb.append(" = ");
				sb.append(value);
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	@Override
	public void deleteObject(ObjectClassComplexTypeDefinition objectClass,
			Collection<Operation> additionalOperations, Collection<? extends ResourceAttribute<?>> identifiers,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".deleteObject");
		result.addCollectionOfSerializablesAsParam("identifiers", identifiers);

		ObjectClass objClass = icfNameMapper.objectClassToIcf(objectClass, getSchemaNamespace(), connectorType);
		Uid uid = getUid(identifiers);

		checkAndExecuteAdditionalOperation(additionalOperations, BeforeAfterType.BEFORE, result);
		
		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".delete");
		icfResult.addArbitraryObjectAsParam("uid", uid);
		icfResult.addArbitraryObjectAsParam("objectClass", objClass);
		icfResult.addContext("connector", icfConnectorFacade.getClass());

		try {

			icfConnectorFacade.delete(objClass, uid, new OperationOptionsBuilder().build());
			
			icfResult.recordSuccess();

		} catch (Throwable ex) {
			String desc = this.getHumanReadableName() + " while deleting object identified by ICF UID '"+uid.getUidValue()+"'";
			Throwable midpointEx = processIcfException(ex, desc, icfResult);
			result.computeStatus("Removing attribute values failed");
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException) midpointEx;
			} else if (midpointEx instanceof CommunicationException) {
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				// Schema exception during delete? It must be a missing UID
				throw new IllegalArgumentException(midpointEx.getMessage(), midpointEx);
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else if (midpointEx instanceof Error) {
				throw (Error) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}
		
		checkAndExecuteAdditionalOperation(additionalOperations, BeforeAfterType.AFTER, result);

		result.computeStatus();
	}

	@Override
	public PrismProperty<?> deserializeToken(Object serializedToken) {
		return createTokenProperty(serializedToken);
	}

	@Override
	public PrismProperty<?> fetchCurrentToken(ObjectClassComplexTypeDefinition objectClass,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".fetchCurrentToken");
		result.addParam("objectClass", objectClass);

		ObjectClass icfObjectClass = icfNameMapper.objectClassToIcf(objectClass, getSchemaNamespace(), connectorType);
		
		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".sync");
		icfResult.addContext("connector", icfConnectorFacade.getClass());
		icfResult.addArbitraryObjectAsParam("icfObjectClass", icfObjectClass);
		
		SyncToken syncToken = null;
		try {
			syncToken = icfConnectorFacade.getLatestSyncToken(icfObjectClass);
			icfResult.recordSuccess();
			icfResult.addReturn("syncToken", syncToken==null?null:String.valueOf(syncToken.getValue()));
		} catch (Throwable ex) {
			Throwable midpointEx = processIcfException(ex, this, icfResult);
			result.computeStatus();
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof CommunicationException) {
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else if (midpointEx instanceof Error) {
				throw (Error) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}

		if (syncToken == null) {
			result.recordWarning("Resource have not provided a current sync token");
			return null;
		}

		PrismProperty<?> property = getToken(syncToken);
		result.recordSuccess();
		return property;
	}

	@Override
	public <T extends ShadowType> List<Change<T>>  fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken,
			AttributesToReturn attrsToReturn, OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			SchemaException, ConfigurationException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".fetchChanges");
		result.addContext("objectClass", objectClass);
		result.addParam("lastToken", lastToken);

		// create sync token from the property last token
		SyncToken syncToken = null;
		try {
			syncToken = getSyncToken(lastToken);
			LOGGER.trace("Sync token created from the property last token: {}", syncToken==null?null:syncToken.getValue());
		} catch (SchemaException ex) {
			result.recordFatalError(ex.getMessage(), ex);
			throw new SchemaException(ex.getMessage(), ex);
		}

		final List<SyncDelta> syncDeltas = new ArrayList<SyncDelta>();
		// get icf object class
		ObjectClass icfObjectClass = icfNameMapper.objectClassToIcf(objectClass, getSchemaNamespace(), connectorType);

		OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
		String[] attributesToGet = convertToIcfAttrsToGet(objectClass, attrsToReturn);
		if (attributesToGet != null) {
			optionsBuilder.setAttributesToGet(attributesToGet);
		}
		OperationOptions options = optionsBuilder.build();
		
		SyncResultsHandler syncHandler = new SyncResultsHandler() {

			@Override
			public boolean handle(SyncDelta delta) {
				LOGGER.trace("Detected sync delta: {}", delta);
				return syncDeltas.add(delta);

			}
		};

		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".sync");
		icfResult.addContext("connector", icfConnectorFacade.getClass());
		icfResult.addArbitraryObjectAsParam("icfObjectClass", icfObjectClass);
		icfResult.addArbitraryObjectAsParam("syncToken", syncToken);
		icfResult.addArbitraryObjectAsParam("syncHandler", syncHandler);

		try {
			icfConnectorFacade.sync(icfObjectClass, syncToken, syncHandler,
					options);
			icfResult.recordSuccess();
			icfResult.addReturn(OperationResult.RETURN_COUNT, syncDeltas.size());
		} catch (Throwable ex) {
			Throwable midpointEx = processIcfException(ex, this, icfResult);
			result.computeStatus();
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof CommunicationException) {
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else if (midpointEx instanceof Error) {
				throw (Error) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}
		// convert changes from icf to midpoint Change
		List<Change<T>> changeList = null;
		try {
			changeList = getChangesFromSyncDeltas(icfObjectClass, syncDeltas, resourceSchema, result);
		} catch (SchemaException ex) {
			result.recordFatalError(ex.getMessage(), ex);
			throw new SchemaException(ex.getMessage(), ex);
		}

		result.recordSuccess();
		result.addReturn(OperationResult.RETURN_COUNT, changeList == null ? 0 : changeList.size());
		return changeList;
	}

	@Override
	public void test(OperationResult parentResult) {

		OperationResult connectionResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
		connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorInstance.class);
		connectionResult.addContext("connector", connectorType);

		try {
			icfConnectorFacade.test();
			connectionResult.recordSuccess();
		} catch (UnsupportedOperationException ex) {
			// Connector does not support test connection.
			connectionResult.recordStatus(OperationResultStatus.NOT_APPLICABLE,
					"Operation not supported by the connector", ex);
			// Do not rethrow. Recording the status is just OK.
		} catch (Throwable icfEx) {
			Throwable midPointEx = processIcfException(icfEx, this, connectionResult);
			connectionResult.recordFatalError(midPointEx);
		}
	}


	@Override
	public <T extends ShadowType> void search(ObjectClassComplexTypeDefinition objectClassDefinition, final ObjectQuery query,
			final ResultHandler<T> handler, AttributesToReturn attributesToReturn, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, SchemaException {

		// Result type for this operation
		final OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".search");
		result.addParam("objectClass", objectClassDefinition);
		result.addContext("connector", connectorType);

		if (objectClassDefinition == null) {
			result.recordFatalError("Object class not defined");
			throw new IllegalArgumentException("objectClass not defined");
		}

		ObjectClass icfObjectClass = icfNameMapper.objectClassToIcf(objectClassDefinition, getSchemaNamespace(), connectorType);
		if (icfObjectClass == null) {
			IllegalArgumentException ex = new IllegalArgumentException(
					"Unable to detemine object class from QName " + objectClassDefinition
							+ " while attempting to search objects by "
							+ ObjectTypeUtil.toShortString(connectorType));
			result.recordFatalError("Unable to detemine object class", ex);
			throw ex;
		}
		final PrismObjectDefinition<T> objectDefinition = toShadowDefinition(objectClassDefinition);


		ResultsHandler icfHandler = new ResultsHandler() {
			int count = 0;
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				// Convert ICF-specific connector object to a generic
				// ResourceObject
				if (query != null && query.getPaging() != null && query.getPaging().getOffset() != null
						&& query.getPaging().getMaxSize() != null) {
					if (count < query.getPaging().getOffset()){
						count++;
						return true;
					}
					
					if (count == (query.getPaging().getOffset() + query.getPaging().getMaxSize())) {
						return false;
				}

				}
				PrismObject<T> resourceObject;
				try {
					resourceObject = convertToResourceObject(connectorObject, objectDefinition, false);
				} catch (SchemaException e) {
					throw new IntermediateException(e);
				}

				// .. and pass it to the handler
				boolean cont = handler.handle(resourceObject);
				if (!cont) {
					result.recordPartialError("Stopped on request from the handler");

				}
				count++;
				return cont;
			}
		};
		
		OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
		String[] attributesToGet = convertToIcfAttrsToGet(objectClassDefinition, attributesToReturn);
		if (attributesToGet != null) {
			optionsBuilder.setAttributesToGet(attributesToGet);
		}
		OperationOptions options = optionsBuilder.build();

		// Connector operation cannot create result for itself, so we need to
		// create result for it
		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".search");
		icfResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
		icfResult.addContext("connector", icfConnectorFacade.getClass());

		try {

			Filter filter = null;
			if (query != null && query.getFilter() != null) {
				// TODO : translation between connector filter and midpoint
				// filter
				FilterInterpreter interpreter = new FilterInterpreter(getSchemaNamespace());
				LOGGER.trace("Start to convert filter: {}", query.getFilter().debugDump());
				filter = interpreter.interpret(query.getFilter(), icfNameMapper);

				LOGGER.trace("ICF filter: {}", filter);
			}
			
			icfConnectorFacade.search(icfObjectClass, filter, icfHandler, options);

			icfResult.recordSuccess();
		} catch (IntermediateException inex) {
			SchemaException ex = (SchemaException) inex.getCause();
			icfResult.recordFatalError(ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (Throwable ex) {
			Throwable midpointEx = processIcfException(ex, this, icfResult);
			result.computeStatus();
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof CommunicationException) {
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else if (midpointEx instanceof Error) {
				throw (Error) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}

		if (result.isUnknown()) {
			result.recordSuccess();
		}
	}

	// UTILITY METHODS

	

	/**
	 * Looks up ICF Uid identifier in a (potentially multi-valued) set of
	 * identifiers. Handy method to convert midPoint identifier style to an ICF
	 * identifier style.
	 * 
	 * @param identifiers
	 *            midPoint resource object identifiers
	 * @return ICF UID or null
	 */
	private Uid getUid(Collection<? extends ResourceAttribute<?>> identifiers) {
		for (ResourceAttribute<?> attr : identifiers) {
			if (attr.getElementName().equals(ConnectorFactoryIcfImpl.ICFS_UID)) {
				return new Uid(((ResourceAttribute<String>) attr).getValue().getValue());
			}
		}
		return null;
	}

	private ResourceAttributeDefinition getUidDefinition(ResourceAttributeContainerDefinition def) {
		return def.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
	}

	private ResourceAttributeDefinition getUidDefinition(Collection<? extends ResourceAttribute<?>> identifiers) {
		for (ResourceAttribute<?> attr : identifiers) {
			if (attr.getElementName().equals(ConnectorFactoryIcfImpl.ICFS_UID)) {
				return attr.getDefinition();
			}
		}
		return null;
	}

	private ResourceAttribute<String> createUidAttribute(Uid uid, ResourceAttributeDefinition uidDefinition) {
		ResourceAttribute<String> uidRoa = uidDefinition.instantiate();
		uidRoa.setValue(new PrismPropertyValue<String>(uid.getUidValue()));
		return uidRoa;
	}

	/**
	 * Converts ICF ConnectorObject to the midPoint ResourceObject.
	 * <p/>
	 * All the attributes are mapped using the same way as they are mapped in
	 * the schema (which is actually no mapping at all now).
	 * <p/>
	 * If an optional ResourceObjectDefinition was provided, the resulting
	 * ResourceObject is schema-aware (getDefinition() method works). If no
	 * ResourceObjectDefinition was provided, the object is schema-less. TODO:
	 * this still needs to be implemented.
	 * 
	 * @param co
	 *            ICF ConnectorObject to convert
	 * @param def
	 *            ResourceObjectDefinition (from the schema) or null
	 * @param full
	 *            if true it describes if the returned resource object should
	 *            contain all of the attributes defined in the schema, if false
	 *            the returned resource object will contain only attributed with
	 *            the non-null values.
	 * @return new mapped ResourceObject instance.
	 * @throws SchemaException
	 */
	private <T extends ShadowType> PrismObject<T> convertToResourceObject(ConnectorObject co,
			PrismObjectDefinition<T> objectDefinition, boolean full) throws SchemaException {

		PrismObject<T> shadowPrism = null;
		if (objectDefinition != null) {
			shadowPrism = objectDefinition.instantiate();
		} else {
			throw new SchemaException("No definition");
		}

		// LOGGER.trace("Instantiated prism object {} from connector object.",
		// shadowPrism.debugDump());

		T shadow = shadowPrism.asObjectable();
		ResourceAttributeContainer attributesContainer = (ResourceAttributeContainer) shadowPrism
				.findOrCreateContainer(ShadowType.F_ATTRIBUTES);
		ResourceAttributeContainerDefinition attributesContainerDefinition = attributesContainer.getDefinition();
		shadow.setObjectClass(attributesContainerDefinition.getTypeName());

		LOGGER.trace("Resource attribute container definition {}.", attributesContainerDefinition.debugDump());

		// Uid is always there
		Uid uid = co.getUid();
		ResourceAttribute<String> uidRoa = createUidAttribute(uid, getUidDefinition(attributesContainerDefinition));
		attributesContainer.getValue().add(uidRoa);

		for (Attribute icfAttr : co.getAttributes()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Reading ICF attribute {}: {}", icfAttr.getName(), icfAttr.getValue());
			}
			if (icfAttr.getName().equals(Uid.NAME)) {
				// UID is handled specially (see above)
				continue;
			}
			if (icfAttr.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
				// password has to go to the credentials section
				ProtectedStringType password = getSingleValue(icfAttr, ProtectedStringType.class);
				ShadowUtil.setPassword(shadow, password);
				LOGGER.trace("Converted password: {}", password);
				continue;
			}
			if (icfAttr.getName().equals(OperationalAttributes.ENABLE_NAME)) {
				Boolean enabled = getSingleValue(icfAttr, Boolean.class);
				ActivationType activationType = ShadowUtil.getOrCreateActivation(shadow);
				ActivationStatusType activationStatusType;
				if (enabled) {
					activationStatusType = ActivationStatusType.ENABLED;
				} else {
					activationStatusType = ActivationStatusType.DISABLED;
				}
				activationType.setAdministrativeStatus(activationStatusType);
				activationType.setEffectiveStatus(activationStatusType);
				LOGGER.trace("Converted activation administrativeStatus: {}", activationStatusType);
				continue;
			}
			
			if (icfAttr.getName().equals(OperationalAttributes.ENABLE_DATE_NAME)) {
				Long millis = getSingleValue(icfAttr, Long.class);
				ActivationType activationType = ShadowUtil.getOrCreateActivation(shadow);
				activationType.setValidFrom(XmlTypeConverter.createXMLGregorianCalendar(millis));
				continue;
			}

			if (icfAttr.getName().equals(OperationalAttributes.DISABLE_DATE_NAME)) {
				Long millis = getSingleValue(icfAttr, Long.class);
				ActivationType activationType = ShadowUtil.getOrCreateActivation(shadow);
				activationType.setValidTo(XmlTypeConverter.createXMLGregorianCalendar(millis));
				continue;
			}
			
			if (icfAttr.getName().equals(OperationalAttributes.LOCK_OUT_NAME)) {
				Boolean lockOut = getSingleValue(icfAttr, Boolean.class);
				ActivationType activationType = ShadowUtil.getOrCreateActivation(shadow);
				LockoutStatusType lockoutStatusType;
				if (lockOut) {
					lockoutStatusType = LockoutStatusType.LOCKED;
				} else {
					lockoutStatusType = LockoutStatusType.NORMAL;
				}
				activationType.setLockoutStatus(lockoutStatusType);
				LOGGER.trace("Converted activation lockoutStatus: {}", lockoutStatusType);
				continue;
			}

			QName qname = icfNameMapper.convertAttributeNameToQName(icfAttr.getName(), getSchemaNamespace());
			ResourceAttributeDefinition attributeDefinition = attributesContainerDefinition.findAttributeDefinition(qname);

			if (attributeDefinition == null) {
				throw new SchemaException("Unknown attribute "+qname+" in definition of object class "+attributesContainerDefinition.getTypeName()+". Original ICF name: "+icfAttr.getName(), qname);
			}

			ResourceAttribute<Object> resourceAttribute = attributeDefinition.instantiate(qname);

			// if true, we need to convert whole connector object to the
			// resource object also with the null-values attributes
			if (full) {
				if (icfAttr.getValue() != null) {
					// Convert the values. While most values do not need
					// conversions, some
					// of them may need it (e.g. GuardedString)
					for (Object icfValue : icfAttr.getValue()) {
						Object value = convertValueFromIcf(icfValue, qname);
						resourceAttribute.add(new PrismPropertyValue<Object>(value));
					}
				}

				LOGGER.trace("Converted attribute {}", resourceAttribute);
				attributesContainer.getValue().add(resourceAttribute);

				// in this case when false, we need only the attributes with the
				// non-null values.
			} else {
				if (icfAttr.getValue() != null && !icfAttr.getValue().isEmpty()) {
					// Convert the values. While most values do not need
					// conversions, some
					// of them may need it (e.g. GuardedString)
					boolean empty = true;
					for (Object icfValue : icfAttr.getValue()) {
						if (icfValue != null) {
							Object value = convertValueFromIcf(icfValue, qname);
							empty = false;
							resourceAttribute.add(new PrismPropertyValue<Object>(value));
						}
					}

					if (!empty) {
						LOGGER.trace("Converted attribute {}", resourceAttribute);
						attributesContainer.getValue().add(resourceAttribute);
					}

				}
			}

		}

		return shadowPrism;
	}

	private <T> T getSingleValue(Attribute icfAttr, Class<T> type) throws SchemaException {
		List<Object> values = icfAttr.getValue();
		if (values != null && !values.isEmpty()) {
			if (values.size() > 1) {
				throw new SchemaException("Expected single value for " + icfAttr.getName());
			}
			Object val = convertValueFromIcf(values.get(0), null);
			if (type.isAssignableFrom(val.getClass())) {
				return (T) val;
			} else {
				throw new SchemaException("Expected type " + type.getName() + " for " + icfAttr.getName()
						+ " but got " + val.getClass().getName());
			}
		} else {
			throw new SchemaException("Empty value for " + icfAttr.getName());
		}

	}

	private Set<Attribute> convertFromResourceObject(ResourceAttributeContainer attributesPrism,
			OperationResult parentResult) throws SchemaException {
		Collection<ResourceAttribute<?>> resourceAttributes = attributesPrism.getAttributes();
		return convertFromResourceObject(resourceAttributes, parentResult);
	}

	private Set<Attribute> convertFromResourceObject(Collection<ResourceAttribute<?>> resourceAttributes,
			OperationResult parentResult) throws SchemaException {

		Set<Attribute> attributes = new HashSet<Attribute>();
		if (resourceAttributes == null) {
			// returning empty set
			return attributes;
		}

		for (ResourceAttribute<?> attribute : resourceAttributes) {
			QName midPointAttrQName = attribute.getElementName();
			if (midPointAttrQName.equals(ConnectorFactoryIcfImpl.ICFS_UID)) {
				throw new SchemaException("ICF UID explicitly specified in attributes");
			}

			String icfAttrName = icfNameMapper.convertAttributeNameToIcf(midPointAttrQName, getSchemaNamespace());

			Set<Object> convertedAttributeValues = new HashSet<Object>();
			for (PrismPropertyValue<?> value : attribute.getValues()) {
				convertedAttributeValues.add(UcfUtil.convertValueToIcf(value, protector, attribute.getElementName()));
			}

			Attribute connectorAttribute = AttributeBuilder.build(icfAttrName, convertedAttributeValues);

			attributes.add(connectorAttribute);
		}
		return attributes;
	}

	private Object convertValueFromIcf(Object icfValue, QName propName) {
		if (icfValue == null) {
			return null;
		}
		if (icfValue instanceof GuardedString) {
			return fromGuardedString((GuardedString) icfValue);
		}
		return icfValue;
	}

	private void convertFromActivation(Set<Attribute> updateAttributes,
			Collection<PropertyDelta<?>> activationDeltas) throws SchemaException {

		for (PropertyDelta<?> propDelta : activationDeltas) {
			if (propDelta.getElementName().equals(ActivationType.F_ADMINISTRATIVE_STATUS)) {
				ActivationStatusType status = propDelta.getPropertyNew().getValue(ActivationStatusType.class).getValue();
				// Not entirely correct, TODO: refactor later
				updateAttributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, status == ActivationStatusType.ENABLED));
			} else if (propDelta.getElementName().equals(ActivationType.F_VALID_FROM)) {
				XMLGregorianCalendar xmlCal = propDelta.getPropertyNew().getValue(XMLGregorianCalendar.class).getValue();
				updateAttributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, XmlTypeConverter.toMillis(xmlCal)));
			} else if (propDelta.getElementName().equals(ActivationType.F_VALID_TO)) {
				XMLGregorianCalendar xmlCal = propDelta.getPropertyNew().getValue(XMLGregorianCalendar.class).getValue();
				updateAttributes.add(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, XmlTypeConverter.toMillis(xmlCal)));
			} else if (propDelta.getElementName().equals(ActivationType.F_LOCKOUT_STATUS)) {
				LockoutStatusType status = propDelta.getPropertyNew().getValue(LockoutStatusType.class).getValue();
				updateAttributes.add(AttributeBuilder.build(OperationalAttributes.LOCK_OUT_NAME, status != LockoutStatusType.NORMAL));
			} else {
				throw new SchemaException("Got unknown activation attribute delta " + propDelta.getElementName());
			}
		}

	}

	private void convertFromPassword(Set<Attribute> attributes, PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
		if (passwordDelta == null) {
			throw new IllegalArgumentException("No password was provided");
		}

		QName elementName = passwordDelta.getElementName();
		if (StringUtils.isBlank(elementName.getNamespaceURI())) {
			if (!QNameUtil.match(elementName, PasswordType.F_VALUE)) {
				return;
			}
		} else if (!passwordDelta.getElementName().equals(PasswordType.F_VALUE)) {
			return;
		}
		PrismProperty<ProtectedStringType> newPassword = passwordDelta.getPropertyNew();
		if (newPassword == null || newPassword.isEmpty()) {
			LOGGER.trace("Skipping processing password delta. Password delta does not contain new value.");
			return;
		}
		GuardedString guardedPassword = toGuardedString(newPassword.getValue().getValue(), "new password");
		attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, guardedPassword));

	}

	private <T extends ShadowType> List<Change<T>> getChangesFromSyncDeltas(ObjectClass objClass, Collection<SyncDelta> icfDeltas, PrismSchema schema,
			OperationResult parentResult) throws SchemaException, GenericFrameworkException {
		List<Change<T>> changeList = new ArrayList<Change<T>>();

		Validate.notNull(icfDeltas, "Sync result must not be null.");
		for (SyncDelta icfDelta : icfDeltas) {

			if (icfDelta.getObject() != null){
				objClass = icfDelta.getObject().getObjectClass();
			}
				QName objectClass = icfNameMapper.objectClassToQname(objClass.getObjectClassValue(), getSchemaNamespace());
				ObjectClassComplexTypeDefinition objClassDefinition = (ObjectClassComplexTypeDefinition) schema
				.findComplexTypeDefinition(objectClass);

			if (SyncDeltaType.DELETE.equals(icfDelta.getDeltaType())) {
				LOGGER.trace("START creating delta of type DELETE");
				ObjectDelta<ShadowType> objectDelta = new ObjectDelta<ShadowType>(
						ShadowType.class, ChangeType.DELETE, prismContext);
				ResourceAttribute<String> uidAttribute = createUidAttribute(
						icfDelta.getUid(),
						getUidDefinition(objClassDefinition
								.toResourceAttributeContainerDefinition(ShadowType.F_ATTRIBUTES)));
				Collection<ResourceAttribute<?>> identifiers = new ArrayList<ResourceAttribute<?>>(1);
				identifiers.add(uidAttribute);
				Change change = new Change(identifiers, objectDelta, getToken(icfDelta.getToken()));
				change.setObjectClassDefinition(objClassDefinition);
				changeList.add(change);
				LOGGER.trace("END creating delta of type DELETE");

			} else if (SyncDeltaType.CREATE_OR_UPDATE.equals(icfDelta.getDeltaType())) {
				PrismObjectDefinition<ShadowType> objectDefinition = toShadowDefinition(objClassDefinition);
				LOGGER.trace("Object definition: {}", objectDefinition);
				
				LOGGER.trace("START creating delta of type CREATE_OR_UPDATE");
				PrismObject<ShadowType> currentShadow = convertToResourceObject(icfDelta.getObject(),
						objectDefinition, false);

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Got current shadow: {}", currentShadow.debugDump());
				}

				Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(currentShadow);

				Change change = new Change(identifiers, currentShadow, getToken(icfDelta.getToken()));
				change.setObjectClassDefinition(objClassDefinition);
				changeList.add(change);
				LOGGER.trace("END creating delta of type CREATE_OR_UPDATE");

			} else {
				throw new GenericFrameworkException("Unexpected sync delta type " + icfDelta.getDeltaType());
			}

		}
		return changeList;
	}

	private SyncToken getSyncToken(PrismProperty tokenProperty) throws SchemaException {
		if (tokenProperty == null){
			return null;
		}
		if (tokenProperty.getValue() == null) {
			return null;
		}
		Object tokenValue = tokenProperty.getValue().getValue();
		if (tokenValue == null) {
			return null;
		}
		SyncToken syncToken = new SyncToken(tokenValue);
		return syncToken;
	}

	private PrismProperty<?> getToken(SyncToken syncToken) {
		Object object = syncToken.getValue();
		return createTokenProperty(object);
	}

	private <T> PrismProperty<T> createTokenProperty(T object) {
		QName type = XsdTypeMapper.toXsdType(object.getClass());

		Set<PrismPropertyValue<T>> syncTokenValues = new HashSet<PrismPropertyValue<T>>();
		syncTokenValues.add(new PrismPropertyValue<T>(object));
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(SchemaConstants.SYNC_TOKEN,
				type, prismContext);
		propDef.setDynamic(true);
		PrismProperty<T> property = propDef.instantiate();
		property.addValues(syncTokenValues);
		return property;
	}

	/**
	 * check additional operation order, according to the order are script
	 * executed before or after operation..
	 */
	private void checkAndExecuteAdditionalOperation(Collection<Operation> additionalOperations, BeforeAfterType order, OperationResult result) throws CommunicationException, GenericFrameworkException {

		if (additionalOperations == null) {
			// TODO: add warning to the result
			return;
		}

		for (Operation op : additionalOperations) {
			if (op instanceof ExecuteProvisioningScriptOperation) {
				ExecuteProvisioningScriptOperation executeOp = (ExecuteProvisioningScriptOperation) op;
				LOGGER.trace("Find execute script operation: {}", SchemaDebugUtil.prettyPrint(executeOp));
				// execute operation in the right order..
				if (order.equals(executeOp.getScriptOrder())) {
					executeScriptIcf(executeOp, result);
				}
			}
		}

	}
	
	@Override
	public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, OperationResult parentResult) throws CommunicationException, GenericFrameworkException {
		
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".executeScript");
		
		Object output = null;
		try {
			
			output = executeScriptIcf(scriptOperation, result);
			
		} catch (CommunicationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (GenericFrameworkException e) {
			result.recordFatalError(e);
			throw e;
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			throw e;
		}
		
		result.computeStatus();
		
		return output;
	}

	private Object executeScriptIcf(ExecuteProvisioningScriptOperation scriptOperation, OperationResult result) throws CommunicationException, GenericFrameworkException {
		
		String icfOpName = null;
		if (scriptOperation.isConnectorHost()) {
			icfOpName = "runScriptOnConnector";
		} else if (scriptOperation.isResourceHost()) {
			icfOpName = "runScriptOnResource";
		} else {
			throw new IllegalArgumentException("Where to execute the script?");
		}
		
		// convert execute script operation to the script context required from
			// the connector
			ScriptContext scriptContext = convertToScriptContext(scriptOperation);
			
			OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + "." + icfOpName);
			icfResult.addContext("connector", icfConnectorFacade.getClass());
			
			Object output = null;
			
			try {
				
				LOGGER.trace("Running script ({})", icfOpName);
				
				if (scriptOperation.isConnectorHost()) {
					output = icfConnectorFacade.runScriptOnConnector(scriptContext, new OperationOptionsBuilder().build());
				} else if (scriptOperation.isResourceHost()) {
					output = icfConnectorFacade.runScriptOnResource(scriptContext, new OperationOptionsBuilder().build());
				}
				
				icfResult.recordSuccess();
				
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Finished running script ({}), script result: {}", icfOpName, PrettyPrinter.prettyPrint(output));
				}
				
			} catch (Throwable ex) {
				
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Finished running script ({}), ERROR: {}", icfOpName, ex.getMessage());
				}
				
				Throwable midpointEx = processIcfException(ex, this, icfResult);
				result.computeStatus();
				// Do some kind of acrobatics to do proper throwing of checked
				// exception
				if (midpointEx instanceof CommunicationException) {
					throw (CommunicationException) midpointEx;
				} else if (midpointEx instanceof GenericFrameworkException) {
					throw (GenericFrameworkException) midpointEx;
				} else if (midpointEx instanceof SchemaException) {
					// Schema exception during delete? It must be a missing UID
					throw new IllegalArgumentException(midpointEx.getMessage(), midpointEx);
				} else if (midpointEx instanceof RuntimeException) {
					throw (RuntimeException) midpointEx;
				} else if (midpointEx instanceof Error) {
					throw (Error) midpointEx;
				} else {
					throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
				}
			}
			
			return output;
	}

	private ScriptContext convertToScriptContext(ExecuteProvisioningScriptOperation executeOp) {
		// creating script arguments map form the execute script operation
		// arguments
		Map<String, Object> scriptArguments = new HashMap<String, Object>();
		for (ExecuteScriptArgument argument : executeOp.getArgument()) {
			scriptArguments.put(argument.getArgumentName(), argument.getArgumentValue());
		}
		ScriptContext scriptContext = new ScriptContext(executeOp.getLanguage(), executeOp.getTextCode(),
				scriptArguments);
		return scriptContext;
	}

	/**
	 * Transforms midPoint XML configuration of the connector to the ICF
	 * configuration.
	 * <p/>
	 * The "configuration" part of the XML resource definition will be used.
	 * <p/>
	 * The provided ICF APIConfiguration will be modified, some values may be
	 * overwritten.
	 * 
	 * @param apiConfig
	 *            ICF connector configuration
	 * @param resourceType
	 *            midPoint XML configuration
	 * @throws SchemaException
	 * @throws ConfigurationException
	 */
	private void transformConnectorConfiguration(APIConfiguration apiConfig, PrismContainerValue configuration)
			throws SchemaException, ConfigurationException {

		ConfigurationProperties configProps = apiConfig.getConfigurationProperties();

		// The namespace of all the configuration properties specific to the
		// connector instance will have a connector instance namespace. This
		// namespace can be found in the resource definition.
		String connectorConfNs = connectorType.getNamespace();

		PrismContainer configurationPropertiesContainer = configuration
				.findContainer(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		if (configurationPropertiesContainer == null) {
			// Also try this. This is an older way.
			configurationPropertiesContainer = configuration.findContainer(new QName(connectorConfNs,
					ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME));
		}

		int numConfingProperties = transformConnectorConfiguration(configProps,
				configurationPropertiesContainer, connectorConfNs);

		PrismContainer connectorPoolContainer = configuration.findContainer(new QName(
				ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME));
		ObjectPoolConfiguration connectorPoolConfiguration = apiConfig.getConnectorPoolConfiguration();
		transformConnectorPoolConfiguration(connectorPoolConfiguration, connectorPoolContainer);

		PrismProperty producerBufferSizeProperty = configuration.findProperty(new QName(
				ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_XML_ELEMENT_NAME));
		if (producerBufferSizeProperty != null) {
			apiConfig.setProducerBufferSize(parseInt(producerBufferSizeProperty));
		}

		PrismContainer connectorTimeoutsContainer = configuration.findContainer(new QName(
				ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME));
		transformConnectorTimeoutsConfiguration(apiConfig, connectorTimeoutsContainer);

        PrismContainer resultsHandlerConfigurationContainer = configuration.findContainer(new QName(
                ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION,
                ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME));
        ResultsHandlerConfiguration resultsHandlerConfiguration = apiConfig.getResultsHandlerConfiguration();
        transformResultsHandlerConfiguration(resultsHandlerConfiguration, resultsHandlerConfigurationContainer);

		if (numConfingProperties == 0) {
			throw new SchemaException("No configuration properties found. Wrong namespace? (expected: "
					+ connectorConfNs + ")");
		}

	}

	private int transformConnectorConfiguration(ConfigurationProperties configProps,
			PrismContainer<?> configurationPropertiesContainer, String connectorConfNs)
			throws ConfigurationException {

		int numConfingProperties = 0;

		if (configurationPropertiesContainer == null || configurationPropertiesContainer.getValue() == null) {
			LOGGER.warn("No configuration properties in connectorType.getOid()");
			return numConfingProperties;
		}

		for (PrismProperty prismProperty : configurationPropertiesContainer.getValue().getProperties()) {
			QName propertyQName = prismProperty.getElementName();

			// All the elements must be in a connector instance
			// namespace.
			if (propertyQName.getNamespaceURI() == null
					|| !propertyQName.getNamespaceURI().equals(connectorConfNs)) {
				LOGGER.warn("Found element with a wrong namespace ({}) in connector OID={}",
						propertyQName.getNamespaceURI(), connectorType.getOid());
			} else {

				numConfingProperties++;

				// Local name of the element is the same as the name
				// of ICF configuration property
				String propertyName = propertyQName.getLocalPart();
				ConfigurationProperty property = configProps.getProperty(propertyName);
				
				if (property == null) {
					throw new ConfigurationException("Unknown configuration property "+propertyName);
				}

				// Check (java) type of ICF configuration property,
				// behave accordingly
				Class<?> type = property.getType();
				if (type.isArray()) {
					property.setValue(convertToIcfArray(prismProperty, type.getComponentType()));
					// property.setValue(prismProperty.getRealValuesArray(type.getComponentType()));
				} else {
					// Single-valued property are easy to convert
					property.setValue(convertToIcfSingle(prismProperty, type));
					// property.setValue(prismProperty.getRealValue(type));
				}
			}
		}
		return numConfingProperties;
	}

	private void transformConnectorPoolConfiguration(ObjectPoolConfiguration connectorPoolConfiguration,
			PrismContainer<?> connectorPoolContainer) throws SchemaException {

		if (connectorPoolContainer == null || connectorPoolContainer.getValue() == null) {
			return;
		}

		for (PrismProperty prismProperty : connectorPoolContainer.getValue().getProperties()) {
			QName propertyQName = prismProperty.getElementName();
			if (propertyQName.getNamespaceURI().equals(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION)) {
				String subelementName = propertyQName.getLocalPart();
				if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_EVICTABLE_IDLE_TIME_MILLIS
						.equals(subelementName)) {
					connectorPoolConfiguration.setMinEvictableIdleTimeMillis(parseLong(prismProperty));
				} else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_IDLE
						.equals(subelementName)) {
					connectorPoolConfiguration.setMinIdle(parseInt(prismProperty));
				} else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_IDLE
						.equals(subelementName)) {
					connectorPoolConfiguration.setMaxIdle(parseInt(prismProperty));
				} else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_OBJECTS
						.equals(subelementName)) {
					connectorPoolConfiguration.setMaxObjects(parseInt(prismProperty));
				} else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_WAIT
						.equals(subelementName)) {
					connectorPoolConfiguration.setMaxWait(parseLong(prismProperty));
				} else {
					throw new SchemaException(
							"Unexpected element "
									+ propertyQName
									+ " in "
									+ ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME);
				}
			} else {
				throw new SchemaException(
						"Unexpected element "
								+ propertyQName
								+ " in "
								+ ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME);
			}
		}
	}

	private void transformConnectorTimeoutsConfiguration(APIConfiguration apiConfig,
			PrismContainer<?> connectorTimeoutsContainer) throws SchemaException {

		if (connectorTimeoutsContainer == null || connectorTimeoutsContainer.getValue() == null) {
			return;
		}

		for (PrismProperty prismProperty : connectorTimeoutsContainer.getValue().getProperties()) {
			QName propertQName = prismProperty.getElementName();

			if (ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION.equals(propertQName.getNamespaceURI())) {
				String opName = propertQName.getLocalPart();
				Class<? extends APIOperation> apiOpClass = ConnectorFactoryIcfImpl.resolveApiOpClass(opName);
				if (apiOpClass != null) {
					apiConfig.setTimeout(apiOpClass, parseInt(prismProperty));
				} else {
					throw new SchemaException("Unknown operation name " + opName + " in "
							+ ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME);
				}
			}
		}
	}

    private void transformResultsHandlerConfiguration(ResultsHandlerConfiguration resultsHandlerConfiguration,
                                                     PrismContainer<?> resultsHandlerConfigurationContainer) throws SchemaException {

        if (resultsHandlerConfigurationContainer == null || resultsHandlerConfigurationContainer.getValue() == null) {
            return;
        }

        for (PrismProperty prismProperty : resultsHandlerConfigurationContainer.getValue().getProperties()) {
            QName propertyQName = prismProperty.getElementName();
            if (propertyQName.getNamespaceURI().equals(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION)) {
                String subelementName = propertyQName.getLocalPart();
                if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_NORMALIZING_RESULTS_HANDLER
                        .equals(subelementName)) {
                    resultsHandlerConfiguration.setEnableNormalizingResultsHandler(parseBoolean(prismProperty));
                } else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_FILTERED_RESULTS_HANDLER
                        .equals(subelementName)) {
                    resultsHandlerConfiguration.setEnableFilteredResultsHandler(parseBoolean(prismProperty));
                } else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_CASE_INSENSITIVE_HANDLER
                        .equals(subelementName)) {
                    resultsHandlerConfiguration.setEnableCaseInsensitiveFilter(parseBoolean(prismProperty));
                } else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER
                        .equals(subelementName)) {
                    resultsHandlerConfiguration.setEnableAttributesToGetSearchResultsHandler(parseBoolean(prismProperty));
                } else {
                    throw new SchemaException(
                            "Unexpected element "
                                    + propertyQName
                                    + " in "
                                    + ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME);
                }
            } else {
                throw new SchemaException(
                        "Unexpected element "
                                + propertyQName
                                + " in "
                                + ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME);
            }
        }
    }

    private int parseInt(PrismProperty<?> prop) {
		return prop.getRealValue(Integer.class);
	}

	private long parseLong(PrismProperty<?> prop) {
		Object realValue = prop.getRealValue();
		if (realValue instanceof Long) {
			return (Long) realValue;
		} else if (realValue instanceof Integer) {
			return ((Integer) realValue);
		} else {
			throw new IllegalArgumentException("Cannot convert " + realValue.getClass() + " to long");
		}
	}

    private boolean parseBoolean(PrismProperty<?> prop) {
        return prop.getRealValue(Boolean.class);
    }

    private Object convertToIcfSingle(PrismProperty<?> configProperty, Class<?> expectedType)
			throws ConfigurationException {
		if (configProperty == null) {
			return null;
		}
		PrismPropertyValue<?> pval = configProperty.getValue();
		return convertToIcf(pval, expectedType);
	}

	private Object[] convertToIcfArray(PrismProperty prismProperty, Class<?> componentType)
			throws ConfigurationException {
		List<PrismPropertyValue> values = prismProperty.getValues();
		Object valuesArrary = Array.newInstance(componentType, values.size());
		for (int j = 0; j < values.size(); ++j) {
			Object icfValue = convertToIcf(values.get(j), componentType);
			Array.set(valuesArrary, j, icfValue);
		}
		return (Object[]) valuesArrary;
	}

	private Object convertToIcf(PrismPropertyValue<?> pval, Class<?> expectedType) throws ConfigurationException {
		Object midPointRealValue = pval.getValue();
		if (expectedType.equals(GuardedString.class)) {
			// Guarded string is a special ICF beast
			// The value must be ProtectedStringType
			if (midPointRealValue instanceof ProtectedStringType) {
				ProtectedStringType ps = (ProtectedStringType) pval.getValue();
				return toGuardedString(ps, pval.getParent().getElementName().getLocalPart());
			} else {
				throw new ConfigurationException(
						"Expected protected string as value of configuration property "
								+ pval.getParent().getElementName().getLocalPart() + " but got "
								+ midPointRealValue.getClass());
			}

		} else if (expectedType.equals(GuardedByteArray.class)) {
			// Guarded string is a special ICF beast
			// TODO
			return new GuardedByteArray(Base64.decodeBase64((String) pval.getValue()));
		} else if (midPointRealValue instanceof PolyString) {
			return ((PolyString)midPointRealValue).getOrig();
		} else if (midPointRealValue instanceof PolyStringType) {
			return ((PolyStringType)midPointRealValue).getOrig();
		} else if (expectedType.equals(File.class) && midPointRealValue instanceof String) {
			return new File((String)midPointRealValue);
		} else if (expectedType.equals(String.class) && midPointRealValue instanceof ProtectedStringType) {
			try {
				return protector.decryptString((ProtectedStringType)midPointRealValue);
			} catch (EncryptionException e) {
				throw new ConfigurationException(e);
			}
		} else {
			return midPointRealValue;
		}
	}

	private GuardedString toGuardedString(ProtectedStringType ps, String propertyName) {
		if (ps == null) {
			return null;
		}
		if (!protector.isEncrypted(ps)) {
			if (ps.getClearValue() == null) {
				return null;
			}
			LOGGER.warn("Using cleartext value for {}", propertyName);
			return new GuardedString(ps.getClearValue().toCharArray());
		}
		try {
			return new GuardedString(protector.decryptString(ps).toCharArray());
		} catch (EncryptionException e) {
			LOGGER.error("Unable to decrypt value of element {}: {}",
					new Object[] { propertyName, e.getMessage(), e });
			throw new SystemException("Unable to dectypt value of element " + propertyName + ": "
					+ e.getMessage(), e);
		}
	}

	private ProtectedStringType fromGuardedString(GuardedString icfValue) {
		final ProtectedStringType ps = new ProtectedStringType();
		icfValue.access(new GuardedString.Accessor() {
			@Override
			public void access(char[] passwordChars) {
				try {
					ps.setClearValue(new String(passwordChars));
					protector.encrypt(ps);
				} catch (EncryptionException e) {
					throw new IllegalStateException("Protector failed to encrypt password");
				}
			}
		});
		return ps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ConnectorInstanceIcfImpl(" + connectorType + ")";
	}

	public String getHumanReadableName() {
		return connectorType.toString() + ": " + description;
	}

}
