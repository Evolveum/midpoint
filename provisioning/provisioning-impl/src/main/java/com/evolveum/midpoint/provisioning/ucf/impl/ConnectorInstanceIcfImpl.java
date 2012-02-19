/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType.EnableDisable;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ScriptCapabilityType.Host;
import org.apache.commons.codec.binary.Base64;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.lang.reflect.Array;
import java.util.*;

import static com.evolveum.midpoint.provisioning.ucf.impl.IcfUtil.processIcfException;

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

	private static final String CUSTOM_OBJECTCLASS_PREFIX = "Custom";
	private static final String CUSTOM_OBJECTCLASS_SUFFIX = "ObjectClass";

	private static final ObjectFactory capabilityObjectFactory = new ObjectFactory();

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorInstanceIcfImpl.class);

	ConnectorInfo cinfo;
	ConnectorType connectorType;
	ConnectorFacade icfConnectorFacade;
	String schemaNamespace;
	Protector protector;
	PrismContext prismContext;

	private ResourceSchema resourceSchema = null;
	Set<Object> capabilities = null;

	public ConnectorInstanceIcfImpl(ConnectorInfo connectorInfo, ConnectorType connectorType,
			String schemaNamespace, Protector protector, PrismContext prismContext) {
		this.cinfo = connectorInfo;
		this.connectorType = connectorType;
		this.schemaNamespace = schemaNamespace;
		this.protector = protector;
		this.prismContext = prismContext;
	}

	public String getSchemaNamespace() {
		return schemaNamespace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance#configure
	 * (com.evolveum.midpoint.xml.ns._public.common.common_1.Configuration)
	 */
	@Override
	public void configure(PrismContainer configuration, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, SchemaException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".configure");
		result.addParam("configuration", configuration);

		try {
			// Get default configuration for the connector. This is important,
			// as
			// it contains types of connector configuration properties.
			// So we do not need to know the connector configuration schema
			// here. We are in fact looking at the data that the schema is
			// generated from.
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
		} catch (Exception ex) {
			Exception midpointEx = processIcfException(ex, result);
			result.computeStatus("Removing attribute values failed");
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
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}

		}

	}

	/**
	 * @param cinfo
	 * @param connectorType
	 */
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
		PrismContainerDefinition configurationContainerDef = mpSchema
				.createPropertyContainerDefinition(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_TYPE_LOCAL_NAME);
		// element with "ConfigurationPropertiesType" - the dynamic part of
		// configuration schema
		configurationContainerDef.createPropertyDefinition(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME);
		// Create common ICF configuration property containers as a references
		// to a static schema
		configurationContainerDef.createPropertyDefinition(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_ELEMENT,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_TYPE, 0, 1);
		configurationContainerDef.createPropertyDefinition(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_ELEMENT,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_TYPE, 0, 1);
		configurationContainerDef.createPropertyDefinition(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_ELEMENT,
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_TYPE, 0, 1);

		// No need to create definition of "configuration" element.
		// midPoint will look for this element, but it will be generated as part
		// of the PropertyContainer serialization to schema

		// Create definition of "configurationProperties" type
		// (CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME)
		PrismContainerDefinition configDef = mpSchema
				.createPropertyContainerDefinition(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME);
		for (String icfPropertyName : icfConfigurationProperties.getPropertyNames()) {
			ConfigurationProperty icfProperty = icfConfigurationProperties.getProperty(icfPropertyName);

			QName propXsdName = new QName(connectorType.getNamespace(), icfPropertyName);
			QName propXsdType = icfTypeToXsdType(icfProperty.getType());
			LOGGER.trace("{}: Mapping ICF config schema property {} from {} to {}", new Object[] { this,
					icfPropertyName, icfProperty.getType(), propXsdType });
			PrismPropertyDefinition propertyDefinifion = configDef.createPropertyDefinition(propXsdName,
					propXsdType);
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
		LOGGER.debug("Generated configuration schema for {}: {} definitions", this, mpSchema.getDefinitions()
				.size());
		return mpSchema;
	}

	private QName icfTypeToXsdType(Class<?> type) {
		// For arrays we are only interested in the component type
		if (isMultivaluedType(type)) {
			type = type.getComponentType();
		}
		QName propXsdType = null;
		if (GuardedString.class.equals(type)) {
			// GuardedString is a special case. It is a ICF-specific
			// type
			// implementing Potemkin-like security. Use a temporary
			// "nonsense" type for now, so this will fail in tests and
			// will be fixed later
			propXsdType = SchemaConstants.R_PROTECTED_STRING_TYPE;
		} else if (GuardedByteArray.class.equals(type)) {
			// GuardedString is a special case. It is a ICF-specific
			// type
			// implementing Potemkin-like security. Use a temporary
			// "nonsense" type for now, so this will fail in tests and
			// will be fixed later
			propXsdType = SchemaConstants.R_PROTECTED_BYTE_ARRAY_TYPE;
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

	/**
	 * Retrieves schema from the resource.
	 * <p/>
	 * Transforms native ICF schema to the midPoint representation.
	 * 
	 * @return midPoint resource schema.
	 * @throws CommunicationException
	 * @see com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance#initialize(com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void initialize(OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException {

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

		// Connector operation cannot create result for itself, so we need to
		// create result for it
		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".schema");
		icfResult.addContext("connector", icfConnectorFacade.getClass());

		org.identityconnectors.framework.common.objects.Schema icfSchema = null;
		try {

			// Fetch the schema from the connector (which actually gets that
			// from the resource).
			icfSchema = icfConnectorFacade.schema();

			icfResult.recordSuccess();
		} catch (Exception ex) {
			// conditions.
			// Therefore this kind of heavy artillery is necessary.
			// ICF interface does not specify exceptions or other error
			// TODO maybe we can try to catch at least some specific exceptions
			Exception midpointEx = processIcfException(ex, icfResult);

			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof CommunicationException) {
				result.recordFatalError("ICF communication error: " + midpointEx.getMessage(), midpointEx);
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				result.recordFatalError("ICF error: " + midpointEx.getMessage(), midpointEx);
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				result.recordFatalError("ICF error: " + midpointEx.getMessage(), midpointEx);
				throw (RuntimeException) midpointEx;
			} else {
				result.recordFatalError("Internal error: " + midpointEx.getMessage(), midpointEx);
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}

		parseResourceSchema(icfSchema);

		result.recordSuccess();
	}

	@Override
	public PrismSchema getResourceSchema(OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException {

		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".getResourceSchema");
		result.addContext("connector", connectorType);

		if (resourceSchema == null) {
			// initialize the connector if it was not initialized yet
			try {
				initialize(result);
			} catch (CommunicationException ex) {
				result.recordFatalError(ex);
				throw ex;
			} catch (GenericFrameworkException ex) {
				result.recordFatalError(ex);
				throw ex;
			}
		}

		result.recordSuccess();

		return resourceSchema;
	}

	private void parseResourceSchema(org.identityconnectors.framework.common.objects.Schema icfSchema) {

		boolean capPassword = false;
		boolean capEnable = false;

		// New instance of midPoint schema object
		resourceSchema = new ResourceSchema(getSchemaNamespace(), prismContext);

		// Let's convert every objectclass in the ICF schema ...
		Set<ObjectClassInfo> objectClassInfoSet = icfSchema.getObjectClassInfo();
		for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {

			// "Flat" ICF object class names needs to be mapped to QNames
			QName objectClassXsdName = objectClassToQname(objectClassInfo.getType());

			// ResourceObjectDefinition is a midPpoint way how to represent an
			// object class.
			// The important thing here is the last "type" parameter
			// (objectClassXsdName). The rest is more-or-less cosmetics.
			ResourceAttributeContainerDefinition roDefinition = resourceSchema
					.createResourceObjectDefinition(objectClassXsdName);

			// The __ACCOUNT__ objectclass in ICF is a default account
			// objectclass. So mark it appropriately.
			if (ObjectClass.ACCOUNT_NAME.equals(objectClassInfo.getType())) {
				roDefinition.setAccountType(true);
				roDefinition.setDefaultAccountType(true);
			}

			// Every object has UID in ICF, therefore add it right now
			ResourceAttributeDefinition uidDefinition = roDefinition.createAttributeDefinition(
					ConnectorFactoryIcfImpl.ICFS_UID, DOMUtil.XSD_STRING);
			// Make it mandatory
			uidDefinition.setMinOccurs(1);
			uidDefinition.setMaxOccurs(1);
			// Make it read-only
			uidDefinition.setReadOnly();
			// Set a default display name
			uidDefinition.setDisplayName("ICF UID");
			// Uid is a primary identifier of every object (this is the ICF way)
			roDefinition.getIdentifiers().add(uidDefinition);

			// Let's iterate over all attributes in this object class ...
			Set<AttributeInfo> attributeInfoSet = objectClassInfo.getAttributeInfo();
			for (AttributeInfo attributeInfo : attributeInfoSet) {

				if (OperationalAttributes.PASSWORD_NAME.equals(attributeInfo.getName())) {
					// This attribute will not go into the schema
					// instead a "password" capability is used
					capPassword = true;
					// Skip this attribute, capability is sufficient
					continue;
				}

				if (OperationalAttributes.ENABLE_NAME.equals(attributeInfo.getName())) {
					capEnable = true;
					// Skip this attribute, capability is sufficient
					continue;
				}

				QName attrXsdName = convertAttributeNameToQName(attributeInfo.getName());
				QName attrXsdType = icfTypeToXsdType(attributeInfo.getType());

				// Create ResourceObjectAttributeDefinition, which is midPoint
				// way how to express attribute schema.
				ResourceAttributeDefinition roaDefinition = roDefinition.createAttributeDefinition(
						attrXsdName, attrXsdType);

				// Set a better display name for __NAME__. The "name" is s very
				// overloaded term, so let's try to make things
				// a bit clearer
				if (attrXsdName.equals(ConnectorFactoryIcfImpl.ICFS_NAME)) {
					roaDefinition.setDisplayName("ICF NAME");
				}

				// Now we are going to process flags such as optional and
				// multi-valued
				Set<Flags> flagsSet = attributeInfo.getFlags();
				// System.out.println(flagsSet);

				roaDefinition.setMinOccurs(0);
				roaDefinition.setMaxOccurs(1);
				boolean canCreate = true;
				boolean canUpdate = true;
				boolean canRead = true;

				for (Flags flags : flagsSet) {
					if (flags == Flags.REQUIRED) {
						roaDefinition.setMinOccurs(1);
					}
					if (flags == Flags.MULTIVALUED) {
						roaDefinition.setMaxOccurs(-1);
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
						// TODO
					}
				}

				roaDefinition.setCreate(canCreate);
				roaDefinition.setUpdate(canUpdate);
				roaDefinition.setRead(canRead);

			}

			// Add schema annotations
			roDefinition.setNativeObjectClass(objectClassInfo.getType());
			roDefinition.setDisplayNameAttribute(ConnectorFactoryIcfImpl.ICFS_NAME);
			roDefinition.setNamingAttribute(ConnectorFactoryIcfImpl.ICFS_NAME);

		}

		capabilities = new HashSet<Object>();

		if (capEnable) {
			ActivationCapabilityType capAct = new ActivationCapabilityType();
			EnableDisable capEnableDisable = new EnableDisable();
			capAct.setEnableDisable(capEnableDisable);

			capabilities.add(capabilityObjectFactory.createActivation(capAct));
		}

		if (capPassword) {
			CredentialsCapabilityType capCred = new CredentialsCapabilityType();
			PasswordCapabilityType capPass = new PasswordCapabilityType();
			capCred.setPassword(capPass);
			capabilities.add(capabilityObjectFactory.createCredentials(capCred));
		}

		// Create capabilities from supported connector operations

		Set<Class<? extends APIOperation>> supportedOperations = icfConnectorFacade.getSupportedOperations();

		if (supportedOperations.contains(SyncApiOp.class)) {
			LiveSyncCapabilityType capSync = new LiveSyncCapabilityType();
			capabilities.add(capabilityObjectFactory.createLiveSync(capSync));
		}

		if (supportedOperations.contains(TestApiOp.class)) {
			TestConnectionCapabilityType capTest = new TestConnectionCapabilityType();
			capabilities.add(capabilityObjectFactory.createTestConnection(capTest));
		}

		if (supportedOperations.contains(ScriptOnResourceApiOp.class)
				|| supportedOperations.contains(ScriptOnConnectorApiOp.class)) {
			ScriptCapabilityType capScript = new ScriptCapabilityType();
			if (supportedOperations.contains(ScriptOnResourceApiOp.class)) {
				Host host = new Host();
				host.setType(ScriptHostType.RESOURCE);
				capScript.getHost().add(host);
				// language is unknown here
			}
			if (supportedOperations.contains(ScriptOnConnectorApiOp.class)) {
				Host host = new Host();
				host.setType(ScriptHostType.CONNECTOR);
				capScript.getHost().add(host);
				// language is unknown here
			}
			capabilities.add(capabilityObjectFactory.createScript(capScript));
		}

	}

	@Override
	public Set<Object> getCapabilities(OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException {

		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".getCapabilities");
		result.addContext("connector", connectorType);

		if (capabilities == null) {
			// initialize the connector if it was not initialized yet
			try {
				initialize(result);
			} catch (CommunicationException ex) {
				result.recordFatalError(ex);
				throw ex;
			} catch (GenericFrameworkException ex) {
				result.recordFatalError(ex);
				throw ex;
			}
		}

		result.recordSuccess();

		return capabilities;
	}

	@Override
	public <T extends ResourceObjectShadowType> PrismObject<T> fetchObject(Class<T> type,
			ResourceAttributeContainerDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute> identifiers, boolean returnDefaultAttributes,
			Collection<? extends ResourceAttributeDefinition> attributesToReturn,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException {

		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".fetchObject");
		result.addParam("resourceObjectDefinition", objectClassDefinition);
		result.addParam("identifiers", identifiers);
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

		ObjectClass icfObjectClass = objectClassToIcf(objectClassDefinition);
		if (icfObjectClass == null) {
			result.recordFatalError("Unable to detemine object class from QName "
					+ objectClassDefinition.getTypeName()
					+ " while attempting to fetch object identified by " + identifiers + " from "
					+ ObjectTypeUtil.toShortString(connectorType));
			throw new IllegalArgumentException("Unable to detemine object class from QName "
					+ objectClassDefinition.getTypeName()
					+ " while attempting to fetch object identified by " + identifiers + " from "
					+ ObjectTypeUtil.toShortString(connectorType));
		}

		ConnectorObject co = null;
		try {

			// Invoke the ICF connector
			co = fetchConnectorObject(icfObjectClass, uid, returnDefaultAttributes, attributesToReturn,
					result);

		} catch (CommunicationException ex) {
			result.recordFatalError("ICF invocation failed due to communication problem");
			// This is fatal. No point in continuing. Just re-throw the
			// exception.
			throw ex;
		} catch (GenericFrameworkException ex) {
			result.recordFatalError("ICF invocation failed due to a generic ICF framework problem");
			// This is fatal. No point in continuing. Just re-throw the
			// exception.
			throw ex;
		}

		if (co == null) {
			result.recordFatalError("Object not found");
			throw new ObjectNotFoundException("Object identified by " + identifiers + " was not found by "
					+ ObjectTypeUtil.toShortString(connectorType));
		}

		PrismObjectDefinition<T> shadowDefinition = constructShadowDefinition(objectClassDefinition, type);
		PrismObject<T> shadow = convertToResourceObject(co, shadowDefinition, true);

		result.recordSuccess();
		return shadow;

	}

	/**
	 * Returns null if nothing is found.
	 */
	private ConnectorObject fetchConnectorObject(ObjectClass icfObjectClass, Uid uid,
			boolean returnDefaultAttributes,
			Collection<? extends ResourceAttributeDefinition> attributesToReturn,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException {

		// Connector operation cannot create result for itself, so we need to
		// create result for it
		OperationResult icfResult = parentResult.createSubresult(ConnectorFacade.class.getName()
				+ ".getObject");
		icfResult.addParam("objectClass", icfObjectClass.toString());
		icfResult.addParam("uid", uid.getUidValue());
		icfResult.addContext("connector", icfConnectorFacade.getClass());

		ConnectorObject co = null;
		try {
			OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();

			// Invoke the ICF connector
			co = icfConnectorFacade.getObject(icfObjectClass, uid, optionsBuilder.build());

			icfResult.recordSuccess();
			icfResult.setReturnValue(co);
		} catch (Exception ex) {
			// ICF interface does not specify exceptions or other error
			// conditions.
			// Therefore this kind of heavy artillery is necessary.
			// TODO maybe we can try to catch at least some specific exceptions
			icfResult.recordFatalError(ex);
			// This is fatal. No point in continuing.
			throw new GenericFrameworkException(ex);
		}

		return co;
	}

	@Override
	public Set<ResourceAttribute> addObject(PrismObject<ResourceObjectShadowType> object, Set<Operation> additionalOperations,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			SchemaException, ObjectAlreadyExistsException {

		ResourceAttributeContainer attributesContainer = 
			(ResourceAttributeContainer) object.asObjectable().getAttributes().asPrismContainer();
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".addObject");
		result.addParam("resourceObject", object);
		result.addParam("additionalOperations", additionalOperations);

		// getting icf object class from resource object class
		ObjectClass objectClass = objectClassToIcf(object);

		if (objectClass == null) {
			result.recordFatalError("Couldn't get icf object class from resource definition.");
			throw new IllegalArgumentException("Couldn't get icf object class from resource definition.");
		}

		// setting ifc attributes from resource object attributes
		Set<Attribute> attributes = null;
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("midPoint object before conversion:\n{}", attributesContainer.dump());
			}
			attributes = convertFromResourceObject(attributesContainer, result);
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

		// Look for a password change operation
		if (additionalOperations != null) {
			for (Operation op : additionalOperations) {
				if (op instanceof PasswordChangeOperation) {
					PasswordChangeOperation passwordChangeOperation = (PasswordChangeOperation) op;
					// Activation change means modification of attributes
					convertFromPassword(attributes, passwordChangeOperation);
				}

			}
		}

		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".create");
		icfResult.addParam("objectClass", objectClass);
		icfResult.addParam("attributes", attributes);
		icfResult.addParam("options", null);
		icfResult.addContext("connector", icfConnectorFacade);

		Uid uid = null;
		try {

			checkAndExecuteAdditionalOperation(additionalOperations, ScriptOrderType.BEFORE);

			// CALL THE ICF FRAMEWORK
			uid = icfConnectorFacade.create(objectClass, attributes, new OperationOptionsBuilder().build());

			checkAndExecuteAdditionalOperation(additionalOperations, ScriptOrderType.AFTER);

		} catch (Exception ex) {
			Exception midpointEx = processIcfException(ex, icfResult);
			result.computeStatus("Add object failed");

			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof ObjectAlreadyExistsException) {
				throw (ObjectAlreadyExistsException) midpointEx;
			} else if (midpointEx instanceof CommunicationException) {
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}

		if (uid == null || uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
			icfResult.recordFatalError("ICF did not returned UID after create");
			result.computeStatus("Add object failed");
			throw new GenericFrameworkException("ICF did not returned UID after create");
		}

		ResourceAttribute attribute = createUidAttribute(uid, getUidDefinition(attributesContainer.getIdentifiers()));
		attributesContainer.getValue().addReplaceExisting(attribute);
		icfResult.recordSuccess();

		result.recordSuccess();
		return attributesContainer.getAttributes();
	}

	@Override
	public Set<PropertyModificationOperation> modifyObject(ResourceAttributeContainerDefinition objectClass,
			Collection<? extends ResourceAttribute> identifiers, Set<Operation> changes,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".modifyObject");
		result.addParam("objectClass", objectClass);
		result.addParam("identifiers", identifiers);
		result.addParam("changes", changes);

		ObjectClass objClass = objectClassToIcf(objectClass);
		Uid uid = getUid(identifiers);
		String originalUid = uid.getUidValue();

		Set<ResourceAttribute> addValues = new HashSet<ResourceAttribute>();
		Set<ResourceAttribute> updateValues = new HashSet<ResourceAttribute>();
		Set<ResourceAttribute> valuesToRemove = new HashSet<ResourceAttribute>();

		Set<Operation> additionalOperations = new HashSet<Operation>();
		PasswordChangeOperation passwordChangeOperation = null;
		Collection<PropertyDelta> activationDeltas = new HashSet<PropertyDelta>();

		for (Operation operation : changes) {
			if (operation instanceof PropertyModificationOperation) {
				PropertyModificationOperation change = (PropertyModificationOperation) operation;
				PropertyDelta delta = change.getPropertyDelta();
				
				if (delta.getParentPath().equals(new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES))) {
					// Change in (ordinary) attributes. Transform to the ICF attributes.
					if (delta.isAdd()) {
						ResourceAttribute addAttribute = delta.instantiateEmptyProperty();
						addAttribute.addValues(delta.getValuesToAdd());
						addValues.add(addAttribute);
					}
					if (delta.isDelete()) {
						ResourceAttribute deleteAttribute = delta.instantiateEmptyProperty();
						deleteAttribute.addValues(delta.getValuesToDelete());
						valuesToRemove.add(deleteAttribute);
					}
					if (delta.isReplace()) {
						ResourceAttribute updateAttribute = delta.instantiateEmptyProperty();
						updateAttribute.addValues(delta.getValuesToReplace());
						updateValues.add(updateAttribute);
					}
				} else if (delta.getParentPath().equals(new PropertyPath(AccountShadowType.F_ACTIVATION))) {
					activationDeltas.add(delta);
				} else {
					throw new SchemaException("Change of unknown attribute " + delta.getName());
				}

			} else if (operation instanceof PasswordChangeOperation) {
				passwordChangeOperation = (PasswordChangeOperation) operation;
				// TODO: check for multiple occurrences and fail

			} else if (operation instanceof ExecuteScriptOperation) {
				ExecuteScriptOperation scriptOperation = (ExecuteScriptOperation) operation;
				additionalOperations.add(scriptOperation);

			} else {
				throw new IllegalArgumentException("Unknown operation type " + operation.getClass().getName()
						+ ": " + operation);
			}

		}

		// Needs three complete try-catch blocks because we need to create
		// icfResult for each operation
		// and handle the faults individually

		checkAndExecuteAdditionalOperation(additionalOperations, ScriptOrderType.BEFORE);

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
				icfResult.addParam("attributes", attributes);
				icfResult.addParam("options", options);
				icfResult.addContext("connector", icfConnectorFacade);

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
							"Invoking ICF addAttributeValues(), objectclass={}, uid={}, attributes=\n{}",
							new Object[] { objClass, uid, dumpAttributes(attributes) });
				}

				uid = icfConnectorFacade.addAttributeValues(objClass, uid, attributes, options);

				icfResult.recordSuccess();
			}
		} catch (Exception ex) {
			Exception midpointEx = processIcfException(ex, icfResult);
			result.computeStatus("Adding attribute values failed");
			// Do some kind of acrobatics to do proper throwing of checked
			// exception
			if (midpointEx instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException) midpointEx;
			} else if (midpointEx instanceof CommunicationException) {
				throw (CommunicationException) midpointEx;
			} else if (midpointEx instanceof GenericFrameworkException) {
				throw (GenericFrameworkException) midpointEx;
			} else if (midpointEx instanceof SchemaException) {
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}

		if (updateValues != null && !updateValues.isEmpty() || activationDeltas != null
				|| passwordChangeOperation != null) {

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

			if (passwordChangeOperation != null) {
				// Activation change means modification of attributes
				convertFromPassword(updateAttributes, passwordChangeOperation);
			}

			OperationOptions options = new OperationOptionsBuilder().build();
			icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".update");
			icfResult.addParam("objectClass", objectClass);
			icfResult.addParam("uid", uid.getUidValue());
			icfResult.addParam("attributes", updateAttributes);
			icfResult.addParam("options", options);
			icfResult.addContext("connector", icfConnectorFacade);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Invoking ICF update(), objectclass={}, uid={}, attributes=\n{}", new Object[] {
						objClass, uid, dumpAttributes(updateAttributes) });
			}

			try {
				// Call ICF
				uid = icfConnectorFacade.update(objClass, uid, updateAttributes, options);

				icfResult.recordSuccess();
			} catch (Exception ex) {
				Exception midpointEx = processIcfException(ex, icfResult);
				result.computeStatus("Update failed");
				// Do some kind of acrobatics to do proper throwing of checked
				// exception
				if (midpointEx instanceof ObjectNotFoundException) {
					throw (ObjectNotFoundException) midpointEx;
				} else if (midpointEx instanceof CommunicationException) {
					throw (CommunicationException) midpointEx;
				} else if (midpointEx instanceof GenericFrameworkException) {
					throw (GenericFrameworkException) midpointEx;
				} else if (midpointEx instanceof SchemaException) {
					throw (SchemaException) midpointEx;
				} else if (midpointEx instanceof RuntimeException) {
					throw (RuntimeException) midpointEx;
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
				icfResult.addParam("attributes", attributes);
				icfResult.addParam("options", options);
				icfResult.addContext("connector", icfConnectorFacade);

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
							"Invoking ICF removeAttributeValues(), objectclass={}, uid={}, attributes=\n{}",
							new Object[] { objClass, uid, dumpAttributes(attributes) });
				}

				uid = icfConnectorFacade.removeAttributeValues(objClass, uid, attributes, options);
				icfResult.recordSuccess();
			}
		} catch (Exception ex) {
			Exception midpointEx = processIcfException(ex, icfResult);
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
				throw (SchemaException) midpointEx;
			} else if (midpointEx instanceof RuntimeException) {
				throw (RuntimeException) midpointEx;
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}
		checkAndExecuteAdditionalOperation(additionalOperations, ScriptOrderType.AFTER);
		result.recordSuccess();

		Set<PropertyModificationOperation> sideEffectChanges = new HashSet<PropertyModificationOperation>();
		if (!originalUid.equals(uid.getUidValue())) {
			// UID was changed during the operation, this is most likely a
			// rename
			PropertyDelta uidDelta = createUidDelta(uid, getUidDefinition(identifiers));
			PropertyModificationOperation uidMod = new PropertyModificationOperation(uidDelta);
			sideEffectChanges.add(uidMod);
		}
		return sideEffectChanges;
	}

	private PropertyDelta createUidDelta(Uid uid, ResourceAttributeDefinition uidDefinition) {
		PropertyDelta uidDelta = new PropertyDelta(new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES), uidDefinition);
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
	public void deleteObject(ResourceAttributeContainerDefinition objectClass, Set<Operation> additionalOperations,
			Collection<? extends ResourceAttribute> identifiers, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".deleteObject");
		result.addParam("identifiers", identifiers);

		ObjectClass objClass = objectClassToIcf(objectClass);
		Uid uid = getUid(identifiers);

		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".delete");
		icfResult.addParam("uid", uid);
		icfResult.addParam("objectClass", objClass);
		icfResult.addContext("connector", icfConnectorFacade);

		try {

			checkAndExecuteAdditionalOperation(additionalOperations, ScriptOrderType.BEFORE);

			icfConnectorFacade.delete(objClass, uid, new OperationOptionsBuilder().build());

			checkAndExecuteAdditionalOperation(additionalOperations, ScriptOrderType.AFTER);
			icfResult.recordSuccess();

		} catch (Exception ex) {
			Exception midpointEx = processIcfException(ex, icfResult);
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
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}

		result.recordSuccess();
	}

	@Override
	public PrismProperty deserializeToken(Object serializedToken) {
		return createTokenProperty(serializedToken);
	}

	@Override
	public PrismProperty fetchCurrentToken(ResourceAttributeContainerDefinition objectClass, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".deleteObject");
		result.addParam("objectClass", objectClass);

		ObjectClass objClass = objectClassToIcf(objectClass);

		SyncToken syncToken = icfConnectorFacade.getLatestSyncToken(objClass);

		if (syncToken == null) {
			result.recordFatalError("No token found");
			throw new IllegalArgumentException("No token found.");
		}

		PrismProperty property = getToken(syncToken);
		result.recordSuccess();
		return property;
	}

	@Override
	public List<Change> fetchChanges(ResourceAttributeContainerDefinition objectClass, PrismProperty lastToken,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			SchemaException {

		OperationResult subresult = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".fetchChanges");
		subresult.addContext("objectClass", objectClass);

		// create sync token from the property last token
		SyncToken syncToken = null;
		try {
			syncToken = getSyncToken(lastToken);
			LOGGER.trace("Sync token created from the property last token: {}", syncToken.getValue());
		} catch (SchemaException ex) {
			subresult.recordFatalError(ex.getMessage(), ex);
			throw new SchemaException(ex.getMessage(), ex);
		}

		final Set<SyncDelta> result = new HashSet<SyncDelta>();
		// get icf object class
		ObjectClass icfObjectClass = objectClassToIcf(objectClass);

		SyncResultsHandler syncHandler = new SyncResultsHandler() {

			@Override
			public boolean handle(SyncDelta delta) {
				LOGGER.trace("Detected sync delta: {}", delta);
				return result.add(delta);

			}
		};

		OperationResult icfResult = subresult.createSubresult(ConnectorFacade.class.getName() + ".sync");
		icfResult.addContext("connector", icfConnectorFacade);
		icfResult.addParam("icfObjectClass", icfObjectClass);
		icfResult.addParam("syncToken", syncToken);
		icfResult.addParam("syncHandler", syncHandler);

		try {
			icfConnectorFacade.sync(icfObjectClass, syncToken, syncHandler,
					new OperationOptionsBuilder().build());
			icfResult.recordSuccess();
		} catch (Exception ex) {
			Exception midpointEx = processIcfException(ex, icfResult);
			subresult.computeStatus();
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
			} else {
				throw new SystemException("Got unexpected exception: " + ex.getClass().getName(), ex);
			}
		}
		// convert changes from icf to midpoint Change
		List<Change> changeList = null;
		try {
			PrismSchema schema = getResourceSchema(subresult);
			changeList = getChangesFromSyncDelta(result, schema, subresult);
		} catch (SchemaException ex) {
			subresult.recordFatalError(ex.getMessage(), ex);
			throw new SchemaException(ex.getMessage(), ex);
		}

		subresult.recordSuccess();
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
		} catch (Exception ex) {
			processIcfException(ex, connectionResult);
		}
	}

	@Override
	public <T extends ResourceObjectShadowType> void search(Class<T> type, ResourceAttributeContainerDefinition objectClassDefinition,
			final ResultHandler<T> handler, OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			SchemaException {

		// Result type for this operation
		final OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".search");
		result.addParam("objectClass", objectClassDefinition);
		result.addContext("connector", connectorType);

		if (objectClassDefinition == null) {
			result.recordFatalError("Object class not defined");
			throw new IllegalArgumentException("objectClass not defined");
		}

		ObjectClass icfObjectClass = objectClassToIcf(objectClassDefinition);
		if (icfObjectClass == null) {
			IllegalArgumentException ex = new IllegalArgumentException(
					"Unable to detemine object class from QName " + objectClassDefinition
							+ " while attempting to search objects by "
							+ ObjectTypeUtil.toShortString(connectorType));
			result.recordFatalError("Unable to detemine object class", ex);
			throw ex;
		}
		final PrismObjectDefinition<T> objectDefinition = 
			constructShadowDefinition(objectClassDefinition, type);

		ResultsHandler icfHandler = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				// Convert ICF-specific connector object to a generic
				// ResourceObject
				PrismObject<T> resourceObject;
				try {
					resourceObject = convertToResourceObject(connectorObject, objectDefinition, true);
				} catch (SchemaException e) {
					throw new IntermediateException(e);
				}

				// .. and pass it to the handler
				boolean cont = handler.handle(resourceObject);
				if (!cont) {
					result.recordPartialError("Stopped on request from the handler");
				}
				return cont;
			}
		};

		// Connector operation cannot create result for itself, so we need to
		// create result for it
		OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".search");
		icfResult.addParam("objectClass", icfObjectClass);
		icfResult.addContext("connector", icfConnectorFacade.getClass());

		try {

			icfConnectorFacade.search(icfObjectClass, null, icfHandler, null);

			icfResult.recordSuccess();
		} catch (IntermediateException inex) {
			SchemaException ex = (SchemaException) inex.getCause();
			throw ex;
		} catch (Exception ex) {
			// ICF interface does not specify exceptions or other error
			// conditions.
			// Therefore this kind of heavy artillery is necessary.
			// TODO maybe we can try to catch at least some specific exceptions
			icfResult.recordFatalError(ex);
			result.recordFatalError("ICF invocation failed");
			// This is fatal. No point in continuing.
			throw new GenericFrameworkException(ex);
		}

		if (result.isUnknown()) {
			result.recordSuccess();
		}
	}

	// UTILITY METHODS

	private QName convertAttributeNameToQName(String icfAttrName) {
		QName attrXsdName = new QName(getSchemaNamespace(), icfAttrName,
				ConnectorFactoryIcfImpl.NS_ICF_RESOURCE_INSTANCE_PREFIX);
		// Handle special cases
		if (Name.NAME.equals(icfAttrName)) {
			// this is ICF __NAME__ attribute. It will look ugly in XML and may
			// even cause problems.
			// so convert to something more friendly such as icfs:name
			attrXsdName = ConnectorFactoryIcfImpl.ICFS_NAME;
		}
		return attrXsdName;
	}

	private String convertAttributeNameToIcf(QName attrQName, OperationResult parentResult)
			throws SchemaException {
		// Attribute QNames in the resource instance namespace are converted
		// "as is"
		if (attrQName.getNamespaceURI().equals(getSchemaNamespace())) {
			return attrQName.getLocalPart();
		}

		// Other namespace are special cases

		if (ConnectorFactoryIcfImpl.ICFS_NAME.equals(attrQName)) {
			return Name.NAME;
		}

		if (ConnectorFactoryIcfImpl.ICFS_UID.equals(attrQName)) {
			// UID is strictly speaking not an attribute. But it acts as an
			// attribute e.g. in create operation. Therefore we need to map it.
			return Uid.NAME;
		}

		// No mapping available

		throw new SchemaException("No mapping from QName " + attrQName + " to an ICF attribute name");
	}

	/**
	 * Maps ICF native objectclass name to a midPoint QName objctclass name.
	 * <p/>
	 * The mapping is "stateless" - it does not keep any mapping database or any
	 * other state. There is a bi-directional mapping algorithm.
	 * <p/>
	 * TODO: mind the special characters in the ICF objectclass names.
	 */
	private QName objectClassToQname(String icfObjectClassString) {
		if (ObjectClass.ACCOUNT_NAME.equals(icfObjectClassString)) {
			return new QName(getSchemaNamespace(), ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME,
					ConnectorFactoryIcfImpl.NS_ICF_SCHEMA_PREFIX);
		} else if (ObjectClass.GROUP_NAME.equals(icfObjectClassString)) {
			return new QName(getSchemaNamespace(), ConnectorFactoryIcfImpl.GROUP_OBJECT_CLASS_LOCAL_NAME,
					ConnectorFactoryIcfImpl.NS_ICF_SCHEMA_PREFIX);
		} else {
			return new QName(getSchemaNamespace(), CUSTOM_OBJECTCLASS_PREFIX + icfObjectClassString
					+ CUSTOM_OBJECTCLASS_SUFFIX, ConnectorFactoryIcfImpl.NS_ICF_RESOURCE_INSTANCE_PREFIX);
		}
	}

	private ObjectClass objectClassToIcf(PrismObject<ResourceObjectShadowType> object) {
		
		ResourceObjectShadowType shadowType = object.asObjectable();
		QName qnameObjectClass = shadowType.getObjectClass();
		if (qnameObjectClass == null) {
			ResourceAttributeContainerDefinition objectClassDefinition = 
				(ResourceAttributeContainerDefinition) shadowType.getAttributes().asPrismContainer().getDefinition();
			qnameObjectClass = objectClassDefinition.getTypeName();
		} 
		
		return objectClassToIcf(qnameObjectClass);
	}
	
	/**
	 * Maps a midPoint QName objctclass to the ICF native objectclass name.
	 * <p/>
	 * The mapping is "stateless" - it does not keep any mapping database or any
	 * other state. There is a bi-directional mapping algorithm.
	 * <p/>
	 * TODO: mind the special characters in the ICF objectclass names.
	 */
	private ObjectClass objectClassToIcf(ResourceAttributeContainerDefinition objectClassDefinition) {
		QName qnameObjectClass = objectClassDefinition.getTypeName();
		return objectClassToIcf(qnameObjectClass);
	}
	
	private ObjectClass objectClassToIcf(QName qnameObjectClass) {
		if (!getSchemaNamespace().equals(qnameObjectClass.getNamespaceURI())) {
			throw new IllegalArgumentException("ObjectClass QName " + qnameObjectClass
					+ " is not in the appropriate namespace for "
					+ ObjectTypeUtil.toShortString(connectorType) + ", expected: " + getSchemaNamespace());
		}
		String lname = qnameObjectClass.getLocalPart();
		if (ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME.equals(lname)) {
			return ObjectClass.ACCOUNT;
		} else if (ConnectorFactoryIcfImpl.GROUP_OBJECT_CLASS_LOCAL_NAME.equals(lname)) {
			return ObjectClass.GROUP;
		} else if (lname.startsWith(CUSTOM_OBJECTCLASS_PREFIX) && lname.endsWith(CUSTOM_OBJECTCLASS_SUFFIX)) {
			String icfObjectClassName = lname.substring(CUSTOM_OBJECTCLASS_PREFIX.length(), lname.length()
					- CUSTOM_OBJECTCLASS_SUFFIX.length());
			return new ObjectClass(icfObjectClassName);
		} else {
			throw new IllegalArgumentException("Cannot recognize objectclass QName " + qnameObjectClass
					+ " for " + ObjectTypeUtil.toShortString(connectorType) + ", expected: "
					+ getSchemaNamespace());
		}
	}

	/**
	 * Looks up ICF Uid identifier in a (potentially multi-valued) set of
	 * identifiers. Handy method to convert midPoint identifier style to an ICF
	 * identifier style.
	 * 
	 * @param identifiers
	 *            midPoint resource object identifiers
	 * @return ICF UID or null
	 */
	private Uid getUid(Collection<? extends ResourceAttribute> identifiers) {
		for (ResourceAttribute attr : identifiers) {
			if (attr.getName().equals(ConnectorFactoryIcfImpl.ICFS_UID)) {
				return new Uid(attr.getValue(String.class).getValue());
			}
		}
		return null;
	}
	
	private ResourceAttributeDefinition getUidDefinition(ResourceAttributeContainerDefinition def) {
		return def.findAttributeDefinition(new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES, ConnectorFactoryIcfImpl.ICFS_UID));
	}

	private ResourceAttributeDefinition getUidDefinition(
			Collection<? extends ResourceAttribute> identifiers) {
		for (ResourceAttribute attr : identifiers) {
			if (attr.getName().equals(ConnectorFactoryIcfImpl.ICFS_UID)) {
				return attr.getDefinition();
			}
		}
		return null;
	}
	

	private ResourceAttribute createUidAttribute(Uid uid, ResourceAttributeDefinition uidDefinition) {
		ResourceAttribute uidRoa = uidDefinition.instantiate();
		uidRoa.setValue(new PrismPropertyValue<String>(uid.getUidValue()));
		return uidRoa;
	}
	
	private <T extends ResourceObjectShadowType> PrismObjectDefinition<T> constructShadowDefinition(
			ResourceAttributeContainerDefinition objectClassDefinition, Class<T> type) {
		PrismObjectDefinition<T> originalObjectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		if (originalObjectDefinition == null) {
			throw new SystemException("No object definition for "+type);
		}
		PrismObjectDefinition<T> shadowDefinition = 
			originalObjectDefinition.cloneWithReplacedDefinition(ResourceObjectShadowType.F_ATTRIBUTES, objectClassDefinition);
		return shadowDefinition;
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
	 *            the returned resource object will contain olny attributed with
	 *            the non-null values.
	 * @return new mapped ResourceObject instance.
	 * @throws SchemaException
	 */
	private <T extends ResourceObjectShadowType> PrismObject<T> convertToResourceObject(ConnectorObject co,
			PrismObjectDefinition<T> objectDefinition, boolean full) throws SchemaException {

		PrismObject<T> shadowPrism = null;
		if (objectDefinition != null) {
			shadowPrism = objectDefinition.instantiate();
		} else {
			throw new SchemaException("No definition");
		}
		
		T shadow = shadowPrism.asObjectable();
		ResourceAttributeContainer attributesContainer = 
			(ResourceAttributeContainer) shadowPrism.findOrCreateContainer(ResourceObjectShadowType.F_ATTRIBUTES);
		ResourceAttributeContainerDefinition attributesDefinition = attributesContainer.getDefinition();

		// Uid is always there
		Uid uid = co.getUid();
		// PropertyDefinition propDef = new
		// PropertyDefinition(SchemaConstants.ICFS_NAME,
		// SchemaConstants.XSD_STRING);
		// Property p = propDef.instantiate();
		ResourceAttribute uidRoa = createUidAttribute(uid,getUidDefinition(attributesDefinition));
		// p = setUidAttribute(uid);
		attributesContainer.getValue().add(uidRoa);
		// ro.getProperties().add(p);

		for (Attribute icfAttr : co.getAttributes()) {
			if (icfAttr.getName().equals(Uid.NAME)) {
				// UID is handled specially (see above)
				continue;
			}
			if (icfAttr.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
				// password has to go to the credentials section
				if (shadow instanceof AccountShadowType) {
					AccountShadowType accountShadowType = (AccountShadowType)shadow;
					ProtectedStringType password = getSingleValue(icfAttr, ProtectedStringType.class);
					accountShadowType.getCredentials().getPassword().setProtectedString(password);
				} else {
					throw new SchemaException("Attempt to set password for non-account object type "+objectDefinition);
				}
				continue;
			}
			if (icfAttr.getName().equals(OperationalAttributes.ENABLE_NAME)) {
				if (shadow instanceof AccountShadowType) {
					AccountShadowType accountShadowType = (AccountShadowType)shadow;
					Boolean enabled = getSingleValue(icfAttr, Boolean.class);
					accountShadowType.getActivation().setEnabled(enabled);
				} else {
					throw new SchemaException("Attempt to set password for non-account object type "+objectDefinition);
				}
				continue;
			}
			
			QName qname = convertAttributeNameToQName(icfAttr.getName());
			ResourceAttributeDefinition attributeDefinition = attributesDefinition.findAttributeDefinition(qname);

			ResourceAttribute roa = attributeDefinition.instantiate(qname);

			// if true, we need to convert whole connector object to the
			// resource object also with the null-values attributes
			if (full) {
				if (icfAttr.getValue() != null) {
					// Convert the values. While most values do not need
					// conversions, some
					// of them may need it (e.g. GuardedString)
					for (Object icfValue : icfAttr.getValue()) {
						Object value = convertValueFromIcf(icfValue, qname);
						roa.getValues().add(new PrismPropertyValue<Object>(value));
					}
				}

				attributesContainer.getValue().add(roa);

				// in this case when false, we need only the attributes with the
				// non-null values.
			} else {
				if (icfAttr.getValue() != null && !icfAttr.getValue().isEmpty()) {
					// Convert the values. While most values do not need
					// conversions, some
					// of them may need it (e.g. GuardedString)
					for (Object icfValue : icfAttr.getValue()) {
						Object value = convertValueFromIcf(icfValue, qname);
						roa.getValues().add(new PrismPropertyValue<Object>(value));

					}

					attributesContainer.getValue().add(roa);

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
		Collection<ResourceAttribute> resourceAttributes = attributesPrism.getAttributes();
		return convertFromResourceObject(resourceAttributes, parentResult);
	}
	
	private Set<Attribute> convertFromResourceObject(Collection<ResourceAttribute> resourceAttributes,
			OperationResult parentResult) throws SchemaException {
		
		Set<Attribute> attributes = new HashSet<Attribute>();
		if (resourceAttributes == null) {
			// returning empty set
			return attributes;
		}

		for (ResourceAttribute attribute : resourceAttributes) {

			String attrName = convertAttributeNameToIcf(attribute.getName(), parentResult);

			Set<Object> convertedAttributeValues = new HashSet<Object>();
			for (PrismPropertyValue<Object> value : attribute.getValues()) {
				convertedAttributeValues.add(convertValueToIcf(value, attribute.getName()));
			}

			Attribute connectorAttribute = AttributeBuilder.build(attrName, convertedAttributeValues);

			attributes.add(connectorAttribute);
		}
		return attributes;
	}

	private Object convertValueToIcf(Object value, QName propName) throws SchemaException {
		if (value == null) {
			return null;
		}

		if (value instanceof PrismPropertyValue) {
			return convertValueToIcf(((PrismPropertyValue) value).getValue(), propName);
		}

		if (value instanceof ProtectedStringType) {
			ProtectedStringType ps = (ProtectedStringType) value;
			return toGuardedString(ps, propName.toString());
		}
		return value;
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
			Collection<PropertyDelta> activationDeltas) throws SchemaException {

		for (PropertyDelta propDelta: activationDeltas) {
			if (propDelta.getName().equals(ActivationType.F_ENABLED)) {
				// Not entirely correct, TODO: refactor later
				updateAttributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,
						propDelta.getPropertyNew().getValue(Boolean.class)));
			} else {
				throw new SchemaException("Got unknown activation attribute delta "+propDelta.getName());
			}
		}

	}

	private void convertFromPassword(Set<Attribute> attributes,
			PasswordChangeOperation passwordChangeOperation) {
		if (passwordChangeOperation == null || passwordChangeOperation.getNewPassword() == null) {
			throw new IllegalArgumentException("No password was provided");
		}

		GuardedString guardedPassword = toGuardedString(passwordChangeOperation.getNewPassword(),
				"new password");
		attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, guardedPassword));

	}

	private List<Change> getChangesFromSyncDelta(Set<SyncDelta> result, PrismSchema schema,
			OperationResult parentResult) throws SchemaException, GenericFrameworkException {
		List<Change> changeList = new ArrayList<Change>();
		
		for (SyncDelta delta : result) {
			
			ObjectClass objClass = delta.getObject().getObjectClass();
			QName objectClass = objectClassToQname(objClass.getObjectClassValue());
			ResourceAttributeContainerDefinition objectClassDefinition = (ResourceAttributeContainerDefinition) schema
					.findContainerDefinitionByType(objectClass);
			// FIXME: we are hadcoding Account here, but we should not
			PrismObjectDefinition<AccountShadowType> objectDefinition = constructShadowDefinition(objectClassDefinition, AccountShadowType.class);

			if (SyncDeltaType.DELETE.equals(delta.getDeltaType())) {
				ObjectDelta<ResourceObjectShadowType> objectDelta = new ObjectDelta<ResourceObjectShadowType>(
						ResourceObjectShadowType.class, ChangeType.DELETE);
				ResourceAttribute uidAttribute = createUidAttribute(delta.getUid(), getUidDefinition(objectClassDefinition));
				Set<ResourceAttribute> identifiers = new HashSet<ResourceAttribute>();
				identifiers.add(uidAttribute);
				Change change = new Change(identifiers, objectDelta, getToken(delta.getToken()));
				changeList.add(change);

			} else if (SyncDeltaType.CREATE_OR_UPDATE.equals(delta.getDeltaType())) {

				PrismObject<AccountShadowType> currentShadow = convertToResourceObject(
						delta.getObject(), objectDefinition, false);

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Got current shadow: {}", currentShadow.dump());
				}
				
				Set<ResourceAttribute> identifiers = ResourceObjectShadowUtil.getIdentifiers(currentShadow);

				Change change = new Change(identifiers, currentShadow, getToken(delta.getToken()));
				changeList.add(change);
				
			} else {
				throw new GenericFrameworkException("Unexpected sync delta type "+delta.getDeltaType());
			}

		}
		return changeList;
	}

	private Element getModificationPath(Document doc) {
		List<XPathSegment> segments = new ArrayList<XPathSegment>();
		XPathSegment attrSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
		segments.add(attrSegment);
		XPathHolder t = new XPathHolder(segments);
		Element xpathElement = t.toElement(SchemaConstants.I_PROPERTY_CONTAINER_REFERENCE_PATH, doc);
		return xpathElement;
	}

	private SyncToken getSyncToken(PrismProperty tokenProperty) throws SchemaException {
		if (tokenProperty.getValue() == null) {
			throw new IllegalArgumentException("Attempt to get token from a null property");
		}
		Object tokenValue = tokenProperty.getValue().getValue();
		if (tokenValue == null) {
			throw new IllegalArgumentException("Attempt to get token from a null-valued property");
		}
//		Document doc = DOMUtil.getDocument();
//		List<Object> elements = null;
//		try {
//			elements = lastToken.serializeToJaxb(doc);
//		} catch (SchemaException ex) {
//			throw new SchemaException("Failed to serialize last token property to dom.", ex);
//		}
//		for (Object e : elements) {
//			tokenValue = XsdTypeConverter.toJavaValue(e, lastToken.getDefinition().getTypeName());
//		}
		SyncToken syncToken = new SyncToken(tokenValue);
		return syncToken;
	}

	private PrismProperty getToken(SyncToken syncToken) {
		Object object = syncToken.getValue();
		return createTokenProperty(object);
	}

	private PrismProperty createTokenProperty(Object object) {
		QName type = XsdTypeMapper.toXsdType(object.getClass());

		Set<PrismPropertyValue<Object>> syncTokenValues = new HashSet<PrismPropertyValue<Object>>();
		syncTokenValues.add(new PrismPropertyValue<Object>(object));
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(SchemaConstants.SYNC_TOKEN, SchemaConstants.SYNC_TOKEN, type, prismContext);
		PrismProperty property = propDef.instantiate();
		property.addValues(syncTokenValues);
		return property;
	}

	/**
	 * check additional operation order, according to the order are scrip
	 * executed before or after operation..
	 * 
	 * @param additionalOperations
	 * @param order
	 */
	private void checkAndExecuteAdditionalOperation(Set<Operation> additionalOperations, ScriptOrderType order) {

		if (additionalOperations == null) {
			// TODO: add warning to the result
			return;
		}

		for (Operation op : additionalOperations) {
			if (op instanceof ExecuteScriptOperation) {

				ExecuteScriptOperation executeOp = (ExecuteScriptOperation) op;
				LOGGER.trace("Find execute script operation: {}", SchemaDebugUtil.prettyPrint(executeOp));
				// execute operation in the right order..
				if (order.equals(executeOp.getScriptOrder())) {
					executeScript(executeOp);
				}
			}
		}

	}

	private void executeScript(ExecuteScriptOperation executeOp) {

		// convert execute script operation to the script context required from
		// the connector
		ScriptContext scriptContext = convertToScriptContext(executeOp);
		// check if the script should be executed on the connector or the
		// resoruce...
		if (executeOp.isConnectorHost()) {
			LOGGER.debug("Start running script on connector.");
			icfConnectorFacade.runScriptOnConnector(scriptContext, new OperationOptionsBuilder().build());
			LOGGER.debug("Finish running script on connector.");
		}
		if (executeOp.isResourceHost()) {
			LOGGER.debug("Start running script on resource.");
			icfConnectorFacade.runScriptOnResource(scriptContext, new OperationOptionsBuilder().build());
			LOGGER.debug("Finish running script on resource.");
		}

	}

	private ScriptContext convertToScriptContext(ExecuteScriptOperation executeOp) {
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
	 * @param resource
	 *            midPoint XML configuration
	 * @throws SchemaException
	 */
	private void transformConnectorConfiguration(APIConfiguration apiConfig,
			PrismContainer configuration) throws SchemaException {

		ConfigurationProperties configProps = apiConfig.getConfigurationProperties();

		// The namespace of all the configuration properties specific to the
		// connector instance will have a connector instance namespace. This
		// namespace can be found in the resource definition.
		String connectorConfNs = connectorType.getNamespace();

		PrismContainer configurationPropertiesContainer = configuration.findContainer(
				new QName(connectorConfNs, ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME));
		int numConfingProperties = transformConnectorConfiguration(configProps, configurationPropertiesContainer, connectorConfNs);
		
		PrismContainer connectorPoolContainer = configuration.findContainer(
				new QName(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION, ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME));
		ObjectPoolConfiguration connectorPoolConfiguration = apiConfig.getConnectorPoolConfiguration();
		transformConnectorPoolConfiguration(connectorPoolConfiguration, connectorPoolContainer);

		PrismProperty producerBufferSizeProperty = 
			configuration.findProperty(new QName(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION,
					ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_XML_ELEMENT_NAME));
		if (producerBufferSizeProperty != null) {
			apiConfig.setProducerBufferSize(parseInt(producerBufferSizeProperty));
		}
		
		PrismContainer connectorTimeoutsContainer = configuration.findContainer(
				new QName(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION, ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME));
		transformConnectorTimeoutsConfiguration(apiConfig, connectorTimeoutsContainer);

		if (numConfingProperties == 0) {
			throw new SchemaException("No configuration properties found. Wrong namespace? (expected: "
					+ connectorConfNs + ")");
		}

	}
		
	private int transformConnectorConfiguration(ConfigurationProperties configProps,
			PrismContainer configurationPropertiesContainer, String connectorConfNs) {

		int numConfingProperties = 0;
		
		for (PrismProperty prismProperty: configurationPropertiesContainer.getValue().getProperties()) {
			QName propertyQName = prismProperty.getName();

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
	
				// Check (java) type of ICF configuration property,
				// behave accordingly
				Class<?> type = property.getType();
				if (type.isArray()) {
					property.setValue(prismProperty.getRealValuesArray(type.getComponentType()));
				} else {
					// Single-valued property are easy to convert
					property.setValue(prismProperty.getRealValue(type));
				}
			}	
		}	
		return numConfingProperties;
	}
	
	private void transformConnectorPoolConfiguration(ObjectPoolConfiguration connectorPoolConfiguration,
			PrismContainer connectorPoolContainer) throws SchemaException {

		for (PrismProperty prismProperty: connectorPoolContainer.getValue().getProperties()) {
			QName propertyQName = prismProperty.getName();
			if (propertyQName.getNamespaceURI().equals(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION)) {
				String subelementName = propertyQName.getLocalPart();
				if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_EVICTABLE_IDLE_TIME_MILLIS
						.equals(subelementName)) {
					connectorPoolConfiguration
							.setMinEvictableIdleTimeMillis(parseLong(prismProperty));
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
					throw new SchemaException("Unexpected element " + propertyQName + " in "
									+ ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME);
				}
			} else {
				throw new SchemaException(
						"Unexpected element " + propertyQName + " in "
								+ ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME);
			}
		}
	}
	
	private void transformConnectorTimeoutsConfiguration(APIConfiguration apiConfig,
			PrismContainer connectorTimeoutsContainer) throws SchemaException {

		for (PrismProperty prismProperty: connectorTimeoutsContainer.getValue().getProperties()) {
			QName propertQName = prismProperty.getName();

			if (ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION.equals(propertQName.getNamespaceURI())) {
				String opName = propertQName.getLocalPart();
				Class<? extends APIOperation> apiOpClass = ConnectorFactoryIcfImpl
						.resolveApiOpClass(opName);
				if (apiOpClass != null) {
					apiConfig.setTimeout(apiOpClass, parseInt(prismProperty));
				} else {
					throw new SchemaException("Unknown operation name " + opName + " in "
							+ ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME);
				}
			}
		}
	}

	private int parseInt(PrismProperty prop) {
		return  prop.getRealValue(Integer.class);
	}

	private long parseLong(PrismProperty prop) {
		return  prop.getRealValue(Long.class);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object convertToJava(Element configElement, Class type) throws SchemaException {
		Object value = null;
		Class midPointClass = type;
		if (type.equals(GuardedString.class)) {
			// Guarded string is a special ICF beast
			midPointClass = ProtectedStringType.class;
		} else if (type.equals(GuardedByteArray.class)) {
			// Guarded byte array is a special ICF beast
			// TODO
		}
		value = XmlTypeConverter.toJavaValue(configElement, midPointClass);
		if (type.equals(GuardedString.class)) {
			// Guarded string is a special ICF beast
			// The value must be ProtectedStringType
			ProtectedStringType ps = (ProtectedStringType) value;
			return toGuardedString(ps, DOMUtil.getQName(configElement).toString());
		} else if (type.equals(GuardedByteArray.class)) {
			// Guarded string is a special ICF beast
			// TODO
			return new GuardedByteArray(Base64.decodeBase64(configElement.getTextContent()));
		}
		return value;
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
		return "ConnectorInstanceIcfImpl(" + ObjectTypeUtil.toShortString(connectorType) + ")";
	}

}
