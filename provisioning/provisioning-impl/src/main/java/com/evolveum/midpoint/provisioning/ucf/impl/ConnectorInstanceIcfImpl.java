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
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType.Attributes;
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

	private Schema resourceSchema = null;
	Set<Object> capabilities = null;

	public ConnectorInstanceIcfImpl(ConnectorInfo connectorInfo, ConnectorType connectorType,
			String schemaNamespace, Protector protector) {
		this.cinfo = connectorInfo;
		this.connectorType = connectorType;
		this.schemaNamespace = schemaNamespace;
		this.protector = protector;
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
	public void configure(ResourceConfigurationType configuration, OperationResult parentResult)
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
	public Schema generateConnectorSchema() {

		LOGGER.trace("Generating configuration schema for {}", this);
		APIConfiguration defaultAPIConfiguration = cinfo.createDefaultAPIConfiguration();
		ConfigurationProperties icfConfigurationProperties = defaultAPIConfiguration
				.getConfigurationProperties();

		if (icfConfigurationProperties == null || icfConfigurationProperties.getPropertyNames() == null
				|| icfConfigurationProperties.getPropertyNames().isEmpty()) {
			LOGGER.debug("No configuration schema for {}", this);
			return null;
		}

		Schema mpSchema = new Schema(connectorType.getNamespace());

		// Create configuration type - the type used by the "configuration"
		// element
		PropertyContainerDefinition configurationContainerDef = mpSchema
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
		PropertyContainerDefinition configDef = mpSchema
				.createPropertyContainerDefinition(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME);
		for (String icfPropertyName : icfConfigurationProperties.getPropertyNames()) {
			ConfigurationProperty icfProperty = icfConfigurationProperties.getProperty(icfPropertyName);

			QName propXsdName = new QName(connectorType.getNamespace(), icfPropertyName);
			QName propXsdType = icfTypeToXsdType(icfProperty.getType());
			LOGGER.trace("{}: Mapping ICF config schema property {} from {} to {}", new Object[] { this,
					icfPropertyName, icfProperty.getType(), propXsdType });
			PropertyDefinition propertyDefinifion = configDef.createPropertyDefinition(propXsdName,
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
			propXsdType = XsdTypeConverter.toXsdType(type);
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
	public Schema getResourceSchema(OperationResult parentResult) throws CommunicationException,
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
		resourceSchema = new Schema(getSchemaNamespace());

		// Let's convert every objectclass in the ICF schema ...
		Set<ObjectClassInfo> objectClassInfoSet = icfSchema.getObjectClassInfo();
		for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {

			// "Flat" ICF object class names needs to be mapped to QNames
			QName objectClassXsdName = objectClassToQname(objectClassInfo.getType());

			// ResourceObjectDefinition is a midPpoint way how to represent an
			// object class.
			// The important thing here is the last "type" parameter
			// (objectClassXsdName). The rest is more-or-less cosmetics.
			ResourceObjectDefinition roDefinition = resourceSchema
					.createResourceObjectDefinition(objectClassXsdName);

			// The __ACCOUNT__ objectclass in ICF is a default account
			// objectclass. So mark it appropriately.
			if (ObjectClass.ACCOUNT_NAME.equals(objectClassInfo.getType())) {
				roDefinition.setAccountType(true);
				roDefinition.setDefaultAccountType(true);
			}

			// Every object has UID in ICF, therefore add it right now
			ResourceObjectAttributeDefinition uidDefinition = roDefinition.createAttributeDefinition(
					ConnectorFactoryIcfImpl.ICFS_UID, DOMUtil.XSD_STRING);
			// Make it mandatory
			uidDefinition.setMinOccurs(1);
			uidDefinition.setMaxOccurs(1);
			// Make it read-only
			uidDefinition.setReadOnly();
			// Set a default display name
			uidDefinition.setAttributeDisplayName("ICF UID");
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
				ResourceObjectAttributeDefinition roaDefinition = roDefinition.createAttributeDefinition(
						attrXsdName, attrXsdType);

				// Set a better display name for __NAME__. The "name" is s very
				// overloaded term, so let's try to make things
				// a bit clearer
				if (attrXsdName.equals(ConnectorFactoryIcfImpl.ICFS_NAME)) {
					roaDefinition.setAttributeDisplayName("ICF NAME");
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
	public ResourceObject fetchObject(ResourceObjectDefinition resourceObjectDefinition,
			Collection<? extends ResourceObjectAttribute> identifiers, boolean returnDefaultAttributes,
			Collection<? extends ResourceObjectAttributeDefinition> attributesToReturn,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException {

		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".fetchObject");
		result.addParam("resourceObjectDefinition", resourceObjectDefinition);
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

		ObjectClass icfObjectClass = objectClassToIcf(resourceObjectDefinition);
		if (icfObjectClass == null) {
			result.recordFatalError("Unable to detemine object class from QName "
					+ resourceObjectDefinition.getTypeName()
					+ " while attempting to fetch object identified by " + identifiers + " from "
					+ ObjectTypeUtil.toShortString(connectorType));
			throw new IllegalArgumentException("Unable to detemine object class from QName "
					+ resourceObjectDefinition.getTypeName()
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

		ResourceObject ro = convertToResourceObject(co, resourceObjectDefinition, true);

		result.recordSuccess();
		return ro;

	}

	/**
	 * Returns null if nothing is found.
	 */
	private ConnectorObject fetchConnectorObject(ObjectClass icfObjectClass, Uid uid,
			boolean returnDefaultAttributes,
			Collection<? extends ResourceObjectAttributeDefinition> attributesToReturn,
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
	public Set<ResourceObjectAttribute> addObject(ResourceObject object, Set<Operation> additionalOperations,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			SchemaException, ObjectAlreadyExistsException {

		OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".addObject");
		result.addParam("resourceObject", object);
		result.addParam("additionalOperations", additionalOperations);

		// getting icf object class from resource object class
		ObjectClass objectClass = objectClassToIcf(object.getDefinition());

		if (objectClass == null) {
			result.recordFatalError("Couldn't get icf object class from resource definition.");
			throw new IllegalArgumentException("Couldn't get icf object class from resource definition.");
		}

		// setting ifc attributes from resource object attributes
		Set<Attribute> attributes = null;
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("midPoint object before conversion:\n{}", object.dump());
			}
			attributes = convertFromResourceObject(object.getAttributes(), result);
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

		ResourceObjectAttribute attribute = setUidAttribute(uid);
		object.addReplaceExisting(attribute);
		icfResult.recordSuccess();

		result.recordSuccess();
		return object.getAttributes();
	}

	@Override
	public Set<AttributeModificationOperation> modifyObject(ResourceObjectDefinition objectClass,
			Collection<? extends ResourceObjectAttribute> identifiers, Set<Operation> changes,
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

		Set<ResourceObjectAttribute> addValues = new HashSet<ResourceObjectAttribute>();
		Set<ResourceObjectAttribute> updateValues = new HashSet<ResourceObjectAttribute>();
		Set<ResourceObjectAttribute> valuesToRemove = new HashSet<ResourceObjectAttribute>();

		Set<Operation> additionalOperations = new HashSet<Operation>();
		ActivationChangeOperation activationChangeOperation = null;
		PasswordChangeOperation passwordChangeOperation = null;

		for (Operation operation : changes) {
			if (operation instanceof AttributeModificationOperation) {
				AttributeModificationOperation change = (AttributeModificationOperation) operation;
				if (change.getChangeType().equals(PropertyModificationTypeType.add)) {
					Property property = change.getNewAttribute();
					ResourceObjectAttribute addAttribute = new ResourceObjectAttribute(property.getName(),
							property.getDefinition(), property.getValues());
					addValues.add(addAttribute);

				}
				if (change.getChangeType().equals(PropertyModificationTypeType.delete)) {
					Property property = change.getNewAttribute();
					ResourceObjectAttribute deleteAttribute = new ResourceObjectAttribute(property.getName(),
							property.getDefinition(), property.getValues());
					valuesToRemove.add(deleteAttribute);
				}
				if (change.getChangeType().equals(PropertyModificationTypeType.replace)) {
					Property property = change.getNewAttribute();
					ResourceObjectAttribute updateAttribute = new ResourceObjectAttribute(property.getName(),
							property.getDefinition(), property.getValues());
					updateValues.add(updateAttribute);

				}

			} else if (operation instanceof ActivationChangeOperation) {
				activationChangeOperation = (ActivationChangeOperation) operation;
				// TODO: check for multiple occurrences and fail

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

		if (updateValues != null && !updateValues.isEmpty() || activationChangeOperation != null
				|| passwordChangeOperation != null) {

			Set<Attribute> attributes = null;

			try {
				attributes = convertFromResourceObject(updateValues, result);
			} catch (SchemaException ex) {
				result.recordFatalError(
						"Error while converting resource object attributes. Reason: " + ex.getMessage(), ex);
				throw new SchemaException("Error while converting resource object attributes. Reason: "
						+ ex.getMessage(), ex);
			}

			if (activationChangeOperation != null) {
				// Activation change means modification of attributes
				convertFromActivation(attributes, activationChangeOperation);
			}

			if (passwordChangeOperation != null) {
				// Activation change means modification of attributes
				convertFromPassword(attributes, passwordChangeOperation);
			}

			OperationOptions options = new OperationOptionsBuilder().build();
			icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".update");
			icfResult.addParam("objectClass", objectClass);
			icfResult.addParam("uid", uid.getUidValue());
			icfResult.addParam("attributes", attributes);
			icfResult.addParam("options", options);
			icfResult.addContext("connector", icfConnectorFacade);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Invoking ICF update(), objectclass={}, uid={}, attributes=\n{}", new Object[] {
						objClass, uid, dumpAttributes(attributes) });
			}

			try {
				// Call ICF
				uid = icfConnectorFacade.update(objClass, uid, attributes, options);

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

		Set<AttributeModificationOperation> sideEffectChanges = new HashSet<AttributeModificationOperation>();
		if (!originalUid.equals(uid.getUidValue())) {
			// UID was changed during the operation, this is most likely a
			// rename
			AttributeModificationOperation uidMod = new AttributeModificationOperation();
			uidMod.setChangeType(PropertyModificationTypeType.replace);
			ResourceObjectAttribute uidAttr = getUidDefinition(identifiers).instantiate();
			uidAttr.setValue(new PropertyValue(uid.getUidValue()));
			sideEffectChanges.add(uidMod);
		}
		return sideEffectChanges;
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
	public void deleteObject(ResourceObjectDefinition objectClass, Set<Operation> additionalOperations,
			Collection<? extends ResourceObjectAttribute> identifiers, OperationResult parentResult)
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
	public Property deserializeToken(Object serializedToken) {
		return createTokenProperty(serializedToken);
	}

	@Override
	public Property fetchCurrentToken(ResourceObjectDefinition objectClass, OperationResult parentResult)
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

		Property property = getToken(syncToken);
		result.recordSuccess();
		return property;
	}

	@Override
	public List<Change> fetchChanges(ResourceObjectDefinition objectClass, Property lastToken,
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
			Schema schema = getResourceSchema(subresult);
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
	public void search(final ResourceObjectDefinition objectClass, final ResultHandler handler,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			SchemaException {

		// Result type for this operation
		final OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
				+ ".search");
		result.addParam("objectClass", objectClass);
		result.addContext("connector", connectorType);

		if (objectClass == null) {
			result.recordFatalError("Object class not defined");
			throw new IllegalArgumentException("objectClass not defined");
		}

		ObjectClass icfObjectClass = objectClassToIcf(objectClass);
		if (icfObjectClass == null) {
			IllegalArgumentException ex = new IllegalArgumentException(
					"Unable to detemine object class from QName " + objectClass
							+ " while attempting to search objects by "
							+ ObjectTypeUtil.toShortString(connectorType));
			result.recordFatalError("Unable to detemine object class", ex);
			throw ex;
		}

		ResultsHandler icfHandler = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				// Convert ICF-specific connector object to a generic
				// ResourceObject
				ResourceObject resourceObject;
				try {
					resourceObject = convertToResourceObject(connectorObject, objectClass, true);
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

	/**
	 * Maps a midPoint QName objctclass to the ICF native objectclass name.
	 * <p/>
	 * The mapping is "stateless" - it does not keep any mapping database or any
	 * other state. There is a bi-directional mapping algorithm.
	 * <p/>
	 * TODO: mind the special characters in the ICF objectclass names.
	 */
	private ObjectClass objectClassToIcf(ResourceObjectDefinition objectClass) {
		QName qnameObjectClass = objectClass.getTypeName();
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
	private Uid getUid(Collection<? extends ResourceObjectAttribute> identifiers) {
		for (ResourceObjectAttribute attr : identifiers) {
			if (attr.getName().equals(ConnectorFactoryIcfImpl.ICFS_UID)) {
				return new Uid(attr.getValue(String.class).getValue());
			}
		}
		return null;
	}

	private ResourceObjectAttributeDefinition getUidDefinition(
			Collection<? extends ResourceObjectAttribute> identifiers) {
		for (ResourceObjectAttribute attr : identifiers) {
			if (attr.getName().equals(ConnectorFactoryIcfImpl.ICFS_UID)) {
				return attr.getDefinition();
			}
		}
		return null;
	}

	private ResourceObjectAttribute setUidAttribute(Uid uid) {
		ResourceObjectAttribute uidRoa = new ResourceObjectAttribute(ConnectorFactoryIcfImpl.ICFS_UID);
		uidRoa.setValue(new PropertyValue(uid.getUidValue()));
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
	 *            the returned resource object will contain olny attributed with
	 *            the non-null values.
	 * @return new mapped ResourceObject instance.
	 * @throws SchemaException
	 */
	private ResourceObject convertToResourceObject(ConnectorObject co, ResourceObjectDefinition def,
			boolean full) throws SchemaException {

		ResourceObject ro = null;
		if (def != null) {
			ro = def.instantiate();
		} else {
			// We don't know the name here. ObjectClass is a type, not name.
			// Therefore it will not help here even if we would have it.
			ro = new ResourceObject();
		}

		// Uid is always there
		Uid uid = co.getUid();
		// PropertyDefinition propDef = new
		// PropertyDefinition(SchemaConstants.ICFS_NAME,
		// SchemaConstants.XSD_STRING);
		// Property p = propDef.instantiate();
		ResourceObjectAttribute uidRoa = setUidAttribute(uid);
		// p = setUidAttribute(uid);
		ro.add(uidRoa);
		// ro.getProperties().add(p);

		for (Attribute icfAttr : co.getAttributes()) {
			if (icfAttr.getName().equals(Uid.NAME)) {
				// UID is handled specially (see above)
				continue;
			}
			if (icfAttr.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
				// password has to go to the credentials section
				ProtectedStringType password = getSingleValue(icfAttr, ProtectedStringType.class);
				MiscSchemaUtil.setPassword(ro.createOrGetCredentials(), (ProtectedStringType) password);
				continue;
			}
			if (icfAttr.getName().equals(OperationalAttributes.ENABLE_NAME)) {
				Boolean enabled = getSingleValue(icfAttr, Boolean.class);
				ro.createOrGetActivation().setEnabled(enabled);
				continue;
			}
			QName qname = convertAttributeNameToQName(icfAttr.getName());

			ResourceObjectAttribute roa = new ResourceObjectAttribute(qname);
			
			//if true, we need to convert whole connector object to the resource object also with the null-values attributes
			if (full){
				if (icfAttr.getValue() != null) {
					// Convert the values. While most values do not need
					// conversions, some
					// of them may need it (e.g. GuardedString)
					for (Object icfValue : icfAttr.getValue()) {
						Object value = convertValueFromIcf(icfValue, qname);
						roa.getValues().add(new PropertyValue<Object>(value));
					}
				}

				ro.add(roa);
			
			//in this case when false, we need only the attributes with the non-null values.
			} else{
				if (icfAttr.getValue() != null && !icfAttr.getValue().isEmpty()) {
					// Convert the values. While most values do not need
					// conversions, some
					// of them may need it (e.g. GuardedString)
					for (Object icfValue : icfAttr.getValue()) {
						Object value = convertValueFromIcf(icfValue, qname);
						roa.getValues().add(new PropertyValue<Object>(value));
						
					}
					
						ro.add(roa);
					
					
				}
			}
			
		}

		return ro;
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

	private Set<Attribute> convertFromResourceObject(Set<ResourceObjectAttribute> resourceAttributes,
			OperationResult parentResult) throws SchemaException {

		Set<Attribute> attributes = new HashSet<Attribute>();
		if (resourceAttributes == null) {
			// returning empty set
			return attributes;
		}

		for (ResourceObjectAttribute attribute : resourceAttributes) {

			String attrName = convertAttributeNameToIcf(attribute.getName(), parentResult);

			Set<Object> convertedAttributeValues = new HashSet<Object>();
			for (PropertyValue<Object> value : attribute.getValues()) {
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

		if (value instanceof PropertyValue) {
			return convertValueToIcf(((PropertyValue) value).getValue(), propName);
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

	private void convertFromActivation(Set<Attribute> attributes,
			ActivationChangeOperation activationChangeOperation) {

		attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME,
				activationChangeOperation.isEnabled()));

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

	private List<Change> getChangesFromSyncDelta(Set<SyncDelta> result, Schema schema,
			OperationResult parentResult) throws SchemaException {
		List<Change> changeList = new ArrayList<Change>();

		for (SyncDelta delta : result) {

			if (SyncDeltaType.DELETE.equals(delta.getDeltaType())) {
				ObjectDelta<ResourceObjectShadowType> objectDelta = new ObjectDelta<ResourceObjectShadowType>(
						ResourceObjectShadowType.class, ChangeType.DELETE);
				ResourceObjectAttribute uidAttribute = setUidAttribute(delta.getUid());
				Set<ResourceObjectAttribute> identifiers = new HashSet<ResourceObjectAttribute>();
				identifiers.add(uidAttribute);
				Change change = new Change(identifiers, objectDelta, getToken(delta.getToken()));
				changeList.add(change);

			} else {
				// CREATE OR UPDATE
				
				ObjectClass objClass = delta.getObject().getObjectClass();
				QName objectClass = objectClassToQname(objClass.getObjectClassValue());
				ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
						.findContainerDefinitionByType(objectClass);
				ResourceObject resourceObject = convertToResourceObject(delta.getObject(), rod, false);
				
				ResourceObjectShadowType currentShadow = createModificationChange(delta,
						resourceObject);
				
				LOGGER.trace("Got current shadow: {}", JAXBUtil.silentMarshalWrap(currentShadow));
				
				if (currentShadow instanceof AccountShadowType){
					currentShadow = (AccountShadowType) currentShadow;
				}

				Change change = new Change(resourceObject.getIdentifiers(), currentShadow,
						getToken(delta.getToken()));
				changeList.add(change);
			}

		}
		return changeList;
	}

	private ResourceObjectShadowType createModificationChange(SyncDelta delta,
			ResourceObject resourceObject) throws SchemaException {
		ResourceObjectShadowType shadow = new ResourceObjectShadowType();
		
		Document doc = DOMUtil.getDocument();
		Attributes attrs = new Attributes();
		shadow.setAttributes(attrs);
		attrs.getAny().addAll(resourceObject.serializePropertiesToJaxb(doc));
		
		return shadow;
		
//		ObjectModificationType modificationType = new ObjectModificationType();
//		modificationType.setOid(delta.getUid().getUidValue());
//
//		for (ResourceObjectAttribute attr : resourceObject.getAttributes()) {
//			
//			PropertyModificationType propertyModification = new PropertyModificationType();
//			propertyModification.setModificationType(PropertyModificationTypeType.add);
//			Document doc = DOMUtil.getDocument();
//			List<Object> elements;
//			try {
//				elements = attr.serializeToJaxb(doc);
//				for (Object e : elements) {
//					LOGGER.trace("Atribute to modify value: {}", JAXBUtil.getTextContentDump(e));
//				}
//				PropertyModificationType.Value value = new PropertyModificationType.Value();
//				value.getAny().addAll(elements);
//				propertyModification.setValue(value);
//				Element path = getModificationPath(doc);
//
//				propertyModification.setPath(path);
//				modificationType.getPropertyModification().add(propertyModification);
//			} catch (SchemaException ex) {
//				throw new SchemaException(
//						"An error occured while serializing resource object properties to DOM. "
//								+ ex.getMessage(), ex);
//			}
//
//		}
//		modificationChangeType.setObjectModification(modificationType);
//		return modificationChangeType;
	}

	private Element getModificationPath(Document doc) {
		List<XPathSegment> segments = new ArrayList<XPathSegment>();
		XPathSegment attrSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
		segments.add(attrSegment);
		XPathHolder t = new XPathHolder(segments);
		Element xpathElement = t.toElement(SchemaConstants.I_PROPERTY_CONTAINER_REFERENCE_PATH, doc);
		return xpathElement;
	}

	private SyncToken getSyncToken(Property lastToken) throws SchemaException {
		Object obj = null;
		Document doc = DOMUtil.getDocument();
		List<Object> elements = null;
		try {
			elements = lastToken.serializeToJaxb(doc);
		} catch (SchemaException ex) {
			throw new SchemaException("Failed to serialize last token property to dom.", ex);
		}
		for (Object e : elements) {
			obj = XsdTypeConverter.toJavaValue(e, lastToken.getDefinition().getTypeName());
		}
		SyncToken syncToken = new SyncToken(obj);
		return syncToken;
	}

	private Property getToken(SyncToken syncToken) {
		Object object = syncToken.getValue();
		return createTokenProperty(object);
	}

	private Property createTokenProperty(Object object) {
		QName type = XsdTypeConverter.toXsdType(object.getClass());

		Set<PropertyValue<Object>> objs = new HashSet<PropertyValue<Object>>();
		objs.add(new PropertyValue<Object>(object));
		PropertyDefinition propDef = new PropertyDefinition(SchemaConstants.SYNC_TOKEN, type);

		Property property = new Property(SchemaConstants.SYNC_TOKEN, propDef, objs);
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
				LOGGER.trace("Find execute script operation: {}", DebugUtil.prettyPrint(executeOp));
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
			ResourceConfigurationType configuration) throws SchemaException {

		ConfigurationProperties configProps = apiConfig.getConfigurationProperties();

		// The namespace of all the configuration properties specific to the
		// connector instance will have a connector instance namespace. This
		// namespace can be found in the resource definition.
		String connectorConfNs = connectorType.getNamespace();

		int numConfingProperties = 0;

		// Iterate over all the elements of XML resource definition that are in
		// the "configuration" part.
		List<Object> xmlConfig = configuration.getAny();
		for (Object element : xmlConfig) {

			// assume DOM elements here.
			// TODO: fix this to also check for JAXB elements
			Element e = (Element) element;

			// Process the "configurationProperties" part of configuration
			if (e.getNamespaceURI() != null
					&& e.getNamespaceURI().equals(connectorConfNs)
					&& e.getLocalName() != null
					&& e.getLocalName()
							.equals(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME)) {

				// Iterate over all the XML elements there. Each element is
				// a configuration property.
				NodeList configurationNodelist = e.getChildNodes();
				for (int i = 0; i < configurationNodelist.getLength(); i++) {
					Node node = configurationNodelist.item(i);
					// We care only about elements, ignoring comments and text
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element configElement = (Element) node;

						// All the elements must be in a connector instance
						// namespace.
						if (configElement.getNamespaceURI() == null
								|| !configElement.getNamespaceURI().equals(connectorConfNs)) {
							LOGGER.warn("Found element with a wrong namespace ({}) in connector OID={}",
									configElement.getNamespaceURI(), connectorType.getOid());
						} else {

							numConfingProperties++;

							// Local name of the element is the same as the name
							// of ICF configuration property
							String propertyName = configElement.getLocalName();
							ConfigurationProperty property = configProps.getProperty(propertyName);

							// Check (java) type of ICF configuration property,
							// behave accordingly
							@SuppressWarnings("rawtypes")
							Class type = property.getType();
							if (type.isArray()) {
								// Special handling for array values. If the
								// type
								// of the property is array, the XML element may
								// appear
								// several times.
								List<Object> values = new ArrayList<Object>();
								// Convert the first value
								Object value = convertToJava(configElement, type.getComponentType());
								values.add(value);
								// Loop over until the elements have the same
								// local name
								while (i + 1 < configurationNodelist.getLength()
										&& configurationNodelist.item(i + 1).getNodeType() == Node.ELEMENT_NODE
										&& ((Element) (configurationNodelist.item(i + 1))).getLocalName()
												.equals(propertyName)) {
									i++;
									configElement = (Element) configurationNodelist.item(i);
									// Convert all the remaining values
									Object avalue = convertToJava(configElement, type.getComponentType());
									values.add(avalue);
								}

								// Convert array to a list with appropriate type
								Object valuesArrary = Array.newInstance(type.getComponentType(),
										values.size());
								for (int j = 0; j < values.size(); ++j) {
									Object avalue = values.get(j);
									Array.set(valuesArrary, j, avalue);
								}
								property.setValue(valuesArrary);

							} else {
								// Single-valued property are easy to convert
								Object value = convertToJava(configElement, type);
								property.setValue(value);
							}
						}
					}
				}

			} else if (e.getNamespaceURI() != null
					&& e.getNamespaceURI().equals(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION)
					&& e.getLocalName() != null
					&& e.getLocalName()
							.equals(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME)) {
				// Process the "connectorPoolConfiguration" part of
				// configuration

				ObjectPoolConfiguration connectorPoolConfiguration = apiConfig
						.getConnectorPoolConfiguration();

				NodeList childNodes = e.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node item = childNodes.item(i);
					if (item.getNodeType() == Node.ELEMENT_NODE) {
						Element subelement = (Element) item;
						if (subelement.getNamespaceURI() != null
								&& subelement.getNamespaceURI().equals(
										ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION)) {
							String subelementName = subelement.getLocalName();
							if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_EVICTABLE_IDLE_TIME_MILLIS
									.equals(subelementName)) {
								connectorPoolConfiguration
										.setMinEvictableIdleTimeMillis(parseLong(subelement));
							} else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_IDLE
									.equals(subelementName)) {
								connectorPoolConfiguration.setMinIdle(parseInt(subelement));
							} else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_IDLE
									.equals(subelementName)) {
								connectorPoolConfiguration.setMaxIdle(parseInt(subelement));
							} else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_OBJECTS
									.equals(subelementName)) {
								connectorPoolConfiguration.setMaxObjects(parseInt(subelement));
							} else if (ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_WAIT
									.equals(subelementName)) {
								connectorPoolConfiguration.setMaxWait(parseLong(subelement));
							} else {
								throw new SchemaException(
										"Unexpected element "
												+ DOMUtil.getQName(subelement)
												+ " in "
												+ ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME);
							}
						} else {
							throw new SchemaException(
									"Unexpected element "
											+ DOMUtil.getQName(subelement)
											+ " in "
											+ ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME);
						}
					}
				}

			} else if (e.getNamespaceURI() != null
					&& e.getNamespaceURI().equals(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION)
					&& e.getLocalName() != null
					&& e.getLocalName().equals(
							ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_XML_ELEMENT_NAME)) {
				// Process the "producerBufferSize" part of configuration
				apiConfig.setProducerBufferSize(parseInt(e));

			} else if (e.getNamespaceURI() != null
					&& e.getNamespaceURI().equals(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION)
					&& e.getLocalName() != null
					&& e.getLocalName().equals(
							ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME)) {
				// Process the "timeouts" part of configuration

				NodeList childNodes = e.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node item = childNodes.item(i);
					if (item.getNodeType() == Node.ELEMENT_NODE) {
						Element subelement = (Element) item;

						if (ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION.equals(subelement.getNamespaceURI())) {
							String opName = subelement.getLocalName();
							Class<? extends APIOperation> apiOpClass = ConnectorFactoryIcfImpl
									.resolveApiOpClass(opName);
							if (apiOpClass != null) {
								apiConfig.setTimeout(apiOpClass, parseInt(subelement));
							} else {
								throw new SchemaException("Unknown operation name " + opName + " in "
										+ ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME);
							}
						}
					}
				}

			} else {
				throw new SchemaException("Unexpected element " + DOMUtil.getQName(e)
						+ " in resource configuration");
			}
		}

		// TODO: pools, etc.

		if (numConfingProperties == 0) {
			throw new SchemaException("No configuration properties found. Wrong namespace? (expected: "
					+ connectorConfNs + ")");
		}
	}

	private int parseInt(Element e) {
		return Integer.parseInt(e.getTextContent());
	}

	private long parseLong(Element e) {
		return Long.parseLong(e.getTextContent());
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
		value = XsdTypeConverter.toJavaValue(configElement, midPointClass);
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
