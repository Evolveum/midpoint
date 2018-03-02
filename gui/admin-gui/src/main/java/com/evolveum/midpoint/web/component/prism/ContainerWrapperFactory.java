/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

import java.util.*;

/**
 * @author Viliam Repan (lazyman)
 */
public class ContainerWrapperFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerWrapperFactory.class);

    private static final String DOT_CLASS = ContainerWrapperFactory.class.getName() + ".";
    private static final String CREATE_PROPERTIES = DOT_CLASS + "createProperties";
    private static final String CREATE_ASSOCIATION_WRAPPER = DOT_CLASS + "createAssociationWrapper";

    private static final List<QName> INHERITED_OBJECT_ATTRIBUTES = Arrays.asList(
            ObjectType.F_NAME,
            ObjectType.F_DESCRIPTION,
            ObjectType.F_FETCH_RESULT,
            ObjectType.F_PARENT_ORG,
            ObjectType.F_PARENT_ORG_REF,
            ObjectType.F_TENANT_REF,
            FocusType.F_LINK,
            FocusType.F_LINK_REF);


    private ModelServiceLocator modelServiceLocator;

    private OperationResult result;

    public ContainerWrapperFactory(ModelServiceLocator modelServiceLocator) {
        Validate.notNull(modelServiceLocator, "Service locator must not be null");

        this.modelServiceLocator = modelServiceLocator;
    }

    public OperationResult getResult() {
        return result;
    }

    public <C extends Containerable> ContainerWrapper createContainerWrapper(
                                                                              PrismContainer<C> container,
                                                                              ContainerStatus objectStatus,
                                                                              ContainerStatus status,
                                                                              ItemPath path) {

        result = new OperationResult(CREATE_PROPERTIES);
        
        ContainerWrapper<C> cWrapper = new ContainerWrapper(container, objectStatus, status, path);
        
        List<ContainerValueWrapper<C>> containerValues = createContainerValues(cWrapper, path);
        cWrapper.setProperties(containerValues);
        cWrapper.computeStripes();
        
       return cWrapper;
    }

	public <C extends Containerable> AbstractAssociationWrapper createAssociationWrapper(PrismObject<ResourceType> resource, ShadowKindType kind, String shadowIntent, PrismContainer<C> association, ContainerStatus objectStatus, ContainerStatus status, ItemPath path) throws SchemaException {
		if (association == null || association.getDefinition() == null
				|| (!(association.getDefinition().getCompileTimeClass().equals(ShadowAssociationType.class))
				&& !(association.getDefinition().getCompileTimeClass().equals(ResourceObjectAssociationType.class)))){
			LOGGER.debug("Association for {} is not supported", association.getComplexTypeDefinition().getTypeClass());
			return null;
		}
		result = new OperationResult(CREATE_ASSOCIATION_WRAPPER);
		//we need to switch association wrapper to single value
		//the transformation will be as following:
		// we have single value ShadowAssociationType || ResourceObjectAssociationType, and from each shadowAssociationType we will create
    	// property - name of the property will be association type(QName) and the value will be shadowRef
    	PrismContainerDefinition<C> associationDefinition = association.getDefinition().clone();
    	associationDefinition.setMaxOccurs(1);
    	
    	RefinedResourceSchema refinedResourceSchema = RefinedResourceSchema.getRefinedSchema(resource);
		RefinedObjectClassDefinition oc = refinedResourceSchema.getRefinedDefinition(kind, shadowIntent);
		if (oc == null) {
			LOGGER.debug("Association for {}/{} not supported by resource {}", kind, shadowIntent, resource);
			return null;
		}
		Collection<RefinedAssociationDefinition> refinedAssociationDefinitions = oc.getAssociationDefinitions();
		
		if (CollectionUtils.isEmpty(refinedAssociationDefinitions)) {
			LOGGER.debug("Association for {}/{} not supported by resource {}", kind, shadowIntent, resource);
			return null;
		}
    	
    	PrismContainer associationTransformed = associationDefinition.instantiate();
    	AbstractAssociationWrapper associationWrapper;
    	if (association.getDefinition().getCompileTimeClass().equals(ShadowAssociationType.class)) {
    		associationWrapper = new ShadowAssociationWrapper(associationTransformed, objectStatus, status, path);
		} else if (association.getDefinition().getCompileTimeClass().equals(ResourceObjectAssociationType.class)) {
			associationWrapper = new ResourceAssociationWrapper(associationTransformed, objectStatus, status, path);
		} else {
    		return null;
		}
    	
    	ContainerValueWrapper<C> shadowValueWrapper = new ContainerValueWrapper<>(associationWrapper,
				associationTransformed.createNewValue(), objectStatus,
				ContainerStatus.ADDING == status ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, path);
		
		List<ItemWrapper> associationValuesWrappers = new ArrayList<>();
		for (RefinedAssociationDefinition refinedAssocationDefinition: refinedAssociationDefinitions) {
			PrismReferenceDefinitionImpl shadowRefDef = new PrismReferenceDefinitionImpl(refinedAssocationDefinition.getName(), ObjectReferenceType.COMPLEX_TYPE, modelServiceLocator.getPrismContext());
			shadowRefDef.setMaxOccurs(-1);
			shadowRefDef.setTargetTypeName(ShadowType.COMPLEX_TYPE);
			PrismReference shadowAss = shadowRefDef.instantiate();
			ItemPath itemPath = null;
			for (PrismContainerValue<C> associationValue : association.getValues()) {
				if (association.getDefinition().getCompileTimeClass().equals(ShadowAssociationType.class)) {
					ShadowAssociationType shadowAssociation = (ShadowAssociationType)associationValue.asContainerable();
					if (shadowAssociation.getName().equals(refinedAssocationDefinition.getName())) {
						itemPath = associationValue.getPath();
						shadowAss.add(associationValue.findReference(ShadowAssociationType.F_SHADOW_REF).getValue().clone());
					}
				} else if (association.getDefinition().getCompileTimeClass().equals(ResourceObjectAssociationType.class)){
					//for now Induced entitlements gui should support only targetRef expression value
					//that is why no need to look for another expression types within association
					ResourceObjectAssociationType resourceAssociation = (ResourceObjectAssociationType) associationValue.asContainerable();
					if (resourceAssociation.getRef() == null || resourceAssociation.getRef().getItemPath() == null){
						continue;
					}
					if (resourceAssociation.getRef().getItemPath().asSingleName().equals(refinedAssocationDefinition.getName())){
						itemPath = associationValue.getPath();
						MappingType outbound = ((ResourceObjectAssociationType)association.getValue().asContainerable()).getOutbound();
						if (outbound == null){
							continue;
						}
						ExpressionType expression = outbound.getExpression();
						if (expression == null){
							continue;
						}
						ObjectReferenceType shadowRef = ExpressionUtil.getShadowRefValue(expression);
						if (shadowRef != null) {
							shadowAss.add(shadowRef.asReferenceValue().clone());
						}
					}
				}
			}
			
			if (itemPath == null) {
				itemPath = new ItemPath(ShadowType.F_ASSOCIATION);
			}		
			
			ReferenceWrapper associationValueWrapper = new ReferenceWrapper(shadowValueWrapper, shadowAss, isItemReadOnly(association.getDefinition(), shadowValueWrapper), shadowAss.isEmpty() ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, itemPath);
			String displayName = refinedAssocationDefinition.getDisplayName();
			if (displayName == null) {
				displayName = refinedAssocationDefinition.getName().getLocalPart();
			}
			associationValueWrapper.setDisplayName(displayName);

			associationValueWrapper.setFilter(WebComponentUtil.createAssociationShadowRefFilter(refinedAssocationDefinition,
					modelServiceLocator.getPrismContext(), resource.getOid()));
			
			for (ValueWrapper valueWrapper : associationValueWrapper.getValues()) {
				valueWrapper.setEditEnabled(isEmpty(valueWrapper));
			}
			associationValueWrapper.setTargetTypes(Collections.singletonList(ShadowType.COMPLEX_TYPE));
			associationValuesWrappers.add(associationValueWrapper);
		}
		
		shadowValueWrapper.setProperties(associationValuesWrappers);
		associationWrapper.setProperties(Arrays.asList(shadowValueWrapper));
		
		result.computeStatus();
		return associationWrapper;
		
    }
    
    private boolean isEmpty(ValueWrapper shadowAssociationRef) {
    	if (shadowAssociationRef == null) {
    		return true;
    	}
    	
    	return shadowAssociationRef.isEmpty();
    	
    }
    
   public <C extends Containerable> ContainerWrapper<C> createContainerWrapper(PrismContainer<C> container, ContainerStatus objectStatus, ContainerStatus status, ItemPath path, boolean readonly) {

		result = new OperationResult(CREATE_PROPERTIES);

		ContainerWrapper<C> cWrapper = new ContainerWrapper<>(container, objectStatus, status, path, readonly);

		List<ContainerValueWrapper<C>> containerValues = createContainerValues(cWrapper, path);
        cWrapper.setProperties(containerValues);
        cWrapper.computeStripes();

        
		return cWrapper;
	}

	  private <C extends Containerable> List<ContainerValueWrapper<C>> createContainerValues(ContainerWrapper<C> cWrapper, ItemPath path) {
	    	List<ContainerValueWrapper<C>> containerValueWrappers = new ArrayList<>();
	    	PrismContainer<C> container = cWrapper.getItem();
	    	
	    	if (container.getValues().isEmpty() && container.isSingleValue()) {
	    		PrismContainerValue<C> pcv = container.createNewValue();
	    		 ContainerValueWrapper<C> containerValueWrapper = createContainerValueWrapper(cWrapper, pcv, cWrapper.getObjectStatus(), ValueStatus.ADDED, cWrapper.getPath());
	    		
	    		containerValueWrappers.add(containerValueWrapper);
	    		return containerValueWrappers;
	    	}
	    	
	    	container.getValues().forEach(pcv -> {
	    		ContainerValueWrapper<C> containerValueWrapper = createContainerValueWrapper(cWrapper, pcv, cWrapper.getObjectStatus(), cWrapper.getStatus() == ContainerStatus.ADDING ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, pcv.getPath());
    			containerValueWrappers.add(containerValueWrapper);
	    	});
	    	
	    	
	    	return containerValueWrappers;
	    }
	  
	  public <C extends Containerable> ContainerValueWrapper<C> createContainerValueWrapper(ContainerWrapper cWrapper, PrismContainerValue<C> value, ContainerStatus objectStatus, ValueStatus status, ItemPath path){
		  ContainerValueWrapper<C> containerValueWrapper = new ContainerValueWrapper<C>(cWrapper, value, objectStatus, status, path);
		    
			List<ItemWrapper> properties = createProperties(containerValueWrapper, false);
			containerValueWrapper.setProperties(properties);
			
			ReferenceWrapper shadowRefWrapper = (ReferenceWrapper) containerValueWrapper.findPropertyWrapper(ShadowAssociationType.F_SHADOW_REF);
    		if (shadowRefWrapper != null && cWrapper.getFilter() != null) {
    			shadowRefWrapper.setFilter(cWrapper.getFilter());
    		}
			
			return containerValueWrapper;
	  }

	public <O extends ObjectType, C extends Containerable> List<ItemWrapper> createProperties(ContainerValueWrapper<C> cWrapper, boolean onlyEmpty) {
		
		result = new OperationResult(CREATE_PROPERTIES);
		
		ContainerWrapper<C> containerWrapper = cWrapper.getContainer();
		PrismContainerDefinition<C> definition = containerWrapper.getItemDefinition();

		List<ItemWrapper> properties = new ArrayList<>();

		if (definition == null) {
			LOGGER.error("Couldn't get property list from null definition {}",
					new Object[] { containerWrapper.getItem().getElementName() });
			return properties;
		}

		Collection<? extends ItemDefinition> propertyDefinitions = definition.getDefinitions();
		List<PropertyOrReferenceWrapper> propertyOrReferenceWrappers = new ArrayList<>();
		List<ContainerWrapper<C>> containerWrappers = new ArrayList<>();
		propertyDefinitions.forEach(itemDef -> {

			if (itemDef.isIgnored() || skipProperty(itemDef)) {
				LOGGER.trace("Skipping creating wrapper for: {}", itemDef);
				return;
			}
			
			if (itemDef.isExperimental() && !WebModelServiceUtils.isEnableExperimentalFeature(modelServiceLocator)) {
				LOGGER.trace("Skipping creating wrapper for {} because it is experimental a experimental features are not enabled.", itemDef.getName());
				return;
			}

			LOGGER.trace("Creating wrapper for {}", itemDef);
			createPropertyOrReferenceWrapper(itemDef, cWrapper, propertyOrReferenceWrappers, onlyEmpty, cWrapper.getPath());
			createContainerWrapper(itemDef, cWrapper, containerWrappers, onlyEmpty);

		});

		Collections.sort(propertyOrReferenceWrappers, new ItemWrapperComparator());
		Collections.sort(containerWrappers, new ItemWrapperComparator());

		properties.addAll(propertyOrReferenceWrappers);
		properties.addAll(containerWrappers);

		result.recomputeStatus();

		return properties;
	}
    

    private <C extends Containerable> void createPropertyOrReferenceWrapper(ItemDefinition itemDef, ContainerValueWrapper<C> cWrapper, List<PropertyOrReferenceWrapper> properties, boolean onlyEmpty, ItemPath parentPath) {
    	PropertyOrReferenceWrapper propertyOrReferenceWrapper = null;
        if (itemDef instanceof PrismPropertyDefinition) {
        	propertyOrReferenceWrapper = createPropertyWrapper((PrismPropertyDefinition) itemDef, cWrapper, onlyEmpty);
        	
        } else if (itemDef instanceof PrismReferenceDefinition) {
        	propertyOrReferenceWrapper = createReferenceWrapper((PrismReferenceDefinition) itemDef, cWrapper, onlyEmpty);

        }
        if (propertyOrReferenceWrapper != null) {
    		properties.add(propertyOrReferenceWrapper);
    	}
    }

    
    private <C extends Containerable> void createContainerWrapper(ItemDefinition itemDef, ContainerValueWrapper<C> cWrapper,
																  List<ContainerWrapper<C>> properties, boolean onlyEmpty){
    	
    	if (itemDef instanceof PrismContainerDefinition) {
        	
        	if (cWrapper.isMain() && !ObjectType.F_EXTENSION.equals(itemDef.getName()) && !ObjectType.F_METADATA.equals(itemDef.getName())) {
        		return;
        	}
        	
        	ContainerWrapper<C> subContainerWrapper;
//        	if (((PrismContainerDefinition) itemDef).getCompileTimeClass() != null
//					&& ((PrismContainerDefinition) itemDef).getCompileTimeClass().equals(ResourceObjectAssociationType.class)){
//        		if (!ConstructionType.class.equals(cWrapper.getDefinition().getCompileTimeClass())){
//        			return;
//				}
//				ConstructionType construction = (ConstructionType)cWrapper.getContainerValue().getValue();
//				Task task = modelServiceLocator.createSimpleTask("Load resource ref");
//				PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(construction.getResourceRef(),
//						modelServiceLocator, task, result);
//
//				result.computeStatusIfUnknown();
//				if (!result.isAcceptable()) {
//					LOGGER.error("Cannot find resource referenced from shadow. {}", result.getMessage());
//					result.recordPartialError("Could not find resource referenced from shadow.");
//					return;
//				}
//				try {
//					PrismContainer<ResourceObjectAssociationType> assocContainer = cWrapper.getContainer().getItem()
//							.findOrCreateContainer(ConstructionType.F_ASSOCIATION);
//					subContainerWrapper = createAssociationWrapper(resource, construction.getKind(), construction.getIntent() , assocContainer,
//							cWrapper.getObjectStatus(), assocContainer == null ? ContainerStatus.ADDING : ContainerStatus.MODIFYING,
//							new ItemPath(ConstructionType.F_ASSOCIATION));
//				} catch (SchemaException ex){
//					LOGGER.error("Cannot create association container wrapper for construction.", ex);
//					return;
//				}
//
//			} else {
        		subContainerWrapper = createContainerWrapper((PrismContainerDefinition<C>) itemDef, cWrapper, onlyEmpty);
//			}
        	
        	if (subContainerWrapper == null) {
        		return;
        	}
        	
        	if (ObjectType.F_EXTENSION.equals(itemDef.getName())) {
        		properties.addAll(((ContainerValueWrapper)subContainerWrapper.getValues().iterator().next()).getItems());
        	} else {
        		properties.add(subContainerWrapper);
        	}
        }
    }
    
	private <T, C extends Containerable> PropertyWrapper<T> createPropertyWrapper(
			PrismPropertyDefinition<T> def, ContainerValueWrapper<C> cWrapper, boolean onlyEmpty) {
		PrismContainerValue<C> containerValue = cWrapper.getContainerValue();
 
		PrismProperty property = containerValue.findProperty(def.getName());
		boolean propertyIsReadOnly = isItemReadOnly(def, cWrapper);
		
		if (property != null && onlyEmpty) {
			return null;
		}
		
		if (property == null) {
			PrismProperty<T> newProperty = def.instantiate();
//			try {
//				newProperty = containerValue.createProperty(def);
//			} catch (SchemaException e) {
//				LoggingUtils.logException(LOGGER, "Failed to create new property " + def, e);
//				return null;
//			}
			return new PropertyWrapper(cWrapper, newProperty, propertyIsReadOnly, ValueStatus.ADDED, cWrapper.getPath().append(newProperty.getPath()));
		}
		return new PropertyWrapper(cWrapper, property, propertyIsReadOnly, cWrapper.getStatus() == ValueStatus.ADDED ? ValueStatus.ADDED: ValueStatus.NOT_CHANGED, property.getPath());
	}

	private <C extends Containerable> ReferenceWrapper createReferenceWrapper(PrismReferenceDefinition def, ContainerValueWrapper<C> cWrapper, boolean onlyEmpty) {
		
		PrismContainerValue<C> containerValue = cWrapper.getContainerValue();

        PrismReference reference = containerValue.findReference(def.getName());
        boolean propertyIsReadOnly = isItemReadOnly(def, cWrapper);
        
        if (reference != null && onlyEmpty) {
        	return null;
        }
          
        ReferenceWrapper refWrapper = null;
        if (reference == null) {
        	PrismReference newReference = def.instantiate();
        	refWrapper = new ReferenceWrapper(cWrapper, newReference, propertyIsReadOnly,
                    ValueStatus.ADDED, cWrapper.getPath().append(newReference.getPath()));
        } else {
        
        	refWrapper = new ReferenceWrapper(cWrapper, reference, propertyIsReadOnly,
        		cWrapper.getStatus() == ValueStatus.ADDED ? ValueStatus.ADDED: ValueStatus.NOT_CHANGED, reference.getPath());
        }
        
        //TODO: other special cases?
	     if (QNameUtil.match(AbstractRoleType.F_APPROVER_REF, def.getName()) || QNameUtil.match(AbstractRoleType.F_APPROVER_REF, def.getName())) {
	    	 refWrapper.setTargetTypes(Arrays.asList(FocusType.COMPLEX_TYPE, OrgType.COMPLEX_TYPE));
	     } else {
	    	 
	    	 QName targetType = def.getTargetTypeName();
	    	 
	    	 if (targetType == null || ObjectType.COMPLEX_TYPE.equals(targetType)) {
	    		 refWrapper.setTargetTypes(WebComponentUtil.createObjectTypeList());
	    	 } else if (AbstractRoleType.COMPLEX_TYPE.equals(targetType)) {
	    		 refWrapper.setTargetTypes(WebComponentUtil.createAbstractRoleTypeList());
	    	 } else if (FocusType.COMPLEX_TYPE.equals(targetType)) {
	    		 refWrapper.setTargetTypes(WebComponentUtil.createFocusTypeList());
	    	 } else {
	    		 refWrapper.setTargetTypes(Arrays.asList(def.getTargetTypeName()));
	    	 }
	     }

		if (QNameUtil.match(AbstractRoleType.F_TENANT_REF, def.getName())) {
			refWrapper.setFilter(EqualFilter.createEqual(new ItemPath(OrgType.F_TENANT), null, null,
					modelServiceLocator.getPrismContext(), Boolean.TRUE));
		}

		return refWrapper;
        
	}
	
	private <C extends Containerable> ContainerWrapper<C> createContainerWrapper(PrismContainerDefinition<C> def,
			ContainerValueWrapper<C> cWrapper, boolean onlyEmpty) {

		PrismContainerValue<C> containerValue = cWrapper.getContainerValue();

		PrismContainer<C> container = containerValue.findContainer(def.getName());

		// TODO: hack, temporary because of recurcive dependecies
		if (PolicyConstraintsType.F_AND.equals(def.getName()) || PolicyConstraintsType.F_OR.equals(def.getName()) ||
				PolicyConstraintsType.F_NOT.equals(def.getName()) || PolicyConstraintsType.F_TRANSITION.equals(def.getName())) {
			return null;
		}
		
		if (container != null && onlyEmpty) {
			return null;
		}

//		if (!def.isSingleValue() && (container == null || container.isEmpty())){
//			return null;
//		}

		if (container == null) {
			PrismContainer<C> newContainer;
			try {
				newContainer = (PrismContainer) def.instantiate();
				newContainer.setParent(containerValue);
//				containerValue.add(newContainer);
			} catch (SchemaException e) {
				LoggingUtils.logException(LOGGER, "Cannot create container " + def.getName(), e);
				return null;
			}
			return createContainerWrapper(newContainer, cWrapper.getObjectStatus(), ContainerStatus.ADDING,
					cWrapper.getPath().append(new ItemPath(newContainer.getElementName())));
		}
		return createContainerWrapper(container, cWrapper.getObjectStatus(), cWrapper.getStatus() == ValueStatus.ADDED ? ContainerStatus.ADDING: ContainerStatus.MODIFYING, container.getPath());
	}

	private <C extends Containerable> boolean isItemReadOnly(ItemDefinition def, ContainerValueWrapper<C> cWrapper) {
		if (cWrapper == null || cWrapper.getStatus() == ValueStatus.NOT_CHANGED) {

			return MetadataType.COMPLEX_TYPE.equals(cWrapper.getDefinition().getName()) || cWrapper.isReadonly() || !def.canModify();
		}
			
		return MetadataType.COMPLEX_TYPE.equals(cWrapper.getDefinition().getName()) || cWrapper.isReadonly() || !def.canAdd();
		
	}

    /**
     * This methods check if we want to show property in form (e.g.
     * failedLogins, fetchResult, lastFailedLoginTimestamp must be invisible)
     *
     * @return
     * @deprecated will be implemented through annotations in schema
     */
    @Deprecated
    private boolean skipProperty(ItemDefinition<? extends Item> def) {
    	if (def == null){
    		return true;
		}
        final List<QName> names = new ArrayList<>();
        names.add(PasswordType.F_FAILED_LOGINS);
        names.add(PasswordType.F_LAST_FAILED_LOGIN);
        names.add(PasswordType.F_LAST_SUCCESSFUL_LOGIN);
        names.add(PasswordType.F_PREVIOUS_SUCCESSFUL_LOGIN);
        names.add(ObjectType.F_FETCH_RESULT);
        // activation
        names.add(ActivationType.F_EFFECTIVE_STATUS);
        names.add(ActivationType.F_VALIDITY_STATUS);
        // user
        names.add(UserType.F_RESULT);
        // org and roles
        names.add(OrgType.F_APPROVAL_PROCESS);
        names.add(OrgType.F_APPROVER_EXPRESSION);
        names.add(OrgType.F_AUTOMATICALLY_APPROVED);
        names.add(OrgType.F_CONDITION);
        // focus
        names.add(FocusType.F_LINK);
        names.add(FocusType.F_LINK_REF);
        names.add(FocusType.F_PERSONA_REF);

        for (QName name : names) {
            if (name.equals(def.getName())) {
                return true;
            }
        }

        return false;
    }
}
