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
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
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
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationEnableDisableCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;

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
public class ResouceObjectConverter {
	
	private static final QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");

	@Autowired
	private PrismContext prismContext;

	private static final Trace LOGGER = TraceManager.getTrace(ResouceObjectConverter.class);

	
	public <T extends ShadowType> PrismObject<T> getResourceObject(ConnectorInstance connector, ResourceType resource, 
			Class<T> type, Collection<? extends ResourceAttribute<?>> identifiers,
			RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, GenericConnectorException {

		AttributesToReturn attributesToReturn = createAttributesToReturn(type, objectClassDefinition, identifiers, resource);
		
		PrismObject<T> resourceShadow = fetchResourceObject(connector, resource, type, objectClassDefinition, identifiers, 
				attributesToReturn, parentResult);
		
		return resourceShadow;

	}

	private AttributesToReturn createAttributesToReturn(Class<?> type, RefinedObjectClassDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute<?>> identifiers, ResourceType resource) throws SchemaException {
		boolean apply = false;
		AttributesToReturn attributesToReturn = new AttributesToReturn();
		attributesToReturn.setReturnDefaultAttributes(true);
		
		// Attributes
		Collection<ResourceAttributeDefinition> explicit = new ArrayList<ResourceAttributeDefinition>();
		for (RefinedAttributeDefinition attributeDefinition: objectClassDefinition.getAttributeDefinitions()) {
			AttributeFetchStrategyType fetchStrategy = attributeDefinition.getFetchStrategy();
			if (fetchStrategy != null && fetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
				explicit.add(attributeDefinition);
			}
		}
		
		if (!explicit.isEmpty()) {
			attributesToReturn.setAttributesToReturn(explicit);
			apply = true;
		}
		
		// Password
		CredentialsCapabilityType credentialsCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
		if (CapabilityUtil.isPasswordReturnedByDefault(credentialsCapabilityType)) {
			// There resource is capable of returning password but it does not do it by default
			AttributeFetchStrategyType passwordFetchStrategy = objectClassDefinition.getPasswordFetchStrategy();
			if (passwordFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
				attributesToReturn.setReturnPasswordExplicit(true);
				apply = true;
			}
		}
		
		// Activation/enabled
		ActivationCapabilityType activationCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
		if (CapabilityUtil.isEnabledReturnedByDefault(activationCapabilityType)) {
			// There resource is capable of returning enable flag but it does not do it by default
			AttributeFetchStrategyType enableFetchStrategy = objectClassDefinition.getActivationEnableFetchStrategy();
			if (enableFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
				attributesToReturn.setReturnEnabledExplicit(true);
				apply = true;
			}
		}
		
		if (apply) {
			return attributesToReturn;
		} else {
			return null;
		}
	}

	public <T extends ShadowType> PrismObject<T> addResourceObject(ConnectorInstance connector, ResourceType resource, 
			PrismObject<T> shadow, ProvisioningScriptsType scripts, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException,
			ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		T shadowType = shadow.asObjectable();

		Collection<ResourceAttribute<?>> resourceAttributesAfterAdd = null;

		if (isProtectedShadow(resource, shadow)) {
			LOGGER.error("Attempt to add protected shadow " + shadowType + "; ignoring the request");
			throw new SecurityViolationException("Cannot get protected shadow " + shadowType);
		}

		Collection<Operation> additionalOperations = new ArrayList<Operation>();
		addExecuteScriptOperation(additionalOperations, ProvisioningOperationTypeType.ADD, scripts, resource,
				parentResult);
		
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("PROVISIONING ADD operation on resource {}\n ADD object:\n{}\n additional operations:\n{}",
						new Object[] { resource.asPrismObject(), shadowType.asPrismObject().debugDump(),
								SchemaDebugUtil.debugDump(additionalOperations,2) });
			}
			ObjectClassComplexTypeDefinition objectClassDefinition = ResourceObjectShadowUtil
					.getObjectClassDefinition(shadowType);
			checkActivationAttribute(shadowType, resource, objectClassDefinition);
			
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
		} catch (ConfigurationException ex){
			parentResult.recordFatalError(ex);
			throw ex;
		} catch (RuntimeException ex) {
			parentResult.recordFatalError(ex);
			throw ex;
		}
		
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow being stored:\n{}", shadowType.asPrismObject().dump());
		}

		parentResult.recordSuccess();
		return shadow;
	}

	public <T extends ShadowType> void deleteResourceObject(ConnectorInstance connector, ResourceType resource, 
			PrismObject<T> shadow,
			ObjectClassComplexTypeDefinition objectClassDefinition,
			ProvisioningScriptsType scripts, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {

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
		if (identifiers.isEmpty() && shadow.asObjectable().getFailedOperationType()!= null){
			throw new GenericConnectorException(
					"Unable to delete account from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
		}

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
	
	public <T extends ShadowType> Collection<PropertyModificationOperation> modifyResourceObject(
			ConnectorInstance connector, ResourceType resource,
			ObjectClassComplexTypeDefinition objectClassDefinition, PrismObject<T> shadow, ProvisioningScriptsType scripts,
			Collection<? extends ItemDelta> objectChanges, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		Collection<Operation> operations = new ArrayList<Operation>();
		
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
		
		getAttributeChanges(objectChanges, operations, resource, shadow, objectClassDefinition);
		
		if (operations.isEmpty()){
			LOGGER.trace("No modifications for connector object specified. Skipping processing of modifyShadow.");
			parentResult.recordSuccess();
			return new ArrayList<PropertyModificationOperation>(0);
		}
		
		// This must go after the skip check above. Otherwise the scripts would be executed even if there is no need to.
		addExecuteScriptOperation(operations, ProvisioningOperationTypeType.MODIFY, scripts, resource, parentResult);
		
		//check identifier if it is not null
		if (identifiers.isEmpty() && shadow.asObjectable().getFailedOperationType()!= null){
			throw new GenericConnectorException(
					"Unable to modify account in the resource. Probably it has not been created yet because of previous unavailability of the resource.");
		}
		
		if (avoidDuplicateValues(resource)) {
			// We need to filter out the deltas that add duplicate values or remove values that are not there
			
			PrismObject<ShadowType> currentShadow = fetchResourceObject(connector, resource, 
					ShadowType.class, objectClassDefinition, identifiers, null, parentResult);
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
	
		Collection<PropertyModificationOperation> sideEffectChanges = null;
		try {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"PROVISIONING MODIFY operation on resource {}\n MODIFY object, object class {}, identified by:\n{}\n changes:\n{}",
						new Object[] { resource, shadow.asObjectable().getObjectClass(),
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
			throw new GenericConnectorException("Generic error in connector connector " + connector + ": "
					+ ex.getMessage(), ex);
		}
		
		parentResult.recordSuccess();
		return sideEffectChanges;
	}

	
	public <T extends ShadowType> void searchResourceObjects(ConnectorInstance connector, 
			final ResourceType resourceType, RefinedObjectClassDefinition objectClassDef,
			final ResultHandler<T> resultHandler, ObjectQuery query, final OperationResult parentResult) throws SchemaException,
			CommunicationException, ObjectNotFoundException, ConfigurationException {
		
		ResultHandler<T> innerResultHandler = new ResultHandler<T>() {
			@Override
			public boolean handle(PrismObject<T> shadow) {
				try {
					shadow = handleResourceObjectRead(resourceType, shadow, parentResult);
				} catch (SchemaException e) {
					throw new SystemException(e.getMessage(), e);
				}
				return resultHandler.handle(shadow);
			}
		};
		
		try {
			connector.search(objectClassDef, query, innerResultHandler, parentResult);
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


	private <T extends ShadowType> PrismObject<T> fetchResourceObject(ConnectorInstance connector, ResourceType resource,
			Class<T> type, ObjectClassComplexTypeDefinition objectClassDefinition,
			Collection<? extends ResourceAttribute<?>> identifiers, 
			AttributesToReturn attributesToReturn,
			OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, SecurityViolationException, ConfigurationException {

		try {
			PrismObject<T> resourceObject = connector.fetchObject(type, objectClassDefinition, identifiers,
					attributesToReturn, parentResult);
			return handleResourceObjectRead(resource, resourceObject, parentResult);
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
		} catch (ConfigurationException e) {
			parentResult.recordFatalError(e);
			throw e;
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void applyAfterOperationAttributes(ShadowType shadow,
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

	private Operation determineActivationChange(ShadowType shadow, Collection<? extends ItemDelta> objectChange,
			ResourceType resource, ObjectClassComplexTypeDefinition objectClassDefinition)
			throws SchemaException {

		PropertyDelta<Boolean> enabledPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ENABLED));
		if (enabledPropertyDelta == null) {
			return null;
		}
		Boolean enabled = enabledPropertyDelta.getPropertyNew().getRealValue();
		LOGGER.trace("Find activation change to: {}", enabled);

		if (enabled != null) {

			LOGGER.trace("enabled not null.");
			if (!ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
				// Try to simulate activation capability
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
	
	private void checkActivationAttribute(ShadowType shadow, ResourceType resource,
			ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException {
		OperationResult result = new OperationResult("Checking activation attribute in the new shadow.");
		if (shadow.getActivation() != null && shadow.getActivation().isEnabled() != null) {
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

				PrismContainer attributesContainer =shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
				if (attributesContainer.findItem(activationSimulateAttribute.getName()) == null){
					attributesContainer.add(activationSimulateAttribute);
				} else{
					attributesContainer.findItem(activationSimulateAttribute.getName()).replace(activationSimulateAttribute.getValue());
				}
				shadow.setActivation(null);
			}
		}		
	}

	private <T extends ShadowType> void getAttributeChanges(Collection<? extends ItemDelta> objectChange, 
			Collection<Operation> changes, ResourceType resource, PrismObject<T> shadow, 
			ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException {
	if (changes == null) {
			changes = new HashSet<Operation>();
		}
		for (ItemDelta itemDelta : objectChange) {
			if (new ItemPath(ShadowType.F_ATTRIBUTES).equals(itemDelta.getParentPath()) || SchemaConstants.PATH_PASSWORD.equals(itemDelta.getParentPath())) {
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
			} else if (SchemaConstants.PATH_ACTIVATION.equals(itemDelta.getParentPath())){
				Operation activationOperation = determineActivationChange(shadow.asObjectable(), objectChange, resource, objectClassDefinition);
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
	
	public <T extends ShadowType> List<Change<T>> fetchChanges(ConnectorInstance connector, ResourceType resource, 
			Class<T> type, ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
			CommunicationException, ConfigurationException, SecurityViolationException, GenericFrameworkException {
		Validate.notNull(resource, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Shadow converter, START fetch changes");

		// get changes from the connector
		List<Change<T>> changes = connector.fetchChanges(objectClass, lastToken, parentResult);

		Iterator<Change<T>> iterator = changes.iterator();
		while (iterator.hasNext()) {
			Change<T> change = iterator.next();
			if (change.getCurrentShadow() == null) {
				// There is no current shadow in a change. Add it by fetching it explicitly.
				if (change.getObjectDelta() == null || !change.getObjectDelta().isDelete()) {						
					// but not if it is a delete event
					try {
						
						PrismObject<T> currentShadow = fetchResourceObject(connector, resource, type, objectClass, 
								change.getIdentifiers(), null, parentResult);
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
				PrismObject<T> currentShadow = handleResourceObjectRead(resource, change.getCurrentShadow(), parentResult);
				change.setCurrentShadow(currentShadow);
			}
		}

		parentResult.recordSuccess();
		LOGGER.trace("Shadow converter, END fetch changes");
		return changes;
	}
	
	
	private <T extends ShadowType> PrismObject<T> handleResourceObjectRead(ResourceType resourceType,
			PrismObject<T> resourceObject, OperationResult parentResult) throws SchemaException {
		
		T resourceObjectType = resourceObject.asObjectable();
		
		// Protected object
		if (isProtectedShadow(resourceType, resourceObject)) {
			resourceObjectType.setProtectedObject(true);
		}
		
		// Simulated Activation
		// FIXME??? when there are not native capabilities for activation, the
		// resourceShadow.getActivation is null and the activation for the repo
		// shadow are not completed..therefore there need to be one more check,
		// we must chceck not only if the activation is null, but if it is, also
		// if the shadow doesn't have defined simulated activation capability
		if (resourceObjectType.getActivation() != null || ResourceTypeUtil.hasActivationCapability(resourceType)) {
			ActivationType activationType = completeActivation(resourceObject, resourceType, parentResult);
			LOGGER.trace("Determined activation: {}",
					activationType == null ? "null activationType" : activationType.isEnabled());
			resourceObjectType.setActivation(activationType);
		} else {
			resourceObjectType.setActivation(null);
		}
		
		return resourceObject;
	}
	
	/**
	 * Completes activation state by determinig simulated activation if
	 * necessary.
	 * 
	 * TODO: The placement of this method is not correct. It should go back to
	 * ShadowConverter
	 */
	private <T extends ShadowType> ActivationType completeActivation(PrismObject<T> shadow, ResourceType resource,
			OperationResult parentResult) {

		if (ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
			return shadow.asObjectable().getActivation();
		} else if (ResourceTypeUtil.hasActivationCapability(resource)) {
			return convertFromSimulatedActivationAttributes(shadow, resource, parentResult);
		} else {
			// No activation capability, nothing to do
			return null;
		}
	}
	
	private <T extends ShadowType> ActivationType convertFromSimulatedActivationAttributes(
			PrismObject<T> shadow, ResourceType resource, OperationResult parentResult) {
		// LOGGER.trace("Start converting activation type from simulated activation atribute");
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(shadow);

		ResourceAttribute<?> activationProperty = attributesContainer.findAttribute(activationCapability
				.getEnableDisable().getAttribute());
		// LOGGER.trace("activation property: {}", activationProperty.dump());
		// if (activationProperty == null) {
		// LOGGER.debug("No simulated activation attribute was defined for the account.");
		// return null;
		// }

		Collection<Object> values = null;

		if (activationProperty != null) {
			values = activationProperty.getRealValues(Object.class);
		}
		ActivationType activation = convertFromSimulatedActivationValues(resource, values, parentResult);
		LOGGER.trace(
				"Detected simulated activation attribute {} on {} with value {}, resolved into {}",
				new Object[] { SchemaDebugUtil.prettyPrint(activationCapability.getEnableDisable().getAttribute()),
						ObjectTypeUtil.toShortString(resource), values,
						activation == null ? "null" : activation.isEnabled() });
		
		// TODO: make this optional
		// Remove the attribute which is the source of simulated activation. If we leave it there then we
		// will have two ways to set activation.
		if (activationProperty != null) {
			attributesContainer.remove(activationProperty);
		}
		
		return activation;

	}
	
	private static ActivationType convertFromSimulatedActivationAttributes(ResourceType resource,
			ShadowType shadow, OperationResult parentResult) {
		// LOGGER.trace("Start converting activation type from simulated activation atribute");
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		QName enableDisableAttribute = activationCapability.getEnableDisable().getAttribute();
		List<Object> values = ResourceObjectShadowUtil.getAttributeValues(shadow, enableDisableAttribute);
		ActivationType activation = convertFromSimulatedActivationValues(resource, values, parentResult);
		LOGGER.trace(
				"Detected simulated activation attribute {} on {} with value {}, resolved into {}",
				new Object[] { SchemaDebugUtil.prettyPrint(activationCapability.getEnableDisable().getAttribute()),
						ObjectTypeUtil.toShortString(resource), values,
						activation == null ? "null" : activation.isEnabled() });
		return activation;
	}

	private static ActivationType convertFromSimulatedActivationValues(ResourceType resource,
			Collection<Object> activationValues, OperationResult parentResult) {

		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		if (activationCapability == null) {
			return null;
		}

		List<String> disableValues = activationCapability.getEnableDisable().getDisableValue();
		List<String> enableValues = activationCapability.getEnableDisable().getEnableValue();

		ActivationType activationType = new ActivationType();

		if (MiscUtil.isNoValue(activationValues)) {

			if (MiscUtil.hasNoValue(disableValues)) {
				activationType.setEnabled(false);
				return activationType;
			}

			if (MiscUtil.hasNoValue(enableValues)) {
				activationType.setEnabled(true);
				return activationType;
			}

			// No activation information.
			LOGGER.warn("The {} does not provide definition for null value of simulated activation attribute",
					ObjectTypeUtil.toShortString(resource));
			if (parentResult != null) {
				parentResult.recordPartialError("The " + ObjectTypeUtil.toShortString(resource)
						+ " has native activation capability but noes not provide value for DISABLE attribute");
			}

			return null;

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

		return null;
	}
	
	private ActivationEnableDisableCapabilityType getEnableDisableFromSimulatedActivation(ShadowType shadow, ResourceType resource, OperationResult result){
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
	
	private ResourceAttribute<?> getSimulatedActivationAttribute(ShadowType shadow, ResourceType resource, ObjectClassComplexTypeDefinition objectClassDefinition, OperationResult result){
		
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

	private PropertyModificationOperation convertToActivationAttribute(ShadowType shadow, ResourceType resource,
			Boolean enabled, ObjectClassComplexTypeDefinition objectClassDefinition)
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
					ShadowType.F_ATTRIBUTES, activationAttribute.getName()), activationAttribute.getDefinition(), enableValue);
//			enableAttributeDelta.setValueToReplace(enableValue);
		} else {
			String disableValue = getDisableValue(enableDisable);
			LOGGER.trace("enable attribute delta: {}", disableValue);
			enableAttributeDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(
					ShadowType.F_ATTRIBUTES, activationAttribute.getName()), activationAttribute.getDefinition(), disableValue);
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

	private <T extends ShadowType> RefinedObjectClassDefinition determineObjectClassDefinition(PrismObject<T> shadow, ResourceType resource) throws SchemaException, ConfigurationException {
		T shadowType = shadow.asObjectable();
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
	
	private <T extends ShadowType> ObjectClassComplexTypeDefinition determineObjectClassDefinition(
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
			ProvisioningScriptsType scripts, ResourceType resource, OperationResult result) throws SchemaException {
		if (scripts == null) {
			// No warning needed, this is quite normal
			LOGGER.trace("Skipping creating script operation to execute. Scripts was not defined.");
			return;
		}

		PrismPropertyDefinition scriptArgumentDefinition = new PrismPropertyDefinition(FAKE_SCRIPT_ARGUMENT_NAME,
				FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING, prismContext);
		for (ProvisioningScriptType script : scripts.getScript()) {
			for (ProvisioningOperationTypeType operationType : script.getOperation()) {
				if (type.equals(operationType)) {
					ExecuteProvisioningScriptOperation scriptOperation = new ExecuteProvisioningScriptOperation();

					for (ProvisioningScriptArgumentType argument : script.getArgument()) {
						ExecuteScriptArgument arg = new ExecuteScriptArgument(argument.getName(),
								Mapping.getStaticOutput(argument, scriptArgumentDefinition,
										"script value for " + operationType + " in " + resource, 
										ExpressionReturnMultiplicityType.SINGLE, prismContext));
						scriptOperation.getArgument().add(arg);
					}

					scriptOperation.setLanguage(script.getLanguage());
					scriptOperation.setTextCode(script.getCode());

					scriptOperation.setScriptOrder(script.getOrder());

					if (script.getHost().equals(ProvisioningScriptHostType.CONNECTOR)) {
						scriptOperation.setConnectorHost(true);
						scriptOperation.setResourceHost(false);
					}
					if (script.getHost().equals(ProvisioningScriptHostType.RESOURCE)) {
						scriptOperation.setConnectorHost(false);
						scriptOperation.setResourceHost(true);
					}
					LOGGER.trace("Created script operation: {}", SchemaDebugUtil.prettyPrint(scriptOperation));
					operations.add(scriptOperation);
				}
			}
		}
	}
	
	public <T extends ShadowType> boolean isProtectedShadow(ResourceType resource,
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
		PrismObject<? extends ShadowType> currentShadow = change.getCurrentShadow();
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
		// HACK FIXME
		RefinedObjectClassDefinition refinedAccountDef = refinedSchema
				.findRefinedDefinitionByObjectClassQName(ShadowKindType.ACCOUNT, objectClass);
		LOGGER.trace("isProtectedShadow: {} -> {}, {}", new Object[] { objectClass, refinedAccountDef,
				attributes });
		if (refinedAccountDef == null) {
			return false;
		}
		Collection<ResourceObjectPattern> protectedAccountPatterns = refinedAccountDef.getProtectedObjectPatterns();
		if (protectedAccountPatterns == null) {
			return false;
		}
		return ResourceObjectPattern.matches(attributes, protectedAccountPatterns);
	}

	
}
