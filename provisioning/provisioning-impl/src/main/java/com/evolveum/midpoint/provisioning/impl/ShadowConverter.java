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

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
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
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType.EnableDisable;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Component
public class ShadowConverter {

	@Autowired
	private ConnectorTypeManager connectorTypeManager;
//	@Autowired
//	private ResourceTypeManager resourceTypeManager;
	@Autowired(required = true)
	private PrismContext prismContext;

	public ShadowConverter() {
	}

	public ConnectorTypeManager getConnectorTypeManager() {
		return connectorTypeManager;
	}

	public void setConnectorTypeManager(ConnectorTypeManager connectorTypeManager) {
		this.connectorTypeManager = connectorTypeManager;
	}

//	public ResourceTypeManager getResourceTypeManager() {
//		return resourceTypeManager;
//	}
//
//	public void setResourceTypeManager(ResourceTypeManager resourceTypeManager) {
//		this.resourceTypeManager = resourceTypeManager;
//	}

	private static final Trace LOGGER = TraceManager.getTrace(ShadowConverter.class);

	public <T extends ResourceObjectShadowType> T getShadow(Class<T> type, ResourceType resource,
			T repoShadow, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);

		QName objectClass = repoShadow.getObjectClass();
		ObjectClassComplexTypeDefinition objectClassDefinition = schema
				.findObjectClassDefinition(objectClass);

		ResourceObjectShadowUtil.fixShadow(repoShadow.asPrismObject(), schema);

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
		Collection<? extends ResourceAttribute<?>> identifiers = ResourceObjectShadowUtil
				.getIdentifiers(repoShadow);

		if (identifiers == null || identifiers.isEmpty()) {
			//check if the account is not only partially created (exist only in repo so far)
			if (repoShadow.getFailedOperationType()!= null){
				throw new CommunicationException("Error communicating with connector");
			}
			// No identifiers found
			SchemaException ex = new SchemaException("No identifiers found in the respository shadow "
					+ ObjectTypeUtil.toShortString(repoShadow) + " with respect to resource "
					+ ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError(
					"No identifiers found in the respository shadow "
							+ ObjectTypeUtil.toShortString(repoShadow), ex);
			throw ex;
		}

		Collection<? extends ResourceAttribute<?>> attributes = ResourceObjectShadowUtil
				.getAttributes(repoShadow);

		if (isProtectedShadow(resource, objectClassDefinition, attributes)) {
			LOGGER.error("Attempt to fetch protected resource object " + objectClassDefinition + ": "
					+ identifiers + "; ignoring the request");
			throw new SecurityViolationException("Cannot get protected resource object "
					+ objectClassDefinition + ": " + identifiers);
		}

		T resourceShadow = fetchResourceObject(type, objectClassDefinition, identifiers, connector, resource,
				parentResult);

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

//	public ResourceType completeResource(ResourceType resource, OperationResult parentResult)
//			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
//
//		return resourceTypeManager.completeResource(resource, null, parentResult);
//	}

	public ResourceObjectShadowType addShadow(ResourceType resource, ResourceObjectShadowType shadowType,
			Set<Operation> additionalOperations, boolean isReconciled, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException,
			ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
//		ResourceSchema resourceSchema = resourceTypeManager.getResourceSchema(resource, connector,
//				parentResult);
		Collection<ResourceAttribute<?>> resourceAttributesAfterAdd = null;
		PrismObject<ResourceObjectShadowType> shadow = shadowType.asPrismObject();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow before conversion:\n{}", shadow.dump());
		}
		ResourceObjectShadowUtil.fixShadow(shadow, resourceSchema);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow after conversion:\n{}", shadow.dump());
		}

		if (isProtectedShadow(resource, shadow)) {
			LOGGER.error("Attempt to add protected shadow " + shadowType + "; ignoring the request");
			throw new SecurityViolationException("Cannot get protected shadow " + shadowType);
		}

		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Connector for resource {}\n ADD object:\n{}\n additional operations:\n{}",
						new Object[] { resource.asPrismObject(), shadowType.asPrismObject().debugDump(),
								SchemaDebugUtil.debugDump(additionalOperations) });
			}

			resourceAttributesAfterAdd = connector.addObject(shadow, additionalOperations, parentResult);

			if (LOGGER.isDebugEnabled()) {
				// TODO: reduce only to new/different attributes. Dump all
				// attributes on trace level only
				LOGGER.debug("Connector ADD successful, returned attributes:\n{}",
						SchemaDebugUtil.prettyPrint(resourceAttributesAfterAdd));
			}

			applyAfterOperationAttributes(shadowType, resourceAttributesAfterAdd);
		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communitacing with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communitacing with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error in connector: " + ex.getMessage(), ex);
			throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow being stored:\n{}", shadowType.asPrismObject().dump());
		}

		if (!isReconciled) {
			shadowType = ShadowCacheUtil.createRepositoryShadow(shadowType, resource);
		}

		parentResult.recordSuccess();
		return shadowType;
	}
	
	public ResourceSchema getResourceSchema(ResourceType resource, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException{
		// unless it has been already pulled
			LOGGER.trace("Fetching resource schema for " + ObjectTypeUtil.toShortString(resource));
			
//			try {
				ConnectorInstance connector = getConnectorInstance(resource, parentResult);
//			} catch (ObjectNotFoundException e) {
//				throw new ObjectNotFoundException("Error resolving connector reference in " + resource
//						+ ": Error creating connector instace: " + e.getMessage(), e);
//			}
				ResourceSchema resourceSchema = null;
			try {
				// Fetch schema from connector, UCF will convert it to
				// Schema Processor format and add all
				// necessary annotations
				resourceSchema = connector.getResourceSchema(parentResult);

			} catch (CommunicationException ex) {
				LOGGER.error("Unable to complete {}: {}", new Object[]{resource, ex.getMessage(), ex});
				// Ignore the error. The resource is not complete but the upper layer code should deal with that
				// Throwing an error will effectively break any operation with the resource (including delete).
			} catch (GenericFrameworkException ex) {
				LOGGER.error("Unable to complete {}: {}", new Object[]{resource, ex.getMessage(), ex});
				// Ignore the error. The resource is not complete but the upper layer code should deal with that
				// Throwing an error will effectively break any operation with the resource (including delete).
			} catch (ConfigurationException ex) {
				LOGGER.error("Unable to complete {}: {}", new Object[]{resource, ex.getMessage(), ex});
				// Ignore the error. The resource is not complete but the upper layer code should deal with that
				// Throwing an error will effectively break any operation with the resource (including delete).
			}
			if (resourceSchema == null) {
				LOGGER.warn("No resource schema generated for {}", resource);
			} else {
				LOGGER.debug("Generated resource schema for " + ObjectTypeUtil.toShortString(resource) + ": "
					+ resourceSchema.getDefinitions().size() + " definitions");
			}
		
		return resourceSchema;
	}

	// /**
	// * Make sure that the shadow is UCF-ready. That means that is has
	// ResourceAttributeContainer as <attributes>,
	// * has definition, etc.
	// */
	// private void convertToUcfShadow(PrismObject<ResourceObjectShadowType>
	// shadow, ResourceSchema resourceSchema) throws SchemaException {
	// PrismContainer<?> attributesContainer =
	// shadow.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
	// if (attributesContainer instanceof ResourceAttributeContainer) {
	// if (attributesContainer.getDefinition() != null) {
	// // Nothing to do. Everything is OK.
	// return;
	// } else {
	// // TODO: maybe we can apply the definition?
	// throw new
	// SchemaException("No definition for attributes container in "+shadow);
	// }
	// }
	// ObjectClassComplexTypeDefinition objectClassDefinition =
	// determineObjectClassDefinition(shadow, resourceSchema);
	// // We need to convert <attributes> to ResourceAttributeContainer
	// ResourceAttributeContainer convertedContainer =
	// ResourceAttributeContainer.convertFromContainer(attributesContainer,objectClassDefinition);
	// shadow.getValue().replace(attributesContainer, convertedContainer);
	// }
	//
	// private ObjectClassComplexTypeDefinition determineObjectClassDefinition(
	// PrismObject<ResourceObjectShadowType> shadow, ResourceSchema
	// resourceSchema) throws SchemaException {
	// QName objectClassName = shadow.asObjectable().getObjectClass();
	// if (objectClassName == null) {
	// throw new SchemaException("No object class specified in shadow " +
	// shadow);
	// }
	// ObjectClassComplexTypeDefinition objectClassDefinition =
	// resourceSchema.findObjectClassDefinition(objectClassName);
	// if (objectClassDefinition == null) {
	// throw new
	// SchemaException("No definition for object class "+objectClassName+" as specified in shadow "
	// + shadow);
	// }
	// return objectClassDefinition;
	// }

	public void deleteShadow(ResourceType resource, ResourceObjectShadowType shadow,
			Set<Operation> additionalOperations, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

//		ResourceSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);
		ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition objectClassDefinition = schema.findObjectClassDefinition(shadow
				.getObjectClass());

		ResourceObjectShadowUtil.fixShadow(shadow.asPrismObject(), schema);

		LOGGER.trace("Getting object identifiers");
		Collection<? extends ResourceAttribute<?>> identifiers = ResourceObjectShadowUtil
				.getIdentifiers(shadow);
		Collection<? extends ResourceAttribute<?>> attributes = ResourceObjectShadowUtil
				.getAttributes(shadow);

		if (isProtectedShadow(resource, objectClassDefinition, attributes)) {
			LOGGER.error("Attempt to delete protected resource object " + objectClassDefinition + ": "
					+ identifiers + "; ignoring the request");
			throw new SecurityViolationException("Cannot delete protected resource object "
					+ objectClassDefinition + ": " + identifiers);
		}

		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"Connector for resource {}\n DELETE object, object class {}, identified by:\n{}\n additional operations:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), shadow.getObjectClass(),
								SchemaDebugUtil.debugDump(identifiers),
								SchemaDebugUtil.debugDump(additionalOperations) });
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
			ResourceObjectShadowType shadow, Set<Operation> operations, String oid,
			Collection<? extends ItemDelta> objectChanges, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

//		ResourceSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);
		ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);

		ObjectClassComplexTypeDefinition objectClassDefinition = schema.findObjectClassDefinition(shadow
				.getObjectClass());

		ResourceObjectShadowUtil.fixShadow(shadow.asPrismObject(), schema);
		ResourceAttributeContainerDefinition resourceAttributeDefinition = ResourceObjectShadowUtil
				.getObjectClassDefinition(shadow);

		if (shadow instanceof AccountShadowType) {

			// Look for password change
			Operation passwordChangeOp = determinePasswordChange(objectChanges, shadow);
			if (passwordChangeOp != null) {
				operations.add(passwordChangeOp);
			}

			// look for activation change
			Operation activationOperation = determineActivationChange(objectChanges, resource,
					resourceAttributeDefinition);
			if (activationOperation != null) {
				operations.add(activationOperation);
			}
		}

		Collection<? extends ResourceAttribute<?>> identifiers = ResourceObjectShadowUtil
				.getIdentifiers(shadow);
		Collection<? extends ResourceAttribute<?>> attributes = ResourceObjectShadowUtil
				.getAttributes(shadow);

		if (isProtectedShadow(resource, objectClassDefinition, attributes)) {
			LOGGER.error("Attempt to modify protected resource object " + objectClassDefinition + ": "
					+ identifiers + "; ignoring the request");
			throw new SecurityViolationException("Cannot modify protected resource object "
					+ objectClassDefinition + ": " + identifiers);
		}

		getAttributeChanges(objectChanges, operations, objectClassDefinition);

	
		Set<PropertyModificationOperation> sideEffectChanges = null;
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"Connector for resource {}\n MODIFY object, object class {}, identified by:\n{}\n changes:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), shadow.getObjectClass(),
								SchemaDebugUtil.debugDump(identifiers), SchemaDebugUtil.debugDump(operations) });
			}

			//check idetifier if it is not null
			if (identifiers.isEmpty() && shadow.getFailedOperationType()!= null){
				throw new CommunicationException("Error communicating with connector");
			}
			
			// Invoke ICF
			sideEffectChanges = connector.modifyObject(objectClassDefinition, identifiers, operations,
					parentResult);

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
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Getting last token");
		ConnectorInstance connector = getConnectorInstance(resourceType, parentResult);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);
		// This is a HACK. It should not work only for default account, but also
		// for other objectclasses (FIXME)
		ObjectClassComplexTypeDefinition objectClass = resourceSchema.findDefaultAccountDefinition();
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

	public List<Change> fetchChanges(ResourceType resource, PrismProperty lastToken,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
			CommunicationException, ConfigurationException {
		Validate.notNull(resource, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		Validate.notNull(lastToken, "Token property must not be null.");

		LOGGER.trace("Shadow converter, START fetch changes");
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		// This is a HACK. It should not work only for default account, but also
		// for other objectclasses (FIXME)
		ObjectClassComplexTypeDefinition objectClass = resourceSchema.findDefaultAccountDefinition();

		// get changes from the connector
		List<Change> changes = null;
		try {
			changes = connector.fetchChanges(objectClass, lastToken, parentResult);

			// TODO: filter out changes of protected objects

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

		Iterator<Change> iterator = changes.iterator();
		while (iterator.hasNext()) {
			Change change = iterator.next();
			if (isProtectedShadowChange(resource, change)) {
				LOGGER.trace("Skipping change to a protected object: {}", change);
				iterator.remove();
			}
		}

		parentResult.recordSuccess();
		LOGGER.trace("Shadow converter, END fetch changes");
		return changes;
	}

	public ResourceObjectShadowType createNewAccountFromChange(Change change, ResourceType resource,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
			CommunicationException, GenericFrameworkException, ConfigurationException,
			SecurityViolationException {

		ResourceAttributeContainer resourceObject = null;

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

//		ResourceSchema schema = resourceTypeManager.getResourceSchema(resource, connector, parentResult);
		ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition rod = schema.findObjectClassDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resource),
				"AccountObjectClass"));

		ResourceObjectShadowType shadow = null;
		try {
			shadow = fetchResourceObject(ResourceObjectShadowType.class, rod, change.getIdentifiers(),
					connector, resource, parentResult);
		} catch (ObjectNotFoundException ex) {
			parentResult
					.recordPartialError("Object detected in change log no longer exist on the resource. Skipping processing this object.");
			LOGGER.warn("Object detected in change log no longer exist on the resource. Skipping processing this object "
					+ ex.getMessage());
			return null;
		}
		try {
			shadow = ShadowCacheUtil.completeShadow(shadow, null, resource, parentResult);
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
			ObjectClassComplexTypeDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute<?>> identifiers, ConnectorInstance connector,
			ResourceType resource, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, SecurityViolationException {

		if (isProtectedShadow(resource, objectClassDefinition, identifiers)) {
			LOGGER.error("Attempt to fetch protected resource object " + objectClassDefinition + ": "
					+ identifiers + "; ignoring the request");
			throw new SecurityViolationException("Cannot get protected resource object "
					+ objectClassDefinition + ": " + identifiers);
		}

		// Set<ResourceObjectAttribute> roIdentifiers = new
		// HashSet<ResourceObjectAttribute>();
		// for (Property p : identifiers) {
		// ResourceObjectAttribute roa = new
		// ResourceObjectAttribute(p.getName(), p.getDefinition(),
		// p.getValues());
		// roIdentifiers.add(roa);
		// }

		try {
			PrismObject<T> resourceObject = connector.fetchObject(type, objectClassDefinition, identifiers,
					true, null, parentResult);
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
			Collection<ResourceAttribute<?>> resourceAttributesAfterAdd) throws SchemaException {
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil
				.getAttributesContainer(shadow);
		for (ResourceAttribute attributeAfter : resourceAttributesAfterAdd) {
			ResourceAttribute attributeBefore = attributesContainer.findAttribute(attributeAfter.getName());
			if (attributeBefore != null) {
				attributesContainer.remove(attributeBefore);
			}
			attributesContainer.add(attributeAfter);
		}
	}

	private ConnectorInstance getConnectorInstance(ResourceType resource, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		return connectorTypeManager.getConfiguredConnectorInstance(resource, parentResult);
	}

	private Operation determineActivationChange(Collection<? extends ItemDelta> objectChange,
			ResourceType resource, ResourceAttributeContainerDefinition objectClassDefinition)
			throws SchemaException {

		PropertyDelta<Boolean> enabledPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				new PropertyPath(ResourceObjectShadowType.F_ACTIVATION, ActivationType.F_ENABLED));
		if (enabledPropertyDelta == null) {
			return null;
		}
		Boolean enabled = enabledPropertyDelta.getPropertyNew().getRealValue();
		LOGGER.trace("Find activation change to: {}", enabled);

		if (enabled != null) {

			LOGGER.trace("enabled not null.");
			if (!ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
				// if resource cannot do activation, resource should
				// have specified policies to do that
				PropertyModificationOperation activationAttribute = convertToActivationAttribute(resource,
						enabled, objectClassDefinition);
				// changes.add(activationAttribute);
				return activationAttribute;
			} else {
				// Navive activation, nothing special to do
				return new PropertyModificationOperation(enabledPropertyDelta);
			}

		}
		return null;
	}

	private Operation determinePasswordChange(Collection<? extends ItemDelta> objectChange,
			ResourceObjectShadowType objectType) throws SchemaException {
		// Look for password change

		PropertyDelta<PasswordType> passwordPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				new PropertyPath(AccountShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD));
		if (passwordPropertyDelta == null) {
			return null;
		}
		PasswordType newPasswordStructure = passwordPropertyDelta.getPropertyNew().getRealValue();

		PropertyModificationOperation passwordChangeOp = null;
		if (newPasswordStructure != null) {
			ProtectedStringType newPasswordPS = newPasswordStructure.getProtectedString();
			if (MiscSchemaUtil.isNullOrEmpty(newPasswordPS)) {
				throw new IllegalArgumentException(
						"ProtectedString is empty in an attempt to change password of "
								+ ObjectTypeUtil.toShortString(objectType));
			}
			passwordChangeOp = new PropertyModificationOperation(passwordPropertyDelta);
			// TODO: other things from the structure
			// changes.add(passwordChangeOp);
		}
		return passwordChangeOp;
	}

	private void getAttributeChanges(Collection<? extends ItemDelta> objectChange, Set<Operation> changes,
			ObjectClassComplexTypeDefinition rod) throws SchemaException {
		if (changes == null) {
			changes = new HashSet<Operation>();
		}
		for (ItemDelta itemDelta : objectChange) {
			if (itemDelta instanceof PropertyDelta) {
				// we need to skip activation change, because it was actually
				// processed
				if (itemDelta.getParentPath().equals(new PropertyPath(ResourceObjectShadowType.F_ACTIVATION))) {
					continue;
				}
				PropertyModificationOperation attributeModification = new PropertyModificationOperation(
						(PropertyDelta) itemDelta);
				changes.add(attributeModification);
			} else if (itemDelta instanceof ContainerDelta) {
				// skip the container delta - most probably password change - it
				// is processed earlier
				continue;
			} else {
				throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
			}
		}
		// return changes;
	}

	private PropertyModificationOperation convertToActivationAttribute(ResourceType resource,
			Boolean enabled, ResourceAttributeContainerDefinition objectClassDefinition)
			throws SchemaException {
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		if (activationCapability == null) {
			throw new SchemaException("Resource " + ObjectTypeUtil.toShortString(resource)
					+ " does not have native or simulated activation capability");
		}

		EnableDisable enableDisable = activationCapability.getEnableDisable();
		if (enableDisable == null) {
			throw new SchemaException("Resource " + ObjectTypeUtil.toShortString(resource)
					+ " does not have native or simulated activation/enableDisable capability");
		}

		QName enableAttributeName = enableDisable.getAttribute();
		LOGGER.debug("Simulated attribute name: {}", enableAttributeName);
		if (enableAttributeName == null) {
			throw new SchemaException(
					"Resource "
							+ ObjectTypeUtil.toShortString(resource)
							+ " does not have attribute specification for simulated activation/enableDisable capability");
		}

		ResourceAttributeDefinition enableAttributeDefinition = objectClassDefinition
				.findAttributeDefinition(enableAttributeName);
		if (enableAttributeDefinition == null) {
			throw new SchemaException("Resource " + ObjectTypeUtil.toShortString(resource)
					+ "  attribute for simulated activation/enableDisable capability" + enableAttributeName
					+ " in not present in the schema for objeclass " + objectClassDefinition);
		}

		PropertyDelta enableAttributeDelta = new PropertyDelta(new PropertyPath(
				ResourceObjectShadowType.F_ATTRIBUTES, enableAttributeName), enableAttributeDefinition);

		List<String> enableValues = enableDisable.getEnableValue();

		Iterator<String> i = enableValues.iterator();
		String enableValue = i.next();
		if ("".equals(enableValue)) {
			if (enableValues.size() < 2) {
				enableValue = "false";
			} else {
				enableValue = i.next();
			}
		}
		String disableValue = enableDisable.getDisableValue().iterator().next();
		if (enabled) {
			LOGGER.trace("enable attribute delta: {}", enableValue);
			enableAttributeDelta.setValueToReplace(new PrismPropertyValue(enableValue));
		} else {
			LOGGER.trace("enable attribute delta: {}", disableValue);
			enableAttributeDelta.setValueToReplace(new PrismPropertyValue(disableValue));
		}

		PropertyModificationOperation attributeChange = new PropertyModificationOperation(
				enableAttributeDelta);
		return attributeChange;
	}

	public <T extends ResourceObjectShadowType> boolean isProtectedShadow(ResourceType resource,
			PrismObject<T> shadow) throws SchemaException {
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil
				.getAttributesContainer(shadow);
		QName objectClass = shadow.asObjectable().getObjectClass();
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		return isProtectedShadow(resource, objectClass, attributes);
	}

	public boolean isProtectedShadow(ResourceType resource,
			ObjectClassComplexTypeDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute<?>> attributes) throws SchemaException {
		return isProtectedShadow(resource, objectClassDefinition.getTypeName(), attributes);
	}

	private boolean isProtectedShadowChange(ResourceType resource, Change change) throws SchemaException {
		PrismObject<? extends ResourceObjectShadowType> currentShadow = change.getCurrentShadow();
		if (currentShadow != null) {
			return isProtectedShadow(resource, currentShadow);
		}
		Collection<ResourceAttribute<?>> identifiers = change.getIdentifiers();
		return isProtectedShadow(resource, change.getObjectClassDefinition().getTypeName(), identifiers);
	}

	private boolean isProtectedShadow(ResourceType resource, QName objectClass,
			Collection<? extends ResourceAttribute<?>> attributes) throws SchemaException {
		// TODO: support also other types except account
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, prismContext);
		RefinedAccountDefinition refinedAccountDef = refinedSchema
				.findAccountDefinitionByObjectClass(objectClass);
		LOGGER.trace("isProtectedShadow: {} -> {}, {}", new Object[] { objectClass, refinedAccountDef,
				attributes });
		if (refinedAccountDef == null) {
			return false;
		}
		Collection<ResourceObjectPattern> protectedAccountPatterns = refinedAccountDef.getProtectedAccounts();
		if (protectedAccountPatterns == null) {
			return false;
		}
		return ResourceObjectPattern.matches(attributes, protectedAccountPatterns);
	}

}
