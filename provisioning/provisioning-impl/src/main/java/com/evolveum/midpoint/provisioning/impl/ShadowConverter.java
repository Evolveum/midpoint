package com.evolveum.midpoint.provisioning.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.ActivationChangeOperation;
import com.evolveum.midpoint.provisioning.ucf.api.AttributeModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PasswordChangeOperation;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.MiscUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType.Password;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType.EnableDisable;

@Component
public class ShadowConverter {

	@Autowired
	private ConnectorTypeManager connectorTypeManager;
	@Autowired
	private ResourceTypeManager resourceTypeManager;

	public ShadowConverter() {
	}

	public ConnectorTypeManager getConnectorTypeManager() {
		return connectorTypeManager;
	}

	public void setConnectorTypeManager(ConnectorTypeManager connectorTypeManager) {
		this.connectorTypeManager = connectorTypeManager;
	}

	public ResourceTypeManager getResourceTypeManager() {
		return resourceTypeManager;
	}

	public void setResourceTypeManager(ResourceTypeManager resourceTypeManager) {
		this.resourceTypeManager = resourceTypeManager;
	}

	private static final Trace LOGGER = TraceManager.getTrace(ShadowConverter.class);

	public ResourceObjectShadowType getShadow(ResourceType resource, ResourceObjectShadowType shadow,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			SchemaException {

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		Schema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		QName objectClass = shadow.getObjectClass();
		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);

		if (rod == null) {
			// Unknown objectclass
			SchemaException ex = new SchemaException("Object class " + objectClass
					+ " defined in the repository shadow is not known in schema of resource "
					+ ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError("Object class " + objectClass
					+ " defined in the repository shadow is not known in resource schema", ex);
			throw ex;
		}

		// Let's get all the identifiers from the Shadow <attributes> part
		Set<ResourceObjectAttribute> identifiers = rod.parseIdentifiers(shadow.getAttributes().getAny());

		if (identifiers == null || identifiers.isEmpty()) {
			// No identifiers found
			SchemaException ex = new SchemaException("No identifiers found in the respository shadow "
					+ ObjectTypeUtil.toShortString(shadow) + " with respect to resource "
					+ ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError(
					"No identifiers found in the respository shadow " + ObjectTypeUtil.toShortString(shadow),
					ex);
			throw ex;
		}

		ResourceObject ro = fetchResourceObject(rod, identifiers, connector, resource, parentResult);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow from repository:\n{}", ObjectTypeUtil.dump(shadow));
			LOGGER.trace("Resource object fetched from resource:\n{}", ro.dump());
		}

		if (shadow instanceof AccountShadowType) {
			// convert resource activation attribute to the <activation>
			// attribute
			// of shadow
			ActivationType activationType = determineActivation(resource, ro, parentResult);
			if (activationType != null) {
				LOGGER.trace("Determined activation: {}", activationType.isEnabled());
				((AccountShadowType) shadow).setActivation(activationType);
			}

		}

		// Complete the shadow by adding attributes from the resource object
		ResourceObjectShadowType resultShadow = resourceTypeManager.assembleShadow(ro, shadow, parentResult);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow when assembled:\n", ObjectTypeUtil.dump(resultShadow));
		}
		parentResult.recordSuccess();
		return resultShadow;

	}

	public ResourceType completeResource(ResourceType resource, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException {

		return resourceTypeManager.completeResource(resource, null, parentResult);
	}

	public ResourceObjectShadowType addShadow(ResourceType resource, ResourceObjectShadowType shadow,
			Set<Operation> additionalOperations, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException,
			ObjectAlreadyExistsException {

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);
		Schema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		// convert xml attributes to ResourceObject
		ResourceObject resourceObject = convertResourceObjectFromXml(shadow, schema, parentResult);

		Set<ResourceObjectAttribute> resourceAttributesAfterAdd = null;
		// add object using connector, setting new properties to the
		// resourceObject
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Connector for resource {}\n ADD object:\n{}\n additional operations:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), resourceObject.debugDump(),
								DebugUtil.debugDump(additionalOperations) });
			}

			resourceAttributesAfterAdd = connector.addObject(resourceObject, additionalOperations,
					parentResult);

			if (LOGGER.isDebugEnabled()) {
				// TODO: reduce only to new/different attributes. Dump all
				// attributes on trace level only
				LOGGER.debug("Connector ADD successful, returned attributes:\n{}",
						DebugUtil.prettyPrint(resourceAttributesAfterAdd));
			}
			// if (LOGGER.isTraceEnabled()) {
			// LOGGER.trace("Added object: {}",
			// DebugUtil.prettyPrint(resourceAttributesAfterAdd));
			// }

			resourceObject.addAllReplaceExisting(resourceAttributesAfterAdd);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communitacing with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communitacing with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error in connector: " + ex.getMessage(), ex);
			throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
		}

		shadow = ShadowCacheUtil.createRepositoryShadow(resourceObject, resource, shadow);

		parentResult.recordSuccess();
		return shadow;
	}

	

	public void deleteShadow(ResourceType resource, ResourceObjectShadowType shadow,
			Set<Operation> additionalOperations, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		Schema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema.findContainerDefinitionByType(shadow
				.getObjectClass());

		LOGGER.trace("Getting object identifiers");
		Set<ResourceObjectAttribute> identifiers = rod.parseIdentifiers(shadow.getAttributes().getAny());

		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"Connector for resource {}\n DELETE object, object class {}, identified by:\n{}\n additional operations:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), shadow.getObjectClass(),
								DebugUtil.debugDump(identifiers), DebugUtil.debugDump(additionalOperations) });
			}

			connector.deleteObject(rod, additionalOperations, identifiers, parentResult);

			LOGGER.debug("Connector DELETE successful");
			parentResult.recordSuccess();

		} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException ex) {
			parentResult.recordFatalError("Can't delete object " + ObjectTypeUtil.toShortString(shadow)
					+ ". Reason: " + ex.getMessage(), ex);
			throw new ObjectNotFoundException("An error occured while deleting resource object " + shadow
					+ "whith identifiers " + identifiers + ": " + ex.getMessage(), ex);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communitacing with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error in connector: " + ex.getMessage(), ex);
			throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
		}
	}

	public Set<AttributeModificationOperation> modifyShadow(ResourceType resource,
			ResourceObjectShadowType shadow, Set<Operation> changes, ObjectModificationType objectChanges,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
			CommunicationException {
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		Schema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema.findContainerDefinitionByType(shadow
				.getObjectClass());
		Set<ResourceObjectAttribute> identifiers = rod.parseIdentifiers(shadow.getAttributes().getAny());


		Set<Operation> attributeChanges = getAttributeChanges(objectChanges, changes, rod);
		if (attributeChanges != null) {
			changes.addAll(attributeChanges);
		}
		Set<AttributeModificationOperation> sideEffectChanges = null;
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"Connector for resource {}\n MODIFY object, object class {}, identified by:\n{}\n changes:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), shadow.getObjectClass(),
								DebugUtil.debugDump(identifiers), DebugUtil.debugDump(changes) });
			}

			// Invoke ICF
			sideEffectChanges = connector.modifyObject(rod, identifiers, changes, parentResult);

			LOGGER.debug("Connector MODIFY successful, side-effect changes {}",
					DebugUtil.debugDump(sideEffectChanges));

		} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException ex) {
			parentResult.recordFatalError("Object to modify not found. Reason: " + ex.getMessage(), ex);
			throw new ObjectNotFoundException("Object to modify not found. " + ex.getMessage(), ex);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicationg with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error comminicationg with connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError(
					"Generic error in the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error comminicationg with connector " + connector + ": "
					+ ex.getMessage(), ex);
		}
		parentResult.recordSuccess();
		return sideEffectChanges;
	}


	public Property fetchCurrentToken(ResourceType resourceType, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Getting last token");
		ConnectorInstance connector = getConnectorInstance(resourceType, parentResult);
		Schema resourceSchema = ResourceTypeUtil.getResourceSchema(resourceType);
		ResourceObjectDefinition objectClass = resourceSchema.findAccountDefinition();
		Property lastToken = null;
		try {
			lastToken = connector.fetchCurrentToken(objectClass, parentResult);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError("Generic error in the connector: " + e.getMessage(), e);
			throw new CommunicationException("Generic error in the connector: " + e.getMessage(), e);

		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communicating with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		}

		LOGGER.trace("Got last token: {}", DebugUtil.prettyPrint(lastToken));
		parentResult.recordSuccess();
		return lastToken;
	}

	public List<Change> fetchChanges(ResourceType resource, Property lastToken, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		Validate.notNull(resource, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		Validate.notNull(lastToken, "Token property must not be null.");

		LOGGER.trace("Shadow cache, fetch changes");
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		Schema resourceSchema = ResourceTypeUtil.getResourceSchema(resource);
		ResourceObjectDefinition objectClass = resourceSchema.findAccountDefinition();

		// get changes from the connector
		List<Change> changes = null;
		try {
			changes = connector.fetchChanges(objectClass, lastToken, parentResult);


		} catch (SchemaException ex) {
			parentResult.recordFatalError("Schema error: " + ex.getMessage(), ex);
			throw new SchemaException("Schema error: " + ex.getMessage(), ex);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			parentResult.recordFatalError("Communication error: " + ex.getMessage(), ex);
			throw new CommunicationException("Communication error: " + ex.getMessage(), ex);
	
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error: " + ex.getMessage(), ex);
			throw new CommunicationException("Generic error: " + ex.getMessage(), ex);
		}
		parentResult.recordSuccess();
		return changes;
	}

	public ResourceObjectShadowType createNewAccountFromChange(Change change, ResourceType resource,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
			CommunicationException, GenericFrameworkException {

		ResourceObject resourceObject = null;

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		Schema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);
		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(new QName(resource.getNamespace(), "AccountObjectClass"));

		resourceObject = fetchResourceObject(rod, change.getIdentifiers(), connector, resource, parentResult);

		ResourceObjectShadowType shadow = null;
		try {
			shadow = ShadowCacheUtil.createShadow(resourceObject, resource, null);
			change.setOldShadow(shadow);
			shadow = ShadowCacheUtil.createRepositoryShadow(resourceObject, resource, shadow);

		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't create account shadow from identifiers: "
					+ change.getIdentifiers());
			throw new SchemaException("Can't create account shadow from identifiers: "
					+ change.getIdentifiers());
		}


		parentResult.recordSuccess();
		return shadow;
	}



	private ResourceObject fetchResourceObject(ResourceObjectDefinition rod,
			Set<ResourceObjectAttribute> identifiers, ConnectorInstance connector, ResourceType resource,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			SchemaException {

		// Set<ResourceObjectAttribute> roIdentifiers = new
		// HashSet<ResourceObjectAttribute>();
		// for (Property p : identifiers) {
		// ResourceObjectAttribute roa = new
		// ResourceObjectAttribute(p.getName(), p.getDefinition(),
		// p.getValues());
		// roIdentifiers.add(roa);
		// }

		try {
			ResourceObject resourceObject = connector.fetchObject(rod, identifiers, true, null, parentResult);
			return resourceObject;
		} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException e) {
			parentResult.recordFatalError(
					"Object not found. Identifiers: " + identifiers + ". Reason: " + e.getMessage(), e);
			throw new ObjectNotFoundException("Object not found. Identifiers: " + identifiers + ". Reason: "
					+ e.getMessage(), e);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException e) {
			parentResult.recordFatalError("Error communication with the connector " + connector
					+ ". Reason: " + e.getMessage(), e);
			throw new CommunicationException("Error communication with the connector " + connector
					+ ". Reason: " + e.getMessage(), e);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError(
					"Generic error in the connector " + connector + ". Reason: " + e.getMessage(), e);
			throw new CommunicationException("Generic error in the connector " + connector + ". Reason: "
					+ e.getMessage(), e);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't get resource schema. Reason: " + ex.getMessage(), ex);
			throw new SchemaException("Can't get resource schema. Reason: " + ex.getMessage(), ex);
		}

	}

	/**
	 * @param resource
	 * @param parentResult
	 * @return
	 * @throws SchemaException
	 * @throws ObjectNotFoundException
	 * @throws CommunicationException
	 */
	private ConnectorInstance getConnectorInstance(ResourceType resource, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		return connectorTypeManager.getConfiguredConnectorInstance(resource, parentResult);
	}

	/**
	 * Get account activation state from the resource object.
	 */
	public ActivationType determineActivation(ResourceType resource, ResourceObject ro,
			OperationResult parentResult) {
		if (ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
			return convertFromNativeActivationAttributes(resource, ro, parentResult);
		} else if (ResourceTypeUtil.hasActivationCapability(resource)) {
			return convertFromSimulatedActivationAttributes(resource, ro, parentResult);
		} else {
			// No activation capability, nothing to do
			return null;
		}
	}

	private ActivationType convertFromNativeActivationAttributes(ResourceType resource, ResourceObject ro,
			OperationResult parentResult) {
		return ro.getActivation();
	}

	private ActivationType convertFromSimulatedActivationAttributes(ResourceType resource, ResourceObject ro,
			OperationResult parentResult) {
		LOGGER.trace("Start converting activation type from simulated activation atribute");
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		List<String> disableValues = activationCapability.getEnableDisable().getDisableValue();
		List<String> enableValues = activationCapability.getEnableDisable().getEnableValue();

		ActivationType activationType = new ActivationType();

		if (null != activationCapability) {
			Property activationProperty = ro.findProperty(activationCapability.getEnableDisable()
					.getAttribute());
			if (activationProperty == null) {
				LOGGER.debug("No simulated activation attribute was defined for the account.");
				activationType.setEnabled(true);
				return activationType;
			}
			Set<Object> activationValues = activationProperty.getValues();
			LOGGER.trace("Detected simulated activation attribute with value {}",
					activationProperty.getValues());
			if (activationValues == null || activationValues.isEmpty()
					|| activationValues.iterator().next() == null) {

				// No activation information.
				LOGGER.warn("The {} does not provide value for DISABLE attribute",
						ObjectTypeUtil.toShortString(resource));
				parentResult
						.recordPartialError("The "
								+ ObjectTypeUtil.toShortString(resource)
								+ " has native activation capability but noes not provide value for DISABLE attribute");
			} else {
				if (activationValues.size() > 1) {
					LOGGER.warn("The {} provides {} values for DISABLE attribute, expecting just one value",
							disableValues.size(), ObjectTypeUtil.toShortString(resource));
					parentResult.recordPartialError("The " + ObjectTypeUtil.toShortString(resource)
							+ " provides " + disableValues.size()
							+ " values for DISABLE attribute, expecting just one value");
				}
				Object disableObj = activationValues.iterator().next();

				for (String disable : disableValues) {
					if (disable.equals(String.valueOf(disableObj))) {
						activationType.setEnabled(false);
						return activationType;
					}
				}

				for (String enable : enableValues) {
					if ("".equals(enable) || enable.equals(String.valueOf(disableObj))) {
						activationType.setEnabled(true);
						return activationType;
					}
				}
			}
		}

		return null;
	}

	/**
	 * convert resource object shadow to the resource object according to given
	 * schema
	 * 
	 * @param resourceObjectShadow
	 *            object from which attributes are converted
	 * @param schema
	 * @return resourceObject
	 * @throws SchemaException
	 *             Object class definition was not found
	 */
	private ResourceObject convertResourceObjectFromXml(ResourceObjectShadowType resourceObjectShadow,
			Schema schema, OperationResult parentResult) throws SchemaException {
		QName objectClass = resourceObjectShadow.getObjectClass();

		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(schema, "Resource schema must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow before conversion:\n{}", ObjectTypeUtil.dump(resourceObjectShadow));
		}

		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow resource object definition:\n{}", rod.dump());
		}

		if (rod == null) {
			parentResult.recordFatalError("Schema definition for object class " + objectClass
					+ " was not found");
			throw new SchemaException("Schema definition for object class " + objectClass + " was not found");
		}
		ResourceObject resourceObject = rod.instantiate();

		List<Object> attributes = resourceObjectShadow.getAttributes().getAny();

		if (attributes == null) {
			throw new IllegalArgumentException("Attributes for the account was not defined.");
		}

		Set<ResourceObjectAttribute> resAttr = rod.parseAttributes(attributes);
		resourceObject.addAll(resAttr);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow converted to resource object:\n{}", resourceObject.dump());
		}

		return resourceObject;
	}

	private Set<Operation> getAttributeChanges(ObjectModificationType objectChange, Set<Operation> changes,
			ResourceObjectDefinition rod) throws SchemaException {
		if (changes == null) {
			changes = new HashSet<Operation>();
		}
		for (PropertyModificationType modification : objectChange.getPropertyModification()) {

			if (modification.getPath() == null) {
				throw new IllegalArgumentException("Path to modificated attributes is null.");
			}

			if (modification.getPath().getTextContent().contains(SchemaConstants.I_ATTRIBUTES.getLocalPart())) {

				Set<ResourceObjectAttribute> changedProperties = rod.parseAttributes(modification.getValue()
						.getAny());
				for (Property p : changedProperties) {

					AttributeModificationOperation attributeModification = new AttributeModificationOperation();
					attributeModification.setChangeType(modification.getModificationType());
					attributeModification.setNewAttribute(p);
					changes.add(attributeModification);
				}
			}
		}
		return changes;
	}
}
