/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

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
	
	@Autowired(required=true)
	private PrismContext prismContext;

	//////////
	// GET
	/////////
	
	public void postProcessEntitlementsRead(ConnectorInstance connector, ResourceType resourceType,
			PrismObject<ShadowType> resourceObject, RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult) throws SchemaException, CommunicationException, GenericFrameworkException {
		
		Collection<ResourceObjectAssociationType> entitlementAssociationDefs = objectClassDefinition.getEntitlementAssociations();
		if (entitlementAssociationDefs != null) {
			ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(resourceObject);
			RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
			
			PrismContainerDefinition<ShadowAssociationType> associationDef = resourceObject.getDefinition().findContainerDefinition(ShadowType.F_ASSOCIATION);
			PrismContainer<ShadowAssociationType> associationContainer = associationDef.instantiate();
			
			for (ResourceObjectAssociationType assocDefType: entitlementAssociationDefs) {
				RefinedObjectClassDefinition entitlementDef = refinedSchema.getRefinedDefinition(ShadowKindType.ENTITLEMENT, assocDefType.getIntent());
				if (entitlementDef == null) {
					throw new SchemaException("No definition for entitlement intent '"+assocDefType.getIntent()+"' in "+resourceType);
				}
				ResourceObjectAssociationDirectionType direction = assocDefType.getDirection();
				if (direction == ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT) {
					postProcessEntitlementSubjectToEntitlement(resourceType, resourceObject, objectClassDefinition, assocDefType, entitlementDef, attributesContainer, associationContainer, parentResult);					
				} else if (direction == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
					postProcessEntitlementEntitlementToSubject(connector, resourceType, resourceObject, objectClassDefinition, assocDefType, entitlementDef, attributesContainer, associationContainer, parentResult);
				} else {
					throw new IllegalArgumentException("Unknown entitlement direction "+direction+" in association "+assocDefType+" in "+resourceType);
				}
			}
			
			if (!associationContainer.isEmpty()) {
				resourceObject.add(associationContainer);
			}
		}
	}
	
	private <S extends ShadowType,T> void postProcessEntitlementSubjectToEntitlement(ResourceType resourceType, PrismObject<S> resourceObject, 
			RefinedObjectClassDefinition objectClassDefinition, ResourceObjectAssociationType assocDefType,
			RefinedObjectClassDefinition entitlementDef,
			ResourceAttributeContainer attributesContainer, PrismContainer<ShadowAssociationType> associationContainer,
			OperationResult parentResult) throws SchemaException {
		QName associationName = assocDefType.getName();
		if (associationName == null) {
			throw new SchemaException("No name in entitlement association "+assocDefType+" in "+resourceType);
		}
		
		QName assocAttrName = assocDefType.getAssociationAttribute();
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
			return;
		}
		
		QName valueAttrName = assocDefType.getValueAttribute();
		if (valueAttrName == null) {
			throw new SchemaException("No value attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		RefinedAttributeDefinition valueAttrDef = entitlementDef.findAttributeDefinition(valueAttrName);
		ResourceAttribute<T> valueAttribute = valueAttrDef.instantiate();
		
		for (PrismPropertyValue<T> assocAttrPVal: assocAttr.getValues()) {
			valueAttribute.add(assocAttrPVal.clone());
		}
		
		PrismContainerValue<ShadowAssociationType> associationCVal = associationContainer.createNewValue();
		associationCVal.asContainerable().setName(associationName);
		ResourceAttributeContainer identifiersContainer = new ResourceAttributeContainer(
				ShadowAssociationType.F_IDENTIFIERS, entitlementDef.toResourceAttributeContainerDefinition(), prismContext);
		associationCVal.add(identifiersContainer);
		identifiersContainer.add(valueAttribute);
	}
	
	private <S extends ShadowType,T> void postProcessEntitlementEntitlementToSubject(ConnectorInstance connector, 
			ResourceType resourceType, PrismObject<S> resourceObject, 
			RefinedObjectClassDefinition objectClassDefinition, ResourceObjectAssociationType assocDefType,
			final RefinedObjectClassDefinition entitlementDef,
			ResourceAttributeContainer attributesContainer, final PrismContainer<ShadowAssociationType> associationContainer,
			OperationResult parentResult) throws SchemaException, CommunicationException, GenericFrameworkException {
		final QName associationName = assocDefType.getName();
		if (associationName == null) {
			throw new SchemaException("No name in entitlement association "+assocDefType+" in "+resourceType);
		}
		
		QName assocAttrName = assocDefType.getAssociationAttribute();
		if (assocAttrName == null) {
			throw new SchemaException("No association attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		RefinedAttributeDefinition assocAttrDef = entitlementDef.findAttributeDefinition(assocAttrName);
		if (assocAttrDef == null) {
			throw new SchemaException("Association attribute '"+assocAttrName+"'defined in entitlement association '"+associationName+"' was not found in schema for "+resourceType);
		}
		
		QName valueAttrName = assocDefType.getValueAttribute();
		if (valueAttrName == null) {
			throw new SchemaException("No value attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		ResourceAttribute<T> valueAttr = attributesContainer.findAttribute(valueAttrName);
		if (valueAttr == null || valueAttr.isEmpty()) {
			throw new SchemaException("Value attribute "+valueAttrName+" has no value; attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		if (valueAttr.size() > 1) {
			throw new SchemaException("Value attribute "+valueAttrName+" has no more than one value; attribute defined in entitlement association '"+associationName+"' in "+resourceType);
		}
		
		ObjectFilter filter = EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES), assocAttrDef, valueAttr.getValue());
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		
		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> entitlementShadow) {
				PrismContainerValue<ShadowAssociationType> associationCVal = associationContainer.createNewValue();
				associationCVal.asContainerable().setName(associationName);
				Collection<ResourceAttribute<?>> entitlementIdentifiers = ShadowUtil.getIdentifiers(entitlementShadow);
				try {
					ResourceAttributeContainer identifiersContainer = new ResourceAttributeContainer(
							ShadowAssociationType.F_IDENTIFIERS, entitlementDef.toResourceAttributeContainerDefinition(), prismContext);
					associationCVal.add(identifiersContainer);
					identifiersContainer.getValue().addAll(ResourceAttribute.cloneCollection(entitlementIdentifiers));
					
					// Remember the full shadow in user data. This is used later as an optimization to create the shadow in repo 
					identifiersContainer.setUserData(ResourceObjectConverter.FULL_SHADOW_KEY, entitlementShadow);
				} catch (SchemaException e) {
					throw new TunnelException(e);
				}
				return true;
			}
		};
		try {
			connector.search(entitlementDef, query, handler, parentResult);
		} catch (TunnelException e) {
			throw (SchemaException)e.getCause();
		}
		
	}
	
	//////////
	// ADD
	/////////
	
	public void processEntitlementsAdd(ResourceType resource, PrismObject<ShadowType> shadow, RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
		PrismContainer<ShadowAssociationType> associationContainer = shadow.findContainer(ShadowType.F_ASSOCIATION);
		if (associationContainer == null || associationContainer.isEmpty()) {
			return;
		}
		Map<QName, PropertyDelta<?>> deltaMap = new HashMap<QName, PropertyDelta<?>>();
		collectEntitlementToAttrsDelta(deltaMap, associationContainer.getValues(), objectClassDefinition, resource, ModificationType.ADD);
		for (PropertyDelta<?> delta: deltaMap.values()) {
			delta.applyTo(shadow);
		}
	}
	
	public <T> void collectEntitlementsAsObjectOperation(Map<ResourceObjectDiscriminator, Collection<Operation>> roMap,
			RefinedObjectClassDefinition objectClassDefinition,
			PrismObject<ShadowType> shadow, RefinedResourceSchema rSchema, ResourceType resource) 
					throws SchemaException {
		PrismContainer<ShadowAssociationType> associationContainer = shadow.findContainer(ShadowType.F_ASSOCIATION);
		if (associationContainer == null || associationContainer.isEmpty()) {
			return;
		}
		collectEntitlementsAsObjectOperation(roMap, associationContainer.getValues(), objectClassDefinition,
				shadow, rSchema, resource, ModificationType.ADD);
	}

	
	//////////
	// MODIFY
	/////////
	
	/**
	 * Collects entitlement changes from the shadow to entitlement section into attribute operations.
	 * NOTE: only collects  SUBJECT_TO_ENTITLEMENT entitlement direction.
	 */
	public void collectEntitlementChange(ContainerDelta<ShadowAssociationType> itemDelta, Collection<Operation> operations, 
			RefinedObjectClassDefinition objectClassDefinition, ResourceType resource) throws SchemaException {
		Map<QName, PropertyDelta<?>> deltaMap = new HashMap<QName, PropertyDelta<?>>();
		
		collectEntitlementToAttrsDelta(deltaMap, itemDelta.getValuesToAdd(), objectClassDefinition, resource, ModificationType.ADD);
		collectEntitlementToAttrsDelta(deltaMap, itemDelta.getValuesToDelete(), objectClassDefinition, resource, ModificationType.DELETE);
		collectEntitlementToAttrsDelta(deltaMap, itemDelta.getValuesToReplace(), objectClassDefinition, resource, ModificationType.REPLACE);

		for(PropertyDelta<?> attrDelta: deltaMap.values()) {
			PropertyModificationOperation attributeModification = new PropertyModificationOperation(attrDelta);
			operations.add(attributeModification);
		}
	}
	
	public <T> void collectEntitlementsAsObjectOperation(Map<ResourceObjectDiscriminator, Collection<Operation>> roMap,
			ContainerDelta<ShadowAssociationType> containerDelta, RefinedObjectClassDefinition objectClassDefinition,
			PrismObject<ShadowType> shadow, RefinedResourceSchema rSchema, ResourceType resource) 
					throws SchemaException {
		collectEntitlementsAsObjectOperation(roMap, containerDelta.getValuesToAdd(), objectClassDefinition,
				shadow, rSchema, resource, ModificationType.ADD);
		collectEntitlementsAsObjectOperation(roMap, containerDelta.getValuesToDelete(), objectClassDefinition,
				shadow, rSchema, resource, ModificationType.DELETE);
		collectEntitlementsAsObjectOperation(roMap, containerDelta.getValuesToReplace(), objectClassDefinition,
				shadow, rSchema, resource, ModificationType.REPLACE);
	}
	
	/////////
	// DELETE
	/////////
	
	/**
	 * This is somehow different that all the other methods. We are not following the content of a shadow or delta. We are following
	 * the definitions. This is to avoid the need to read the object that is going to be deleted. In fact, the object should not be there
	 * any more, but we still want to clean up entitlement membership based on the information from the shadow. 
	 * @param parentResult 
	 */
	public <T> void collectEntitlementsAsObjectOperationDelete(ConnectorInstance connector, final Map<ResourceObjectDiscriminator, Collection<Operation>> roMap,
			RefinedObjectClassDefinition objectClassDefinition,
			PrismObject<ShadowType> shadow, RefinedResourceSchema rSchema, ResourceType resourceType, OperationResult parentResult) 
					throws SchemaException, CommunicationException {

		Collection<ResourceObjectAssociationType> entitlementAssociationDefs = objectClassDefinition.getEntitlementAssociations();
		if (entitlementAssociationDefs == null || entitlementAssociationDefs.isEmpty()) {
			// Nothing to do
			return;
		}
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
		for (ResourceObjectAssociationType assocDefType: objectClassDefinition.getEntitlementAssociations()) {
			if (assocDefType.getDirection() != ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
				// We can ignore these. They will die together with the object. No need to explicitly delete them.
				continue;
			}
			QName associationName = assocDefType.getName();
			if (associationName == null) {
				throw new SchemaException("No name in entitlement association "+assocDefType+" in "+resourceType);
			}
			final RefinedObjectClassDefinition entitlementOcDef = refinedSchema.getRefinedDefinition(ShadowKindType.ENTITLEMENT, assocDefType.getIntent());
			if (entitlementOcDef == null) {
				throw new SchemaException("No definition for entitlement intent '"+assocDefType.getIntent()+"' defined in entitlement association "+associationName+" in "+resourceType);
			}
			
			final QName assocAttrName = assocDefType.getAssociationAttribute();
			if (assocAttrName == null) {
				throw new SchemaException("No association attribute defined in entitlement association '"+associationName+"' in "+resourceType);
			}
			final RefinedAttributeDefinition assocAttrDef = entitlementOcDef.findAttributeDefinition(assocAttrName);
			if (assocAttrDef == null) {
				throw new SchemaException("Association attribute '"+assocAttrName+"'defined in entitlement association '"+associationName+"' was not found in schema for "+resourceType);
			}
			
			QName valueAttrName = assocDefType.getValueAttribute();
			if (valueAttrName == null) {
				throw new SchemaException("No value attribute defined in entitlement association '"+associationName+"' in "+resourceType);
			}
			final ResourceAttribute<T> valueAttr = attributesContainer.findAttribute(valueAttrName);
			if (valueAttr == null || valueAttr.isEmpty()) {
				throw new SchemaException("Value attribute "+valueAttrName+" has no value; attribute defined in entitlement association '"+associationName+"' in "+resourceType);
			}
			if (valueAttr.size() > 1) {
				throw new SchemaException("Value attribute "+valueAttrName+" has no more than one value; attribute defined in entitlement association '"+associationName+"' in "+resourceType);
			}
			
			ObjectFilter filter = EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES), assocAttrDef, valueAttr.getValue());
			ObjectQuery query = new ObjectQuery();
			query.setFilter(filter);
			
			ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
				@Override
				public boolean handle(PrismObject<ShadowType> entitlementShadow) {
					Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(entitlementShadow);
					ResourceObjectDiscriminator disc = new ResourceObjectDiscriminator(entitlementOcDef, identifiers);
					Collection<Operation> operations = roMap.get(disc);
					if (operations == null) {
						operations = new ArrayList<Operation>();
						roMap.put(disc, operations);
					}
					
					PropertyDelta<T> attributeDelta = null;
					for(Operation operation: operations) {
						if (operation instanceof PropertyModificationOperation) {
							PropertyModificationOperation propOp = (PropertyModificationOperation)operation;
							if (propOp.getPropertyDelta().getName().equals(assocAttrName)) {
								attributeDelta = propOp.getPropertyDelta();
							}
						}
					}
					if (attributeDelta == null) {
						attributeDelta = assocAttrDef.createEmptyDelta(new ItemPath(ShadowType.F_ATTRIBUTES, assocAttrName));
						PropertyModificationOperation attributeModification = new PropertyModificationOperation(attributeDelta);
						operations.add(attributeModification);
					}
					
					attributeDelta.addValuesToDelete(valueAttr.getClonedValues());

					return true;
				}
			};
			try {
				connector.search(entitlementOcDef, query, handler, parentResult);
			} catch (TunnelException e) {
				throw (SchemaException)e.getCause();
			} catch (GenericFrameworkException e) {
				throw new GenericConnectorException(e.getMessage(), e);
			}
			
		}
		
	}
	
	/////////
	// common
	/////////
	
	private <T> void collectEntitlementToAttrsDelta(Map<QName, PropertyDelta<?>> deltaMap, 
			Collection<PrismContainerValue<ShadowAssociationType>> set,
			RefinedObjectClassDefinition objectClassDefinition, ResourceType resource, ModificationType modificationType) throws SchemaException {
		if (set == null) {
			return;
		}
		for (PrismContainerValue<ShadowAssociationType> associationCVal: set) {
			collectEntitlementToAttrDelta(deltaMap, associationCVal, objectClassDefinition, resource, modificationType);
		}
	}
	
	/**
	 *  Collects entitlement changes from the shadow to entitlement section into attribute operations.
	 *  Collects a single value.
	 *  NOTE: only collects  SUBJECT_TO_ENTITLEMENT entitlement direction.
	 */
	private <T> void collectEntitlementToAttrDelta(Map<QName, PropertyDelta<?>> deltaMap, PrismContainerValue<ShadowAssociationType> associationCVal,
			RefinedObjectClassDefinition objectClassDefinition, ResourceType resource, ModificationType modificationType) throws SchemaException {
		
		ShadowAssociationType associationType = associationCVal.asContainerable();
		QName associationName = associationType.getName();
		if (associationName == null) {
			throw new SchemaException("No name in entitlement association "+associationCVal);
		}
		ResourceObjectAssociationType assocDefType = objectClassDefinition.findEntitlementAssociation(associationName);
		if (assocDefType == null) {
			throw new SchemaException("No entitlement association with name "+assocDefType+" in schema of "+resource);
		}
		
		ResourceObjectAssociationDirectionType direction = assocDefType.getDirection();
		if (direction != ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT) {
			// Process just this one direction. The other direction means modification of another object and
			// therefore will be processed later
			return;
		}
		
		QName assocAttrName = assocDefType.getAssociationAttribute();
		if (assocAttrName == null) {
			throw new SchemaException("No association attribute definied in entitlement association '"+associationName+"' in "+resource);
		}
		RefinedAttributeDefinition assocAttrDef = objectClassDefinition.findAttributeDefinition(assocAttrName);
		if (assocAttrDef == null) {
			throw new SchemaException("Association attribute '"+assocAttrName+"'definied in entitlement association '"+associationName+"' was not found in schema for "+resource);
		}
		PropertyDelta<T> attributeDelta = (PropertyDelta<T>) deltaMap.get(assocAttrName);
		if (attributeDelta == null) {
			attributeDelta = assocAttrDef.createEmptyDelta(new ItemPath(ShadowType.F_ATTRIBUTES, assocAttrName));
			deltaMap.put(assocAttrName, attributeDelta);
		}
		
		QName valueAttrName = assocDefType.getValueAttribute();
		if (valueAttrName == null) {
			throw new SchemaException("No value attribute definied in entitlement association '"+associationName+"' in "+resource);
		}
		ResourceAttributeContainer identifiersContainer = 
				ShadowUtil.getAttributesContainer(associationCVal, ShadowAssociationType.F_IDENTIFIERS);
		PrismProperty<T> valueAttr = identifiersContainer.findProperty(valueAttrName);
		if (valueAttr == null) {
			throw new SchemaException("No value attribute "+valueAttrName+" present in entitlement association '"+associationName+"' in shadow for "+resource);
		}
		
		if (modificationType == ModificationType.ADD) {
			attributeDelta.addValuesToAdd(valueAttr.getClonedValues());
		} else if (modificationType == ModificationType.DELETE) {
			attributeDelta.addValuesToDelete(valueAttr.getClonedValues());
		} else if (modificationType == ModificationType.REPLACE) {
			// TODO: check if already exists
			attributeDelta.setValuesToReplace(valueAttr.getClonedValues());
		}
	}
	
	private <T> void collectEntitlementsAsObjectOperation(Map<ResourceObjectDiscriminator, Collection<Operation>> roMap,
			Collection<PrismContainerValue<ShadowAssociationType>> set, RefinedObjectClassDefinition objectClassDefinition,
			PrismObject<ShadowType> shadow, RefinedResourceSchema rSchema, ResourceType resource, ModificationType modificationType) 
					throws SchemaException {
		if (set == null) {
			return;
		}
		for (PrismContainerValue<ShadowAssociationType> associationCVal: set) {
			collectEntitlementAsObjectOperation(roMap, associationCVal, objectClassDefinition, shadow,
					rSchema, resource, modificationType);
		}
	}
	
	private <T> void collectEntitlementAsObjectOperation(Map<ResourceObjectDiscriminator, Collection<Operation>> roMap,
			PrismContainerValue<ShadowAssociationType> associationCVal, RefinedObjectClassDefinition objectClassDefinition,
			PrismObject<ShadowType> shadow, RefinedResourceSchema rSchema, ResourceType resource, ModificationType modificationType) 
					throws SchemaException {
		
		ShadowAssociationType associationType = associationCVal.asContainerable();
		QName associationName = associationType.getName();
		if (associationName == null) {
			throw new SchemaException("No name in entitlement association "+associationCVal);
		}
		ResourceObjectAssociationType assocDefType = objectClassDefinition.findEntitlementAssociation(associationName);
		if (assocDefType == null) {
			throw new SchemaException("No entitlement association with name "+assocDefType+" in schema of "+resource);
		}
		
		ResourceObjectAssociationDirectionType direction = assocDefType.getDirection();
		if (direction != ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
			// Process just this one direction. The other direction was processed before
			return;
		}
		
		String entitlementIntent = assocDefType.getIntent();
		if (entitlementIntent == null) {
			throw new SchemaException("No entitlement intent specified in association "+associationCVal+" in "+resource);
		}
		RefinedObjectClassDefinition entitlementOcDef = rSchema.getRefinedDefinition(ShadowKindType.ENTITLEMENT, entitlementIntent);
		if (entitlementOcDef == null) {
			throw new SchemaException("No definition of entitlement intent '"+entitlementIntent+"' specified in association "+associationCVal+" in "+resource);
		}
		
		QName assocAttrName = assocDefType.getAssociationAttribute();
		if (assocAttrName == null) {
			throw new SchemaException("No association attribute defined in entitlement association in "+resource);
		}
		
		RefinedAttributeDefinition assocAttrDef = entitlementOcDef.findAttributeDefinition(assocAttrName);
		if (assocAttrDef == null) {
			throw new SchemaException("Association attribute '"+assocAttrName+"'defined in entitlement association was not found in entitlement intent '"+entitlementIntent+"' in schema for "+resource);
		}
		
		ResourceAttributeContainer identifiersContainer = 
				ShadowUtil.getAttributesContainer(associationCVal, ShadowAssociationType.F_IDENTIFIERS);
		Collection<ResourceAttribute<?>> identifiers = identifiersContainer.getAttributes();
		
		ResourceObjectDiscriminator disc = new ResourceObjectDiscriminator(entitlementOcDef, identifiers);
		Collection<Operation> operations = roMap.get(disc);
		if (operations == null) {
			operations = new ArrayList<Operation>();
			roMap.put(disc, operations);
		}
		
		QName valueAttrName = assocDefType.getValueAttribute();
		if (valueAttrName == null) {
			throw new SchemaException("No value attribute definied in entitlement association in "+resource);
		}
		ResourceAttribute<T> valueAttr = ShadowUtil.getAttribute(shadow, valueAttrName);
		if (valueAttr == null) {
			// TODO: check schema and try to fetch full shadow if necessary
			throw new SchemaException("No value attribute "+valueAttrName+" in shadow");
		}
		
		PropertyDelta<T> attributeDelta = null;
		for(Operation operation: operations) {
			if (operation instanceof PropertyModificationOperation) {
				PropertyModificationOperation propOp = (PropertyModificationOperation)operation;
				if (propOp.getPropertyDelta().getName().equals(assocAttrName)) {
					attributeDelta = propOp.getPropertyDelta();
				}
			}
		}
		if (attributeDelta == null) {
			attributeDelta = assocAttrDef.createEmptyDelta(new ItemPath(ShadowType.F_ATTRIBUTES, assocAttrName));
			PropertyModificationOperation attributeModification = new PropertyModificationOperation(attributeDelta);
			operations.add(attributeModification);
		}
		
		if (modificationType == ModificationType.ADD) {
			attributeDelta.addValuesToAdd(valueAttr.getClonedValues());
		} else if (modificationType == ModificationType.DELETE) {
			attributeDelta.addValuesToDelete(valueAttr.getClonedValues());
		} else if (modificationType == ModificationType.REPLACE) {
			// TODO: check if already exists
			attributeDelta.setValuesToReplace(valueAttr.getClonedValues());
		}
	}

	
}
