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
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.SearchHierarchyConstraints;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

/**
 * Class that collects the entitlement-related methods used by ResourceObjectConverter
 * 
 * Should NOT be used by any class other than ResourceObjectConverter.
 * 
 * @author Radovan Semancik
 *
 */
@Component
class EntitlementConverter {
	
	private static final Trace LOGGER = TraceManager.getTrace(EntitlementConverter.class);
	
	@Autowired(required=true)
	private ResourceObjectReferenceResolver resourceObjectReferenceResolver;
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
	private MatchingRuleRegistry matchingRuleRegistry;

	//////////
	// GET
	/////////
	
	public void postProcessEntitlementsRead(ProvisioningContext subjectCtx,
			PrismObject<ShadowType> resourceObject, OperationResult parentResult) throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ResourceType resourceType = subjectCtx.getResource();
		LOGGER.trace("Starting postProcessEntitlementRead");
		RefinedObjectClassDefinition objectClassDefinition = subjectCtx.getObjectClassDefinition();
		Collection<RefinedAssociationDefinition> entitlementAssociationDefs = objectClassDefinition.getAssociationDefinitions();
		if (entitlementAssociationDefs != null) {
			ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(resourceObject);
			
			PrismContainerDefinition<ShadowAssociationType> associationDef = resourceObject.getDefinition().findContainerDefinition(ShadowType.F_ASSOCIATION);
			PrismContainer<ShadowAssociationType> associationContainer = associationDef.instantiate();
			
			for (RefinedAssociationDefinition assocDefType: entitlementAssociationDefs) {
				ShadowKindType entitlementKind = assocDefType.getKind();
				if (entitlementKind == null) {
					entitlementKind = ShadowKindType.ENTITLEMENT;
				}
				for (String entitlementIntent: assocDefType.getIntents()) {
					LOGGER.trace("Resolving association {} for kind {} and intent {}", assocDefType.getName(), entitlementKind, entitlementIntent);
					ProvisioningContext entitlementCtx = subjectCtx.spawn(entitlementKind, entitlementIntent);
					RefinedObjectClassDefinition entitlementDef = entitlementCtx.getObjectClassDefinition();
					if (entitlementDef == null) {
						throw new SchemaException("No definition for entitlement intent(s) '"+assocDefType.getIntents()+"' in "+resourceType);
					}
					ResourceObjectAssociationDirectionType direction = assocDefType.getResourceObjectAssociationType().getDirection();
					if (direction == ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT) {
						postProcessEntitlementSubjectToEntitlement(resourceType, resourceObject, objectClassDefinition, assocDefType, entitlementDef, attributesContainer, associationContainer, parentResult);					
					} else if (direction == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
						if (assocDefType.getResourceObjectAssociationType().getShortcutAssociationAttribute() != null) {
							postProcessEntitlementSubjectToEntitlement(resourceType, resourceObject, objectClassDefinition, 
									assocDefType, entitlementDef, attributesContainer, associationContainer, 
									assocDefType.getResourceObjectAssociationType().getShortcutAssociationAttribute(),
									assocDefType.getResourceObjectAssociationType().getShortcutValueAttribute(), parentResult);
						} else {
							postProcessEntitlementEntitlementToSubject(subjectCtx, resourceObject, assocDefType, entitlementCtx, attributesContainer, associationContainer, parentResult);
						}
					} else {
						throw new IllegalArgumentException("Unknown entitlement direction "+direction+" in association "+assocDefType+" in "+resourceType);
					}
				}
			}
			
			if (!associationContainer.isEmpty()) {
				resourceObject.add(associationContainer);
			}
		}
	}
	
	private <S extends ShadowType,T> void postProcessEntitlementSubjectToEntitlement(ResourceType resourceType, PrismObject<S> resourceObject, 
			RefinedObjectClassDefinition objectClassDefinition, RefinedAssociationDefinition assocDefType,
			RefinedObjectClassDefinition entitlementDef,
			ResourceAttributeContainer attributesContainer, PrismContainer<ShadowAssociationType> associationContainer,
			OperationResult parentResult) throws SchemaException {
		QName assocAttrName = assocDefType.getResourceObjectAssociationType().getAssociationAttribute();
		QName valueAttrName = assocDefType.getResourceObjectAssociationType().getValueAttribute();
		postProcessEntitlementSubjectToEntitlement(resourceType, resourceObject, objectClassDefinition, 
				assocDefType, entitlementDef, attributesContainer, associationContainer, assocAttrName, 
				valueAttrName, parentResult);
    }
	
	private <S extends ShadowType,T> void postProcessEntitlementSubjectToEntitlement(ResourceType resourceType, 
			PrismObject<S> resourceObject, 
			RefinedObjectClassDefinition objectClassDefinition, RefinedAssociationDefinition assocDefType,
			RefinedObjectClassDefinition entitlementDef,
			ResourceAttributeContainer attributesContainer, PrismContainer<ShadowAssociationType> associationContainer,
			QName assocAttrName, QName valueAttrName,
			OperationResult parentResult) throws SchemaException {
		QName associationName = assocDefType.getName();
		if (associationName == null) {
			throw new SchemaException("No name in entitlement association "+assocDefType+" in "+resourceType);
		}

		if (assocAttrName == null) {
			throw new SchemaException("No association attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		RefinedAttributeDefinition assocAttrDef = objectClassDefinition.findAttributeDefinition(assocAttrName);
		if (assocAttrDef == null) {
			throw new SchemaException("Association attribute '"+assocAttrName+"'defined in entitlement association '"+associationName+"' was not found in schema for "+resourceType);
		}
		ResourceAttribute<T> assocAttr = attributesContainer.findAttribute(assocAttrName);
		if (assocAttr == null || assocAttr.isEmpty()) {
			// Nothing to do. No attribute to base the association on.
			LOGGER.trace("Association attribute {} is empty, skipping association {}", assocAttrName, associationName);
			return;
		}

		if (valueAttrName == null) {
			throw new SchemaException("No value attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		
		RefinedAttributeDefinition valueAttrDef = entitlementDef.findAttributeDefinition(valueAttrName);

        for (PrismPropertyValue<T> assocAttrPVal : assocAttr.getValues()) {

            ResourceAttribute<T> valueAttribute = valueAttrDef.instantiate();
            valueAttribute.add(assocAttrPVal.clone());

            PrismContainerValue<ShadowAssociationType> associationCVal = associationContainer.createNewValue();
            associationCVal.asContainerable().setName(associationName);
            ResourceAttributeContainer identifiersContainer = new ResourceAttributeContainer(
                    ShadowAssociationType.F_IDENTIFIERS, entitlementDef.toResourceAttributeContainerDefinition(), prismContext);
            associationCVal.add(identifiersContainer);
            identifiersContainer.add(valueAttribute);
            LOGGER.trace("Assocciation attribute value resolved to valueAtrribute {}  and identifiers container {}", valueAttribute, identifiersContainer);
        }
    }
	
	private <S extends ShadowType,T> void postProcessEntitlementEntitlementToSubject(ProvisioningContext subjectCtx, final PrismObject<S> resourceObject, 
			RefinedAssociationDefinition assocDefType, final ProvisioningContext entitlementCtx,
			ResourceAttributeContainer attributesContainer, final PrismContainer<ShadowAssociationType> associationContainer,
			OperationResult parentResult) throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ResourceType resourceType = subjectCtx.getResource();
		final QName associationName = assocDefType.getName();
		final RefinedObjectClassDefinition entitlementDef = entitlementCtx.getObjectClassDefinition();
		if (associationName == null) {
			throw new SchemaException("No name in entitlement association "+assocDefType+" in "+resourceType);
		}
		
		QName associationAuxiliaryObjectClass = assocDefType.getAuxiliaryObjectClass();
		if (associationAuxiliaryObjectClass != null && associationAuxiliaryObjectClass.getNamespaceURI() != null && 
				!associationAuxiliaryObjectClass.getNamespaceURI().equals(ResourceTypeUtil.getResourceNamespace(resourceType))) {
			LOGGER.warn("Auxiliary object class {} in association {} does not have namespace that matches {}", 
					associationAuxiliaryObjectClass, assocDefType.getName(), resourceType);
		}
		if (associationAuxiliaryObjectClass != null && !subjectCtx.getObjectClassDefinition().hasAuxiliaryObjectClass(associationAuxiliaryObjectClass)) {
			LOGGER.trace("Ignoring association {} because subject does not have auxiliary object class {}, it has {}", 
					associationName, associationAuxiliaryObjectClass, subjectCtx.getObjectClassDefinition().getAuxiliaryObjectClassDefinitions());
			return;
		}
		
		QName assocAttrName = assocDefType.getResourceObjectAssociationType().getAssociationAttribute();
		if (assocAttrName == null) {
			throw new SchemaException("No association attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		RefinedAttributeDefinition assocAttrDef = entitlementDef.findAttributeDefinition(assocAttrName);
		if (assocAttrDef == null) {
			throw new SchemaException("Association attribute '"+assocAttrName+"'defined in entitlement association '"+associationName+"' was not found in schema for "+resourceType);
		}
		
		QName valueAttrName = assocDefType.getResourceObjectAssociationType().getValueAttribute();
		if (valueAttrName == null) {
			throw new SchemaException("No value attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		ResourceAttribute<T> valueAttr = attributesContainer.findAttribute(valueAttrName);
		if (valueAttr == null || valueAttr.isEmpty()) {
			LOGGER.trace("Ignoring association {} because subject does not have any value in attribute {}", associationName, valueAttrName);
			return;
		}
		if (valueAttr.size() > 1) {
			throw new SchemaException("Value attribute "+valueAttrName+" has no more than one value; attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		
		ObjectQuery query = createQuery(assocDefType, assocAttrDef, valueAttr);
		
		AttributesToReturn attributesToReturn = ProvisioningUtil.createAttributesToReturn(entitlementCtx);
		
		SearchHierarchyConstraints searchHierarchyConstraints = null;
		ResourceObjectReferenceType baseContextRef = entitlementDef.getBaseContext();
		if (baseContextRef != null) {
			// TODO: this should be done once per search. Not in every run of postProcessEntitlementEntitlementToSubject
			// this has to go outside of this method
			PrismObject<ShadowType> baseContextShadow = resourceObjectReferenceResolver.resolve(subjectCtx, baseContextRef, 
					null, "base context specification in "+entitlementDef, parentResult);
			RefinedObjectClassDefinition baseContextObjectClassDefinition = subjectCtx.getRefinedSchema().determineCompositeObjectClassDefinition(baseContextShadow);
			ResourceObjectIdentification baseContextIdentification =  ShadowUtil.getResourceObjectIdentification(baseContextShadow, baseContextObjectClassDefinition);
			searchHierarchyConstraints = new SearchHierarchyConstraints(baseContextIdentification, null);
		}
		
		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> entitlementShadow) {
				PrismContainerValue<ShadowAssociationType> associationCVal = associationContainer.createNewValue();
				associationCVal.asContainerable().setName(associationName);
				Collection<ResourceAttribute<?>> entitlementIdentifiers = ShadowUtil.getAllIdentifiers(entitlementShadow);
				try {
					ResourceAttributeContainer identifiersContainer = new ResourceAttributeContainer(
							ShadowAssociationType.F_IDENTIFIERS, entitlementDef.toResourceAttributeContainerDefinition(), prismContext);
					associationCVal.add(identifiersContainer);
					identifiersContainer.getValue().addAll(ResourceAttribute.cloneCollection(entitlementIdentifiers));
					
					// Remember the full shadow in user data. This is used later as an optimization to create the shadow in repo 
					identifiersContainer.setUserData(ResourceObjectConverter.FULL_SHADOW_KEY, entitlementShadow);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Processed entitlement-to-subject association for account {} and entitlement {}",
								ShadowUtil.getHumanReadableName(resourceObject), ShadowUtil.getHumanReadableName(entitlementShadow));
					}
				} catch (SchemaException e) {
					throw new TunnelException(e);
				}
				return true;
			}
		};
		
		ConnectorInstance connector = subjectCtx.getConnector(ReadCapabilityType.class, parentResult);
		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Processed entitlement-to-subject association for account {}: query {}",
						ShadowUtil.getHumanReadableName(resourceObject), query);
			}
			try {
				connector.search(entitlementDef, query, handler, attributesToReturn, null, searchHierarchyConstraints, subjectCtx, parentResult);
			} catch (GenericFrameworkException e) {
				throw new GenericConnectorException("Generic error in the connector " + connector + ". Reason: "
						+ e.getMessage(), e);
			}
		} catch (TunnelException e) {
			throw (SchemaException)e.getCause();
		}
		
	}

    // precondition: valueAttr has exactly one value
	private <TV,TA> ObjectQuery createQuery(RefinedAssociationDefinition assocDefType, RefinedAttributeDefinition<TA> assocAttrDef, ResourceAttribute<TV> valueAttr) throws SchemaException{
		MatchingRule<TA> matchingRule = matchingRuleRegistry.getMatchingRule(assocDefType.getResourceObjectAssociationType().getMatchingRule(),
				assocAttrDef.getTypeName());
		PrismPropertyValue<TA> converted = PrismUtil.convertPropertyValue(valueAttr.getValue(0), valueAttr.getDefinition(), assocAttrDef);
		TA normalizedRealValue = matchingRule.normalize(converted.getValue());
		PrismPropertyValue<TA> normalized = new PrismPropertyValue<TA>(normalizedRealValue);
		LOGGER.trace("Converted entitlement filter value: {} ({}) def={}", normalized, normalized.getValue().getClass(), assocAttrDef);
		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(new ItemPath(ShadowType.F_ATTRIBUTES, assocAttrDef.getName()), assocAttrDef).eq(normalized)
				.build();
		query.setAllowPartialResults(true);
		return query;
	}
	
	//////////
	// ADD
	/////////
	
	public void processEntitlementsAdd(ProvisioningContext ctx, PrismObject<ShadowType> shadow) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		PrismContainer<ShadowAssociationType> associationContainer = shadow.findContainer(ShadowType.F_ASSOCIATION);
		if (associationContainer == null || associationContainer.isEmpty()) {
			return;
		}
		Map<QName, PropertyModificationOperation> operationMap = new HashMap<>();
		collectEntitlementToAttrsDelta(ctx, operationMap, associationContainer.getValues(), ModificationType.ADD);
		for (PropertyModificationOperation operation : operationMap.values()) {
			operation.getPropertyDelta().applyTo(shadow);
		}
	}
	
	public <T> PrismObject<ShadowType> collectEntitlementsAsObjectOperationInShadowAdd(ProvisioningContext ctx, Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap,
			PrismObject<ShadowType> shadow, OperationResult result) 
					throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
		PrismContainer<ShadowAssociationType> associationContainer = shadow.findContainer(ShadowType.F_ASSOCIATION);
		if (associationContainer == null || associationContainer.isEmpty()) {
			return shadow;
		}
		return collectEntitlementsAsObjectOperation(ctx, roMap, associationContainer.getValues(), null, shadow, 
				ModificationType.ADD, result);
	}

	
	//////////
	// MODIFY
	/////////
	
	/**
	 * Collects entitlement changes from the shadow to entitlement section into attribute operations.
	 * NOTE: only collects  SUBJECT_TO_ENTITLEMENT entitlement direction.
	 */
	public void collectEntitlementChange(ProvisioningContext ctx, ContainerDelta<ShadowAssociationType> itemDelta, 
			Collection<Operation> operations) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		Map<QName, PropertyModificationOperation> operationsMap = new HashMap<QName, PropertyModificationOperation>();
		
		collectEntitlementToAttrsDelta(ctx, operationsMap, itemDelta.getValuesToAdd(), ModificationType.ADD);
		collectEntitlementToAttrsDelta(ctx, operationsMap, itemDelta.getValuesToDelete(), ModificationType.DELETE);
		collectEntitlementToAttrsDelta(ctx, operationsMap, itemDelta.getValuesToReplace(), ModificationType.REPLACE);

		operations.addAll(operationsMap.values());
	}
	
	public <T> PrismObject<ShadowType> collectEntitlementsAsObjectOperation(ProvisioningContext ctx, Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap,
			ContainerDelta<ShadowAssociationType> containerDelta,
			PrismObject<ShadowType> subjectShadowBefore, PrismObject<ShadowType> subjectShadowAfter, 
			OperationResult result)
					throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
		subjectShadowAfter = collectEntitlementsAsObjectOperation(ctx, roMap, containerDelta.getValuesToAdd(), 
				subjectShadowBefore, subjectShadowAfter, ModificationType.ADD, result);
		subjectShadowAfter = collectEntitlementsAsObjectOperation(ctx, roMap, containerDelta.getValuesToDelete(),
                subjectShadowBefore, subjectShadowAfter, ModificationType.DELETE, result);
		subjectShadowAfter = collectEntitlementsAsObjectOperation(ctx, roMap, containerDelta.getValuesToReplace(),
                subjectShadowBefore, subjectShadowAfter, ModificationType.REPLACE, result);
		return subjectShadowAfter;
	}
	
	/////////
	// DELETE
	/////////
	
	/**
	 * This is somehow different that all the other methods. We are not following the content of a shadow or delta. We are following
	 * the definitions. This is to avoid the need to read the object that is going to be deleted. In fact, the object should not be there
	 * any more, but we still want to clean up entitlement membership based on the information from the shadow.  
	 */
	public <T> void collectEntitlementsAsObjectOperationDelete(ProvisioningContext subjectCtx, final Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap,
			PrismObject<ShadowType> subjectShadow, OperationResult parentResult) 
					throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		Collection<RefinedAssociationDefinition> entitlementAssociationDefs = subjectCtx.getObjectClassDefinition().getAssociationDefinitions();
		if (entitlementAssociationDefs == null || entitlementAssociationDefs.isEmpty()) {
			// Nothing to do
			LOGGER.trace("No associations in deleted shadow");
			return;
		}
		ResourceAttributeContainer subjectAttributesContainer = ShadowUtil.getAttributesContainer(subjectShadow);
		for (final RefinedAssociationDefinition assocDefType: subjectCtx.getObjectClassDefinition().getAssociationDefinitions()) {
			if (assocDefType.getResourceObjectAssociationType().getDirection() != ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
				// We can ignore these. They will die together with the object. No need to explicitly delete them.
				LOGGER.trace("Ignoring subject-to-object association in deleted shadow");
				continue;
			}
			if (!assocDefType.requiresExplicitReferentialIntegrity()) {
				// Referential integrity not required for this one
				LOGGER.trace("Ignoring association in deleted shadow because it does not require explicit referential integrity assurance");
				continue;
			}
			if (assocDefType.getAuxiliaryObjectClass() != null && 
					!subjectCtx.getObjectClassDefinition().hasAuxiliaryObjectClass(assocDefType.getAuxiliaryObjectClass())) {
				LOGGER.trace("Ignoring association in deleted shadow because subject does not have {} auxiliary object class", assocDefType.getAuxiliaryObjectClass());
				continue;
			}
			QName associationName = assocDefType.getName();
			if (associationName == null) {
				throw new SchemaException("No name in entitlement association "+assocDefType+" in "+subjectCtx.getResource());
			}
			ShadowKindType entitlementKind = assocDefType.getKind();
			if (entitlementKind == null) {
				entitlementKind = ShadowKindType.ENTITLEMENT;
			}
			for (String entitlementIntent: assocDefType.getIntents()) {
				final ProvisioningContext entitlementCtx = subjectCtx.spawn(entitlementKind, entitlementIntent);
				final RefinedObjectClassDefinition entitlementOcDef = entitlementCtx.getObjectClassDefinition();
				if (entitlementOcDef == null) {
					throw new SchemaException("No definition for entitlement intent(s) '"+assocDefType.getIntents()+"' defined in entitlement association "+associationName+" in "+subjectCtx.getResource());
				}
				
				final QName assocAttrName = assocDefType.getResourceObjectAssociationType().getAssociationAttribute();
				if (assocAttrName == null) {
					throw new SchemaException("No association attribute defined in entitlement association '"+associationName+"' in "+subjectCtx.getResource());
				}
				final RefinedAttributeDefinition assocAttrDef = entitlementOcDef.findAttributeDefinition(assocAttrName);
				if (assocAttrDef == null) {
					throw new SchemaException("Association attribute '"+assocAttrName+"'defined in entitlement association '"+associationName+"' was not found in schema for "+subjectCtx.getResource());
				}
				
				QName valueAttrName = assocDefType.getResourceObjectAssociationType().getValueAttribute();
				if (valueAttrName == null) {
					throw new SchemaException("No value attribute defined in entitlement association '"+associationName+"' in "+subjectCtx.getResource());
				}
				final ResourceAttribute<T> valueAttr = subjectAttributesContainer.findAttribute(valueAttrName);
				if (valueAttr == null || valueAttr.isEmpty()) {
					// We really want to throw the exception here. We cannot ignore this. If we ignore it then there may be
					// entitlement membership value left undeleted and this situation will go undetected.
					// Although we cannot really remedy the situation now, we at least throw an error so the problem is detected.
					throw new SchemaException("Value attribute "+valueAttrName+" has no value; attribute defined in entitlement association '"+associationName+"' in "+subjectCtx.getResource());
				}
				if (valueAttr.size() > 1) {
					throw new SchemaException("Value attribute "+valueAttrName+" has no more than one value; attribute defined in entitlement association '"+associationName+"' in "+subjectCtx.getResource());
				}
				
				ObjectQuery query = createQuery(assocDefType, assocAttrDef, valueAttr);
				
				AttributesToReturn attributesToReturn = ProvisioningUtil.createAttributesToReturn(entitlementCtx);
				
				SearchHierarchyConstraints searchHierarchyConstraints = null;
				ResourceObjectReferenceType baseContextRef = entitlementOcDef.getBaseContext();
				if (baseContextRef != null) {
					PrismObject<ShadowType> baseContextShadow = resourceObjectReferenceResolver.resolve(subjectCtx, 
							baseContextRef, null, "base context specification in "+entitlementOcDef, parentResult);
					RefinedObjectClassDefinition baseContextObjectClassDefinition = subjectCtx.getRefinedSchema().determineCompositeObjectClassDefinition(baseContextShadow);
					ResourceObjectIdentification baseContextIdentification =  ShadowUtil.getResourceObjectIdentification(baseContextShadow, baseContextObjectClassDefinition);
					searchHierarchyConstraints = new SearchHierarchyConstraints(baseContextIdentification, null);
				}
				
				ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
					@Override
					public boolean handle(PrismObject<ShadowType> entitlementShadow) {
						Collection<? extends ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(entitlementShadow);
						ResourceObjectDiscriminator disc = new ResourceObjectDiscriminator(entitlementOcDef.getTypeName(), primaryIdentifiers);
						ResourceObjectOperations operations = roMap.get(disc);
						if (operations == null) {
							operations = new ResourceObjectOperations();
							roMap.put(disc, operations);
							operations.setResourceObjectContext(entitlementCtx);
							Collection<? extends ResourceAttribute<?>> allIdentifiers = ShadowUtil.getAllIdentifiers(entitlementShadow);
							operations.setAllIdentifiers(allIdentifiers);
						}
						
						PropertyDelta<T> attributeDelta = null;
						for(Operation operation: operations.getOperations()) {
							if (operation instanceof PropertyModificationOperation) {
								PropertyModificationOperation propOp = (PropertyModificationOperation)operation;
								if (propOp.getPropertyDelta().getElementName().equals(assocAttrName)) {
									attributeDelta = propOp.getPropertyDelta();
								}
							}
						}
						if (attributeDelta == null) {
							attributeDelta = assocAttrDef.createEmptyDelta(new ItemPath(ShadowType.F_ATTRIBUTES, assocAttrName));
							PropertyModificationOperation attributeModification = new PropertyModificationOperation(attributeDelta);
							attributeModification.setMatchingRuleQName(assocDefType.getMatchingRule());
							operations.add(attributeModification);
						}
						
						attributeDelta.addValuesToDelete(valueAttr.getClonedValues());
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Association in deleted shadow delta:\n{}", attributeDelta.debugDump());
						}
	
						return true;
					}
				};
				try {
					LOGGER.trace("Searching for associations in deleted shadow, query: {}", query);
					ConnectorInstance connector = subjectCtx.getConnector(ReadCapabilityType.class, parentResult);
					connector.search(entitlementOcDef, query, handler, attributesToReturn, null, searchHierarchyConstraints, subjectCtx, parentResult);
				} catch (TunnelException e) {
					throw (SchemaException)e.getCause();
				} catch (GenericFrameworkException e) {
					throw new GenericConnectorException(e.getMessage(), e);
				}
			}
			
		}
		
	}
	
	/////////
	// common
	/////////
	
	private <T> void collectEntitlementToAttrsDelta(ProvisioningContext subjectCtx, Map<QName, PropertyModificationOperation> operationMap,
			Collection<PrismContainerValue<ShadowAssociationType>> set, ModificationType modificationType) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		if (set == null) {
			return;
		}
		for (PrismContainerValue<ShadowAssociationType> associationCVal: set) {
			collectEntitlementToAttrDelta(subjectCtx, operationMap, associationCVal, modificationType);
		}
	}
	
	/**
	 *  Collects entitlement changes from the shadow to entitlement section into attribute operations.
	 *  Collects a single value.
	 *  NOTE: only collects  SUBJECT_TO_ENTITLEMENT entitlement direction.
	 */
	private <T> void collectEntitlementToAttrDelta(ProvisioningContext ctx, Map<QName, PropertyModificationOperation> operationMap,
			PrismContainerValue<ShadowAssociationType> associationCVal, ModificationType modificationType) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
		
		ShadowAssociationType associationType = associationCVal.asContainerable();
		QName associationName = associationType.getName();
		if (associationName == null) {
			throw new SchemaException("No name in entitlement association "+associationCVal);
		}
		RefinedAssociationDefinition assocDefType = objectClassDefinition.findAssociationDefinition(associationName);
		if (assocDefType == null) {
			throw new SchemaException("No association with name " + associationName + " in " + objectClassDefinition + " in schema of " + ctx.getResource());
		}
		
		ResourceObjectAssociationDirectionType direction = assocDefType.getResourceObjectAssociationType().getDirection();
		if (direction != ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT) {
			// Process just this one direction. The other direction means modification of another object and
			// therefore will be processed later
			return;
		}
		
		QName assocAttrName = assocDefType.getResourceObjectAssociationType().getAssociationAttribute();
		if (assocAttrName == null) {
			throw new SchemaException("No association attribute definied in entitlement association '"+associationName+"' in "+ctx.getResource());
		}
		RefinedAttributeDefinition assocAttrDef = objectClassDefinition.findAttributeDefinition(assocAttrName);
		if (assocAttrDef == null) {
			throw new SchemaException("Association attribute '"+assocAttrName+"'definied in entitlement association '"+associationName+"' was not found in schema for "+ctx.getResource());
		}
		PropertyModificationOperation attributeOperation = operationMap.get(assocAttrName);
		if (attributeOperation == null) {
			attributeOperation = new PropertyModificationOperation(assocAttrDef.createEmptyDelta(new ItemPath(ShadowType.F_ATTRIBUTES, assocAttrName)));
			attributeOperation.setMatchingRuleQName(assocDefType.getMatchingRule());
			operationMap.put(assocAttrName, attributeOperation);
		}
		
		QName valueAttrName = assocDefType.getResourceObjectAssociationType().getValueAttribute();
		if (valueAttrName == null) {
			throw new SchemaException("No value attribute defined in entitlement association '"+associationName+"' in "+ctx.getResource());
		}
		ResourceAttributeContainer identifiersContainer = 
				ShadowUtil.getAttributesContainer(associationCVal, ShadowAssociationType.F_IDENTIFIERS);
		PrismProperty<T> valueAttr = identifiersContainer.findProperty(valueAttrName);
		if (valueAttr == null) {
			throw new SchemaException("No value attribute "+valueAttrName+" present in entitlement association '"+associationName+"' in shadow for "+ctx.getResource());
		}
		
		if (modificationType == ModificationType.ADD) {
			attributeOperation.getPropertyDelta().addValuesToAdd(valueAttr.getClonedValues());
		} else if (modificationType == ModificationType.DELETE) {
			attributeOperation.getPropertyDelta().addValuesToDelete(valueAttr.getClonedValues());
		} else if (modificationType == ModificationType.REPLACE) {
			// TODO: check if already exists
			attributeOperation.getPropertyDelta().setValuesToReplace(valueAttr.getClonedValues());
		}
	}
	
	private <T> PrismObject<ShadowType> collectEntitlementsAsObjectOperation(ProvisioningContext ctx, Map<ResourceObjectDiscriminator, 
			ResourceObjectOperations> roMap, Collection<PrismContainerValue<ShadowAssociationType>> set,
			PrismObject<ShadowType> subjectShadowBefore, PrismObject<ShadowType> subjectShadowAfter, 
			ModificationType modificationType, OperationResult result)
					throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
		if (set == null) {
			return subjectShadowAfter;
		}
		for (PrismContainerValue<ShadowAssociationType> associationCVal: set) {
			subjectShadowAfter = collectEntitlementAsObjectOperation(ctx, roMap, associationCVal, subjectShadowBefore, subjectShadowAfter,
					modificationType, result);
		}
		return subjectShadowAfter;
	}
	
	private <TV,TA> PrismObject<ShadowType> collectEntitlementAsObjectOperation(ProvisioningContext subjectCtx, Map<ResourceObjectDiscriminator, 
			ResourceObjectOperations> roMap, PrismContainerValue<ShadowAssociationType> associationCVal,
			PrismObject<ShadowType> subjectShadowBefore, PrismObject<ShadowType> subjectShadowAfter, 
			ModificationType modificationType, OperationResult result)
					throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
		ResourceType resource = subjectCtx.getResource();
		ShadowAssociationType associationType = associationCVal.asContainerable();
		QName associationName = associationType.getName();
		if (associationName == null) {
			throw new SchemaException("No name in entitlement association "+associationCVal);
		}
		RefinedAssociationDefinition assocDefType = subjectCtx.getObjectClassDefinition().findAssociationDefinition(associationName);
		if (assocDefType == null) {
			throw new SchemaException("No entitlement association with name "+assocDefType+" in schema of "+resource);
		}
		
		ResourceObjectAssociationDirectionType direction = assocDefType.getResourceObjectAssociationType().getDirection();
		if (direction != ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
			// Process just this one direction. The other direction was processed before
			return subjectShadowAfter;
		}
		
		Collection<String> entitlementIntents = assocDefType.getIntents();
		if (entitlementIntents == null || entitlementIntents.isEmpty()) {
			throw new SchemaException("No entitlement intent specified in association "+associationCVal+" in "+resource);
		}
		ShadowKindType entitlementKind = assocDefType.getKind();
		if (entitlementKind == null) {
			entitlementKind = ShadowKindType.ENTITLEMENT;
		}
		for (String entitlementIntent: entitlementIntents) {
			ProvisioningContext entitlementCtx = subjectCtx.spawn(entitlementKind, entitlementIntent);
			RefinedObjectClassDefinition entitlementOcDef = entitlementCtx.getObjectClassDefinition();
			if (entitlementOcDef == null) {
				throw new SchemaException("No definition of entitlement intent(s) '"+entitlementIntents+"' specified in association "+associationCVal+" in "+resource);
			}
			
			QName assocAttrName = assocDefType.getResourceObjectAssociationType().getAssociationAttribute();
			if (assocAttrName == null) {
				throw new SchemaException("No association attribute defined in entitlement association in "+resource);
			}
			
			RefinedAttributeDefinition assocAttrDef = entitlementOcDef.findAttributeDefinition(assocAttrName);
			if (assocAttrDef == null) {
				throw new SchemaException("Association attribute '"+assocAttrName+"'defined in entitlement association was not found in entitlement intent(s) '"+entitlementIntents+"' in schema for "+resource);
			}
			
			ResourceAttributeContainer identifiersContainer = 
					ShadowUtil.getAttributesContainer(associationCVal, ShadowAssociationType.F_IDENTIFIERS);
			Collection<ResourceAttribute<?>> entitlementIdentifiersFromAssociation = identifiersContainer.getAttributes();
			
			ResourceObjectDiscriminator disc = new ResourceObjectDiscriminator(entitlementOcDef.getTypeName(), entitlementIdentifiersFromAssociation);
			ResourceObjectOperations operations = roMap.get(disc);
			if (operations == null) {
				operations = new ResourceObjectOperations();
				operations.setResourceObjectContext(entitlementCtx);
				roMap.put(disc, operations);
			}
			
			QName valueAttrName = assocDefType.getResourceObjectAssociationType().getValueAttribute();
			if (valueAttrName == null) {
				throw new SchemaException("No value attribute defined in entitlement association in "+resource);
			}
	
	        // Which shadow would we use - shadowBefore or shadowAfter?
	        //
	        // If the operation is ADD or REPLACE, we use current version of the shadow (shadowAfter), because we want
	        // to ensure that we add most-recent data to the subject.
	        //
	        // If the operation is DELETE, we have two possibilities:
	        //  - if the resource provides referential integrity, the subject has already
	        //    new data (because the object operation was already carried out), so we use shadowAfter
	        //  - if the resource does not provide referential integrity, the subject has OLD data
	        //    so we use shadowBefore
	        PrismObject<ShadowType> subjectShadow;
	        if (modificationType != ModificationType.DELETE) {
	            subjectShadow = subjectShadowAfter;
	        } else {
	            if (assocDefType.requiresExplicitReferentialIntegrity()) {
					// we must ensure the referential integrity
					subjectShadow = subjectShadowBefore;
	            } else {
					// i.e. resource has ref integrity assured by itself
					subjectShadow = subjectShadowAfter;
	            }
	        }
	
			ResourceAttribute<TV> valueAttr = ShadowUtil.getAttribute(subjectShadow, valueAttrName);
			if (valueAttr == null) {
				if (!ShadowUtil.isFullShadow(subjectShadow)) {
					Collection<ResourceAttribute<?>> subjectIdentifiers = ShadowUtil.getAllIdentifiers(subjectShadow);
					LOGGER.trace("Fetching {} ({})", subjectShadow, subjectIdentifiers);
					subjectShadow = resourceObjectReferenceResolver.fetchResourceObject(subjectCtx, subjectIdentifiers, null, result);
					subjectShadowAfter = subjectShadow;
					valueAttr = ShadowUtil.getAttribute(subjectShadow, valueAttrName);
				}
				if (valueAttr == null) {
					LOGGER.error("No value attribute {} in shadow\n{}", valueAttrName, subjectShadow.debugDump());
					// TODO: check schema and try to fetch full shadow if necessary
					throw new SchemaException("No value attribute "+valueAttrName+" in " + subjectShadow);
				}
			}
	
			PropertyDelta<TA> attributeDelta = null;
			for(Operation operation: operations.getOperations()) {
				if (operation instanceof PropertyModificationOperation) {
					PropertyModificationOperation propOp = (PropertyModificationOperation)operation;
					if (propOp.getPropertyDelta().getElementName().equals(assocAttrName)) {
						attributeDelta = propOp.getPropertyDelta();
					}
				}
			}
			if (attributeDelta == null) {
				attributeDelta = assocAttrDef.createEmptyDelta(new ItemPath(ShadowType.F_ATTRIBUTES, assocAttrName));
			}
			
			PrismProperty<TA> changedAssocAttr = PrismUtil.convertProperty(valueAttr, assocAttrDef);
			
			if (modificationType == ModificationType.ADD) {
				attributeDelta.addValuesToAdd(changedAssocAttr.getClonedValues());
			} else if (modificationType == ModificationType.DELETE) {
				attributeDelta.addValuesToDelete(changedAssocAttr.getClonedValues());
			} else if (modificationType == ModificationType.REPLACE) {
				// TODO: check if already exists
				attributeDelta.setValuesToReplace(changedAssocAttr.getClonedValues());
			}
			
			if (ResourceTypeUtil.isAvoidDuplicateValues(resource)) {
				PrismObject<ShadowType> currentObjectShadow = operations.getCurrentShadow();
				if (currentObjectShadow == null) {
					LOGGER.trace("Fetching entitlement shadow {} to avoid value duplication (intent={})", entitlementIdentifiersFromAssociation, entitlementIntent);
					currentObjectShadow = resourceObjectReferenceResolver.fetchResourceObject(entitlementCtx, entitlementIdentifiersFromAssociation, null, result);
					operations.setCurrentShadow(currentObjectShadow);
				}
				// TODO it seems that duplicate values are checked twice: once here and the second time in ResourceObjectConverter.executeModify
				// TODO check that and fix if necessary
				PropertyDelta<TA> attributeDeltaAfterNarrow = ProvisioningUtil.narrowPropertyDelta(attributeDelta, currentObjectShadow, assocDefType.getMatchingRule(), matchingRuleRegistry);
				if (LOGGER.isTraceEnabled() && (attributeDeltaAfterNarrow == null || attributeDeltaAfterNarrow.isEmpty())) {
					LOGGER.trace("Not collecting entitlement object operations ({}) association {}: attribute delta is empty after narrow, orig delta: {}",
							modificationType, associationName.getLocalPart(), attributeDelta);
				}
				attributeDelta = attributeDeltaAfterNarrow;
			}
			
			if (attributeDelta != null && !attributeDelta.isEmpty()) {
				PropertyModificationOperation attributeModification = new PropertyModificationOperation(attributeDelta);
				attributeModification.setMatchingRuleQName(assocDefType.getMatchingRule());
				LOGGER.trace("Collecting entitlement object operations ({}) association {}: {}", modificationType, associationName.getLocalPart(), attributeModification);
				operations.add(attributeModification);
			}
			
		}
		return subjectShadowAfter;
	}

	
}
