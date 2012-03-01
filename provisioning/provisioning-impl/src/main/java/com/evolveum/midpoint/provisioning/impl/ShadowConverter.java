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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
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
	@Autowired(required=true)
	private PrismContext prismContext;

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

	public <T extends ResourceObjectShadowType> T getShadow(Class<T> type, ResourceType resource, T repoShadow,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			SchemaException {

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		PrismSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		QName objectClass = repoShadow.getObjectClass();
		ResourceAttributeContainerDefinition objectClassDefinition = (ResourceAttributeContainerDefinition) schema
				.findContainerDefinitionByType(objectClass);

		if (objectClassDefinition == null) {
			// Unknown objectclass
			SchemaException ex = new SchemaException("Object class " + objectClass
					+ " defined in the repository shadow is not known in schema of resource "
					+ ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError("Object class " + objectClass
					+ " defined in the repository shadow is not known in resource schema", ex);
			throw ex;
		}

		// Let's get all the identifiers from the Shadow <attributes> part
		Collection<? extends ResourceAttribute> identifiers = ResourceObjectShadowUtil.getIdentifiers(repoShadow);

		if (identifiers == null || identifiers.isEmpty()) {
			// No identifiers found
			SchemaException ex = new SchemaException("No identifiers found in the respository shadow "
					+ ObjectTypeUtil.toShortString(repoShadow) + " with respect to resource "
					+ ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError(
					"No identifiers found in the respository shadow " + ObjectTypeUtil.toShortString(repoShadow),
					ex);
			throw ex;
		}

		T resourceShadow = fetchResourceObject(type, objectClassDefinition, identifiers, connector, resource, parentResult);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow from repository:\n{}", repoShadow.asPrismObject().dump());
			LOGGER.trace("Resource object fetched from resource:\n{}", resourceShadow.asPrismObject().dump());
		}

		// Complete the shadow by adding attributes from the resource object
		T resultShadow = ShadowCacheUtil.completeShadow(resourceShadow, repoShadow, resource, parentResult);

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

		Set<ResourceAttribute> resourceAttributesAfterAdd = null;
		// add object using connector, setting new properties to the
		// resourceObject
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Connector for resource {}\n ADD object:\n{}\n additional operations:\n{}",
						new Object[] { resource.asPrismObject(), shadow.asPrismObject().debugDump(),
								SchemaDebugUtil.debugDump(additionalOperations) });
			}

			resourceAttributesAfterAdd = connector.addObject(shadow.asPrismObject(), additionalOperations,
					parentResult);

			if (LOGGER.isDebugEnabled()) {
				// TODO: reduce only to new/different attributes. Dump all
				// attributes on trace level only
				LOGGER.debug("Connector ADD successful, returned attributes:\n{}",
						SchemaDebugUtil.prettyPrint(resourceAttributesAfterAdd));
			}

			applyAfterOperationAttributes(shadow, resourceAttributesAfterAdd);
		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communitacing with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communitacing with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error in connector: " + ex.getMessage(), ex);
			throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
		}

		shadow = ShadowCacheUtil.createRepositoryShadow(shadow, resource);

		parentResult.recordSuccess();
		return shadow;
	}

	public void deleteShadow(ResourceType resource, ResourceObjectShadowType shadow,
			Set<Operation> additionalOperations, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		PrismSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		ResourceAttributeContainerDefinition objectClassDefinition = (ResourceAttributeContainerDefinition) schema.findContainerDefinitionByType(shadow
				.getObjectClass());

		LOGGER.trace("Getting object identifiers");
		Collection<? extends ResourceAttribute> identifiers =  ResourceObjectShadowUtil.getIdentifiers(shadow);

		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"Connector for resource {}\n DELETE object, object class {}, identified by:\n{}\n additional operations:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), shadow.getObjectClass(),
								SchemaDebugUtil.debugDump(identifiers), SchemaDebugUtil.debugDump(additionalOperations) });
			}

			connector.deleteObject(objectClassDefinition, additionalOperations, identifiers, parentResult);

			LOGGER.debug("Connector DELETE successful");
			parentResult.recordSuccess();

		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Can't delete object " + ObjectTypeUtil.toShortString(shadow)
					+ ". Reason: " + ex.getMessage(), ex);
			throw new ObjectNotFoundException("An error occured while deleting resource object " + shadow
					+ "whith identifiers " + identifiers + ": " + ex.getMessage(), ex);
		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communitacing with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error in connector: " + ex.getMessage(), ex);
			throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
		}
	}

	public Set<PropertyModificationOperation> modifyShadow(ResourceType resource,
			ResourceObjectShadowType shadow, Set<Operation> operations, String oid, Collection<? extends ItemDelta> objectChanges,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
			CommunicationException {
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		PrismSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);

		ResourceAttributeContainerDefinition objectClassDefinition = (ResourceAttributeContainerDefinition) schema.findContainerDefinitionByType(shadow
				.getObjectClass());
		Collection<? extends ResourceAttribute> identifiers = ResourceObjectShadowUtil.getIdentifiers(shadow); 

		Set<Operation> attributeChanges = getAttributeChanges(objectChanges, operations, objectClassDefinition);
		if (attributeChanges != null) {
			operations.addAll(attributeChanges);
		}
		Set<PropertyModificationOperation> sideEffectChanges = null;
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"Connector for resource {}\n MODIFY object, object class {}, identified by:\n{}\n changes:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), shadow.getObjectClass(),
								SchemaDebugUtil.debugDump(identifiers), SchemaDebugUtil.debugDump(operations) });
			}

			// Invoke ICF
			sideEffectChanges = connector.modifyObject(objectClassDefinition, identifiers, operations, parentResult);

			LOGGER.debug("Connector MODIFY successful, side-effect changes {}",
					SchemaDebugUtil.debugDump(sideEffectChanges));

		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Object to modify not found. Reason: " + ex.getMessage(), ex);
			throw new ObjectNotFoundException("Object to modify not found. " + ex.getMessage(), ex);
		} catch (CommunicationException ex) {
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
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);
		// This is a HACK. It should not work only for default account, but also for other objectclasses (FIXME)
		ResourceAttributeContainerDefinition objectClass = resourceSchema.findDefaultAccountDefinition();
		PrismProperty lastToken = null;
		try {
			lastToken = connector.fetchCurrentToken(objectClass, parentResult);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError("Generic error in the connector: " + e.getMessage(), e);
			throw new CommunicationException("Generic error in the connector: " + e.getMessage(), e);

		} catch (CommunicationException ex) {
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
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		Validate.notNull(resource, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		Validate.notNull(lastToken, "Token property must not be null.");

		LOGGER.trace("Shadow cache, fetch changes");
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		// This is a HACK. It should not work only for default account, but also for other objectclasses (FIXME)
		ResourceAttributeContainerDefinition objectClass = resourceSchema.findDefaultAccountDefinition();

		// get changes from the connector
		List<Change> changes = null;
		try {
			changes = connector.fetchChanges(objectClass, lastToken, parentResult);

		} catch (SchemaException ex) {
			parentResult.recordFatalError("Schema error: " + ex.getMessage(), ex);
			throw ex;
		} catch (CommunicationException ex) {
			parentResult.recordFatalError("Communication error: " + ex.getMessage(), ex);
			throw ex;

		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error: " + ex.getMessage(), ex);
			throw new GenericConnectorException(ex.getMessage(), ex);
		} catch (ConfigurationException ex) {
			parentResult.recordFatalError("Configuration error: " + ex.getMessage(), ex);
			throw ex;
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

		ResourceObjectShadowType shadow = null;
		try {
			shadow = fetchResourceObject(ResourceObjectShadowType.class, rod, change.getIdentifiers(), connector, resource,
					parentResult);
		} catch (ObjectNotFoundException ex) {
			parentResult
					.recordPartialError("Object detected in change log no longer exist on the resource. Skipping processing this object.");
			LOGGER.warn("Object detected in change log no longer exist on the resource. Skipping processing this object " + ex.getMessage());
			return null;
		}
		try {

			shadow = ShadowCacheUtil.createRepositoryShadow(shadow, resource);

		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't create account shadow from identifiers: "
					+ change.getIdentifiers());
			throw new SchemaException("Can't create account shadow from identifiers: "
					+ change.getIdentifiers());
		}

		parentResult.recordSuccess();
		return shadow;
	}

	private <T extends ResourceObjectShadowType> T fetchResourceObject(Class<T> type,
			ResourceAttributeContainerDefinition objectClassDefinition,
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
			PrismObject<T> resourceObject = connector.fetchObject(type, objectClassDefinition, identifiers, true, null, parentResult);
			return resourceObject.asObjectable();
		} catch (ObjectNotFoundException e) {
			parentResult.recordFatalError(
					"Object not found. Identifiers: " + identifiers + ". Reason: " + e.getMessage(), e);
			throw new ObjectNotFoundException("Object not found. Identifiers: " + identifiers + ". Reason: "
					+ e.getMessage(), e);
		} catch (CommunicationException e) {
			parentResult.recordFatalError("Error communication with the connector " + connector
					+ ". Reason: " + e.getMessage(), e);
			throw new CommunicationException("Error communication with the connector " + connector
					+ ". Reason: " + e.getMessage(), e);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError(
					"Generic error in the connector " + connector + ". Reason: " + e.getMessage(), e);
			throw new GenericConnectorException("Generic error in the connector " + connector + ". Reason: "
					+ e.getMessage(), e);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't get resource schema. Reason: " + ex.getMessage(), ex);
			throw new SchemaException("Can't get resource schema. Reason: " + ex.getMessage(), ex);
		}

	}

	private void applyAfterOperationAttributes(ResourceObjectShadowType shadow,
			Set<ResourceAttribute> resourceAttributesAfterOperation) {
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(shadow);
		for (ResourceAttribute attributeAfter: resourceAttributesAfterOperation) {
			ResourceAttribute attributeBefore = attributesContainer.findAttribute(attributeAfter.getName());
			if (attributeBefore != null) {
				attributesContainer.remove(attributeBefore);
			}
			attributesContainer.add(attributeAfter);
		}
	}
	
	private ConnectorInstance getConnectorInstance(ResourceType resource, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		return connectorTypeManager.getConfiguredConnectorInstance(resource, parentResult);
	}


	private Set<Operation> getAttributeChanges(Collection<? extends ItemDelta> objectChange, Set<Operation> changes,
			ResourceAttributeContainerDefinition rod) throws SchemaException {
		if (changes == null) {
			changes = new HashSet<Operation>();
		}
		for (ItemDelta itemDelta : objectChange) {
			if (itemDelta instanceof PropertyDelta) {
				PropertyModificationOperation attributeModification = new PropertyModificationOperation((PropertyDelta)itemDelta);
				changes.add(attributeModification);
			} else {
				throw new UnsupportedOperationException("Not supported delta: "+itemDelta);
			}
		}
		return changes;
	}
}
