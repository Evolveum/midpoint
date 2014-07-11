/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;

/**
 * 
 * Responsibilities:
 *     protected objects
 *     simulated activation
 *     script execution
 *     avoid duplicate values
 *     attributes returned by default/not returned by default
 *   
 * Limitations:
 *     must NOT access repository
 *     does not know about OIDs
 * 
 * @author Katarina Valalikova
 * @author Radovan Semancik
 *
 */
@Component
public class ResourceObjectConverter {
	
	@Autowired(required=true)
	private EntitlementConverter entitlementConverter;

	@Autowired(required=true)
	private MatchingRuleRegistry matchingRuleRegistry;

	@Autowired(required=true)
	private PrismContext prismContext;

	private PrismObjectDefinition<ShadowType> shadowTypeDefinition;

	private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectConverter.class);

	public static final String FULL_SHADOW_KEY = ResourceObjectConverter.class.getName()+".fullShadow";

	
	public PrismObject<ShadowType> getResourceObject(ConnectorInstance connector, ResourceType resource, 
			Collection<? extends ResourceAttribute<?>> identifiers,
			RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult)
					throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
					SecurityViolationException, GenericConnectorException {
		
		AttributesToReturn attributesToReturn = ProvisioningUtil.createAttributesToReturn(objectClassDefinition, resource);
		
		PrismObject<ShadowType> resourceShadow = fetchResourceObject(connector, resource, objectClassDefinition, identifiers, 
				attributesToReturn, parentResult);
		
		return resourceShadow;

	}
	
	/**
	 * Tries to get the object directly if primary identifiers are present. Tries to search for the object if they are not. 
	 */
	public PrismObject<ShadowType> locateResourceObject(ConnectorInstance connector, ResourceType resource, 
			Collection<? extends ResourceAttribute<?>> identifiers,
			RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, GenericConnectorException {

		AttributesToReturn attributesToReturn = ProvisioningUtil.createAttributesToReturn(objectClassDefinition, resource);
		
		if (hasAllIdentifiers(identifiers, objectClassDefinition)) {
			return fetchResourceObject(connector, resource, objectClassDefinition, identifiers, attributesToReturn, parentResult);
		} else {
			// Search
			Collection<? extends RefinedAttributeDefinition> secondaryIdentifierDefs = objectClassDefinition.getSecondaryIdentifiers();
			// Assume single secondary identifier for simplicity
			if (secondaryIdentifierDefs.size() > 1) {
				throw new UnsupportedOperationException("Composite secondary identifier is not supported yet");
			} else if (secondaryIdentifierDefs.isEmpty()) {
				throw new SchemaException("No secondary identifier defined, cannot search");
			}
			RefinedAttributeDefinition secondaryIdentifierDef = secondaryIdentifierDefs.iterator().next();
			ResourceAttribute<?> secondaryIdentifier = null;
			for (ResourceAttribute<?> identifier: identifiers) {
				if (identifier.getElementName().equals(secondaryIdentifierDef.getName())) {
					secondaryIdentifier = identifier;
				}
			}
			if (secondaryIdentifier == null) {
				throw new SchemaException("No secondary identifier present, cannot search. Identifiers: "+identifiers);
			}
			
			final ResourceAttribute<?> finalSecondaryIdentifier = secondaryIdentifier;
			
			ObjectFilter filter = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, secondaryIdentifierDef.getName()), secondaryIdentifierDef, secondaryIdentifier.getValue());
			ObjectQuery query = ObjectQuery.createObjectQuery(filter);
//			query.setFilter(filter);
			final Holder<PrismObject<ShadowType>> shadowHolder = new Holder<PrismObject<ShadowType>>();
			ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
				@Override
				public boolean handle(PrismObject<ShadowType> shadow) {
					if (!shadowHolder.isEmpty()) {
						throw new IllegalStateException("More than one value found for secondary identifier "+finalSecondaryIdentifier);
					}
					shadowHolder.setValue(shadow);
					return true;
				}
			};
			try {
				connector.search(objectClassDefinition, query, handler, attributesToReturn, parentResult);
				if (shadowHolder.isEmpty()) {
					throw new ObjectNotFoundException("No object found for secondary identifier "+secondaryIdentifier);
				}
				PrismObject<ShadowType> shadow = shadowHolder.getValue();
				return postProcessResourceObjectRead(connector, resource, shadow, objectClassDefinition, parentResult);
			} catch (GenericFrameworkException e) {
				throw new GenericConnectorException(e.getMessage(), e);
			}
		}

	}

	private boolean hasAllIdentifiers(Collection<? extends ResourceAttribute<?>> attributes,
			RefinedObjectClassDefinition objectClassDefinition) {
		Collection<? extends RefinedAttributeDefinition> identifierDefs = objectClassDefinition.getIdentifiers();
		for (RefinedAttributeDefinition identifierDef: identifierDefs) {
			boolean found = false;
			for(ResourceAttribute<?> attribute: attributes) {
				if (attribute.getElementName().equals(identifierDef.getName()) && !attribute.isEmpty()) {
					found = true;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	

	public PrismObject<ShadowType> addResourceObject(ConnectorInstance connector, ResourceType resource, 
			PrismObject<ShadowType> shadow, RefinedObjectClassDefinition objectClassDefinition, OperationProvisioningScriptsType scripts, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException,
			ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		// We might be modifying the shadow (e.g. for simulated capabilities). But we do not want the changes
		// to propagate back to the calling code. Hence the clone.
		PrismObject<ShadowType> shadowClone = shadow.clone();
		ShadowType shadowType = shadowClone.asObjectable();

		Collection<ResourceAttribute<?>> resourceAttributesAfterAdd = null;

		if (isProtectedShadow(resource, objectClassDefinition, shadowClone)) {
			LOGGER.error("Attempt to add protected shadow " + shadowType + "; ignoring the request");
			throw new SecurityViolationException("Cannot get protected shadow " + shadowType);
		}

		Collection<Operation> additionalOperations = new ArrayList<Operation>();
		addExecuteScriptOperation(additionalOperations, ProvisioningOperationTypeType.ADD, scripts, resource,
				parentResult);
		entitlementConverter.processEntitlementsAdd(resource, shadowClone, objectClassDefinition);
		
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("PROVISIONING ADD operation on resource {}\n ADD object:\n{}\n additional operations:\n{}",
						new Object[] { resource.asPrismObject(), shadowType.asPrismObject().debugDump(),
								SchemaDebugUtil.debugDump(additionalOperations,2) });
			}
			checkActivationAttribute(shadowType, resource, objectClassDefinition);
			
			if (!ResourceTypeUtil.hasCreateCapability(resource)){
				throw new UnsupportedOperationException("Resource does not support 'create' operation");
			}
			
			resourceAttributesAfterAdd = connector.addObject(shadowClone, additionalOperations, parentResult);

			if (LOGGER.isDebugEnabled()) {
				// TODO: reduce only to new/different attributes. Dump all
				// attributes on trace level only
				LOGGER.debug("PROVISIONING ADD successful, returned attributes:\n{}",
						SchemaDebugUtil.prettyPrint(resourceAttributesAfterAdd));
			}

			// Be careful not to apply this to the cloned shadow. This needs to be propagated
			// outside this method.
			applyAfterOperationAttributes(shadow, resourceAttributesAfterAdd);
		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Could not create account on the resource. Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communicating with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Could not create account on the resource. Generic error in connector: " + ex.getMessage(), ex);
//			LOGGER.info("Schema for add:\n{}",
//					DOMUtil.serializeDOMToString(ResourceTypeUtil.getResourceXsdSchema(resource)));
//			
			throw new GenericConnectorException("Generic error in connector: " + ex.getMessage(), ex);
		} catch (ObjectAlreadyExistsException ex){
			parentResult.recordFatalError("Could not create account on the resource. Account already exists on the resource: " + ex.getMessage(), ex);
			throw new ObjectAlreadyExistsException("Account already exists on the resource: " + ex.getMessage(), ex);
		} catch (ConfigurationException ex){
			parentResult.recordFatalError(ex);
			throw ex;
		} catch (RuntimeException ex) {
			parentResult.recordFatalError(ex);
			throw ex;
		}
		
		// Execute entitlement modification on other objects (if needed)
		executeEntitlementChangesAdd(connector, resource, objectClassDefinition, shadowClone, scripts, parentResult);

		parentResult.recordSuccess();
		return shadow;
	}

	public void deleteResourceObject(ConnectorInstance connector, ResourceType resource, 
			PrismObject<ShadowType> shadow, RefinedObjectClassDefinition objectClassDefinition,
			OperationProvisioningScriptsType scripts, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		LOGGER.trace("Getting object identifiers");
		Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil
				.getIdentifiers(shadow);

		if (isProtectedShadow(resource, objectClassDefinition, shadow)) {
			LOGGER.error("Attempt to delete protected resource object " + objectClassDefinition + ": "
					+ identifiers + "; ignoring the request");
			throw new SecurityViolationException("Cannot delete protected resource object "
					+ objectClassDefinition + ": " + identifiers);
		}
		
		//check idetifier if it is not null
		if (identifiers.isEmpty() && shadow.asObjectable().getFailedOperationType()!= null){
			throw new GenericConnectorException(
					"Unable to delete account from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
		}
		
		// Execute entitlement modification on other objects (if needed)
		executeEntitlementChangesDelete(connector, resource, objectClassDefinition, shadow, scripts, parentResult);

		Collection<Operation> additionalOperations = new ArrayList<Operation>();
		addExecuteScriptOperation(additionalOperations, ProvisioningOperationTypeType.DELETE, scripts, resource,
				parentResult);

		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"PROVISIONING DELETE operation on {}\n DELETE object, object class {}, identified by:\n{}\n additional operations:\n{}",
						new Object[] { resource, shadow.asObjectable().getObjectClass(),
								SchemaDebugUtil.debugDump(identifiers),
								SchemaDebugUtil.debugDump(additionalOperations) });
			}
			
			if (!ResourceTypeUtil.hasDeleteCapability(resource)){
				throw new UnsupportedOperationException("Resource does not support 'delete' operation");
			}

			connector.deleteObject(objectClassDefinition, additionalOperations, identifiers, parentResult);

			LOGGER.debug("PROVISIONING DELETE successful");
			parentResult.recordSuccess();

		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Can't delete object " + shadow
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
	
	public Collection<PropertyModificationOperation> modifyResourceObject(
			ConnectorInstance connector, ResourceType resource,
			RefinedObjectClassDefinition objectClassDefinition, PrismObject<ShadowType> shadow, OperationProvisioningScriptsType scripts,
			Collection<? extends ItemDelta> itemDeltas, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException, ObjectAlreadyExistsException {

		Collection<Operation> operations = new ArrayList<Operation>();
		
		Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(shadow);

		if (isProtectedShadow(resource, objectClassDefinition, shadow)) {
			if (hasChangesOnResource(itemDeltas)) {
				LOGGER.error("Attempt to modify protected resource object " + objectClassDefinition + ": "
						+ identifiers);
				throw new SecurityViolationException("Cannot modify protected resource object "
						+ objectClassDefinition + ": " + identifiers);
			} else {
				// Return immediately. This structure of the code makes sure that we do not execute any
				// resource operation for protected account even if there is a bug in the code below.
				LOGGER.trace("No resource modfications for protected resource object {}: {}; skipping",
						objectClassDefinition, identifiers);
				return null;
			}
		}

        /*
         *  State of the shadow before execution of the deltas - e.g. with original attributes, as it may be recorded in such a way in
         *  groups of which this account is a member of. (In case of object->subject associations.)
         *
         *  This is used when the resource does NOT provide referential integrity by itself. This is e.g. the case of OpenDJ with default
         *  settings.
         *
         *  On the contrary, AD and OpenDJ with referential integrity plugin do provide automatic referential integrity, so this feature is
         *  not needed.
         *
         *  We decide based on setting of explicitReferentialIntegrity in association definition.
         */
        PrismObject<ShadowType> shadowBefore = shadow.clone();

		collectAttributeAndEntitlementChanges(itemDeltas, operations, resource, shadow, objectClassDefinition);
		
		Collection<PropertyModificationOperation> sideEffectChanges = null;
				
		if (operations.isEmpty()){
			// We have to check BEFORE we add script operations, otherwise the check would be pointless
			LOGGER.trace("No modifications for connector object specified. Skipping processing of modifyShadow.");
		} else {
		
			// This must go after the skip check above. Otherwise the scripts would be executed even if there is no need to.
			addExecuteScriptOperation(operations, ProvisioningOperationTypeType.MODIFY, scripts, resource, parentResult);
			
			//check identifier if it is not null
			if (identifiers.isEmpty() && shadow.asObjectable().getFailedOperationType()!= null){
				throw new GenericConnectorException(
						"Unable to modify account in the resource. Probably it has not been created yet because of previous unavailability of the resource.");
			}
	
			// Execute primary ICF operation on this shadow
			sideEffectChanges = executeModify(connector, resource, objectClassDefinition, identifiers, operations, parentResult);
		}

        /*
         *  State of the shadow after execution of the deltas - e.g. with new DN (if it was part of the delta), because this one should be recorded
         *  in groups of which this account is a member of. (In case of object->subject associations.)
         */
        PrismObject<ShadowType> shadowAfter = shadow.clone();
        for (ItemDelta itemDelta : itemDeltas) {
            itemDelta.applyTo(shadowAfter);
        }

        // Execute entitlement modification on other objects (if needed)
		executeEntitlementChangesModify(connector, resource, objectClassDefinition, shadowBefore, shadowAfter, scripts, itemDeltas, parentResult);
		
		parentResult.recordSuccess();
		return sideEffectChanges;
	}

	private Collection<PropertyModificationOperation> executeModify(ConnectorInstance connector, ResourceType resource,
			RefinedObjectClassDefinition objectClassDefinition, Collection<? extends ResourceAttribute<?>> identifiers, 
					Collection<Operation> operations, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException {
		Collection<PropertyModificationOperation> sideEffectChanges = null;

		if (operations.isEmpty()){
			LOGGER.trace("No modifications for connector object. Skipping modification.");
		}
		
		// Invoke ICF
		try {
			
			if (avoidDuplicateValues(resource)) {
				// We need to filter out the deltas that add duplicate values or remove values that are not there
				
				PrismObject<ShadowType> currentShadow = fetchResourceObject(connector, resource, objectClassDefinition, identifiers, null, parentResult);
				Collection<Operation> filteredOperations = new ArrayList(operations.size());
				for (Operation origOperation: operations) {
					if (origOperation instanceof PropertyModificationOperation) {
						PropertyDelta<?> propertyDelta = ((PropertyModificationOperation)origOperation).getPropertyDelta();
						PropertyDelta<?> filteredDelta = propertyDelta.narrow(currentShadow);
						if (filteredDelta != null && !filteredDelta.isEmpty()) {
							if (propertyDelta == filteredDelta) {
								filteredOperations.add(origOperation);
							} else {
								PropertyModificationOperation newOp = new PropertyModificationOperation(filteredDelta);
								filteredOperations.add(newOp);
							}
						}
					}else if (origOperation instanceof ExecuteProvisioningScriptOperation){
						filteredOperations.add(origOperation);					
					}
				}
				if (filteredOperations.isEmpty()){
					LOGGER.debug("No modifications for connector object specified (after filtering). Skipping processing.");
					parentResult.recordSuccess();
					return new HashSet<PropertyModificationOperation>();
				}
				operations = filteredOperations;
			}
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"PROVISIONING MODIFY operation on {}\n MODIFY object, object class {}, identified by:\n{}\n changes:\n{}",
						new Object[] { resource, PrettyPrinter.prettyPrint(objectClassDefinition.getTypeName()),
								SchemaDebugUtil.debugDump(identifiers,1), SchemaDebugUtil.debugDump(operations,1) });
			}
			
			if (!ResourceTypeUtil.hasUpdateCapability(resource)){
				throw new UnsupportedOperationException("Resource does not support 'update' operation");
			}
			
			sideEffectChanges = connector.modifyObject(objectClassDefinition, identifiers, operations,
					parentResult);

		LOGGER.debug("PROVISIONING MODIFY successful, side-effect changes {}",
				SchemaDebugUtil.debugDump(sideEffectChanges));

		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Object to modify not found: " + ex.getMessage(), ex);
			throw new ObjectNotFoundException("Object to modify not found: " + ex.getMessage(), ex);
		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicationg with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error comminicationg with connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Schema violation: " + ex.getMessage(), ex);
			throw new SchemaException("Schema violation: " + ex.getMessage(), ex);
		} catch (SecurityViolationException ex) {
			parentResult.recordFatalError("Security violation: " + ex.getMessage(), ex);
			throw new SecurityViolationException("Security violation: " + ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError(
					"Generic error in the connector " + connector + ": " + ex.getMessage(), ex);
			throw new GenericConnectorException("Generic error in connector connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (ConfigurationException ex) {
			parentResult.recordFatalError("Configuration error: " + ex.getMessage(), ex);
			throw new ConfigurationException("Configuration error: " + ex.getMessage(), ex);
		} catch (ObjectAlreadyExistsException ex) {
			parentResult.recordFatalError("Conflict during modify: " + ex.getMessage(), ex);
			throw new ObjectAlreadyExistsException("Conflict during modify: " + ex.getMessage(), ex);
		}
		
		return sideEffectChanges;
	}
	
	private void executeEntitlementChangesAdd(ConnectorInstance connector, ResourceType resource,
			RefinedObjectClassDefinition objectClassDefinition, PrismObject<ShadowType> shadow, OperationProvisioningScriptsType scripts,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException {
		
		Map<ResourceObjectDiscriminator, Collection<Operation>> roMap = new HashMap<ResourceObjectDiscriminator, Collection<Operation>>();
		RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
		
		entitlementConverter.collectEntitlementsAsObjectOperationInShadowAdd(roMap, objectClassDefinition, shadow, rSchema, resource);
		
		executeEntitlements(connector, resource, roMap, parentResult);
		
	}
	
	private void executeEntitlementChangesModify(ConnectorInstance connector, ResourceType resource,
			RefinedObjectClassDefinition objectClassDefinition, PrismObject<ShadowType> shadowBefore, PrismObject<ShadowType> shadowAfter,
            OperationProvisioningScriptsType scripts, Collection<? extends ItemDelta> objectDeltas, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException {
		
		Map<ResourceObjectDiscriminator, Collection<Operation>> roMap = new HashMap<ResourceObjectDiscriminator, Collection<Operation>>();
		RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
		
		for (ItemDelta itemDelta : objectDeltas) {
			if (new ItemPath(ShadowType.F_ASSOCIATION).equals(itemDelta.getPath())) {
				ContainerDelta<ShadowAssociationType> containerDelta = (ContainerDelta<ShadowAssociationType>)itemDelta;				
				entitlementConverter.collectEntitlementsAsObjectOperation(roMap, containerDelta, objectClassDefinition,
                        shadowBefore, shadowAfter, rSchema, resource);
			}
		}
		
		executeEntitlements(connector, resource, roMap, parentResult);
		
	}
	
	private void executeEntitlementChangesDelete(ConnectorInstance connector, ResourceType resource,
			RefinedObjectClassDefinition objectClassDefinition, PrismObject<ShadowType> shadow, OperationProvisioningScriptsType scripts,
			OperationResult parentResult) throws SchemaException  {
		
		try {
			Map<ResourceObjectDiscriminator, Collection<Operation>> roMap = new HashMap<ResourceObjectDiscriminator, Collection<Operation>>();
			RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
				
			entitlementConverter.collectEntitlementsAsObjectOperationDelete(connector, roMap, objectClassDefinition,
					shadow, rSchema, resource, parentResult);
		
			executeEntitlements(connector, resource, roMap, parentResult);
			
		// TODO: now just log the errors, but not NOT re-throw the exception (except for some exceptions)
		// we want the original delete to take place, throwing an exception would spoil that
		} catch (SchemaException e) {
			throw e;
		} catch (CommunicationException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (ObjectNotFoundException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (SecurityViolationException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (ConfigurationException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (ObjectAlreadyExistsException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
	}
	
	private void executeEntitlements(ConnectorInstance connector, ResourceType resource,
			Map<ResourceObjectDiscriminator, Collection<Operation>> roMap, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectAlreadyExistsException {
		for (Entry<ResourceObjectDiscriminator,Collection<Operation>> entry: roMap.entrySet()) {
			ResourceObjectDiscriminator disc = entry.getKey();
			RefinedObjectClassDefinition ocDef = disc.getObjectClassDefinition();
			Collection<? extends ResourceAttribute<?>> identifiers = disc.getIdentifiers();
			Collection<Operation> operations = entry.getValue();
			
			// TODO: better handling of result, partial failures, etc.
			
			executeModify(connector, resource, ocDef, identifiers, operations, parentResult);
			
		}
	}

	public void searchResourceObjects(final ConnectorInstance connector, 
			final ResourceType resourceType, final RefinedObjectClassDefinition objectClassDef,
			final ResultHandler<ShadowType> resultHandler, ObjectQuery query, final OperationResult parentResult) throws SchemaException,
			CommunicationException, ObjectNotFoundException, ConfigurationException {
		
		AttributesToReturn attributesToReturn = ProvisioningUtil.createAttributesToReturn(objectClassDef, resourceType);
		
		ResultHandler<ShadowType> innerResultHandler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> shadow) {
				try {
					shadow = postProcessResourceObjectRead(connector, resourceType, shadow, objectClassDef, parentResult);
				} catch (SchemaException e) {
					throw new TunnelException(e);
				} catch (CommunicationException e) {
					throw new TunnelException(e);
				} catch (GenericFrameworkException e) {
					throw new TunnelException(e);
				}
				return resultHandler.handle(shadow);
			}
		};
		
		try {
			connector.search(objectClassDef, query, innerResultHandler, attributesToReturn, parentResult);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError("Generic error in the connector: " + e.getMessage(), e);
			throw new SystemException("Generic error in the connector: " + e.getMessage(), e);

		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
			throw new CommunicationException("Error communicating with the connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (TunnelException e) {
			Throwable cause = e.getCause();
			if (cause instanceof SchemaException) {
				throw (SchemaException)cause;
			} else if (cause instanceof CommunicationException) {
				throw (CommunicationException)cause;
			} if (cause instanceof GenericFrameworkException) {
				new GenericConnectorException(cause.getMessage(), cause);
			} else {
				new SystemException(cause.getMessage(), cause);
			}
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
	public PrismProperty fetchCurrentToken(ConnectorInstance connector, ResourceType resourceType, 
			ObjectClassComplexTypeDefinition objectClass, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(objectClass, "objectclass must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

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


	private PrismObject<ShadowType> fetchResourceObject(ConnectorInstance connector, ResourceType resource,
			RefinedObjectClassDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute<?>> identifiers, 
			AttributesToReturn attributesToReturn,
			OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, SecurityViolationException, ConfigurationException {

		try {
			
			if (!ResourceTypeUtil.hasReadCapability(resource)){
				throw new UnsupportedOperationException("Resource does not support 'read' operation");
			}
			
			PrismObject<ShadowType> resourceObject = connector.fetchObject(ShadowType.class, objectClassDefinition, identifiers,
					attributesToReturn, parentResult);
			return postProcessResourceObjectRead(connector, resource, resourceObject, objectClassDefinition, parentResult);
		} catch (ObjectNotFoundException e) {
			parentResult.recordFatalError(
					"Object not found. Identifiers: " + identifiers + ". Reason: " + e.getMessage(), e);
			throw new ObjectNotFoundException("Object not found. identifiers=" + identifiers + ", objectclass="+
						PrettyPrinter.prettyPrint(objectClassDefinition.getTypeName())+": "
					+ e.getMessage(), e);
		} catch (CommunicationException e) {
			parentResult.recordFatalError("Error communication with the connector " + connector
					+ ": " + e.getMessage(), e);
			throw e;
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError(
					"Generic error in the connector " + connector + ". Reason: " + e.getMessage(), e);
			throw new GenericConnectorException("Generic error in the connector " + connector + ". Reason: "
					+ e.getMessage(), e);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't get resource object, schema error: " + ex.getMessage(), ex);
			throw ex;
		} catch (ConfigurationException e) {
			parentResult.recordFatalError(e);
			throw e;
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void applyAfterOperationAttributes(PrismObject<ShadowType> shadow,
			Collection<ResourceAttribute<?>> resourceAttributesAfterAdd) throws SchemaException {
		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(shadow);
		for (ResourceAttribute attributeAfter : resourceAttributesAfterAdd) {
			ResourceAttribute attributeBefore = attributesContainer.findAttribute(attributeAfter.getElementName());
			if (attributeBefore != null) {
				attributesContainer.remove(attributeBefore);
			}
			if (!attributesContainer.contains(attributeAfter)) {
				attributesContainer.add(attributeAfter.clone());
			}
		}
	}

	private Collection<Operation> determineActivationChange(ShadowType shadow, Collection<? extends ItemDelta> objectChange,
			ResourceType resource, ObjectClassComplexTypeDefinition objectClassDefinition)
			throws SchemaException {

		Collection<Operation> operations = new ArrayList<Operation>();
		
		ActivationCapabilityType activationCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
		
		// administrativeStatus
		PropertyDelta<ActivationStatusType> enabledPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		if (enabledPropertyDelta != null) {
			if (activationCapabilityType == null) {
				throw new SchemaException("Attempt to change activation administrativeStatus on "+resource+" which does not have the capability");
			}
			ActivationStatusType status = enabledPropertyDelta.getPropertyNew().getRealValue();
			LOGGER.trace("Found activation administrativeStatus change to: {}", status);
	
//			if (status != null) {
	
				if (ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
					// Native activation, need to check if there is not also change to simulated activation which may be in conflict
					checkSimulatedActivation(objectChange, status, shadow, resource, objectClassDefinition);
					operations.add(new PropertyModificationOperation(enabledPropertyDelta));
				} else {
					// Try to simulate activation capability
					PropertyModificationOperation activationAttribute = convertToSimulatedActivationAttribute(enabledPropertyDelta, shadow, resource,
							status, objectClassDefinition);
					operations.add(activationAttribute);
				}	
//			}
		}
		
		// validFrom
		PropertyDelta<XMLGregorianCalendar> validFromPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				SchemaConstants.PATH_ACTIVATION_VALID_FROM);
		if (validFromPropertyDelta != null) {
			if (activationCapabilityType == null || activationCapabilityType.getValidFrom() == null) {
				throw new SchemaException("Attempt to change activation validFrom on "+resource+" which does not have the capability");
			}
			XMLGregorianCalendar xmlCal = validFromPropertyDelta.getPropertyNew().getRealValue();
			LOGGER.trace("Found activation validFrom change to: {}", xmlCal);
			if (xmlCal != null) {
				operations.add(new PropertyModificationOperation(validFromPropertyDelta));
			}
		}

		// validTo
		PropertyDelta<XMLGregorianCalendar> validToPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				SchemaConstants.PATH_ACTIVATION_VALID_TO);
		if (validToPropertyDelta != null) {
			if (activationCapabilityType == null || activationCapabilityType.getValidTo() == null) {
				throw new SchemaException("Attempt to change activation validTo on "+resource+" which does not have the capability");
			}
			XMLGregorianCalendar xmlCal = validToPropertyDelta.getPropertyNew().getRealValue();
			LOGGER.trace("Found activation validTo change to: {}", xmlCal);
			if (xmlCal != null) {
				operations.add(new PropertyModificationOperation(validToPropertyDelta));
			}
		}
		
		PropertyDelta<LockoutStatusType> lockoutPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
		if (lockoutPropertyDelta != null) {
			if (activationCapabilityType == null) {
				throw new SchemaException("Attempt to change activation lockoutStatus on "+resource+" which does not have the capability");
			}
			LockoutStatusType status = lockoutPropertyDelta.getPropertyNew().getRealValue();
			LOGGER.trace("Found activation lockoutStatus change to: {}", status);

			// TODO: simulated
			if (ResourceTypeUtil.hasResourceNativeActivationLockoutCapability(resource)) {
				// Native lockout, need to check if there is not also change to simulated activation which may be in conflict
//				checkSimulatedActivation(objectChange, status, shadow, resource, objectClassDefinition);
				operations.add(new PropertyModificationOperation(lockoutPropertyDelta));
			} else {
				// Try to simulate activation capability
				
				// TODO
//				PropertyModificationOperation activationAttribute = convertToSimulatedActivationAttribute(lockoutPropertyDelta, shadow, resource,
//						status, objectClassDefinition);
//				operations.add(activationAttribute);
			}	
		}
		
		return operations;
	}
	
	private void checkSimulatedActivation(Collection<? extends ItemDelta> objectChange, ActivationStatusType status, ShadowType shadow, ResourceType resource, ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException{
		if (!ResourceTypeUtil.hasResourceConfiguredActivationCapability(resource)) {
			//nothing to do, resource does not have simulated activation, so there can be no conflict, continue in processing
			return;
		}
		
		OperationResult result = new OperationResult("Modify activation attribute.");
		
		ResourceAttribute<?> activationAttribute = getSimulatedActivationAttribute(shadow, resource, objectClassDefinition, result);
		if (activationAttribute == null){
			return;
		}
		
		PropertyDelta simulatedActivationDelta = PropertyDelta.findPropertyDelta(objectChange, activationAttribute.getPath());
		PrismProperty simulatedAcviationProperty = simulatedActivationDelta.getPropertyNew();
		Collection realValues = simulatedAcviationProperty.getRealValues();
		if (realValues.isEmpty()){
			//nothing to do, no value for simulatedActivation
			return;
		}
		
		if (realValues.size() > 1){
			throw new SchemaException("Found more than one value for simulated activation.");
		}
		
		Object simluatedActivationValue = realValues.iterator().next();
		boolean transformedValue = getTransformedValue(shadow, resource, simluatedActivationValue, result);
		
		if (transformedValue && status == ActivationStatusType.ENABLED){
			//this is ok, simulated value and also value for native capability resulted to the same vale
		} else{
			throw new SchemaException("Found conflicting change for activation. Simulated activation resulted to " + transformedValue +", but native activation resulted to " + status);
		}
		
	}
	
	private boolean getTransformedValue(ShadowType shadow, ResourceType resource, Object simulatedValue, OperationResult result) throws SchemaException{
		ActivationStatusCapabilityType capActStatus = getActivationStatusFromSimulatedActivation(shadow, resource, result);
		List<String> disableValues = capActStatus.getDisableValue();
		for (String disable : disableValues){
			if (disable.equals(simulatedValue)){
				return false; 
			}
		}
		
		List<String> enableValues = capActStatus.getEnableValue();
		for (String enable : enableValues){
			if (enable.equals(simulatedValue)){
				return true;
			}
		}
		
		throw new SchemaException("Could not map value for simulated activation: " + simulatedValue + " neither to enable nor disable values.");		
	}
	
	private void checkActivationAttribute(ShadowType shadow, ResourceType resource,
			ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException {
		OperationResult result = new OperationResult("Checking activation attribute in the new shadow.");
		if (shadow.getActivation() != null && shadow.getActivation().getAdministrativeStatus() != null) {
			if (!ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
				ActivationStatusCapabilityType capActStatus = getActivationStatusFromSimulatedActivation(
						shadow, resource, result);
				if (capActStatus == null) {
					throw new SchemaException("Attempt to change activation/enabled on "+resource+" that has neither native" +
							" nor simulated activation capability");
				}
				ResourceAttribute<?> activationSimulateAttribute = getSimulatedActivationAttribute(shadow, resource,
						objectClassDefinition, result);
				if (activationSimulateAttribute == null) {
					return;
				}
				ActivationStatusType status = shadow.getActivation().getAdministrativeStatus();
				PrismPropertyValue activationValue = null;
				if (status == ActivationStatusType.ENABLED) {
					activationValue = new PrismPropertyValue(getEnableValue(capActStatus));
				} else {
					activationValue = new PrismPropertyValue(getDisableValue(capActStatus));
				}
				activationSimulateAttribute.add(activationValue);

				PrismContainer attributesContainer =shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
				if (attributesContainer.findItem(activationSimulateAttribute.getElementName()) == null){
					attributesContainer.add(activationSimulateAttribute);
				} else{
					attributesContainer.findItem(activationSimulateAttribute.getElementName()).replace(activationSimulateAttribute.getValue());
				}
				shadow.setActivation(null);
			}
		}		
	}
	
	private boolean hasChangesOnResource(
			Collection<? extends ItemDelta> itemDeltas) {
		for (ItemDelta itemDelta : itemDeltas) {
			if (new ItemPath(ShadowType.F_ATTRIBUTES).equals(itemDelta.getParentPath()) || SchemaConstants.PATH_PASSWORD.equals(itemDelta.getParentPath())) {
				return true;
			} else if (SchemaConstants.PATH_ACTIVATION.equals(itemDelta.getParentPath())){
				return true;
			} else if (new ItemPath(ShadowType.F_ASSOCIATION).equals(itemDelta.getPath())) { 
				return true;				
			}
		}
		return false;
	}


	private void collectAttributeAndEntitlementChanges(Collection<? extends ItemDelta> objectChange,
			Collection<Operation> operations, ResourceType resource, PrismObject<ShadowType> shadow,
			RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
		if (operations == null) {
			operations = new ArrayList<Operation>();
		}
		for (ItemDelta itemDelta : objectChange) {
			if (new ItemPath(ShadowType.F_ATTRIBUTES).equals(itemDelta.getParentPath()) || SchemaConstants.PATH_PASSWORD.equals(itemDelta.getParentPath())) {
				if (itemDelta instanceof PropertyDelta) {
					PropertyModificationOperation attributeModification = new PropertyModificationOperation(
							(PropertyDelta) itemDelta);
					operations.add(attributeModification);
				} else if (itemDelta instanceof ContainerDelta) {
					// skip the container delta - most probably password change
					// - it is processed earlier
					continue;
				} else {
					throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
				}
			} else if (SchemaConstants.PATH_ACTIVATION.equals(itemDelta.getParentPath())){
				Collection<Operation> activationOperations = determineActivationChange(shadow.asObjectable(), objectChange, resource, objectClassDefinition);
				if (activationOperations != null){
					operations.addAll(activationOperations);
				}
			} else if (new ItemPath(ShadowType.F_ASSOCIATION).equals(itemDelta.getPath())) { 
				if (itemDelta instanceof ContainerDelta) {
					entitlementConverter.collectEntitlementChange((ContainerDelta<ShadowAssociationType>)itemDelta, operations, objectClassDefinition, resource);
				} else {
					throw new UnsupportedOperationException("Not supported delta: " + itemDelta);
				}				
			} else {
				LOGGER.trace("Skipp converting item delta: {}. It's not account change, but it it shadow change.", itemDelta);	
			}
			
		}
	}
		
	public List<Change<ShadowType>> fetchChanges(ConnectorInstance connector, ResourceType resource,
			RefinedObjectClassDefinition objectClass, PrismProperty<?> lastToken,
			OperationResult parentResult) throws SchemaException,
			CommunicationException, ConfigurationException, SecurityViolationException, GenericFrameworkException {
		Validate.notNull(resource, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("START fetch changes");

		AttributesToReturn attrsToReturn = ProvisioningUtil.createAttributesToReturn(objectClass, resource);
		
		// get changes from the connector
		List<Change<ShadowType>> changes = connector.fetchChanges(objectClass, lastToken, attrsToReturn, parentResult);

		Iterator<Change<ShadowType>> iterator = changes.iterator();
		while (iterator.hasNext()) {
			Change<ShadowType> change = iterator.next();
			if (change.getCurrentShadow() == null) {
				// There is no current shadow in a change. Add it by fetching it explicitly.
				if (change.getObjectDelta() == null || !change.getObjectDelta().isDelete()) {						
					// but not if it is a delete event
					try {
						
						PrismObject<ShadowType> currentShadow = fetchResourceObject(connector, resource, objectClass, 
								change.getIdentifiers(), attrsToReturn, parentResult);
						change.setCurrentShadow(currentShadow);
						
					} catch (ObjectNotFoundException ex) {
						parentResult.recordHandledError(
								"Object detected in change log no longer exist on the resource. Skipping processing this object.", ex);
						LOGGER.warn("Object detected in change log no longer exist on the resource. Skipping processing this object "
								+ ex.getMessage());
						// TODO: Maybe change to DELETE instead of this?
						iterator.remove();
						continue;
					}
				}
			} else {
				PrismObject<ShadowType> currentShadow = postProcessResourceObjectRead(connector, resource, change.getCurrentShadow(), 
						objectClass, parentResult);
				change.setCurrentShadow(currentShadow);
			}
		}

		parentResult.recordSuccess();
		LOGGER.trace("END fetch changes ({} changes)", changes == null ? "null" : changes.size());
		return changes;
	}
	
	/**
	 * Process simulated activation, credentials and other properties that are added to the object by midPoint. 
	 */
	private PrismObject<ShadowType> postProcessResourceObjectRead(ConnectorInstance connector, ResourceType resourceType,
			PrismObject<ShadowType> resourceObject, RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult) throws SchemaException, CommunicationException, GenericFrameworkException {
		
		ShadowType resourceObjectType = resourceObject.asObjectable();
		setProtectedFlag(resourceType, objectClassDefinition, resourceObject);
		
		// Simulated Activation
		// FIXME??? when there are not native capabilities for activation, the
		// resourceShadow.getActivation is null and the activation for the repo
		// shadow are not completed..therefore there need to be one more check,
		// we must check not only if the activation is null, but if it is, also
		// if the shadow doesn't have defined simulated activation capability
		if (resourceObjectType.getActivation() != null || ResourceTypeUtil.hasActivationCapability(resourceType)) {
			ActivationType activationType = completeActivation(resourceObject, resourceType, parentResult);
			LOGGER.trace("Determined activation: {}",
					activationType == null ? "null activationType" : activationType.getAdministrativeStatus());
			resourceObjectType.setActivation(activationType);
		} else {
			resourceObjectType.setActivation(null);
		}
		
		// Entitlements
		entitlementConverter.postProcessEntitlementsRead(connector, resourceType, resourceObject, objectClassDefinition, parentResult);
		
		return resourceObject;
	}
	
	public void setProtectedFlag(ResourceType resourceType, RefinedObjectClassDefinition rOcDef, PrismObject<ShadowType> resourceObject) throws SchemaException {
		if (isProtectedShadow(resourceType, rOcDef, resourceObject)) {
			resourceObject.asObjectable().setProtectedObject(true);
		}
	}

	/**
	 * Completes activation state by determinig simulated activation if
	 * necessary.
	 * 
	 * TODO: The placement of this method is not correct. It should go back to
	 * ShadowConverter
	 */
	private ActivationType completeActivation(PrismObject<ShadowType> shadow, ResourceType resource,
			OperationResult parentResult) {

		if (ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
			return shadow.asObjectable().getActivation();
		} else if (ResourceTypeUtil.hasActivationCapability(resource)) {
			return convertFromSimulatedActivationAttributes(resource, shadow, parentResult);
		} else {
			// No activation capability, nothing to do
			return null;
		}
	}
	
	private static ActivationType convertFromSimulatedActivationAttributes(ResourceType resource,
			PrismObject<ShadowType> shadow, OperationResult parentResult) {
		// LOGGER.trace("Start converting activation type from simulated activation atribute");
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		if (activationCapability == null) {
			return null;
		}
		
		ActivationType activationType = new ActivationType();
		
		converFromSimulatedActivationAdministrativeStatus(activationType, activationCapability, resource, shadow, parentResult);
		
		return activationType;
	}

	private static ActivationStatusCapabilityType getStatusCapability(ResourceType resource, ActivationCapabilityType activationCapability) {
		ActivationStatusCapabilityType statusCapabilityType = activationCapability.getStatus();
		if (statusCapabilityType != null) {
			return statusCapabilityType;
		}
		return null;
	}
	
	private static void converFromSimulatedActivationAdministrativeStatus(ActivationType activationType, ActivationCapabilityType activationCapability,
			ResourceType resource, PrismObject<ShadowType> shadow, OperationResult parentResult) {
		
		ActivationStatusCapabilityType statusCapabilityType = getStatusCapability(resource, activationCapability);
		
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);		
		ResourceAttribute<?> activationProperty = null;
		if (statusCapabilityType != null && statusCapabilityType.getAttribute() != null) {
			activationProperty = attributesContainer.findAttribute(statusCapabilityType.getAttribute());
		}
		
		// LOGGER.trace("activation property: {}", activationProperty.dump());
		// if (activationProperty == null) {
		// LOGGER.debug("No simulated activation attribute was defined for the account.");
		// return null;
		// }

		Collection<Object> activationValues = null;
		if (activationProperty != null) {
			activationValues = activationProperty.getRealValues(Object.class);
		}
		
		converFromSimulatedActivationAdministrativeStatusInternal(activationType, statusCapabilityType, resource, activationValues, parentResult);
		
		LOGGER.trace(
				"Detected simulated activation attribute {} on {} with value {}, resolved into {}",
				new Object[] { SchemaDebugUtil.prettyPrint(statusCapabilityType.getAttribute()),
						ObjectTypeUtil.toShortString(resource), activationValues,
						activationType == null ? "null" : activationType.getAdministrativeStatus() });
		
		// Remove the attribute which is the source of simulated activation. If we leave it there then we
		// will have two ways to set activation.
		if (statusCapabilityType.isIgnoreAttribute() == null
				|| statusCapabilityType.isIgnoreAttribute().booleanValue()) {
			if (activationProperty != null) {
				attributesContainer.remove(activationProperty);
			}
		}
	}
	
	/**
	 * Moved to a separate method especially to enable good logging (see above). 
	 */
	private static void converFromSimulatedActivationAdministrativeStatusInternal(ActivationType activationType, ActivationStatusCapabilityType statusCapabilityType,
				ResourceType resource, Collection<Object> activationValues, OperationResult parentResult) {
		
		List<String> disableValues = statusCapabilityType.getDisableValue();
		List<String> enableValues = statusCapabilityType.getEnableValue();		

		if (MiscUtil.isNoValue(activationValues)) {

			if (MiscUtil.hasNoValue(disableValues)) {
				activationType.setAdministrativeStatus(ActivationStatusType.DISABLED);
				return;
			}

			if (MiscUtil.hasNoValue(enableValues)) {
				activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
				return;
			}

			// No activation information.
			LOGGER.warn("The {} does not provide definition for null value of simulated activation attribute",
					ObjectTypeUtil.toShortString(resource));
			if (parentResult != null) {
				parentResult.recordPartialError("The " + ObjectTypeUtil.toShortString(resource)
						+ " has native activation capability but noes not provide value for DISABLE attribute");
			}

			return;

		} else {
			if (activationValues.size() > 1) {
				LOGGER.warn("The {} provides {} values for DISABLE attribute, expecting just one value",
						disableValues.size(), ObjectTypeUtil.toShortString(resource));
				if (parentResult != null) {
					parentResult.recordPartialError("The " + ObjectTypeUtil.toShortString(resource) + " provides "
							+ disableValues.size() + " values for DISABLE attribute, expecting just one value");
				}
			}
			Object disableObj = activationValues.iterator().next();

			for (String disable : disableValues) {
				if (disable.equals(String.valueOf(disableObj))) {
					activationType.setAdministrativeStatus(ActivationStatusType.DISABLED);
					return;
				}
			}

			for (String enable : enableValues) {
				if ("".equals(enable) || enable.equals(String.valueOf(disableObj))) {
					activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
					return;
				}
			}
		}

	}

	private ActivationStatusCapabilityType getActivationStatusFromSimulatedActivation(ShadowType shadow, ResourceType resource, OperationResult result){
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		if (activationCapability == null) {
			result.recordWarning("Resource " + resource
					+ " does not have native or simulated activation capability. Processing of activation for account "+ shadow +" was skipped");
			shadow.setFetchResult(result.createOperationResultType());
			return null;
		}

		ActivationStatusCapabilityType capActStatus = getStatusCapability(resource, activationCapability);
		if (capActStatus == null) {
			result.recordWarning("Resource " + resource
					+ " does not have native or simulated activation status capability. Processing of activation for account "+ shadow +" was skipped");
			shadow.setFetchResult(result.createOperationResultType());
			return null;
		}
		return capActStatus;

	}
	
	private ResourceAttribute<?> getSimulatedActivationAttribute(ShadowType shadow, ResourceType resource, ObjectClassComplexTypeDefinition objectClassDefinition, OperationResult result){
		
		ActivationStatusCapabilityType capActStatus = getActivationStatusFromSimulatedActivation(shadow, resource, result);
		if (capActStatus == null){
			return null;
		}
		QName enableAttributeName = capActStatus.getAttribute();
		LOGGER.trace("Simulated attribute name: {}", enableAttributeName);
		if (enableAttributeName == null) {
			result.recordWarning("Resource "
							+ ObjectTypeUtil.toShortString(resource)
							+ " does not have attribute specification for simulated activation status capability. Processing of activation for account "+ shadow +" was skipped");
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

	private PropertyModificationOperation convertToSimulatedActivationAttribute(PropertyDelta activationDelta, ShadowType shadow, ResourceType resource,
			ActivationStatusType status, ObjectClassComplexTypeDefinition objectClassDefinition)
			throws SchemaException {
		OperationResult result = new OperationResult("Modify activation attribute.");

		ActivationStatusCapabilityType capActStatus = getActivationStatusFromSimulatedActivation(shadow, resource, result);
		if (capActStatus == null){
			throw new SchemaException("Attempt to modify activation on "+resource+" which does not have activation capability");
		}
		
		ResourceAttribute<?> activationAttribute = getSimulatedActivationAttribute(shadow, resource, objectClassDefinition, result);
		if (activationAttribute == null){
			return null;
		}
		
		PropertyDelta<?> enableAttributeDelta = null;
		
		if (status == null && activationDelta.isDelete()){
			LOGGER.trace("deleting activation property.");
			enableAttributeDelta = PropertyDelta.createModificationDeleteProperty(new ItemPath(ShadowType.F_ATTRIBUTES, activationAttribute.getElementName()), activationAttribute.getDefinition(), activationAttribute.getRealValue());
			
		} else if (status == ActivationStatusType.ENABLED) {
			String enableValue = getEnableValue(capActStatus);
			LOGGER.trace("enable attribute delta: {}", enableValue);
			enableAttributeDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(
					ShadowType.F_ATTRIBUTES, activationAttribute.getElementName()), activationAttribute.getDefinition(), enableValue);
		} else {
			String disableValue = getDisableValue(capActStatus);
			LOGGER.trace("enable attribute delta: {}", disableValue);
			enableAttributeDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(
					ShadowType.F_ATTRIBUTES, activationAttribute.getElementName()), activationAttribute.getDefinition(), disableValue);
		}

		PropertyModificationOperation attributeChange = new PropertyModificationOperation(
				enableAttributeDelta);
		return attributeChange;
	}
	
	private String getDisableValue(ActivationStatusCapabilityType capActStatus){
		//TODO some checks
		String disableValue = capActStatus.getDisableValue().iterator().next();
		return disableValue;
//		return new PrismPropertyValue(disableValue);
	}
	
	private String getEnableValue(ActivationStatusCapabilityType capActStatus){
		List<String> enableValues = capActStatus.getEnableValue();

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

	private RefinedObjectClassDefinition determineObjectClassDefinition(PrismObject<ShadowType> shadow, ResourceType resource) throws SchemaException, ConfigurationException {
		ShadowType shadowType = shadow.asObjectable();
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, prismContext);
		if (refinedSchema == null) {
			throw new ConfigurationException("No schema definied for "+resource);
		}
		
		
		RefinedObjectClassDefinition objectClassDefinition = null;
		ShadowKindType kind = shadowType.getKind();
		String intent = shadowType.getIntent();
		QName objectClass = shadow.asObjectable().getObjectClass();
		if (kind != null) {
			objectClassDefinition = refinedSchema.getRefinedDefinition(kind, intent);
		} else {
			// Fallback to objectclass only
			if (objectClass == null) {
				throw new SchemaException("No kind nor objectclass definied in "+shadow);
			}
			objectClassDefinition = refinedSchema.findRefinedDefinitionByObjectClassQName(null, objectClass);
		}
		
		if (objectClassDefinition == null) {
			throw new SchemaException("Definition for "+shadow+" not found (objectClass=" + PrettyPrinter.prettyPrint(objectClass) +
					", kind="+kind+", intent='"+intent+"') in schema of " + resource);
		}		
		
		return objectClassDefinition;
	}
	
	private ObjectClassComplexTypeDefinition determineObjectClassDefinition(
			ResourceShadowDiscriminator discriminator, ResourceType resource) throws SchemaException {
		ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		// HACK FIXME
		ObjectClassComplexTypeDefinition objectClassDefinition = schema.findObjectClassDefinition(ShadowKindType.ACCOUNT, discriminator.getIntent());

		if (objectClassDefinition == null) {
			// Unknown objectclass
			throw new SchemaException("Account type " + discriminator.getIntent()
					+ " is not known in schema of " + resource);
		}
		
		return objectClassDefinition;
	}
	
	private void addExecuteScriptOperation(Collection<Operation> operations, ProvisioningOperationTypeType type,
			OperationProvisioningScriptsType scripts, ResourceType resource, OperationResult result) throws SchemaException {
		if (scripts == null) {
			// No warning needed, this is quite normal
			LOGGER.trace("Skipping creating script operation to execute. Scripts was not defined.");
			return;
		}

		for (OperationProvisioningScriptType script : scripts.getScript()) {
			for (ProvisioningOperationTypeType operationType : script.getOperation()) {
				if (type.equals(operationType)) {
					ExecuteProvisioningScriptOperation scriptOperation = ProvisioningUtil.convertToScriptOperation(
							script, "script value for " + operationType + " in " + resource, prismContext);

					scriptOperation.setScriptOrder(script.getOrder());

					LOGGER.trace("Created script operation: {}", SchemaDebugUtil.prettyPrint(scriptOperation));
					operations.add(scriptOperation);
				}
			}
		}
	}
	
	private boolean isProtectedShadow(ResourceType resource, RefinedObjectClassDefinition objectClassDefinition,
			PrismObject<ShadowType> shadow) throws SchemaException {
		boolean isProtected = false;
		if (objectClassDefinition == null) {
			isProtected = false;
		} else {
			Collection<ResourceObjectPattern> protectedAccountPatterns = objectClassDefinition.getProtectedObjectPatterns();
			if (protectedAccountPatterns == null) {
				isProtected = false;
			} else {
				isProtected = ResourceObjectPattern.matches(shadow, protectedAccountPatterns, matchingRuleRegistry);
			}
		}
		LOGGER.trace("isProtectedShadow: {}: {} = {}", new Object[] { objectClassDefinition,
				shadow, isProtected });
		return isProtected;
	}
}
