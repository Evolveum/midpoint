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

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

		PrismSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		QName objectClass = shadow.getObjectClass();
		ResourceAttributeContainerDefinition rod = (ResourceAttributeContainerDefinition) schema
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
		Collection<? extends ResourceAttribute> identifiers = rod.parseIdentifiers(shadow
				.getAttributes().getAny());

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

		ResourceAttributeContainer ro = fetchResourceObject(rod, identifiers, connector, resource, parentResult);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow from repository:\n{}", ObjectTypeUtil.dump(shadow));
			LOGGER.trace("Resource object fetched from resource:\n{}", ro.dump());
		}

		if (shadow instanceof AccountShadowType) {
			// convert resource activation attribute to the <activation>
			// attribute
			// of shadow
			ActivationType activationType = ShadowCacheUtil.determineActivation(resource, ro, parentResult);
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
		PrismSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		// convert xml attributes to ResourceObject
		ResourceAttributeContainer resourceObject = convertResourceObjectFromXml(shadow, schema, parentResult);

		Set<ResourceAttribute> resourceAttributesAfterAdd = null;
		// add object using connector, setting new properties to the
		// resourceObject
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Connector for resource {}\n ADD object:\n{}\n additional operations:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), resourceObject.debugDump(),
								SchemaDebugUtil.debugDump(additionalOperations) });
			}

			resourceAttributesAfterAdd = connector.addObject(resourceObject, additionalOperations,
					parentResult);

			if (LOGGER.isDebugEnabled()) {
				// TODO: reduce only to new/different attributes. Dump all
				// attributes on trace level only
				LOGGER.debug("Connector ADD successful, returned attributes:\n{}",
						SchemaDebugUtil.prettyPrint(resourceAttributesAfterAdd));
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

		PrismSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		ResourceAttributeContainerDefinition rod = (ResourceAttributeContainerDefinition) schema.findContainerDefinitionByType(shadow
				.getObjectClass());

		LOGGER.trace("Getting object identifiers");
		Collection<? extends ResourceAttribute> identifiers = rod.parseIdentifiers(shadow
				.getAttributes().getAny());

		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"Connector for resource {}\n DELETE object, object class {}, identified by:\n{}\n additional operations:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), shadow.getObjectClass(),
								SchemaDebugUtil.debugDump(identifiers), SchemaDebugUtil.debugDump(additionalOperations) });
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

		PrismSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		ResourceAttributeContainerDefinition rod = (ResourceAttributeContainerDefinition) schema.findContainerDefinitionByType(shadow
				.getObjectClass());
		Collection<? extends ResourceAttribute> identifiers = rod.parseIdentifiers(shadow
				.getAttributes().getAny());

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
								SchemaDebugUtil.debugDump(identifiers), SchemaDebugUtil.debugDump(changes) });
			}

			// Invoke ICF
			sideEffectChanges = connector.modifyObject(rod, identifiers, changes, parentResult);

			LOGGER.debug("Connector MODIFY successful, side-effect changes {}",
					SchemaDebugUtil.debugDump(sideEffectChanges));

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

	public PrismProperty fetchCurrentToken(ResourceType resourceType, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Getting last token");
		ConnectorInstance connector = getConnectorInstance(resourceType, parentResult);
		PrismSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resourceType);
		ResourceAttributeContainerDefinition objectClass = resourceSchema.findAccountDefinition();
		PrismProperty lastToken = null;
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

		LOGGER.trace("Got last token: {}", SchemaDebugUtil.prettyPrint(lastToken));
		parentResult.recordSuccess();
		return lastToken;
	}

	public List<Change> fetchChanges(ResourceType resource, PrismProperty lastToken, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		Validate.notNull(resource, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		Validate.notNull(lastToken, "Token property must not be null.");

		LOGGER.trace("Shadow cache, fetch changes");
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		PrismSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource);
		ResourceAttributeContainerDefinition objectClass = resourceSchema.findAccountDefinition();

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

		ResourceAttributeContainer resourceObject = null;

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		PrismSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);
		ResourceAttributeContainerDefinition rod = (ResourceAttributeContainerDefinition) schema
				.findContainerDefinitionByType(new QName(resource.getNamespace(), "AccountObjectClass"));

		try {
			resourceObject = fetchResourceObject(rod, change.getIdentifiers(), connector, resource,
					parentResult);
		} catch (ObjectNotFoundException ex) {
			parentResult
					.recordPartialError("Object detected in change log no longer exist on the resource. Skipping processing this object.");
			LOGGER.warn("Object detected in change log no longer exist on the resource. Skipping processing this object " + ex.getMessage());
			return null;
		}
		ResourceObjectShadowType shadow = null;
		try {
			// shadow = ShadowCacheUtil.createShadow(resourceObject, resource,
			// null);
			// change.setOldShadow(shadow);
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

	private ResourceAttributeContainer fetchResourceObject(ResourceAttributeContainerDefinition rod,
			Collection<? extends ResourceAttribute> identifiers, ConnectorInstance connector,
			ResourceType resource, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException {

		// Set<ResourceObjectAttribute> roIdentifiers = new
		// HashSet<ResourceObjectAttribute>();
		// for (Property p : identifiers) {
		// ResourceObjectAttribute roa = new
		// ResourceObjectAttribute(p.getName(), p.getDefinition(),
		// p.getValues());
		// roIdentifiers.add(roa);
		// }

		try {
			ResourceAttributeContainer resourceObject = connector.fetchObject(rod, identifiers, true, null, parentResult);
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
	private ResourceAttributeContainer convertResourceObjectFromXml(ResourceObjectShadowType resourceObjectShadow,
			PrismSchema schema, OperationResult parentResult) throws SchemaException {
		QName objectClass = resourceObjectShadow.getObjectClass();

		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(schema, "Resource schema must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow before conversion:\n{}", ObjectTypeUtil.dump(resourceObjectShadow));
		}

		ResourceAttributeContainerDefinition rod = (ResourceAttributeContainerDefinition) schema
				.findContainerDefinitionByType(objectClass);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow resource object definition:\n{}", rod.dump());
		}

		if (rod == null) {
			parentResult.recordFatalError("Schema definition for object class " + objectClass
					+ " was not found");
			throw new SchemaException("Schema definition for object class " + objectClass + " was not found");
		}
		ResourceAttributeContainer resourceObject = rod.instantiate();

		List<Object> attributes = resourceObjectShadow.getAttributes().getAny();

		if (attributes == null) {
			throw new IllegalArgumentException("Attributes for the account was not defined.");
		}

		Set<ResourceAttribute> resAttr = rod.parseAttributes(attributes);
		resourceObject.addAll(resAttr);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow converted to resource object:\n{}", resourceObject.dump());
		}

		return resourceObject;
	}

	private Set<Operation> getAttributeChanges(ObjectModificationType objectChange, Set<Operation> changes,
			ResourceAttributeContainerDefinition rod) throws SchemaException {
		if (changes == null) {
			changes = new HashSet<Operation>();
		}
		for (PropertyModificationType modification : objectChange.getPropertyModification()) {

			if (modification.getPath() == null) {
				throw new IllegalArgumentException("Path to modificated attributes is null.");
			}

			if (modification.getPath().getTextContent().contains(SchemaConstants.I_ATTRIBUTES.getLocalPart())) {

				Set<ResourceAttribute> changedProperties = rod.parseAttributes(modification.getValue()
						.getAny());
				for (PrismProperty p : changedProperties) {

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
