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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationEnableDisableCapabilityType;

@Component
public class ShadowConverter {
	
//	private static final ItemPath ATTRIBUTES_PATH = new ItemPath(ResourceObjectShadowType.F_ATTRIBUTES);

	@Autowired
	private ConnectorTypeManager connectorTypeManager;
	@Autowired
	private PrismContext prismContext;

	public ShadowConverter() {
	}

	public ConnectorTypeManager getConnectorTypeManager() {
		return connectorTypeManager;
	}

	public void setConnectorTypeManager(ConnectorTypeManager connectorTypeManager) {
		this.connectorTypeManager = connectorTypeManager;
	}

	private static final Trace LOGGER = TraceManager.getTrace(ShadowConverter.class);

	@SuppressWarnings("unchecked")
	public <T extends ResourceObjectShadowType> T getShadow(Class<T> type, ResourceType resource,
			T repoShadow, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, GenericConnectorException {

		
		ObjectClassComplexTypeDefinition objectClassDefinition = applyAttributesDefinition(repoShadow.asPrismObject(), resource);

	
		// Let's get all the identifiers from the Shadow <attributes> part
		Collection<? extends ResourceAttribute<?>> identifiers = ResourceObjectShadowUtil
				.getIdentifiers(repoShadow);

		if (identifiers == null || identifiers.isEmpty()) {
			//check if the account is not only partially created (exist only in repo so far)
			if (repoShadow.getFailedOperationType() != null) {
				throw new GenericConnectorException(
						"Unable to get account from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
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
		
		//try to apply changes to the account only if the resource if UP
		if (repoShadow.getObjectChange() != null && repoShadow.getFailedOperationType() != null
				&& resource.getOperationalState() != null
				&& resource.getOperationalState().getLastAvailabilityStatus() == AvailabilityStatusType.UP) {
			throw new GenericConnectorException(
					"Found changes that have been not applied to the account yet. Trying to apply them now.");
		}

		Collection<? extends ResourceAttribute<?>> attributes = ResourceObjectShadowUtil
				.getAttributes(repoShadow);

		if (isProtectedShadow(resource, objectClassDefinition, attributes)) {
			LOGGER.error("Attempt to fetch protected resource object " + objectClassDefinition + ": "
					+ identifiers + "; ignoring the request");
			throw new SecurityViolationException("Cannot get protected resource object "
					+ objectClassDefinition + ": " + identifiers);
		}

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);
		
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

	@SuppressWarnings("unchecked")
	public ResourceObjectShadowType addShadow(ResourceType resource, ResourceObjectShadowType shadowType,
			Set<Operation> additionalOperations, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException,
			ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		PrismObject<ResourceObjectShadowType> shadow = shadowType.asPrismObject();

//		ObjectClassComplexTypeDefinition objectClass = 
		applyAttributesDefinition(shadow, resource);
		
		Collection<ResourceAttribute<?>> resourceAttributesAfterAdd = null;

		if (isProtectedShadow(resource, shadow)) {
			LOGGER.error("Attempt to add protected shadow " + shadowType + "; ignoring the request");
			throw new SecurityViolationException("Cannot get protected shadow " + shadowType);
		}

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);
		
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("PROVISIONING ADD operation on resource {}\n ADD object:\n{}\n additional operations:\n{}",
						new Object[] { resource.asPrismObject(), shadowType.asPrismObject().debugDump(),
								SchemaDebugUtil.debugDump(additionalOperations,2) });
			}
			ResourceAttributeContainerDefinition resourceAttributeDefinition = ResourceObjectShadowUtil
					.getObjectClassDefinition(shadowType);
			checkActivationAttribute(shadowType, resource, resourceAttributeDefinition);
			
			resourceAttributesAfterAdd = connector.addObject(shadow, additionalOperations, parentResult);

			if (LOGGER.isDebugEnabled()) {
				// TODO: reduce only to new/different attributes. Dump all
				// attributes on trace level only
				LOGGER.debug("PROVISIONING ADD successful, returned attributes:\n{}",
						SchemaDebugUtil.prettyPrint(resourceAttributesAfterAdd));
			}

			applyAfterOperationAttributes(shadowType, resourceAttributesAfterAdd);
		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Could not create account on the resource. Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communicating with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Could not create account on the resource. Generic error in connector: " + ex.getMessage(), ex);
			throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
		} catch (ObjectAlreadyExistsException ex){
			parentResult.recordFatalError("Could not create account on the resource. Account already exists on the resource: " + ex.getMessage(), ex);
			throw new ObjectAlreadyExistsException("Account already exists on the resource: " + ex.getMessage(), ex);
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow being stored:\n{}", shadowType.asPrismObject().dump());
		}

		parentResult.recordSuccess();
		return shadowType;
	}


	@SuppressWarnings("unchecked")
	public void deleteShadow(ResourceType resource, ResourceObjectShadowType shadow,
			Set<Operation> additionalOperations, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

		ObjectClassComplexTypeDefinition objectClassDefinition = applyAttributesDefinition(shadow.asPrismObject(), resource);
		
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
		
		//check idetifier if it is not null
		if (identifiers.isEmpty() && shadow.getFailedOperationType()!= null){
			throw new GenericConnectorException(
					"Unable to delete account from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
		}


		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"PROVISIONING DELETE operation on resource {}\n DELETE object, object class {}, identified by:\n{}\n additional operations:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), shadow.getObjectClass(),
								SchemaDebugUtil.debugDump(identifiers),
								SchemaDebugUtil.debugDump(additionalOperations) });
			}

			connector.deleteObject(objectClassDefinition, additionalOperations, identifiers, parentResult);

			LOGGER.debug("PROVISIONING DELETE successful");
			parentResult.recordSuccess();

		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Can't delete object " + ObjectTypeUtil.toShortString(shadow)
					+ ". Reason: " + ex.getMessage(), ex);
			throw new ObjectNotFoundException("An error occured while deleting resource object " + shadow
					+ "whith identifiers " + identifiers + ": " + ex.getMessage(), ex);
		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communicating with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error in connector: " + ex.getMessage(), ex);
			throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Set<PropertyModificationOperation> modifyShadow(ResourceType resource,
			ResourceObjectShadowType shadow, Collection<Operation> operations,
			Collection<? extends ItemDelta> objectChanges, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		ObjectClassComplexTypeDefinition objectClassDefinition = applyAttributesDefinition(shadow.asPrismObject(), resource);
		
		ResourceAttributeContainerDefinition resourceAttributeDefinition = ResourceObjectShadowUtil
				.getObjectClassDefinition(shadow);


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
		
			getAttributeChanges(objectChanges, operations, resource, shadow, resourceAttributeDefinition);

		
		if (shadow.getFetchResult() != null){
			parentResult.addParam("shadow", shadow);
		}
		
		if (operations.isEmpty()){
			LOGGER.trace("No modifications for connector object specified. Skipping processing of modifyShadow.");
			parentResult.recordSuccess();
			return new HashSet<PropertyModificationOperation>();
		}
		
		//check idetifier if it is not null
		if (identifiers.isEmpty() && shadow.getFailedOperationType()!= null){
			throw new GenericConnectorException(
					"Unable to modify account in the resource. Probably it has not been created yet because of previous unavailability of the resource.");
		}
		
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);
		
		if (avoidDuplicateValues(resource)) {
			// We need to filter out the deltas that add duplicate values or remove values that are not there
			
			ResourceObjectShadowType currentShadow = fetchResourceObject(ResourceObjectShadowType.class, objectClassDefinition,
					identifiers, connector, resource, parentResult);
			Collection<Operation> filteredOperations = new ArrayList(operations.size());
			for (Operation origOperation: operations) {
				if (origOperation instanceof PropertyModificationOperation) {
					PropertyDelta<?> propertyDelta = ((PropertyModificationOperation)origOperation).getPropertyDelta();
					PropertyDelta<?> filteredDelta = propertyDelta.narrow(currentShadow.asPrismObject());
					if (filteredDelta != null && !filteredDelta.isEmpty()) {
						if (propertyDelta == filteredDelta) {
							filteredOperations.add(origOperation);
						} else {
							PropertyModificationOperation newOp = new PropertyModificationOperation(filteredDelta);
							filteredOperations.add(newOp);
						}
					}
				}
			}
			if (filteredOperations.isEmpty()){
				LOGGER.debug("No modifications for connector object specified (after filtering). Skipping processing.");
				parentResult.recordSuccess();
				return new HashSet<PropertyModificationOperation>();
			}
			operations = filteredOperations;
		}
	
		Set<PropertyModificationOperation> sideEffectChanges = null;
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"PROVISIONING MODIFY operation on resource {}\n MODIFY object, object class {}, identified by:\n{}\n changes:\n{}",
						new Object[] { ObjectTypeUtil.toShortString(resource), shadow.getObjectClass(),
								SchemaDebugUtil.debugDump(identifiers), SchemaDebugUtil.debugDump(operations) });
			}
			
			// Invoke ICF
			sideEffectChanges = connector.modifyObject(objectClassDefinition, identifiers, operations,
					parentResult);

			LOGGER.debug("PROVISIONING MODIFY successful, side-effect changes {}",
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

	
	public <T extends ResourceObjectShadowType> void searchObjects(ResourceType resourceType, ResourceSchema resourceSchema, QName objectClass,
			ResultHandler<T> resultHandler, ObjectQuery query, OperationResult parentResult) throws SchemaException,
			CommunicationException, ObjectNotFoundException, ConfigurationException {

		ObjectClassComplexTypeDefinition objectClassDef = resourceSchema.findObjectClassDefinition(objectClass);

		if (objectClassDef == null) {
			String message = "Object class " + objectClass + " is not defined in schema of "
					+ ObjectTypeUtil.toShortString(resourceType);
			LOGGER.error(message);
			parentResult.recordFatalError(message);
			throw new SchemaException(message);
		}
		
		ConnectorInstance connector = getConnectorInstance(resourceType, parentResult);

		try {
			connector.search(objectClassDef, query, resultHandler, parentResult);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError("Generic error in the connector: " + e.getMessage(), e);
			throw new SystemException("Generic error in the connector: " + e.getMessage(), e);

		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communicating with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		}

		parentResult.recordSuccess();

	}
	private boolean avoidDuplicateValues(ResourceType resource) {
		if (resource.getConsistency() == null) {
			return false;
		}
		if (resource.getConsistency().isAvoidDuplicateValues() == null) {
			return false;
		}
		return resource.getConsistency().isAvoidDuplicateValues();
	}

	@SuppressWarnings("rawtypes")
	public PrismProperty fetchCurrentToken(ResourceType resourceType, ResourceSchema resourceSchema, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(resourceSchema, "Resource schema must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Getting last token");
		ConnectorInstance connector = getConnectorInstance(resourceType, parentResult);

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

	@SuppressWarnings("rawtypes")
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

	public ResourceObjectShadowType createNewAccountFromChange(Change change, ResourceType resource, ResourceSchema resourceSchema,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
			CommunicationException, GenericFrameworkException, ConfigurationException,
			SecurityViolationException {

//		ResourceAttributeContainer resourceObject = null;

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);

//		ResourceSchema schema = resourceTypeManager.getResourceSchema(resource, parentResult);
		ObjectClassComplexTypeDefinition rod = resourceSchema.findObjectClassDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resource),
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

		try {
			PrismObject<T> resourceObject = connector.fetchObject(type, objectClassDefinition, identifiers,
					true, null, parentResult);
			return resourceObject.asObjectable();
		} catch (ObjectNotFoundException e) {
			parentResult.recordFatalError(
					"Object not found. Identifiers: " + identifiers + ". Reason: " + e.getMessage(), e);
//			parentResult.getLastSubresult().muteError();
			throw new ObjectNotFoundException("Object not found. Identifiers: " + identifiers + ". Reason: "
					+ e.getMessage(), e);
		} catch (CommunicationException e) {
			parentResult.recordFatalError("Error communication with the connector " + connector
					+ ": " + e.getMessage(), e);
//			parentResult.getLastSubresult().muteError();
			throw new CommunicationException("Error communication with the connector " + connector
					+ ": " + e.getMessage(), e);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError(
					"Generic error in the connector " + connector + ". Reason: " + e.getMessage(), e);
			throw new GenericConnectorException("Generic error in the connector " + connector + ". Reason: "
					+ e.getMessage(), e);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't get resource object, schema error: " + ex.getMessage(), ex);
			throw new SchemaException("Can't get resource object, schema error: " + ex.getMessage(), ex);
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
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
		return connectorTypeManager.getConfiguredConnectorInstance(resource, false, parentResult);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Operation determineActivationChange(ResourceObjectShadowType shadow, Collection<? extends ItemDelta> objectChange,
			ResourceType resource, ResourceAttributeContainerDefinition objectClassDefinition)
			throws SchemaException {

		PropertyDelta<Boolean> enabledPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				new ItemPath(ResourceObjectShadowType.F_ACTIVATION, ActivationType.F_ENABLED));
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
				PropertyModificationOperation activationAttribute = convertToActivationAttribute(shadow, resource,
						enabled, objectClassDefinition);
				return activationAttribute;
			} else {
				// Navive activation, nothing special to do
				return new PropertyModificationOperation(enabledPropertyDelta);
			}

		}
		return null;
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void checkActivationAttribute(ResourceObjectShadowType shadow, ResourceType resource,
			ResourceAttributeContainerDefinition objectClassDefinition) throws SchemaException {
		OperationResult result = new OperationResult("Checking activation attribute in the new shadow.");
		if (shadow instanceof AccountShadowType) {
			if (((AccountShadowType) shadow).getActivation() != null && shadow.getActivation().isEnabled() != null) {
				if (!ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
					ActivationEnableDisableCapabilityType enableDisable = getEnableDisableFromSimulatedActivation(
							shadow, resource, result);
					if (enableDisable == null) {
						throw new SchemaException("Attempt to change activation/enabled on "+resource+" that has neither native" +
								" nor simulated activation capability");
					}
					ResourceAttribute<?> activationSimulateAttribute = getSimulatedActivationAttribute(shadow, resource,
							objectClassDefinition, result);
					boolean enabled = shadow.getActivation().isEnabled().booleanValue();
					PrismPropertyValue activationValue = null;
					if (enabled) {
						activationValue = new PrismPropertyValue(getEnableValue(enableDisable));
					} else {
						activationValue = new PrismPropertyValue(getDisableValue(enableDisable));
					}
					activationSimulateAttribute.add(activationValue);

					PrismContainer attributesContainer =shadow.asPrismObject().findContainer(AccountShadowType.F_ATTRIBUTES);
					if (attributesContainer.findItem(activationSimulateAttribute.getName()) == null){
						attributesContainer.add(activationSimulateAttribute);
					} else{
						attributesContainer.findItem(activationSimulateAttribute.getName()).replace(activationSimulateAttribute.getValue());
					}
					shadow.setActivation(null);
				}
			}
		}
		
	}

//	private Operation determinePasswordChange(Collection<? extends ItemDelta> objectChange, ResourceObjectShadowType objectType) throws SchemaException {
//		// Look for password change
//
//		PropertyDelta<PasswordType> passwordPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
//				SchemaConstants.PATH_PASSWORD_VALUE);
//		if (passwordPropertyDelta == null) {
//			return null;
//		}
//		PasswordType newPasswordStructure = passwordPropertyDelta.getPropertyNew().getRealValue();
//
//		PropertyModificationOperation passwordChangeOp = null;
//		if (newPasswordStructure != null) {
//			ProtectedStringType newPasswordPS = newPasswordStructure.getValue();
//			if (MiscSchemaUtil.isNullOrEmpty(newPasswordPS)) {
//				throw new IllegalArgumentException(
//						"ProtectedString is empty in an attempt to change password of "
//								+ ObjectTypeUtil.toShortString(objectType));
//			}
//			passwordChangeOp = new PropertyModificationOperation(passwordPropertyDelta);
//			// TODO: other things from the structure
//			// changes.add(passwordChangeOp);
//		}
//		return passwordChangeOp;
//	}

	@SuppressWarnings("rawtypes")
	private void getAttributeChanges(Collection<? extends ItemDelta> objectChange, Collection<Operation> changes,
			ResourceType resource, ResourceObjectShadowType shadow, ResourceAttributeContainerDefinition objectClassDefinition) throws SchemaException {
	if (changes == null) {
			changes = new HashSet<Operation>();
		}
		for (ItemDelta itemDelta : objectChange) {
			if (new ItemPath(ResourceObjectShadowType.F_ATTRIBUTES).equals(itemDelta.getParentPath()) || SchemaConstants.PATH_PASSWORD.equals(itemDelta.getParentPath())) {
				if (itemDelta instanceof PropertyDelta) {
					PropertyModificationOperation attributeModification = new PropertyModificationOperation(
							(PropertyDelta) itemDelta);
					changes.add(attributeModification);
				} else if (itemDelta instanceof ContainerDelta) {
					// skip the container delta - most probably password change
					// - it is processed earlier
					continue;
				} else {
					throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
				}
			}else if (SchemaConstants.PATH_PASSWORD.equals(itemDelta.getParentPath())){
				//processed in the previous if clause
//				LOGGER.trace("Determinig password change");
//				Operation passwordOperation = determinePasswordChange(objectChange, shadow);
//				if (passwordOperation != null){
//					changes.add(passwordOperation);
//				}				
			}else if (SchemaConstants.PATH_ACTIVATION.equals(itemDelta.getParentPath())){
				Operation activationOperation = determineActivationChange(shadow, objectChange, resource, objectClassDefinition);
				LOGGER.trace("Determinig activation change");
				if (activationOperation != null){
					changes.add(activationOperation);
				}
			} else {
				LOGGER.trace("Skipp converting item delta: {}. It's not account change, but it it shadow change.", itemDelta);	
			}
			
			
		}
		// return changes;
	}
	
	private ActivationEnableDisableCapabilityType getEnableDisableFromSimulatedActivation(ResourceObjectShadowType shadow, ResourceType resource, OperationResult result){
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		if (activationCapability == null) {
			result.recordWarning("Resource " + ObjectTypeUtil.toShortString(resource)
					+ " does not have native or simulated activation capability. Processing of activation for account "+ ObjectTypeUtil.toShortString(shadow)+" was skipped");
			shadow.setFetchResult(result.createOperationResultType());
			return null;
		}

		ActivationEnableDisableCapabilityType enableDisable = activationCapability.getEnableDisable();
		if (enableDisable == null) {
			result.recordWarning("Resource " + ObjectTypeUtil.toShortString(resource)
					+ " does not have native or simulated activation/enableDisable capability. Processing of activation for account "+ ObjectTypeUtil.toShortString(shadow)+" was skipped");
			shadow.setFetchResult(result.createOperationResultType());
			return null;
		}
		return enableDisable;

	}
	
	private ResourceAttribute<?> getSimulatedActivationAttribute(ResourceObjectShadowType shadow, ResourceType resource, ResourceAttributeContainerDefinition objectClassDefinition, OperationResult result){
		
		ActivationEnableDisableCapabilityType enableDisable = getEnableDisableFromSimulatedActivation(shadow, resource, result);
		if (enableDisable == null){
			return null;
		}
		QName enableAttributeName = enableDisable.getAttribute();
		LOGGER.trace("Simulated attribute name: {}", enableAttributeName);
		if (enableAttributeName == null) {
			result.recordWarning("Resource "
							+ ObjectTypeUtil.toShortString(resource)
							+ " does not have attribute specification for simulated activation/enableDisable capability. Processing of activation for account "+ ObjectTypeUtil.toShortString(shadow)+" was skipped");
			shadow.setFetchResult(result.createOperationResultType());
			return null;
		}

		ResourceAttributeDefinition enableAttributeDefinition = objectClassDefinition
				.findAttributeDefinition(enableAttributeName);
		if (enableAttributeDefinition == null) {
			result.recordWarning("Resource " + ObjectTypeUtil.toShortString(resource)
					+ "  attribute for simulated activation/enableDisable capability" + enableAttributeName
					+ " in not present in the schema for objeclass " + objectClassDefinition+". Processing of activation for account "+ ObjectTypeUtil.toShortString(shadow)+" was skipped");
			shadow.setFetchResult(result.createOperationResultType());
			return null;
		}

		return enableAttributeDefinition.instantiate(enableAttributeName);

	}

	private PropertyModificationOperation convertToActivationAttribute(ResourceObjectShadowType shadow, ResourceType resource,
			Boolean enabled, ResourceAttributeContainerDefinition objectClassDefinition)
			throws SchemaException {
		OperationResult result = new OperationResult("Modify activation attribute.");

		ResourceAttribute<?> activationAttribute = getSimulatedActivationAttribute(shadow, resource, objectClassDefinition, result);
		if (activationAttribute == null){
			return null;
		}

		ActivationEnableDisableCapabilityType enableDisable = getEnableDisableFromSimulatedActivation(shadow, resource, result);
		if (enableDisable == null){
			return null;
		}
		
		PropertyDelta<?> enableAttributeDelta = null;
		
		if (enabled) {
			String enableValue = getEnableValue(enableDisable);
			LOGGER.trace("enable attribute delta: {}", enableValue);
			enableAttributeDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(
					ResourceObjectShadowType.F_ATTRIBUTES, activationAttribute.getName()), activationAttribute.getDefinition(), enableValue);
//			enableAttributeDelta.setValueToReplace(enableValue);
		} else {
			String disableValue = getDisableValue(enableDisable);
			LOGGER.trace("enable attribute delta: {}", disableValue);
			enableAttributeDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(
					ResourceObjectShadowType.F_ATTRIBUTES, activationAttribute.getName()), activationAttribute.getDefinition(), disableValue);
//			enableAttributeDelta.setValueToReplace(disableValue);
		}

		PropertyModificationOperation attributeChange = new PropertyModificationOperation(
				enableAttributeDelta);
		return attributeChange;
	}
	
	private String getDisableValue(ActivationEnableDisableCapabilityType enableDisable){
		//TODO some checks
		String disableValue = enableDisable.getDisableValue().iterator().next();
		return disableValue;
//		return new PrismPropertyValue(disableValue);
	}
	
	private String getEnableValue(ActivationEnableDisableCapabilityType enableDisable){
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
		return enableValue;
//		return new PrismPropertyValue(enableValue);
	}

	public <T extends ResourceObjectShadowType> boolean isProtectedShadow(ResourceType resource,
			PrismObject<T> shadow) throws SchemaException {
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil
				.getAttributesContainer(shadow);
		if (attributesContainer == null) {
			return false;
		}
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
	
	public <T extends ResourceObjectShadowType> ObjectClassComplexTypeDefinition applyAttributesDefinition(ObjectDelta<T> delta, 
			ResourceShadowDiscriminator discriminator, ResourceType resource) throws SchemaException, ConfigurationException {
		ObjectClassComplexTypeDefinition objectClassDefinition = determineObjectClassDefinition(discriminator, resource);
		return applyAttributesDefinition(delta, objectClassDefinition, resource);
	}
	
	public <T extends ResourceObjectShadowType> ObjectClassComplexTypeDefinition applyAttributesDefinition(ObjectDelta<T> delta, 
			PrismObject<T> shadow, ResourceType resource) throws SchemaException, ConfigurationException {
		ObjectClassComplexTypeDefinition objectClassDefinition = determineObjectClassDefinition(shadow, resource);
		return applyAttributesDefinition(delta, objectClassDefinition, resource);
	}

	private <T extends ResourceObjectShadowType> ObjectClassComplexTypeDefinition applyAttributesDefinition(ObjectDelta<T> delta, 
			ObjectClassComplexTypeDefinition objectClassDefinition, ResourceType resource) throws SchemaException, ConfigurationException {
		if (delta.isAdd()) {
			applyAttributesDefinition(delta.getObjectToAdd(), resource);
		} else if (delta.isModify()) {
			ItemPath attributesPath = new ItemPath(ResourceObjectShadowType.F_ATTRIBUTES);
			for(ItemDelta<?> modification: delta.getModifications()) {
				if (modification.getDefinition() == null && attributesPath.equals(modification.getParentPath())) {
					QName attributeName = modification.getName();
					ResourceAttributeDefinition attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
					if (attributeDefinition == null) {
						throw new SchemaException("No definition for attribute "+attributeName+" in object delta "+delta);
					}
					modification.applyDefinition(attributeDefinition);
				}
			}
		}

		return objectClassDefinition;
	}

	public <T extends ResourceObjectShadowType> ObjectClassComplexTypeDefinition applyAttributesDefinition(
			PrismObject<T> shadow, ResourceType resource) throws SchemaException, ConfigurationException {
		ObjectClassComplexTypeDefinition objectClassDefinition = determineObjectClassDefinition(shadow, resource);
		ResourceAttributeContainerDefinition attributesContainerDefinition = new ResourceAttributeContainerDefinition(ResourceObjectShadowType.F_ATTRIBUTES,
				objectClassDefinition, objectClassDefinition.getPrismContext());

		PrismContainer<?> attributesContainer = shadow.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
		if (attributesContainer != null) {
			if (attributesContainer instanceof ResourceAttributeContainer) {
				if (attributesContainer.getDefinition() == null) {
					attributesContainer.applyDefinition(attributesContainerDefinition);
				}
			} else {
				// We need to convert <attributes> to ResourceAttributeContainer
				ResourceAttributeContainer convertedContainer = ResourceAttributeContainer.convertFromContainer(
						attributesContainer, objectClassDefinition);
				shadow.getValue().replace(attributesContainer, convertedContainer);
			}
		}
		
		// We also need to replace the entire object definition to inject correct object class definition here
		// If we don't do this then the patch (delta.applyTo) will not work correctly because it will not be able to
		// create the attribute container if needed.

		PrismObjectDefinition<T> objectDefinition = shadow.getDefinition();
		PrismContainerDefinition<ResourceObjectShadowAttributesType> origAttrContainerDef = objectDefinition.findContainerDefinition(ResourceObjectShadowType.F_ATTRIBUTES);
		if (origAttrContainerDef == null || !(origAttrContainerDef instanceof ResourceAttributeContainerDefinition)) {
			PrismObjectDefinition<T> clonedDefinition = objectDefinition.cloneWithReplacedDefinition(ResourceObjectShadowType.F_ATTRIBUTES,
					attributesContainerDefinition);
			shadow.setDefinition(clonedDefinition);
		}
		
		return objectClassDefinition;
	}

	private <T extends ResourceObjectShadowType> ObjectClassComplexTypeDefinition determineObjectClassDefinition(PrismObject<T> shadow, ResourceType resource) throws SchemaException, ConfigurationException {
		ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		if (schema == null) {
			throw new ConfigurationException("No schema definied for "+resource);
		}
		QName objectClass = shadow.asObjectable().getObjectClass();
		if (objectClass == null) {
			throw new SchemaException("No objectclass definied in "+shadow);
		}
		
		ObjectClassComplexTypeDefinition objectClassDefinition = schema.findObjectClassDefinition(objectClass);
		if (objectClassDefinition == null) {
			// Unknown objectclass
			throw new SchemaException("Object class " + objectClass
					+ " defined in the repository shadow is not known in schema of " + resource);
		}
		
		return objectClassDefinition;
	}
	
	private <T extends ResourceObjectShadowType> ObjectClassComplexTypeDefinition determineObjectClassDefinition(
			ResourceShadowDiscriminator discriminator, ResourceType resource) throws SchemaException {
		ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition objectClassDefinition = schema.findAccountDefinition(discriminator.getIntent());

		if (objectClassDefinition == null) {
			// Unknown objectclass
			throw new SchemaException("Account type " + discriminator.getIntent()
					+ " is not known in schema of " + resource);
		}
		
		return objectClassDefinition;
	}
	
}
