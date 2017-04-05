/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdUtil.processIcfException;

import java.util.*;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AddRemoveAttributeValuesCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
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
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptionInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.QualifiedUid;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;
import org.identityconnectors.framework.impl.api.local.LocalConnectorInfoImpl;
import org.identityconnectors.framework.impl.api.local.ObjectPool;
import org.identityconnectors.framework.impl.api.local.ObjectPool.Statistics;
import org.identityconnectors.framework.impl.api.local.operations.ConnectorOperationalContext;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.PoolableConnector;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.DistinguishedNameMatchingRule;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.match.UuidMatchingRule;
import com.evolveum.midpoint.prism.match.XmlMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationResult;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationReturnValue;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PasswordChangeOperation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.api.UcfUtil;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.query.FilterInterpreter;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ActivationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.StateReporter;
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
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AuxiliaryObjectClassesCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
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
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Implementation of ConnectorInstance for ConnId connectors.
 * <p/>
 * This class implements the ConnectorInstance interface. The methods are
 * converting the data from the "midPoint semantics" as seen by the
 * ConnectorInstance interface to the "ConnId semantics" as seen by the ICF
 * framework.
 * 
 * @author Radovan Semancik
 */
public class ConnectorInstanceConnIdImpl implements ConnectorInstance {

	private static final com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory CAPABILITY_OBJECT_FACTORY 
		= new com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory();

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorInstanceConnIdImpl.class);

	ConnectorInfo cinfo;
	ConnectorType connectorType;
	ConnectorFacade connIdConnectorFacade;
	String resourceSchemaNamespace;
	private PrismSchema connectorSchema;
	private APIConfiguration apiConfig = null;
	
	Protector protector;
	PrismContext prismContext;
	private ConnIdNameMapper connIdNameMapper;
	private ConnIdConvertor connIdConvertor;

	private ResourceSchema resourceSchema = null;
	private Collection<Object> capabilities = null;
	private String description;
	private boolean caseIgnoreAttributeNames = false;
	private Boolean legacySchema = null;
	private boolean supportsReturnDefaultAttributes = false;

	ConnectorInstanceConnIdImpl(ConnectorInfo connectorInfo, ConnectorType connectorType,
			String schemaNamespace, PrismSchema connectorSchema, Protector protector,
			PrismContext prismContext) {
		this.cinfo = connectorInfo;
		this.connectorType = connectorType;
		this.resourceSchemaNamespace = schemaNamespace;
		this.connectorSchema = connectorSchema;
		this.protector = protector;
		this.prismContext = prismContext;
		connIdNameMapper = new ConnIdNameMapper(schemaNamespace);
		connIdConvertor = new ConnIdConvertor(protector, resourceSchemaNamespace);
		connIdConvertor.setIcfNameMapper(connIdNameMapper);
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
	
	public void setResourceSchema(ResourceSchema resourceSchema) {
		this.resourceSchema = resourceSchema;
		connIdNameMapper.setResourceSchema(resourceSchema);
	}
	
	public void resetResourceSchema() {
		setResourceSchema(null);
	}

	@Override
	public void configure(PrismContainerValue<?> configuration, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_CONFIGURE);
		result.addParam("configuration", configuration);

		try {
			// Get default configuration for the connector. This is important,
			// as it contains types of connector configuration properties.

			// Make sure that the proper configuration schema is applied. This
			// will cause that all the "raw" elements are parsed
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

			ConnIdConfigurationTransformer configTransformer = new ConnIdConfigurationTransformer(connectorType, cinfo, protector);
			// Transform XML configuration from the resource to the ICF connector configuration
			try {
				apiConfig = configTransformer.transformConnectorConfiguration(configuration);
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
			connIdConnectorFacade = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

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
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
			}
		}
		
		PrismProperty<Boolean> legacySchemaConfigProperty = configuration.findProperty(new QName(
				SchemaConstants.NS_ICF_CONFIGURATION,
				ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_LEGACY_SCHEMA_XML_ELEMENT_NAME));
		if (legacySchemaConfigProperty != null) {
			legacySchema = legacySchemaConfigProperty.getRealValue();
		}
		LOGGER.trace("Legacy schema (config): {}", legacySchema);
	}

	private PrismContainerDefinition<?> getConfigurationContainerDefinition() throws SchemaException {
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
	
	private QName icfAttributeInfoToMatchingRule(AttributeInfo attributeInfo) {
		String icfSubtype = attributeInfo.getSubtype();
		if (icfSubtype == null) {
			return null;
		}
		if (AttributeInfo.Subtypes.STRING_CASE_IGNORE.toString().equals(icfSubtype)) {
			return StringIgnoreCaseMatchingRule.NAME;
		}
		if (AttributeInfo.Subtypes.STRING_LDAP_DN.toString().equals(icfSubtype)) {
			return DistinguishedNameMatchingRule.NAME;
		}
		if (AttributeInfo.Subtypes.STRING_XML.toString().equals(icfSubtype)) {
			return XmlMatchingRule.NAME;
		}
		if (AttributeInfo.Subtypes.STRING_UUID.toString().equals(icfSubtype)) {
			return UuidMatchingRule.NAME;
		}
		LOGGER.debug("Unknown subtype {} defined for attribute {}, ignoring (no matching rule definition)", icfSubtype, attributeInfo.getName());
		return null;
	}
	
	@Override
	public ConnectorOperationalStatus getOperationalStatus() throws ObjectNotFoundException {
		
		if (!(cinfo instanceof LocalConnectorInfoImpl)) {
			LOGGER.trace("Cannot get operational status of a remote connector {}", connectorType);
			return null;
		}
		
		if (apiConfig == null) {
			LOGGER.trace("Cannot get operational status of a connector {}: connector not yet configured", connectorType);
			throw new IllegalStateException("Connector "+connectorType+" not yet configured");
		}
		
		ConnectorOperationalStatus status = new ConnectorOperationalStatus();
		
		ConnectorOperationalContext connectorOperationalContext = new ConnectorOperationalContext((LocalConnectorInfoImpl)cinfo, (APIConfigurationImpl) apiConfig);
		
		Class<? extends Connector> connectorClass = connectorOperationalContext.getConnectorClass();
		if (connectorClass != null) {
			status.setConnectorClassName(connectorClass.getName());
		}
		
		ObjectPoolConfiguration poolConfiguration = apiConfig.getConnectorPoolConfiguration();
		if (poolConfiguration != null) {
			status.setPoolConfigMaxSize(poolConfiguration.getMaxObjects());
			status.setPoolConfigMinIdle(poolConfiguration.getMinIdle());
			status.setPoolConfigMaxIdle(poolConfiguration.getMaxIdle());
			status.setPoolConfigWaitTimeout(poolConfiguration.getMaxWait());
			status.setPoolConfigMinEvictableIdleTime(poolConfiguration.getMinEvictableIdleTimeMillis());
		}
		
		ObjectPool<PoolableConnector> pool = connectorOperationalContext.getPool();
		if (pool != null) {
			Statistics poolStats = pool.getStatistics();
			if (poolStats != null) {
				status.setPoolStatusNumActive(poolStats.getNumActive());
				status.setPoolStatusNumIdle(poolStats.getNumIdle());
			}
		}
		
		return status;
	}

	@Override
	public void initialize(ResourceSchema resourceSchema, Collection<Object> capabilities, boolean caseIgnoreAttributeNames, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ConfigurationException {

		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_INITIALIZE);
		result.addContext("connector", connectorType);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorFactoryConnIdImpl.class);

		if (connIdConnectorFacade == null) {
			result.recordFatalError("Attempt to use unconfigured connector");
			throw new IllegalStateException("Attempt to use unconfigured connector "
					+ ObjectTypeUtil.toShortString(connectorType));
		}

		setResourceSchema(resourceSchema);
		this.capabilities = capabilities;
		this.caseIgnoreAttributeNames = caseIgnoreAttributeNames;
		
		if (resourceSchema != null && legacySchema == null) {
			legacySchema = detectLegacySchema(resourceSchema);
		}

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
		icfResult.addContext("connector", connIdConnectorFacade.getClass());

		org.identityconnectors.framework.common.objects.Schema icfSchema = null;
		try {

			// Fetch the schema from the connector (which actually gets that
			// from the resource).
			InternalMonitor.recordConnectorOperation("schema");
			// TODO have context present
			//recordIcfOperationStart(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null);
			icfSchema = connIdConnectorFacade.schema();
			//recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null);

			icfResult.recordSuccess();
		} catch (UnsupportedOperationException ex) {
			//recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null, ex);
			// The connector does no support schema() operation.
			icfResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, ex.getMessage());
			resetResourceSchema();
			return;
		} catch (Throwable ex) {
			//recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null, ex);
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
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
			}
		}
		
		if (icfSchema == null) {
			icfResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Null schema returned");
			resetResourceSchema();
			return;
		}


		parseResourceSchema(icfSchema, generateObjectClasses);
	}

	private void parseResourceSchema(org.identityconnectors.framework.common.objects.Schema icfSchema, List<QName> generateObjectClasses) {

		AttributeInfo passwordAttributeInfo = null;
		AttributeInfo enableAttributeInfo = null;
		AttributeInfo enableDateAttributeInfo = null;
		AttributeInfo disableDateAttributeInfo = null;
		AttributeInfo lockoutAttributeInfo = null;
		AttributeInfo auxiliaryObjectClasseAttributeInfo = null;

		// New instance of midPoint schema object
		setResourceSchema(new ResourceSchemaImpl(getSchemaNamespace(), prismContext));

		if (legacySchema == null) {
			legacySchema = detectLegacySchema(icfSchema);
		}
		LOGGER.trace("Converting resource schema (legacy mode: {})", legacySchema);
		
		Set<ObjectClassInfo> objectClassInfoSet = icfSchema.getObjectClassInfo();
		// Let's convert every objectclass in the ICF schema ...		
		for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {

			// "Flat" ICF object class names needs to be mapped to QNames
			QName objectClassXsdName = connIdNameMapper.objectClassToQname(new ObjectClass(objectClassInfo.getType()), getSchemaNamespace(), legacySchema);

			if (!shouldBeGenerated(generateObjectClasses, objectClassXsdName)){
				LOGGER.trace("Skipping object class {} ({})", objectClassInfo.getType(), objectClassXsdName);
				continue;
			}
			
			LOGGER.trace("Convering object class {} ({})", objectClassInfo.getType(), objectClassXsdName);
			
			// ResourceObjectDefinition is a midPpoint way how to represent an
			// object class.
			// The important thing here is the last "type" parameter
			// (objectClassXsdName). The rest is more-or-less cosmetics.
			ObjectClassComplexTypeDefinition ocDef = ((ResourceSchemaImpl) resourceSchema)
					.createObjectClassDefinition(objectClassXsdName);

			// The __ACCOUNT__ objectclass in ICF is a default account
			// objectclass. So mark it appropriately.
			if (ObjectClass.ACCOUNT_NAME.equals(objectClassInfo.getType())) {
				((ObjectClassComplexTypeDefinitionImpl) ocDef).setKind(ShadowKindType.ACCOUNT);
				((ObjectClassComplexTypeDefinitionImpl) ocDef).setDefaultInAKind(true);
			}
			
			ResourceAttributeDefinition<String> uidDefinition = null;
			ResourceAttributeDefinition<String> nameDefinition = null;
			boolean hasUidDefinition = false;

			int displayOrder = ConnectorFactoryConnIdImpl.ATTR_DISPLAY_ORDER_START;
			// Let's iterate over all attributes in this object class ...
			Set<AttributeInfo> attributeInfoSet = objectClassInfo.getAttributeInfo();
			for (AttributeInfo attributeInfo : attributeInfoSet) {
				String icfName = attributeInfo.getName();

				if (OperationalAttributes.PASSWORD_NAME.equals(icfName)) {
					// This attribute will not go into the schema
					// instead a "password" capability is used
					passwordAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}

				if (OperationalAttributes.ENABLE_NAME.equals(icfName)) {
					enableAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}
				
				if (OperationalAttributes.ENABLE_DATE_NAME.equals(icfName)) {
					enableDateAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}
				
				if (OperationalAttributes.DISABLE_DATE_NAME.equals(icfName)) {
					disableDateAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}
				
				if (OperationalAttributes.LOCK_OUT_NAME.equals(icfName)) {
					lockoutAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}
				
				if (PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME.equals(icfName)) {
					auxiliaryObjectClasseAttributeInfo = attributeInfo;
					// Skip this attribute, capability is sufficient
					continue;
				}

				String processedAttributeName = icfName;
				if ((Name.NAME.equals(icfName) || Uid.NAME.equals(icfName)) && attributeInfo.getNativeName() != null ) {
					processedAttributeName = attributeInfo.getNativeName();					
				}
				
				QName attrXsdName = connIdNameMapper.convertAttributeNameToQName(processedAttributeName, ocDef);
				QName attrXsdType = ConnIdUtil.icfTypeToXsdType(attributeInfo.getType(), false);
				
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Attr conversion ICF: {}({}) -> XSD: {}({})",
							icfName, attributeInfo.getType().getSimpleName(),
							PrettyPrinter.prettyPrint(attrXsdName), PrettyPrinter.prettyPrint(attrXsdType));
				}

				// Create ResourceObjectAttributeDefinition, which is midPoint
				// way how to express attribute schema.
				ResourceAttributeDefinitionImpl attrDef = new ResourceAttributeDefinitionImpl(
						attrXsdName, attrXsdType, prismContext);

				attrDef.setMatchingRuleQName(icfAttributeInfoToMatchingRule(attributeInfo));
				
				if (Name.NAME.equals(icfName)) {
					nameDefinition = attrDef;
					if (uidDefinition != null && attrXsdName.equals(uidDefinition.getName())) {
						attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
						uidDefinition = attrDef;
						hasUidDefinition = true;
					} else {
						if (attributeInfo.getNativeName() == null) {
							// Set a better display name for __NAME__. The "name" is s very
							// overloaded term, so let's try to make things
							// a bit clearer
							attrDef.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_NAME);
						}
						attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_ORDER);
					}
					
				} else if (Uid.NAME.equals(icfName)) {
					// UID can be the same as other attribute
					ResourceAttributeDefinition existingDefinition = ocDef.findAttributeDefinition(attrXsdName);
					if (existingDefinition != null) {
						hasUidDefinition = true;
						((ResourceAttributeDefinitionImpl) existingDefinition).setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
						uidDefinition = existingDefinition;
						continue;
					} else {
						uidDefinition = attrDef;
						if (attributeInfo.getNativeName() == null) {
							attrDef.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_NAME);
						}
						attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
					}					
						
				} else {
					// Check conflict with UID definition
					if (uidDefinition != null && attrXsdName.equals(uidDefinition.getName())) {
						attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
						uidDefinition = attrDef;
						hasUidDefinition = true;
					} else {
						attrDef.setDisplayOrder(displayOrder);
						displayOrder += ConnectorFactoryConnIdImpl.ATTR_DISPLAY_ORDER_INCREMENT;
					}
				}
				
				attrDef.setNativeAttributeName(attributeInfo.getNativeName());
				attrDef.setFrameworkAttributeName(icfName);

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
				
				if (!Uid.NAME.equals(icfName)) {
					((ObjectClassComplexTypeDefinitionImpl) ocDef).add(attrDef);
				}
			}

			if (uidDefinition == null) {
				// Every object has UID in ICF, therefore add a default definition if no other was specified
				uidDefinition = new ResourceAttributeDefinitionImpl<>(
						SchemaConstants.ICFS_UID, DOMUtil.XSD_STRING, prismContext);
				// DO NOT make it mandatory. It must not be present on create hence it cannot be mandatory.
				((ResourceAttributeDefinitionImpl) uidDefinition).setMinOccurs(0);
				((ResourceAttributeDefinitionImpl) uidDefinition).setMaxOccurs(1);
				// Make it read-only
				((ResourceAttributeDefinitionImpl) uidDefinition).setReadOnly();
				// Set a default display name
				((ResourceAttributeDefinitionImpl) uidDefinition).setDisplayName(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_NAME);
				((ResourceAttributeDefinitionImpl) uidDefinition).setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
				// Uid is a primary identifier of every object (this is the ICF way)
			}
			if (!hasUidDefinition) {
				((ObjectClassComplexTypeDefinitionImpl) ocDef).add(uidDefinition);
			}
			((ObjectClassComplexTypeDefinitionImpl)ocDef).addPrimaryIdentifier(uidDefinition);
			if (uidDefinition != nameDefinition) {
				((ObjectClassComplexTypeDefinitionImpl)ocDef).addSecondaryIdentifier(nameDefinition);
			}

			// Add schema annotations
			((ObjectClassComplexTypeDefinitionImpl) ocDef).setNativeObjectClass(objectClassInfo.getType());
			((ObjectClassComplexTypeDefinitionImpl) ocDef).setDisplayNameAttribute(nameDefinition.getName());
			((ObjectClassComplexTypeDefinitionImpl) ocDef).setNamingAttribute(nameDefinition.getName());
			((ObjectClassComplexTypeDefinitionImpl) ocDef).setAuxiliary(objectClassInfo.isAuxiliary());

		}

		capabilities = new ArrayList<>();

		// This is the default for all resources.
		// (Currently there is no way how to obtain it from the connector.)
		// It can be disabled manually.
		AddRemoveAttributeValuesCapabilityType addRemove = new AddRemoveAttributeValuesCapabilityType();
		capabilities.add(CAPABILITY_OBJECT_FACTORY.createAddRemoveAttributeValues(addRemove));
		
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
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createActivation(capAct));
		}

		if (passwordAttributeInfo != null) {
			CredentialsCapabilityType capCred = new CredentialsCapabilityType();
			PasswordCapabilityType capPass = new PasswordCapabilityType();
			if (!passwordAttributeInfo.isReturnedByDefault()) {
				capPass.setReturnedByDefault(false);
			}
			if (passwordAttributeInfo.isReadable()) {
				capPass.setReadable(true);
			}
			capCred.setPassword(capPass);
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createCredentials(capCred));
		}
		
		if (auxiliaryObjectClasseAttributeInfo != null) {
			AuxiliaryObjectClassesCapabilityType capAux = new AuxiliaryObjectClassesCapabilityType();
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createAuxiliaryObjectClasses(capAux));
		}

		// Create capabilities from supported connector operations

		InternalMonitor.recordConnectorOperation("getSupportedOperations");
		Set<Class<? extends APIOperation>> supportedOperations = connIdConnectorFacade.getSupportedOperations();
		
		LOGGER.trace("Connector supported operations: {}", supportedOperations);

		if (supportedOperations.contains(SyncApiOp.class)) {
			LiveSyncCapabilityType capSync = new LiveSyncCapabilityType();
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createLiveSync(capSync));
		}

		if (supportedOperations.contains(TestApiOp.class)) {
			TestConnectionCapabilityType capTest = new TestConnectionCapabilityType();
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createTestConnection(capTest));
		}
		
		if (supportedOperations.contains(CreateApiOp.class)){
			CreateCapabilityType capCreate = new CreateCapabilityType();
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createCreate(capCreate));
		}
		
		if (supportedOperations.contains(GetApiOp.class) || supportedOperations.contains(SearchApiOp.class)){
			ReadCapabilityType capRead = new ReadCapabilityType();
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createRead(capRead));
		}
		
		if (supportedOperations.contains(UpdateApiOp.class)){
			UpdateCapabilityType capUpdate = new UpdateCapabilityType();
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createUpdate(capUpdate));
		}
		
		if (supportedOperations.contains(DeleteApiOp.class)){
			DeleteCapabilityType capDelete = new DeleteCapabilityType();
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createDelete(capDelete));
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
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createScript(capScript));
		}
		
		boolean canPageSize = false;
		boolean canPageOffset = false;
		boolean canSort = false;
		for (OperationOptionInfo searchOption: icfSchema.getSupportedOptionsByOperation(SearchApiOp.class)) {
			switch (searchOption.getName()) {
				case OperationOptions.OP_PAGE_SIZE:
					canPageSize = true;
					break;
				case OperationOptions.OP_PAGED_RESULTS_OFFSET:
					canPageOffset = true;
					break;
				case OperationOptions.OP_SORT_KEYS:
					canSort = true;
					break;
				case OperationOptions.OP_RETURN_DEFAULT_ATTRIBUTES:
					supportsReturnDefaultAttributes = true;
					break;
			}
			
		}
		
		if (canPageSize || canPageOffset || canSort) {
			PagedSearchCapabilityType capPage = new PagedSearchCapabilityType();
			capabilities.add(CAPABILITY_OBJECT_FACTORY.createPagedSearch(capPage));
		}

	}

	private boolean detectLegacySchema(Schema icfSchema) {
		Set<ObjectClassInfo> objectClassInfoSet = icfSchema.getObjectClassInfo();
		for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {
			if (objectClassInfo.is(ObjectClass.ACCOUNT_NAME) || objectClassInfo.is(ObjectClass.GROUP_NAME)) {
				LOGGER.trace("This is legacy schema");
				return true;
			}
		}
		return false;
	}
	
	private boolean detectLegacySchema(ResourceSchema resourceSchema) {
		ComplexTypeDefinition accountObjectClass = resourceSchema.findComplexTypeDefinition(
				new QName(getSchemaNamespace(), SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME));
		return accountObjectClass != null;
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
	
	 private <C extends CapabilityType> C getCapability(Class<C> capClass) {
		if (capabilities == null) {
			return null;
		}
		for (Object cap: capabilities) {
			if (capClass.isAssignableFrom(cap.getClass())) {
				return (C) cap;
			}
		}
		return null;
	}

	@Override
	public <T extends ShadowType> PrismObject<T> fetchObject(Class<T> type, ResourceObjectIdentification resourceObjectIdentification, AttributesToReturn attributesToReturn, StateReporter reporter,
															 OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
			SchemaException, SecurityViolationException, ConfigurationException {

		Validate.notNull(resourceObjectIdentification, "Null primary identifiers");
		ObjectClassComplexTypeDefinition objectClassDefinition = resourceObjectIdentification.getObjectClassDefinition();
		// Result type for this operation
		OperationResult result = parentResult.createMinorSubresult(ConnectorInstance.class.getName()
				+ ".fetchObject");
		result.addParam("resourceObjectDefinition", objectClassDefinition);
		result.addParam("identification", resourceObjectIdentification);
		result.addContext("connector", connectorType);

		if (connIdConnectorFacade == null) {
			result.recordFatalError("Attempt to use unconfigured connector");
			throw new IllegalStateException("Attempt to use unconfigured connector "
					+ ObjectTypeUtil.toShortString(connectorType) + " " + description);
		}

		// Get UID from the set of identifiers
		Uid uid;
		try {
			uid = getUid(resourceObjectIdentification);
		} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		}
		if (uid == null) {
			result.recordFatalError("Required attribute UID not found in identification set while attempting to fetch object identified by "
					+ resourceObjectIdentification + " from " + description);
			throw new IllegalArgumentException(
					"Required attribute UID not found in identification set while attempting to fetch object identified by "
							+ resourceObjectIdentification + " from " + description);
		}

		ObjectClass icfObjectClass = connIdNameMapper.objectClassToIcf(objectClassDefinition, getSchemaNamespace(), connectorType, legacySchema);
		if (icfObjectClass == null) {
			result.recordFatalError("Unable to determine object class from QName "
					+ objectClassDefinition.getTypeName()
					+ " while attempting to fetch object identified by " + resourceObjectIdentification + " from "
					+ description);
			throw new IllegalArgumentException("Unable to determine object class from QName "
					+ objectClassDefinition.getTypeName()
					+ " while attempting to fetch object identified by " + resourceObjectIdentification + " from "
					+ description);
		}
		
		OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
		convertToIcfAttrsToGet(objectClassDefinition, attributesToReturn, optionsBuilder);
		optionsBuilder.setAllowPartialResults(true);
		OperationOptions options = optionsBuilder.build();
		
		ConnectorObject co = null;
		try {

			// Invoke the ICF connector
			co = fetchConnectorObject(reporter, objectClassDefinition, icfObjectClass, uid, options,
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
			throw new ObjectNotFoundException("Object identified by " + resourceObjectIdentification + " (ConnId UID "+uid+"), objectClass " + objectClassDefinition.getTypeName() + "  was not found in "
					+ description);
		} catch (SchemaException ex) {
			result.recordFatalError(ex);
			throw ex;
		} catch (RuntimeException ex) {
			result.recordFatalError(ex);
			throw ex;
		}

		if (co == null) {
			result.recordFatalError("Object not found");
			throw new ObjectNotFoundException("Object identified by " + resourceObjectIdentification + " (ConnId UID "+uid+"), objectClass " + objectClassDefinition.getTypeName() + " was not in "
					+ description);
		}

		PrismObjectDefinition<T> shadowDefinition = toShadowDefinition(objectClassDefinition);
		PrismObject<T> shadow = connIdConvertor.convertToResourceObject(co, shadowDefinition, false, caseIgnoreAttributeNames, legacySchema);

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
	private ConnectorObject fetchConnectorObject(StateReporter reporter, ObjectClassComplexTypeDefinition objectClassDefinition, ObjectClass icfObjectClass, Uid uid,
												 OperationOptions options, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SecurityViolationException, SchemaException, ConfigurationException {

		// Connector operation cannot create result for itself, so we need to
		// create result for it
		OperationResult icfResult = parentResult.createMinorSubresult(ConnectorFacade.class.getName()
				+ ".getObject");
		icfResult.addParam("objectClass", icfObjectClass.toString());
		icfResult.addParam("uid", uid.getUidValue());
		icfResult.addArbitraryObjectAsParam("options", options);
		icfResult.addContext("connector", connIdConnectorFacade.getClass());
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Fetching connector object ObjectClass={}, UID={}, options={}",
					new Object[]{icfObjectClass, uid, ConnIdUtil.dumpOptions(options)});
		}
		
		ConnectorObject co = null;
		try {

			// Invoke the ICF connector
			InternalMonitor.recordConnectorOperation("getObject");
			recordIcfOperationStart(reporter, ProvisioningOperation.ICF_GET, objectClassDefinition, uid);
			co = connIdConnectorFacade.getObject(icfObjectClass, uid, options);
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET, objectClassDefinition, uid);

			icfResult.recordSuccess();
		} catch (Throwable ex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET, objectClassDefinition, ex, uid);
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
			} else if (midpointEx instanceof SecurityViolationException) {
				throw (SecurityViolationException) midpointEx;
			} else if (midpointEx instanceof ObjectNotFoundException) {
				LOGGER.trace("Got ObjectNotFoundException while looking for resource object ConnId UID: {}", uid);
				return null;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException)midpointEx;
			} else if (midpointEx instanceof Error) {
				// This should not happen. But some connectors are very strange.
				throw new SystemException("ERROR: "+midpointEx.getClass().getName()+": "+midpointEx.getMessage(), midpointEx);
			} else {
				throw new SystemException(midpointEx.getClass().getName()+": "+midpointEx.getMessage(), midpointEx);
			}
		
		}

		return co;
	}

	private void convertToIcfAttrsToGet(ObjectClassComplexTypeDefinition objectClassDefinition,
			AttributesToReturn attributesToReturn, OperationOptionsBuilder optionsBuilder) throws SchemaException {
		if (attributesToReturn == null) {
			return;
		}
		Collection<? extends ResourceAttributeDefinition> attrs = attributesToReturn.getAttributesToReturn();
		if (attributesToReturn.isReturnDefaultAttributes() && !attributesToReturn.isReturnPasswordExplicit()
				&& (attrs == null || attrs.isEmpty())) {
			return;
		}
		List<String> icfAttrsToGet = new ArrayList<String>(); 
		if (attributesToReturn.isReturnDefaultAttributes()) {
			if (supportsReturnDefaultAttributes) {
				optionsBuilder.setReturnDefaultAttributes(true);
			} else {
				// Add all the attributes that are defined as "returned by default" by the schema
				for (ResourceAttributeDefinition attributeDef: objectClassDefinition.getAttributeDefinitions()) {
					if (attributeDef.isReturnedByDefault()) {
						String attrName = connIdNameMapper.convertAttributeNameToIcf(attributeDef);
						icfAttrsToGet.add(attrName);
					}
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
		if (attributesToReturn.isReturnValidFromExplicit()
				|| (attributesToReturn.isReturnDefaultAttributes() && validFromReturnedByDefault())) {
			icfAttrsToGet.add(OperationalAttributes.ENABLE_DATE_NAME);
		}
		if (attributesToReturn.isReturnValidToExplicit()
				|| (attributesToReturn.isReturnDefaultAttributes() && validToReturnedByDefault())) {
			icfAttrsToGet.add(OperationalAttributes.DISABLE_DATE_NAME);
		}
		
		if (attrs != null) {
			for (ResourceAttributeDefinition attrDef: attrs) {
				String attrName = connIdNameMapper.convertAttributeNameToIcf(attrDef);
				if (!icfAttrsToGet.contains(attrName)) {
					icfAttrsToGet.add(attrName);
				}
			}
		}
		// Log full list here. ConnId is shortening it and it cannot be seen in logs.
		LOGGER.trace("Converted attributes ConnId attibutesToGet: {}", icfAttrsToGet);
		optionsBuilder.setAttributesToGet(icfAttrsToGet);
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
	
	private boolean validFromReturnedByDefault() {
		ActivationCapabilityType capability = CapabilityUtil.getCapability(capabilities, ActivationCapabilityType.class);
		return CapabilityUtil.isActivationValidFromReturnedByDefault(capability);
	}
	
	private boolean validToReturnedByDefault() {
		ActivationCapabilityType capability = CapabilityUtil.getCapability(capabilities, ActivationCapabilityType.class);
		return CapabilityUtil.isActivationValidToReturnedByDefault(capability);
	}

	@Override
	public ConnectorOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(PrismObject<? extends ShadowType> shadow, Collection<Operation> additionalOperations, StateReporter reporter,
													  OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		validateShadow(shadow, "add", false);
		ShadowType shadowType = shadow.asObjectable();

		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".addObject");
		result.addParam("resourceObject", shadow);
		result.addParam("additionalOperations", DebugUtil.debugDump(additionalOperations));         // because of serialization issues

		ObjectClassComplexTypeDefinition ocDef;
		ResourceAttributeContainerDefinition attrContDef = attributesContainer.getDefinition();
		if (attrContDef != null) {
			ocDef = attrContDef.getComplexTypeDefinition();
		} else {
			ocDef = resourceSchema.findObjectClassDefinition(shadow.asObjectable().getObjectClass());
			if (ocDef == null) {
				throw new SchemaException("Unknown object class "+shadow.asObjectable().getObjectClass());
			}
		}
		
		// getting icf object class from resource object class
		ObjectClass icfObjectClass = connIdNameMapper.objectClassToIcf(shadow, getSchemaNamespace(), connectorType, legacySchema);

		if (icfObjectClass == null) {
			result.recordFatalError("Couldn't get icf object class from " + shadow);
			throw new IllegalArgumentException("Couldn't get icf object class from " + shadow);
		}

		// setting ifc attributes from resource object attributes
		Set<Attribute> attributes = null;
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("midPoint object before conversion:\n{}", attributesContainer.debugDump());
			}
			attributes = connIdConvertor.convertFromResourceObject(attributesContainer, ocDef);

			if (shadowType.getCredentials() != null && shadowType.getCredentials().getPassword() != null) {
				PasswordType password = shadowType.getCredentials().getPassword();
				ProtectedStringType protectedString = password.getValue();
				GuardedString guardedPassword = ConnIdUtil.toGuardedString(protectedString, "new password", protector);
				if (guardedPassword != null) {
					attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME,
						guardedPassword));
				}
			}
			
			if (ActivationUtil.hasAdministrativeActivation(shadowType)){
				attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, ActivationUtil.isAdministrativeEnabled(shadowType)));
			}
			
			if (ActivationUtil.hasValidFrom(shadowType)){
				attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, XmlTypeConverter.toMillis(shadowType.getActivation().getValidFrom())));
			}

			if (ActivationUtil.hasValidTo(shadowType)){
				attributes.add(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, XmlTypeConverter.toMillis(shadowType.getActivation().getValidTo())));
			}
			
			if (ActivationUtil.hasLockoutStatus(shadowType)){
				attributes.add(AttributeBuilder.build(OperationalAttributes.LOCK_OUT_NAME, ActivationUtil.isLockedOut(shadowType)));
			}
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("ICF attributes after conversion:\n{}", ConnIdUtil.dump(attributes));
			}
		} catch (SchemaException | RuntimeException ex) {
			result.recordFatalError(
					"Error while converting resource object attributes. Reason: " + ex.getMessage(), ex);
			throw new SchemaException("Error while converting resource object attributes. Reason: "
					+ ex.getMessage(), ex);
		}
		
		if (attributes == null) {
			result.recordFatalError("Couldn't set attributes for icf.");
			throw new IllegalStateException("Couldn't set attributes for icf.");
		}

		List<String> icfAuxiliaryObjectClasses = new ArrayList<>();
		for (QName auxiliaryObjectClass: shadowType.getAuxiliaryObjectClass()) {
			icfAuxiliaryObjectClasses.add(
					connIdNameMapper.objectClassToIcf(auxiliaryObjectClass, resourceSchemaNamespace, 
							connectorType, false).getObjectClassValue());
		}
		if (!icfAuxiliaryObjectClasses.isEmpty()) {
			AttributeBuilder ab = new AttributeBuilder();
			ab.setName(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME);
			ab.addValue(icfAuxiliaryObjectClasses);
			attributes.add(ab.build());
		}
		
		OperationOptionsBuilder operationOptionsBuilder = new OperationOptionsBuilder();
		OperationOptions options = operationOptionsBuilder.build();
		
		checkAndExecuteAdditionalOperation(reporter, additionalOperations, BeforeAfterType.BEFORE, result);

		OperationResult connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".create");
		connIdResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
		connIdResult.addArbitraryCollectionAsParam("auxiliaryObjectClasses", icfAuxiliaryObjectClasses);
		connIdResult.addArbitraryCollectionAsParam("attributes", attributes);
		connIdResult.addArbitraryObjectAsParam("options", options);
		connIdResult.addContext("connector", connIdConnectorFacade.getClass());

		Uid uid = null;
		try {

			// CALL THE ICF FRAMEWORK
			InternalMonitor.recordConnectorOperation("create");
			recordIcfOperationStart(reporter, ProvisioningOperation.ICF_CREATE, ocDef, null);		// TODO provide object name
			uid = connIdConnectorFacade.create(icfObjectClass, attributes, options);
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_CREATE, ocDef, uid);

		} catch (Throwable ex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_CREATE, ocDef, ex, null);		// TODO name
			Throwable midpointEx = processIcfException(ex, this, connIdResult);
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
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
			}
		}
		
		checkAndExecuteAdditionalOperation(reporter, additionalOperations, BeforeAfterType.AFTER, result);

		if (uid == null || uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
			connIdResult.recordFatalError("ICF did not returned UID after create");
			result.computeStatus("Add object failed");
			throw new GenericFrameworkException("ICF did not returned UID after create");
		}

		
		Collection<ResourceAttribute<?>> identifiers = ConnIdUtil.convertToIdentifiers(uid, 
				attributesContainer.getDefinition().getComplexTypeDefinition(), resourceSchema);
		for (ResourceAttribute<?> identifier: identifiers) {
			attributesContainer.getValue().addReplaceExisting(identifier);
		}
		connIdResult.recordSuccess();

		result.recordSuccess();
		return ConnectorOperationReturnValue.wrap(attributesContainer.getAttributes(), result);
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
			Collection<ResourceAttribute<?>> identifiers = resourceAttributesContainer.getPrimaryIdentifiers();
			if (identifiers == null || identifiers.isEmpty()) {
				throw new IllegalArgumentException("Cannot " + operation + " shadow without identifiers");
			}
		}
	}

	// TODO [med] beware, this method does not obey its contract specified in the interface
	// (1) currently it does not return all the changes, only the 'side effect' changes
	// (2) it throws exceptions even if some of the changes were made
	// (3) among identifiers, only the UID value is updated on object rename
	//     (other identifiers are ignored on input and output of this method)

	@Override
	public ConnectorOperationReturnValue<Collection<PropertyModificationOperation>> modifyObject(ObjectClassComplexTypeDefinition objectClassDef, Collection<? extends ResourceAttribute<?>> identifiers, Collection<Operation> changes, StateReporter reporter,
														   OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".modifyObject");
		result.addParam("objectClass", objectClassDef);
		result.addCollectionOfSerializablesAsParam("identifiers", identifiers);
		result.addArbitraryCollectionAsParam("changes", changes);
		
		if (changes.isEmpty()){
			LOGGER.info("No modifications for connector object specified. Skipping processing.");
			result.recordNotApplicableIfUnknown();
			return ConnectorOperationReturnValue.wrap(new ArrayList<PropertyModificationOperation>(0), result);
		}

		ObjectClass objClass = connIdNameMapper.objectClassToIcf(objectClassDef, getSchemaNamespace(), connectorType, legacySchema);
		
		Uid uid;
		try {
			uid = getUid(objectClassDef, identifiers);
		} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		}
		
		if (uid == null) {
			result.recordFatalError("Cannot detemine UID from identifiers: " + identifiers);
			throw new IllegalArgumentException("Cannot detemine UID from identifiers: " + identifiers);
		}
		String originalUid = uid.getUidValue();

		Set<Attribute> attributesToAdd = new HashSet<>();
		Set<Attribute> attributesToUpdate = new HashSet<>();
		Set<Attribute> attributesToRemove = new HashSet<>();
		
		Set<Operation> additionalOperations = new HashSet<Operation>();
		PasswordChangeOperation passwordChangeOperation = null;
		Collection<PropertyDelta<?>> activationDeltas = new HashSet<PropertyDelta<?>>();
		PropertyDelta<ProtectedStringType> passwordDelta = null;
		PropertyDelta<QName> auxiliaryObjectClassDelta = null;

		for (Operation operation : changes) {
			if (operation == null) {
				IllegalArgumentException e = new IllegalArgumentException("Null operation in modifyObject");
				result.recordFatalError(e);
				throw e;
			}
			if (operation instanceof PropertyModificationOperation) {
				PropertyDelta<?> delta = ((PropertyModificationOperation)operation).getPropertyDelta();
				if (delta.getPath().equivalent(new ItemPath(ShadowType.F_AUXILIARY_OBJECT_CLASS))) {
					auxiliaryObjectClassDelta = (PropertyDelta<QName>) delta;
				}
			}
		}
		
		try {
			ObjectClassComplexTypeDefinition structuralObjectClassDefinition = resourceSchema.findObjectClassDefinition(objectClassDef.getTypeName());
			Map<QName,ObjectClassComplexTypeDefinition> auxiliaryObjectClassMap = new HashMap<>();
			if (auxiliaryObjectClassDelta != null) {
				// Activation change means modification of attributes
				if (auxiliaryObjectClassDelta.isReplace()) {
					if (auxiliaryObjectClassDelta.getValuesToReplace() == null || auxiliaryObjectClassDelta.getValuesToReplace().isEmpty()) {
						attributesToUpdate.add(AttributeBuilder.build(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME));
					} else {
						addConvertedValues(auxiliaryObjectClassDelta.getValuesToReplace(), attributesToUpdate, auxiliaryObjectClassMap);
					}
				} else {
					addConvertedValues(auxiliaryObjectClassDelta.getValuesToAdd(), attributesToAdd, auxiliaryObjectClassMap);
					addConvertedValues(auxiliaryObjectClassDelta.getValuesToDelete(), attributesToRemove, auxiliaryObjectClassMap);
				}		
			}
			
			for (Operation operation : changes) {
				if (operation instanceof PropertyModificationOperation) {
					PropertyModificationOperation change = (PropertyModificationOperation) operation;
					PropertyDelta<?> delta = change.getPropertyDelta();
	
					if (delta.getParentPath().equivalent(new ItemPath(ShadowType.F_ATTRIBUTES))) {
						if (delta.getDefinition() == null || !(delta.getDefinition() instanceof ResourceAttributeDefinition)) {
							ResourceAttributeDefinition def = objectClassDef
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
						boolean isInRemovedAuxClass = false;
						boolean isInAddedAuxClass = false;
						ResourceAttributeDefinition<Object> structAttrDef = structuralObjectClassDefinition.findAttributeDefinition(delta.getElementName());
						// if this attribute is also in the structural object class. It does not matter if it is in
						// aux object class, we cannot add/remove it with the object class unless it is normally requested
						if (structAttrDef == null) {
							if (auxiliaryObjectClassDelta != null && auxiliaryObjectClassDelta.isDelete()) {
								// We need to change all the deltas of all the attributes that belong
								// to the removed auxiliary object class from REPLACE to DELETE. The change of
								// auxiliary object class and the change of the attributes must be done in
								// one operation. Otherwise we get schema error. And as auxiliary object class
								// is removed, the attributes must be removed as well.
								for (PrismPropertyValue<QName> auxPval: auxiliaryObjectClassDelta.getValuesToDelete()) {
									ObjectClassComplexTypeDefinition auxDef = auxiliaryObjectClassMap.get(auxPval.getValue());
									ResourceAttributeDefinition<Object> attrDef = auxDef.findAttributeDefinition(delta.getElementName());
									if (attrDef != null) {
										isInRemovedAuxClass = true;
										break;
									}
								}
							}
							if (auxiliaryObjectClassDelta != null && auxiliaryObjectClassDelta.isAdd()) {
								// We need to change all the deltas of all the attributes that belong
								// to the new auxiliary object class from REPLACE to ADD. The change of
								// auxiliary object class and the change of the attributes must be done in
								// one operation. Otherwise we get schema error. And as auxiliary object class
								// is added, the attributes must be added as well.
								for (PrismPropertyValue<QName> auxPval: auxiliaryObjectClassDelta.getValuesToAdd()) {
									ObjectClassComplexTypeDefinition auxOcDef = auxiliaryObjectClassMap.get(auxPval.getValue());
									ResourceAttributeDefinition<Object> auxAttrDef = auxOcDef.findAttributeDefinition(delta.getElementName());
									if (auxAttrDef != null) {
										isInAddedAuxClass = true;
										break;
									}
								}
							}
						}
						// Change in (ordinary) attributes. Transform to the ConnId attributes.
						if (delta.isAdd()) {
							ResourceAttribute<?> mpAttr = (ResourceAttribute<?>) delta.instantiateEmptyProperty();
							mpAttr.addValues((Collection)PrismValue.cloneCollection(delta.getValuesToAdd()));
							Attribute connIdAttr = connIdConvertor.convertToConnIdAttribute(mpAttr, objectClassDef);
							if (mpAttr.getDefinition().isMultiValue()) {
								attributesToAdd.add(connIdAttr);
							} else {
								// Force "update" for single-valued attributes instead of "add". This is saving one
								// read in some cases. It should also make no substantial difference in such case.
								// But it is working around some connector bugs.
								attributesToUpdate.add(connIdAttr);
							}
						}
						if (delta.isDelete()) {
							ResourceAttribute<?> mpAttr = (ResourceAttribute<?>) delta.instantiateEmptyProperty();
							if (mpAttr.getDefinition().isMultiValue() || isInRemovedAuxClass) {
								mpAttr.addValues((Collection)PrismValue.cloneCollection(delta.getValuesToDelete()));
								Attribute connIdAttr = connIdConvertor.convertToConnIdAttribute(mpAttr, objectClassDef);
								attributesToRemove.add(connIdAttr);
							} else {
								// Force "update" for single-valued attributes instead of "add". This is saving one
								// read in some cases. 
								// Update attribute to no values. This will efficiently clean up the attribute.
								// It should also make no substantial difference in such case. 
								// But it is working around some connector bugs.
								Attribute connIdAttr = connIdConvertor.convertToConnIdAttribute(mpAttr, objectClassDef);
								// update with EMTPY value. The mpAttr.addValues() is NOT in this branch
								attributesToUpdate.add(connIdAttr);
							}
						}
						if (delta.isReplace()) {
							ResourceAttribute<?> mpAttr = (ResourceAttribute<?>) delta.instantiateEmptyProperty();
							mpAttr.addValues((Collection)PrismValue.cloneCollection(delta.getValuesToReplace()));
							Attribute connIdAttr = connIdConvertor.convertToConnIdAttribute(mpAttr, objectClassDef);
							if (isInAddedAuxClass) {
								attributesToAdd.add(connIdAttr);
							} else {
								attributesToUpdate.add(connIdAttr);
							}
						}
					} else if (delta.getParentPath().equivalent(new ItemPath(ShadowType.F_ACTIVATION))) {
						activationDeltas.add(delta);
					} else if (delta.getParentPath().equivalent(
							new ItemPath(new ItemPath(ShadowType.F_CREDENTIALS),
									CredentialsType.F_PASSWORD))) {
						passwordDelta = (PropertyDelta<ProtectedStringType>) delta;
					} else if (delta.getPath().equivalent(new ItemPath(ShadowType.F_AUXILIARY_OBJECT_CLASS))) {
						// already processed
					} else {
						throw new SchemaException("Change of unknown attribute " + delta.getPath());
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
		} catch (SchemaException | RuntimeException e) {
			result.recordFatalError(e);
			throw e;
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("attributes:\nADD: {}\nUPDATE: {}\nREMOVE: {}", attributesToAdd, attributesToUpdate, attributesToRemove);
		}
		
		// Needs three complete try-catch blocks because we need to create
		// icfResult for each operation
		// and handle the faults individually

		checkAndExecuteAdditionalOperation(reporter, additionalOperations, BeforeAfterType.BEFORE, result);

		OperationResult connIdResult = null;
		try {
			if (!attributesToAdd.isEmpty()) {
				OperationOptions options = new OperationOptionsBuilder().build();
				connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".addAttributeValues");
				connIdResult.addParam("objectClass", objectClassDef);
				connIdResult.addParam("uid", uid.getUidValue());
				connIdResult.addArbitraryCollectionAsParam("attributes", attributesToAdd);
				connIdResult.addArbitraryObjectAsParam("options", options);
				connIdResult.addContext("connector", connIdConnectorFacade.getClass());

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
							"Invoking ICF addAttributeValues(), objectclass={}, uid={}, attributes: {}",
							new Object[] { objClass, uid, dumpAttributes(attributesToAdd) });
				}

				InternalMonitor.recordConnectorOperation("addAttributeValues");
				
				// Invoking ConnId
				recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);
				uid = connIdConnectorFacade.addAttributeValues(objClass, uid, attributesToAdd, options);
				recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, null, uid);

				connIdResult.recordSuccess();
			}
		} catch (Throwable ex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, ex, uid);
			String desc = this.getHumanReadableName() + " while adding attribute values to object identified by ICF UID '"+uid.getUidValue()+"'";
			Throwable midpointEx = processIcfException(ex, desc, connIdResult);
			result.computeStatus("Adding attribute values failed");
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException) midpointEx;
			} else if (midpointEx instanceof CommunicationException) {
				//in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
				result.muteError();
				connIdResult.muteError();
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
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
			}
		}

		if (!attributesToUpdate.isEmpty() || activationDeltas != null
				|| passwordDelta != null || auxiliaryObjectClassDelta != null) {


			try {				
				if (activationDeltas != null) {
					// Activation change means modification of attributes
					convertFromActivation(attributesToUpdate, activationDeltas);
				}

				if (passwordDelta != null) {
					// Activation change means modification of attributes
					convertFromPassword(attributesToUpdate, passwordDelta);
				}
				
			} catch (SchemaException ex) {
				result.recordFatalError(
						"Error while converting resource object attributes. Reason: " + ex.getMessage(), ex);
				throw new SchemaException("Error while converting resource object attributes. Reason: "
						+ ex.getMessage(), ex);
			} catch (RuntimeException ex) {
				result.recordFatalError("Error while converting resource object attributes. Reason: " + ex.getMessage(), ex);
				throw ex;
			}

			if (!attributesToUpdate.isEmpty()) {
				OperationOptions options = new OperationOptionsBuilder().build();
				connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".update");
				connIdResult.addParam("objectClass", objectClassDef);
				connIdResult.addParam("uid", uid==null?"null":uid.getUidValue());
				connIdResult.addArbitraryCollectionAsParam("attributes", attributesToUpdate);
				connIdResult.addArbitraryObjectAsParam("options", options);
				connIdResult.addContext("connector", connIdConnectorFacade.getClass());
	
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Invoking ICF update(), objectclass={}, uid={}, attributes: {}", new Object[] {
							objClass, uid, dumpAttributes(attributesToUpdate) });
				}
	
				try {
					// Call ICF
					InternalMonitor.recordConnectorOperation("update");
					recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);
					uid = connIdConnectorFacade.update(objClass, uid, attributesToUpdate, options);
					recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, null, uid);
	
					connIdResult.recordSuccess();
				} catch (Throwable ex) {
					recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, ex, uid);
					String desc = this.getHumanReadableName() + " while updating object identified by ICF UID '"+uid.getUidValue()+"'";
					Throwable midpointEx = processIcfException(ex, desc, connIdResult);
					result.computeStatus("Update failed");
					// Do some kind of acrobatics to do proper throwing of checked
					// exception
					if (midpointEx instanceof ObjectNotFoundException) {
						throw (ObjectNotFoundException) midpointEx;
					} else if (midpointEx instanceof CommunicationException) {
						//in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
						result.muteError();
						connIdResult.muteError();
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
						throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
					}
				}
			}
		}

		try {
			if (!attributesToRemove.isEmpty()) {
				OperationOptions options = new OperationOptionsBuilder().build();
				connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".removeAttributeValues");
				connIdResult.addParam("objectClass", objectClassDef);
				connIdResult.addParam("uid", uid.getUidValue());
				connIdResult.addArbitraryCollectionAsParam("attributes", attributesToRemove);
				connIdResult.addArbitraryObjectAsParam("options", options);
				connIdResult.addContext("connector", connIdConnectorFacade.getClass());

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
							"Invoking ICF removeAttributeValues(), objectclass={}, uid={}, attributes: {}",
							new Object[] { objClass, uid, dumpAttributes(attributesToRemove) });
				}

				InternalMonitor.recordConnectorOperation("removeAttributeValues");
				recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);
				uid = connIdConnectorFacade.removeAttributeValues(objClass, uid, attributesToRemove, options);
				recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, null, uid);
				connIdResult.recordSuccess();
			}
		} catch (Throwable ex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, ex, uid);
			String desc = this.getHumanReadableName() + " while removing attribute values from object identified by ICF UID '"+uid.getUidValue()+"'";
			Throwable midpointEx = processIcfException(ex, desc, connIdResult);
			result.computeStatus("Removing attribute values failed");
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException) midpointEx;
			} else if (midpointEx instanceof CommunicationException) {
				//in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
				result.muteError();
				connIdResult.muteError();
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
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
			}
		}
		checkAndExecuteAdditionalOperation(reporter, additionalOperations, BeforeAfterType.AFTER, result);
		
		result.computeStatus();

		Collection<PropertyModificationOperation> sideEffectChanges = new ArrayList<>();
		if (!originalUid.equals(uid.getUidValue())) {
			// UID was changed during the operation, this is most likely a
			// rename
			PropertyDelta<String> uidDelta = createUidDelta(uid, getUidDefinition(objectClassDef, identifiers));
			PropertyModificationOperation uidMod = new PropertyModificationOperation(uidDelta);
			// TODO what about matchingRuleQName ?
			sideEffectChanges.add(uidMod);

			replaceUidValue(objectClassDef, identifiers, uid);
		}
		return ConnectorOperationReturnValue.wrap(sideEffectChanges, result);
	}

	private PropertyDelta<String> createUidDelta(Uid uid, ResourceAttributeDefinition uidDefinition) {
		PropertyDelta<String> uidDelta = new PropertyDelta<String>(new ItemPath(ShadowType.F_ATTRIBUTES, uidDefinition.getName()),
				uidDefinition, prismContext);
		uidDelta.setValueToReplace(new PrismPropertyValue<String>(uid.getUidValue()));
		return uidDelta;
	}

	private String dumpAttributes(Set<Attribute> attributes) {
		if (attributes == null) {
			return "(null)";
		}
		if (attributes.isEmpty()) {
			return "(empty)";
		}
		StringBuilder sb = new StringBuilder();
		for (Attribute attr : attributes) {
			sb.append("\n");
			if (attr.getValue() == null || attr.getValue().isEmpty()) {
				sb.append(attr.getName());
				sb.append(" (empty)");
			} else {
				for (Object value : attr.getValue()) {
					sb.append(attr.getName());
					sb.append(" = ");
					sb.append(value);
				}
			}
		}
		return sb.toString();
	}

	@Override
	public ConnectorOperationResult deleteObject(ObjectClassComplexTypeDefinition objectClass, Collection<Operation> additionalOperations, Collection<? extends ResourceAttribute<?>> identifiers, StateReporter reporter,
							 OperationResult parentResult) 
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException {
		Validate.notNull(objectClass, "No objectclass");

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".deleteObject");
		result.addCollectionOfSerializablesAsParam("identifiers", identifiers);

		ObjectClass objClass = connIdNameMapper.objectClassToIcf(objectClass, getSchemaNamespace(), connectorType, legacySchema);
		Uid uid;
		try {
			uid = getUid(objectClass, identifiers);
		} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		}

		checkAndExecuteAdditionalOperation(reporter, additionalOperations, BeforeAfterType.BEFORE, result);
		
		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".delete");
		icfResult.addArbitraryObjectAsParam("uid", uid);
		icfResult.addArbitraryObjectAsParam("objectClass", objClass);
		icfResult.addContext("connector", connIdConnectorFacade.getClass());

		try {

			InternalMonitor.recordConnectorOperation("delete");
			recordIcfOperationStart(reporter, ProvisioningOperation.ICF_DELETE, objectClass, uid);
			connIdConnectorFacade.delete(objClass, uid, new OperationOptionsBuilder().build());
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_DELETE, objectClass, null, uid);

			icfResult.recordSuccess();

		} catch (Throwable ex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_DELETE, objectClass, ex, uid);
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
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
			}
		}
		
		checkAndExecuteAdditionalOperation(reporter, additionalOperations, BeforeAfterType.AFTER, result);

		result.computeStatus();
		return ConnectorOperationResult.wrap(result);
	}
	
	@Override
	public PrismProperty<?> deserializeToken(Object serializedToken) {
		return createTokenProperty(serializedToken);
	}

	@Override
	public <T> PrismProperty<T> fetchCurrentToken(ObjectClassComplexTypeDefinition objectClassDef, StateReporter reporter,
												  OperationResult parentResult) throws CommunicationException, GenericFrameworkException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".fetchCurrentToken");
		result.addParam("objectClass", objectClassDef);

		ObjectClass icfObjectClass;
		if (objectClassDef == null) {
			icfObjectClass = ObjectClass.ALL;
		} else {
			icfObjectClass = connIdNameMapper.objectClassToIcf(objectClassDef, getSchemaNamespace(), connectorType, legacySchema);
		}
		
		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".sync");
		icfResult.addContext("connector", connIdConnectorFacade.getClass());
		icfResult.addArbitraryObjectAsParam("icfObjectClass", icfObjectClass);
		
		SyncToken syncToken = null;
		try {
			InternalMonitor.recordConnectorOperation("getLatestSyncToken");
			recordIcfOperationStart(reporter, ProvisioningOperation.ICF_GET_LATEST_SYNC_TOKEN, objectClassDef);
			syncToken = connIdConnectorFacade.getLatestSyncToken(icfObjectClass);
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_LATEST_SYNC_TOKEN, objectClassDef);
			icfResult.recordSuccess();
			icfResult.addReturn("syncToken", syncToken==null?null:String.valueOf(syncToken.getValue()));
		} catch (Throwable ex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_LATEST_SYNC_TOKEN, objectClassDef, ex);
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
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
			}
		}

		if (syncToken == null) {
			result.recordWarning("Resource have not provided a current sync token");
			return null;
		}

		PrismProperty<T> property = getToken(syncToken);
		result.recordSuccess();
		return property;
	}

	@Override
	public List<Change>  fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken, AttributesToReturn attrsToReturn, StateReporter reporter,
																OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
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
		ObjectClass icfObjectClass;
		if (objectClass == null) {
			icfObjectClass = ObjectClass.ALL;
		} else {
			icfObjectClass = connIdNameMapper.objectClassToIcf(objectClass, getSchemaNamespace(), connectorType, legacySchema);
		}

		OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
		if (objectClass != null) {
			convertToIcfAttrsToGet(objectClass, attrsToReturn, optionsBuilder);
		}
		OperationOptions options = optionsBuilder.build();
		
		SyncResultsHandler syncHandler = new SyncResultsHandler() {
			@Override
			public boolean handle(SyncDelta delta) {
				LOGGER.trace("Detected sync delta: {}", delta);
				return syncDeltas.add(delta);
			}
		};

		OperationResult connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".sync");
		connIdResult.addContext("connector", connIdConnectorFacade.getClass());
		connIdResult.addArbitraryObjectAsParam("connIdObjectClass", icfObjectClass);
		connIdResult.addArbitraryObjectAsParam("syncToken", syncToken);
		connIdResult.addArbitraryObjectAsParam("syncHandler", syncHandler);

		SyncToken lastReceivedToken;
		try {
			InternalMonitor.recordConnectorOperation("sync");
			recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SYNC, objectClass);
			lastReceivedToken = connIdConnectorFacade.sync(icfObjectClass, syncToken, syncHandler,
					options);
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SYNC, objectClass);
			connIdResult.recordSuccess();
			connIdResult.addReturn(OperationResult.RETURN_COUNT, syncDeltas.size());
		} catch (Throwable ex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SYNC, objectClass, ex);
			Throwable midpointEx = processIcfException(ex, this, connIdResult);
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
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
			}
		}
		// convert changes from icf to midpoint Change
		List<Change> changeList;
		try {
			changeList = getChangesFromSyncDeltas(icfObjectClass, syncDeltas, resourceSchema, result);
		} catch (SchemaException ex) {
			result.recordFatalError(ex.getMessage(), ex);
			throw new SchemaException(ex.getMessage(), ex);
		}
		
		if (lastReceivedToken != null) {
			Change lastChange = new Change((ObjectDelta)null, getToken(lastReceivedToken));
			LOGGER.trace("Adding last change: {}", lastChange);
			changeList.add(lastChange);
		}

		result.recordSuccess();
		result.addReturn(OperationResult.RETURN_COUNT, changeList == null ? 0 : changeList.size());
		return changeList;
	}

	@Override
	public void test(OperationResult parentResult) {

		OperationResult connectionResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
		connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorInstanceConnIdImpl.class);
		connectionResult.addContext("connector", connectorType);

		try {
			InternalMonitor.recordConnectorOperation("test");
			connIdConnectorFacade.test();
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
    public <T extends ShadowType> SearchResultMetadata search(final ObjectClassComplexTypeDefinition objectClassDefinition, 
    		final ObjectQuery query, final ResultHandler<T> handler, AttributesToReturn attributesToReturn, 
    		PagedSearchCapabilityType pagedSearchCapabilityType, SearchHierarchyConstraints searchHierarchyConstraints, 
    		final StateReporter reporter, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SecurityViolationException, SchemaException,
            			ObjectNotFoundException {

		// Result type for this operation
		final OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".search");
		result.addParam("objectClass", objectClassDefinition);
		result.addContext("connector", connectorType);

		if (objectClassDefinition == null) {
			result.recordFatalError("Object class not defined");
			throw new IllegalArgumentException("objectClass not defined");
		}

		ObjectClass icfObjectClass = connIdNameMapper.objectClassToIcf(objectClassDefinition, getSchemaNamespace(), connectorType, legacySchema);
		if (icfObjectClass == null) {
			IllegalArgumentException ex = new IllegalArgumentException(
					"Unable to determine object class from QName " + objectClassDefinition
							+ " while attempting to search objects by "
							+ ObjectTypeUtil.toShortString(connectorType));
			result.recordFatalError("Unable to determine object class", ex);
			throw ex;
		}
		final PrismObjectDefinition<T> objectDefinition = toShadowDefinition(objectClassDefinition);

		if (pagedSearchCapabilityType == null) {
			pagedSearchCapabilityType = getCapability(PagedSearchCapabilityType.class);
		}
		
        final boolean useConnectorPaging = pagedSearchCapabilityType != null;
        if (!useConnectorPaging && query != null && query.getPaging() != null && 
        		(query.getPaging().getOffset() != null || query.getPaging().getMaxSize() != null)) {
        	InternalMonitor.recordConnectorSimulatedPagingSearchCount();
        }

        final Holder<Integer> countHolder = new Holder<>(0);

		ResultsHandler icfHandler = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				// Convert ICF-specific connector object to a generic
				// ResourceObject
				recordIcfOperationSuspend(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
                int count = countHolder.getValue();
                countHolder.setValue(count+1);
                if (!useConnectorPaging) {
                    if (query != null && query.getPaging() != null && query.getPaging().getOffset() != null
                            && query.getPaging().getMaxSize() != null) {
                        if (count < query.getPaging().getOffset()) {
							recordResume();
                            return true;
                        }

                        if (count == (query.getPaging().getOffset() + query.getPaging().getMaxSize())) {
							recordResume();
                            return false;
                        }
                    }
				}
				PrismObject<T> resourceObject;
				try {
					resourceObject = connIdConvertor.convertToResourceObject(connectorObject, objectDefinition, false, caseIgnoreAttributeNames, legacySchema);
				} catch (SchemaException e) {
					recordResume();
					throw new IntermediateException(e);
				}

				// .. and pass it to the handler
				boolean cont = handler.handle(resourceObject);
				if (!cont) {
					result.recordWarning("Stopped on request from the handler");
				}
				recordResume();
				return cont;
			}

			private void recordResume() {
				recordIcfOperationResume(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
			}
		};
		
		OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
		
		try {
			convertToIcfAttrsToGet(objectClassDefinition, attributesToReturn, optionsBuilder);
			if (query != null && query.isAllowPartialResults()) {
				optionsBuilder.setAllowPartialResults(query.isAllowPartialResults());
			}
	        // preparing paging-related options
	        if (useConnectorPaging && query != null && query.getPaging() != null) {
	            ObjectPaging paging = query.getPaging();
	            if (paging.getOffset() != null) {
	                optionsBuilder.setPagedResultsOffset(paging.getOffset() + 1);       // ConnId API says the numbering starts at 1
	            }
	            if (paging.getMaxSize() != null) {
	                optionsBuilder.setPageSize(paging.getMaxSize());
	            }
	            QName orderByAttributeName;
	            boolean isAscending;
				ItemPath orderByPath = paging.getOrderBy();
				String desc;
	            if (orderByPath != null && !orderByPath.isEmpty()) {
	            	orderByAttributeName = ShadowUtil.getAttributeName(orderByPath, "OrderBy path");
	                if (SchemaConstants.C_NAME.equals(orderByAttributeName)) {
	                	orderByAttributeName = SchemaConstants.ICFS_NAME;
	                }
	                isAscending = paging.getDirection() != OrderDirection.DESCENDING;
	                desc = "(explicitly specified orderBy attribute)";
	            } else {
	            	orderByAttributeName = pagedSearchCapabilityType.getDefaultSortField();
	                isAscending = pagedSearchCapabilityType.getDefaultSortDirection() != OrderDirectionType.DESCENDING;
	                desc = "(default orderBy attribute from capability definition)";
	            }
	            if (orderByAttributeName != null) {
	                String orderByIcfName = connIdNameMapper.convertAttributeNameToIcf(orderByAttributeName, objectClassDefinition, desc);
	                optionsBuilder.setSortKeys(new SortKey(orderByIcfName, isAscending));
	            }
	        }
	        if (searchHierarchyConstraints != null) {
	        	ResourceObjectIdentification baseContextIdentification = searchHierarchyConstraints.getBaseContext();
	        	// Only LDAP connector really supports base context. And this one will work better with
	        	// DN. And DN is secondary identifier (__NAME__). This is ugly, but practical. It works around ConnId problems.
	        	ResourceAttribute<?> secondaryIdentifier = baseContextIdentification.getSecondaryIdentifier();
	        	if (secondaryIdentifier == null) {
	        		SchemaException e = new SchemaException("No secondary identifier in base context identification "+baseContextIdentification);
	        		result.recordFatalError(e);
	        		throw e;
	        	}
	        	String secondaryIdentifierValue = secondaryIdentifier.getRealValue(String.class);
	        	ObjectClass baseContextIcfObjectClass = connIdNameMapper.objectClassToIcf(baseContextIdentification.getObjectClassDefinition(), getSchemaNamespace(), connectorType, legacySchema);
	        	QualifiedUid containerQualifiedUid = new QualifiedUid(baseContextIcfObjectClass, new Uid(secondaryIdentifierValue));
				optionsBuilder.setContainer(containerQualifiedUid);
	        }
	        
		} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		}
        
        // Relax completeness requirements. This is a search, not get. So it is OK to
        // return incomplete member lists and similar attributes.
        optionsBuilder.setAllowPartialAttributeValues(true);
        
		OperationOptions options = optionsBuilder.build();

		Filter filter;
		try {
			filter = convertFilterToIcf(query, objectClassDefinition);
		} catch (SchemaException | RuntimeException e) {
			result.recordFatalError(e);
			throw e;
		}
		
		// Connector operation cannot create result for itself, so we need to
		// create result for it
		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".search");
		icfResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
		icfResult.addContext("connector", connIdConnectorFacade.getClass());

		SearchResult icfSearchResult;
		try {

			InternalMonitor.recordConnectorOperation("search");
			recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
			icfSearchResult = connIdConnectorFacade.search(icfObjectClass, filter, icfHandler, options);
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);

			icfResult.recordSuccess();
		} catch (IntermediateException inex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, inex);
			SchemaException ex = (SchemaException) inex.getCause();
			icfResult.recordFatalError(ex);
			result.recordFatalError(ex);
			throw ex;
		} catch (Throwable ex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, ex);
			Throwable midpointEx = processIcfException(ex, this, icfResult);
			result.computeStatus();
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof CommunicationException) {
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof SecurityViolationException) {
				throw (SecurityViolationException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else if (midpointEx instanceof Error) {
				throw (Error) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
			}
		}
		
		SearchResultMetadata metadata = null;
		if (icfSearchResult != null) {
			metadata = new SearchResultMetadata();
			metadata.setPagingCookie(icfSearchResult.getPagedResultsCookie());
			if (icfSearchResult.getRemainingPagedResults() >= 0) {
				metadata.setApproxNumberOfAllResults(icfSearchResult.getRemainingPagedResults());
			}
			if (!icfSearchResult.isAllResultsReturned()) {
				metadata.setPartialResults(true);
			}
		}

		if (result.isUnknown()) {
			result.recordSuccess();
		}
		
		return metadata;
	}

	@Override
    public int count(ObjectClassComplexTypeDefinition objectClassDefinition, final ObjectQuery query, PagedSearchCapabilityType pagedSearchCapabilityType, StateReporter reporter,
					 OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, UnsupportedOperationException {

        // Result type for this operation
        final OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
                + ".count");
        result.addParam("objectClass", objectClassDefinition);
        result.addContext("connector", connectorType);

        if (objectClassDefinition == null) {
            result.recordFatalError("Object class not defined");
            throw new IllegalArgumentException("objectClass not defined");
        }

        ObjectClass icfObjectClass = connIdNameMapper.objectClassToIcf(objectClassDefinition, getSchemaNamespace(), connectorType, legacySchema);
        if (icfObjectClass == null) {
            IllegalArgumentException ex = new IllegalArgumentException(
                    "Unable to determine object class from QName " + objectClassDefinition
                            + " while attempting to search objects by "
                            + ObjectTypeUtil.toShortString(connectorType));
            result.recordFatalError("Unable to determine object class", ex);
            throw ex;
        }
        final boolean useConnectorPaging = pagedSearchCapabilityType != null;
        if (!useConnectorPaging) {
            throw new UnsupportedOperationException("ConnectorInstanceIcfImpl.count operation is supported only in combination with connector-implemented paging");
        }

        OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
        optionsBuilder.setAttributesToGet(Name.NAME);
        optionsBuilder.setPagedResultsOffset(1);
        optionsBuilder.setPageSize(1);
        if (pagedSearchCapabilityType.getDefaultSortField() != null) {
            String orderByIcfName = connIdNameMapper.convertAttributeNameToIcf(pagedSearchCapabilityType.getDefaultSortField(), objectClassDefinition, "(default sorting field)");
            boolean isAscending = pagedSearchCapabilityType.getDefaultSortDirection() != OrderDirectionType.DESCENDING;
            optionsBuilder.setSortKeys(new SortKey(orderByIcfName, isAscending));
        }
        OperationOptions options = optionsBuilder.build();

        // Connector operation cannot create result for itself, so we need to
        // create result for it
        OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".search");
        icfResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
        icfResult.addContext("connector", connIdConnectorFacade.getClass());

        int retval;

        try {

            Filter filter = convertFilterToIcf(query, objectClassDefinition);
            final Holder<Integer> fetched = new Holder<>(0);

            ResultsHandler icfHandler = new ResultsHandler() {
                @Override
                public boolean handle(ConnectorObject connectorObject) {
                    fetched.setValue(fetched.getValue()+1);         // actually, this should execute at most once
                    return false;
                }
            };
            InternalMonitor.recordConnectorOperation("search");
			recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
            SearchResult searchResult = connIdConnectorFacade.search(icfObjectClass, filter, icfHandler, options);
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);

            if (searchResult == null || searchResult.getRemainingPagedResults() == -1) {
                throw new UnsupportedOperationException("Connector does not seem to support paged searches or does not provide object count information");
            } else {
                retval = fetched.getValue() + searchResult.getRemainingPagedResults();
            }

            icfResult.recordSuccess();
        } catch (IntermediateException inex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, inex);
            SchemaException ex = (SchemaException) inex.getCause();
            icfResult.recordFatalError(ex);
            result.recordFatalError(ex);
            throw ex;
        } catch (UnsupportedOperationException uoe) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, uoe);
            icfResult.recordFatalError(uoe);
            result.recordFatalError(uoe);
            throw uoe;
        } catch (Throwable ex) {
			recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, ex);
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
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        if (result.isUnknown()) {
            result.recordSuccess();
        }

        return retval;
    }

    private Filter convertFilterToIcf(ObjectQuery query, ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException {
        Filter filter = null;
        if (query != null && query.getFilter() != null) {
            FilterInterpreter interpreter = new FilterInterpreter(objectClassDefinition);
            LOGGER.trace("Start to convert filter: {}", query.getFilter().debugDump());
            filter = interpreter.interpret(query.getFilter(), connIdNameMapper);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("ICF filter: {}", ConnIdUtil.dump(filter));
            }
        }
        return filter;
    }

    // UTILITY METHODS

    private Uid getUid(ResourceObjectIdentification resourceObjectIdentification) throws SchemaException {
    	ResourceAttribute<String> primaryIdentifier = resourceObjectIdentification.getPrimaryIdentifier();
		if (primaryIdentifier == null) {
			return null;
		}
		String uidValue = primaryIdentifier.getRealValue();
		String nameValue = null;
		Collection<? extends ResourceAttribute<?>> secondaryIdentifiers = resourceObjectIdentification.getSecondaryIdentifiers();
		if (secondaryIdentifiers != null && secondaryIdentifiers.size() == 1) {
			nameValue = (String) secondaryIdentifiers.iterator().next().getRealValue();
		}
		if (uidValue != null) {
			if (nameValue == null) {
				return new Uid(uidValue);
			} else {
				return new Uid(uidValue, new Name(nameValue));
			}
		}
		return null;
	}

	/**
	 * Looks up ConnId Uid identifier in a (potentially multi-valued) set of
	 * identifiers. Handy method to convert midPoint identifier style to an ICF
	 * identifier style.
	 * 
	 * @param identifiers
	 *            midPoint resource object identifiers
	 * @return ConnId UID or null
	 */
	private Uid getUid(ObjectClassComplexTypeDefinition objectClass, Collection<? extends ResourceAttribute<?>> identifiers) throws SchemaException {
		if (identifiers.size() == 0) {
			return null;
		}
		if (identifiers.size() == 1) {
			try {
				return new Uid((String) identifiers.iterator().next().getRealValue());
			} catch (IllegalArgumentException e) {
				throw new SchemaException(e.getMessage(), e);
			}
		}
		String uidValue = null;
		String nameValue = null;
		for (ResourceAttribute<?> attr : identifiers) {
			if (objectClass.isPrimaryIdentifier(attr.getElementName())) {
				uidValue = ((ResourceAttribute<String>) attr).getValue().getValue();
			}
			if (objectClass.isSecondaryIdentifier(attr.getElementName())) {
				ResourceAttributeDefinition<?> attrDef = objectClass.findAttributeDefinition(attr.getElementName());
				String frameworkAttributeName = attrDef.getFrameworkAttributeName();
				if (Name.NAME.equals(frameworkAttributeName)) {
					nameValue = ((ResourceAttribute<String>) attr).getValue().getValue();
				}
			}
		}
		if (uidValue != null) {
			if (nameValue == null) {
				return new Uid(uidValue);
			} else {
				return new Uid(uidValue, new Name(nameValue));
			}
		}
		// fallback, compatibility
		for (ResourceAttribute<?> attr : identifiers) {
			if (attr.getElementName().equals(SchemaConstants.ICFS_UID)) {
				return new Uid(((ResourceAttribute<String>) attr).getValue().getValue());
			}
		}
		return null;
	}

	private void replaceUidValue(ObjectClassComplexTypeDefinition objectClass, Collection<? extends ResourceAttribute<?>> identifiers, Uid newUid) {
		if (identifiers.size() == 0) {
			throw new IllegalStateException("No identifiers");
		}
		if (identifiers.size() == 1) {
			identifiers.iterator().next().setValue(new PrismPropertyValue(newUid.getUidValue()));
			return;
		}
		for (ResourceAttribute<?> attr : identifiers) {
			if (objectClass.isPrimaryIdentifier(attr.getElementName())) {
				((ResourceAttribute<String>) attr).setValue(new PrismPropertyValue(newUid.getUidValue()));
				return;
			}
		}
		// fallback, compatibility
		for (ResourceAttribute<?> attr : identifiers) {
			if (attr.getElementName().equals(SchemaConstants.ICFS_UID)) {
				attr.setValue(new PrismPropertyValue(newUid.getUidValue()));			// expecting the UID property is of type String
				return;
			}
		}
		throw new IllegalStateException("No UID attribute in " + identifiers);
	}

	private ResourceAttributeDefinition getUidDefinition(ObjectClassComplexTypeDefinition objectClass, Collection<? extends ResourceAttribute<?>> identifiers) {
		if (identifiers.size() == 0) {
			return null;
		}
		if (identifiers.size() == 1) {
			return identifiers.iterator().next().getDefinition();
		}
		for (ResourceAttribute<?> attr : identifiers) {
			if (objectClass.isPrimaryIdentifier(attr.getElementName())) {
				return ((ResourceAttribute<String>) attr).getDefinition();
			}
		}
		// fallback, compatibility
		for (ResourceAttribute<?> attr : identifiers) {
			if (attr.getElementName().equals(SchemaConstants.ICFS_UID)) {
				return attr.getDefinition();
			}
		}
		return null;
	}

	private void convertFromActivation(Set<Attribute> updateAttributes,
			Collection<PropertyDelta<?>> activationDeltas) throws SchemaException {

		for (PropertyDelta<?> propDelta : activationDeltas) {
			if (propDelta.getElementName().equals(ActivationType.F_ADMINISTRATIVE_STATUS)) {
				ActivationStatusType status = getPropertyNewValue(propDelta, ActivationStatusType.class);
				if (status == null) {
					updateAttributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME));
				} else {
					updateAttributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, status == ActivationStatusType.ENABLED));
				}
			} else if (propDelta.getElementName().equals(ActivationType.F_VALID_FROM)) {
				XMLGregorianCalendar xmlCal = getPropertyNewValue(propDelta, XMLGregorianCalendar.class);//propDelta.getPropertyNew().getValue(XMLGregorianCalendar.class).getValue();
				updateAttributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, xmlCal != null ? XmlTypeConverter.toMillis(xmlCal) : null));
			} else if (propDelta.getElementName().equals(ActivationType.F_VALID_TO)) {
				XMLGregorianCalendar xmlCal = getPropertyNewValue(propDelta, XMLGregorianCalendar.class);//propDelta.getPropertyNew().getValue(XMLGregorianCalendar.class).getValue();
				updateAttributes.add(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, xmlCal != null ? XmlTypeConverter.toMillis(xmlCal) : null));
			} else if (propDelta.getElementName().equals(ActivationType.F_LOCKOUT_STATUS)) {
				LockoutStatusType status = getPropertyNewValue(propDelta, LockoutStatusType.class);//propDelta.getPropertyNew().getValue(LockoutStatusType.class).getValue();
				updateAttributes.add(AttributeBuilder.build(OperationalAttributes.LOCK_OUT_NAME, status != LockoutStatusType.NORMAL));
			} else {
				throw new SchemaException("Got unknown activation attribute delta " + propDelta.getElementName());
			}
		}

	}

	private <T> T getPropertyNewValue(PropertyDelta propertyDelta, Class<T> clazz) throws SchemaException {
		PrismProperty<PrismPropertyValue<T>> prop = propertyDelta.getPropertyNewMatchingPath();
		if (prop == null){
			return null;
		}
		PrismPropertyValue<T> propValue = prop.getValue(clazz);
		
		if (propValue == null){
			return null;
		}
		
		return propValue.getValue();
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
		PrismProperty<ProtectedStringType> newPassword = passwordDelta.getPropertyNewMatchingPath();
		if (newPassword == null || newPassword.isEmpty()) {
			// This is the case of setting no password. E.g. removing existing password
			LOGGER.debug("Setting null password.");
			attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, Collections.EMPTY_LIST));
		} else if (newPassword.getRealValue().canGetCleartext()) {
			// We have password and we can get a cleartext value of the passowrd. This is normal case
			GuardedString guardedPassword = ConnIdUtil.toGuardedString(newPassword.getRealValue(), "new password", protector);
			attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, guardedPassword));
		} else {
			// We have password, but we cannot get a cleartext value. Just to nothing.
			LOGGER.debug("We would like to set password, but we do not have cleartext value. Skipping the opearation.");
		}
	}
	
	private void addConvertedValues(Collection<PrismPropertyValue<QName>> pvals,
			Set<Attribute> attributes, Map<QName,ObjectClassComplexTypeDefinition> auxiliaryObjectClassMap) throws SchemaException {
		if (pvals == null) {
			return;
		}
		AttributeBuilder ab = new AttributeBuilder();
		ab.setName(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME);
		for (PrismPropertyValue<QName> pval: pvals) {
			QName auxQName = pval.getValue();
			ObjectClassComplexTypeDefinition auxDef = resourceSchema.findObjectClassDefinition(auxQName);
			if (auxDef == null) {
				throw new SchemaException("Auxiliary object class "+auxQName+" not found in the schema");
			}
			auxiliaryObjectClassMap.put(auxQName, auxDef);
			ObjectClass icfOc = connIdNameMapper.objectClassToIcf(pval.getValue(), resourceSchemaNamespace, connectorType, false);
			ab.addValue(icfOc.getObjectClassValue());
		}
		attributes.add(ab.build());
	}

	private List<Change> getChangesFromSyncDeltas(ObjectClass connIdObjClass, Collection<SyncDelta> connIdDeltas,
			PrismSchema schema, OperationResult parentResult)
			throws SchemaException, GenericFrameworkException {
		List<Change> changeList = new ArrayList<Change>();

		QName objectClass = connIdNameMapper.objectClassToQname(connIdObjClass, getSchemaNamespace(), legacySchema);
		ObjectClassComplexTypeDefinition objClassDefinition = null;
		if (objectClass != null) {
			objClassDefinition = (ObjectClassComplexTypeDefinition) schema.findComplexTypeDefinition(objectClass);
		}
		
		Validate.notNull(connIdDeltas, "Sync result must not be null.");
		for (SyncDelta icfDelta : connIdDeltas) {

			ObjectClass deltaIcfObjClass = connIdObjClass;
			QName deltaObjectClass = objectClass;
			ObjectClassComplexTypeDefinition deltaObjClassDefinition = objClassDefinition;
			if (objectClass == null) {
				deltaIcfObjClass = icfDelta.getObjectClass();
				deltaObjectClass = connIdNameMapper.objectClassToQname(deltaIcfObjClass, getSchemaNamespace(), legacySchema);
				if (deltaIcfObjClass != null) {
					deltaObjClassDefinition = (ObjectClassComplexTypeDefinition) schema.findComplexTypeDefinition(deltaObjectClass);
				}
			}
			if (deltaObjClassDefinition == null) {
				if (icfDelta.getDeltaType() == SyncDeltaType.DELETE) {
					// tolerate this. E.g. LDAP changelogs do not have objectclass in delete deltas.
				} else {
					throw new SchemaException("Got delta with object class "+deltaObjectClass+" ("+deltaIcfObjClass+") that has no definition in resource schema");
				}
			}
			
			SyncDeltaType icfDeltaType = icfDelta.getDeltaType();
			if (SyncDeltaType.DELETE.equals(icfDeltaType)) {
				LOGGER.trace("START creating delta of type DELETE");
				ObjectDelta<ShadowType> objectDelta = new ObjectDelta<ShadowType>(
						ShadowType.class, ChangeType.DELETE, prismContext);
				Collection<ResourceAttribute<?>> identifiers = ConnIdUtil.convertToIdentifiers(icfDelta.getUid(), 
						deltaObjClassDefinition, resourceSchema);
				Change change = new Change(identifiers, objectDelta, getToken(icfDelta.getToken()));
				change.setObjectClassDefinition(deltaObjClassDefinition);
				changeList.add(change);
				LOGGER.trace("END creating delta of type DELETE");

			} else if (SyncDeltaType.CREATE.equals(icfDeltaType)) {
				PrismObjectDefinition<ShadowType> objectDefinition = toShadowDefinition(deltaObjClassDefinition);
				LOGGER.trace("Object definition: {}", objectDefinition);
				
				LOGGER.trace("START creating delta of type CREATE");
				PrismObject<ShadowType> currentShadow = connIdConvertor.convertToResourceObject(icfDelta.getObject(),
						objectDefinition, false, caseIgnoreAttributeNames, legacySchema);

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Got current shadow: {}", currentShadow.debugDump());
				}

				Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getAllIdentifiers(currentShadow);
				
				ObjectDelta<ShadowType> objectDelta = new ObjectDelta<ShadowType>(
						ShadowType.class, ChangeType.ADD, prismContext);
				objectDelta.setObjectToAdd(currentShadow);

				Change change = new Change(identifiers, objectDelta, getToken(icfDelta.getToken()));
				change.setObjectClassDefinition(deltaObjClassDefinition);
				changeList.add(change);
				LOGGER.trace("END creating delta of type CREATE");

			} else if (SyncDeltaType.CREATE_OR_UPDATE.equals(icfDeltaType) || 
					SyncDeltaType.UPDATE.equals(icfDeltaType)) {
				PrismObjectDefinition<ShadowType> objectDefinition = toShadowDefinition(deltaObjClassDefinition);
				LOGGER.trace("Object definition: {}", objectDefinition);
				
				LOGGER.trace("START creating delta of type {}", icfDeltaType);
				PrismObject<ShadowType> currentShadow = connIdConvertor.convertToResourceObject(icfDelta.getObject(),
						objectDefinition, false, caseIgnoreAttributeNames, legacySchema);

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Got current shadow: {}", currentShadow.debugDump());
				}

				Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getAllIdentifiers(currentShadow);

				Change change = new Change(identifiers, currentShadow, getToken(icfDelta.getToken()));
				change.setObjectClassDefinition(deltaObjClassDefinition);
				changeList.add(change);
				LOGGER.trace("END creating delta of type {}:\n{}", icfDeltaType, change.debugDump());
				
			} else {
				throw new GenericFrameworkException("Unexpected sync delta type " + icfDeltaType);
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

	private <T> PrismProperty<T> getToken(SyncToken syncToken) {
		T object = (T) syncToken.getValue();
		return createTokenProperty(object);
	}

	private <T> PrismProperty<T> createTokenProperty(T object) {
		QName type = XsdTypeMapper.toXsdType(object.getClass());

		Set<PrismPropertyValue<T>> syncTokenValues = new HashSet<PrismPropertyValue<T>>();
		syncTokenValues.add(new PrismPropertyValue<T>(object));
		PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(SchemaConstants.SYNC_TOKEN,
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
	private void checkAndExecuteAdditionalOperation(StateReporter reporter, Collection<Operation> additionalOperations, BeforeAfterType order, OperationResult result) throws CommunicationException, GenericFrameworkException {

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
					executeScriptIcf(reporter, executeOp, result);
				}
			}
		}

	}
	
	@Override
	public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, StateReporter reporter, OperationResult parentResult) throws CommunicationException, GenericFrameworkException {
		
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".executeScript");
		
		Object output = null;
		try {
			
			output = executeScriptIcf(reporter, scriptOperation, result);
			
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

	private Object executeScriptIcf(StateReporter reporter, ExecuteProvisioningScriptOperation scriptOperation, OperationResult result) throws CommunicationException, GenericFrameworkException {
		
		String icfOpName = null;
		if (scriptOperation.isConnectorHost()) {
			icfOpName = "runScriptOnConnector";
		} else if (scriptOperation.isResourceHost()) {
			icfOpName = "runScriptOnResource";
		} else {
			result.recordFatalError("Where to execute the script?");
			throw new IllegalArgumentException("Where to execute the script?");
		}
		
		// convert execute script operation to the script context required from
			// the connector
			ScriptContext scriptContext = convertToScriptContext(scriptOperation);
			
			OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + "." + icfOpName);
			icfResult.addContext("connector", connIdConnectorFacade.getClass());
			
			Object output = null;
			
			try {
				
				LOGGER.trace("Running script ({})", icfOpName);

				recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SCRIPT, null);
				if (scriptOperation.isConnectorHost()) {
					InternalMonitor.recordConnectorOperation("runScriptOnConnector");
					output = connIdConnectorFacade.runScriptOnConnector(scriptContext, new OperationOptionsBuilder().build());
				} else if (scriptOperation.isResourceHost()) {
					InternalMonitor.recordConnectorOperation("runScriptOnResource");
					output = connIdConnectorFacade.runScriptOnResource(scriptContext, new OperationOptionsBuilder().build());
				}
				recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SCRIPT, null);

				icfResult.recordSuccess();
				
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Finished running script ({}), script result: {}", icfOpName, PrettyPrinter.prettyPrint(output));
				}
				
			} catch (Throwable ex) {

				recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SCRIPT, null, ex);

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
					throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
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

	@Override
	public void dispose() {
		// Nothing to do
	}

	private void recordIcfOperationStart(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition, Uid uid) {
		if (reporter != null) {
			reporter.recordIcfOperationStart(operation, objectClassDefinition, uid==null?null:uid.getUidValue());
		} else {
			LOGGER.warn("Couldn't record ICF operation start as reporter is null.");
		}
	}

	private void recordIcfOperationStart(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition) {
		if (reporter != null) {
			reporter.recordIcfOperationStart(operation, objectClassDefinition, null);
		} else {
			LOGGER.warn("Couldn't record ICF operation start as reporter is null.");
		}
	}

	private void recordIcfOperationResume(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition) {
		if (reporter != null) {
			reporter.recordIcfOperationResume(operation, objectClassDefinition);
		} else {
			LOGGER.warn("Couldn't record ICF operation resume as reporter is null.");
		}
	}

	private void recordIcfOperationSuspend(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition) {
		if (reporter != null) {
			reporter.recordIcfOperationSuspend(operation, objectClassDefinition);
		} else {
			LOGGER.warn("Couldn't record ICF operation suspension as reporter is null.");
		}
	}

	private void recordIcfOperationEnd(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition, Uid uid) {
		if (reporter != null) {
			reporter.recordIcfOperationEnd(operation, objectClassDefinition, null, uid==null?null:uid.getUidValue());
		} else {
			LOGGER.warn("Couldn't record ICF operation end as reporter is null.");
		}
	}

	private void recordIcfOperationEnd(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition, Throwable ex) {
		if (reporter != null) {
			reporter.recordIcfOperationEnd(operation, objectClassDefinition, ex, null);
		} else {
			LOGGER.warn("Couldn't record ICF operation end as reporter is null.");
		}
	}

	private void recordIcfOperationEnd(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition, Throwable ex, Uid uid) {
		if (reporter != null) {
			reporter.recordIcfOperationEnd(operation, objectClassDefinition, ex, uid==null?null:uid.getUidValue());
		} else {
			LOGGER.warn("Couldn't record ICF operation end as reporter is null.");
		}
	}


	private void recordIcfOperationEnd(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition) {
		if (reporter != null) {
			reporter.recordIcfOperationEnd(operation, objectClassDefinition, null, null);
		} else {
			LOGGER.warn("Couldn't record ICF operation end as reporter is null.");
		}
	}
	
}
