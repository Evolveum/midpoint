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

import com.evolveum.midpoint.common.refinery.CompositeRefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;

import org.apache.commons.lang.Validate;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.text.DateFormat;
import java.util.*;

/**
 * @author Viliam Repan (lazyman)
 */
public class ContainerWrapperFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerWrapperFactory.class);

    private static final String DOT_CLASS = ContainerWrapperFactory.class.getName() + ".";
    private static final String CREATE_PROPERTIES = DOT_CLASS + "createProperties";

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
                                                                              ContainerStatus status,
                                                                              ItemPath path) {

        result = new OperationResult(CREATE_PROPERTIES);

        ContainerWrapper<C> cWrapper = new ContainerWrapper(container, status, path);
        
        List<ContainerValueWrapper<C>> containerValues = createContainerValues(cWrapper, path);
        cWrapper.setProperties(containerValues);
        cWrapper.computeStripes();

        return cWrapper;
    }
    
  
	public <C extends Containerable> ContainerWrapper<C> createContainerWrapper(PrismContainer<C> container, ContainerStatus status, ItemPath path, boolean readonly) {

		result = new OperationResult(CREATE_PROPERTIES);

		ContainerWrapper<C> cWrapper = new ContainerWrapper<C>(container, status, path, readonly, false);

		List<ContainerValueWrapper<C>> containerValues = createContainerValues(cWrapper, path);
        cWrapper.setProperties(containerValues);
        cWrapper.computeStripes();

		
//		List<ItemWrapper> properties = createProperties(cWrapper, result);
//		cWrapper.setProperties(properties);
//
//		cWrapper.computeStripes();

		return cWrapper;
	}

	  private <C extends Containerable> List<ContainerValueWrapper<C>> createContainerValues(ContainerWrapper<C> cWrapper, ItemPath path) {
	    	List<ContainerValueWrapper<C>> containerValueWrappers = new ArrayList<>();
	    	PrismContainer<C> container = cWrapper.getItem();
	    	
	    	if (container.isEmpty()) {
	    		PrismContainerValue newValue = container.createNewValue();
	    		ContainerValueWrapper<C> containerValueWrapper = new ContainerValueWrapper<>(cWrapper, newValue, ValueStatus.ADDED, path);
		    	
    			List<ItemWrapper> properties = createProperties(containerValueWrapper, result);
    			containerValueWrapper.setProperties(properties);
    			containerValueWrappers.add(containerValueWrapper);
    			return containerValueWrappers;
	    	}
	    	
	    	for (PrismContainerValue<C> containerValue : container.getValues()){
	    			ContainerValueWrapper<C> containerValueWrapper = new ContainerValueWrapper<>(cWrapper, containerValue, ValueStatus.NOT_CHANGED, containerValue.getPath());
	    	
	    			List<ItemWrapper> properties = createProperties(containerValueWrapper, result);
	    			containerValueWrapper.setProperties(properties);
	    			containerValueWrappers.add(containerValueWrapper);
	    	}
	    	
	    	return containerValueWrappers;
	    }

    private <O extends ObjectType, C extends Containerable> List<ItemWrapper> createProperties(ContainerValueWrapper<C> cWrapper, OperationResult result) {
        ContainerWrapper<C> containerWrapper = cWrapper.getContainer();
//        PrismContainerValue<C> containerValue = cWrapper.getContainerValue();
        PrismContainerDefinition<C> definition = containerWrapper.getItemDefinition();

        List<ItemWrapper> properties = new ArrayList<>();

//        PrismContainerDefinition<C> definition = null;
//		if (containerWrapper == null) {
//			definition = containerDefinition;
//		} else {
////			PrismObject<O> parent = containerWrapper.getObject().getObject();
//			if (containerWrapper.isMain()) {
//			
//			
//			Class<O> clazz = ((PrismObject<O>) containerWrapper.getItem()).getCompileTimeClass();
//			if (ShadowType.class.isAssignableFrom(clazz)) {
//				try {
//					createShadowContainer(containerDefinition, containerWrapper, definition);
//				} catch (Exception ex) {
//					LoggingUtils.logUnexpectedException(LOGGER,
//							"Couldn't load definitions from refined schema for shadow", ex);
//					result.recordFatalError(
//							"Couldn't load definitions from refined schema for shadow, reason: "
//									+ ex.getMessage(), ex);
//	
//					return properties;
//				}
//			} else if (ResourceType.class.isAssignableFrom(clazz)) {
//				if (containerDefinition != null) {
//					definition = containerDefinition;
//				} else {
//					definition = containerWrapper.getItemDefinition();
//				}
//			} else {
//				definition = containerDefinition;
//			}
//			} else {
//				definition = containerDefinition;
//			}
//		}

        if (definition == null) {
            LOGGER.error("Couldn't get property list from null definition {}",
                    new Object[]{containerWrapper.getItem().getElementName()});
            return properties;
        }

        // assignments are treated in a special way -- we display names of
        // org.units and roles
        // (but only if ObjectWrapper.isShowAssignments() is true; otherwise
        // they are filtered out by ObjectWrapper)
//        if (containerWrapper.getItem().getCompileTimeClass() != null
//                && AssignmentType.class.isAssignableFrom(containerWrapper.getItem().getCompileTimeClass())) {
//
//            for (Object o : container.getValues()) {
//                PrismContainerValue<AssignmentType> pcv = (PrismContainerValue<AssignmentType>) o;
//
//                AssignmentType assignmentType = pcv.asContainerable();
//
//                if (assignmentType.getTargetRef() == null) {
//                    continue;
//                }
//
//                // hack... we want to create a definition for Name
//                // PrismPropertyDefinition def = ((PrismContainerValue)
//                // pcv.getContainer().getParent()).getContainer().findProperty(ObjectType.F_NAME).getDefinition();
//                PrismPropertyDefinitionImpl def = new PrismPropertyDefinitionImpl(ObjectType.F_NAME,
//                        DOMUtil.XSD_STRING, pcv.getPrismContext());
//
//                if (OrgType.COMPLEX_TYPE.equals(assignmentType.getTargetRef().getType())) {
//                    def.setDisplayName("Org.Unit");
//                    def.setDisplayOrder(100);
//                } else if (RoleType.COMPLEX_TYPE.equals(assignmentType.getTargetRef().getType())) {
//                    def.setDisplayName("Role");
//                    def.setDisplayOrder(200);
//                } else {
//                    continue;
//                }
//
//                PrismProperty<Object> temp = def.instantiate();
//
//                String value = formatAssignmentBrief(assignmentType);
//
//                temp.setValue(new PrismPropertyValue<Object>(value));
//                // TODO: do this.isReadOnly() - is that OK? (originally it was the default behavior for all cases)
//                properties.add(new PropertyWrapper(cWrapper, temp, cWrapper.isReadonly(), ValueStatus.NOT_CHANGED));
//            }
//
//        } else if (isShadowAssociation(cWrapper)) {
//
//        	// HACK: this should not be here. Find a better place.
//        	cWrapper.setDisplayName("prismContainer.shadow.associations");
//
//            PrismContext prismContext = containerWrapper.getObject().getPrismContext();
//            Map<QName, PrismContainer<ShadowAssociationType>> assocMap = new HashMap<>();
//            PrismContainer<ShadowAssociationType> associationContainer = cWrapper.getItem();
//        	if (associationContainer != null && associationContainer.getValues() != null) {
//	            // Do NOT load shadows here. This will be huge overhead if there are many associations.
//	        	// Load them on-demand (if necessary at all).
//	            List<PrismContainerValue<ShadowAssociationType>> associations = associationContainer.getValues();
//	            if (associations != null) {
//	                for (PrismContainerValue<ShadowAssociationType> cval : associations) {
//	                    ShadowAssociationType associationType = cval.asContainerable();
//	                    QName assocName = associationType.getName();
//	                    PrismContainer<ShadowAssociationType> fractionalContainer = assocMap.get(assocName);
//	                    if (fractionalContainer == null) {
//	                        fractionalContainer = new PrismContainer<>(ShadowType.F_ASSOCIATION, ShadowAssociationType.class, cval.getPrismContext());
//	                        fractionalContainer.setDefinition(cval.getParent().getDefinition());
//	                        // HACK: set the name of the association as the element name so wrapper.getName() will return correct data.
//	                        fractionalContainer.setElementName(assocName);
//	                        assocMap.put(assocName, fractionalContainer);
//	                    }
//	                    try {
//	                        fractionalContainer.add(cval.clone());
//	                    } catch (SchemaException e) {
//	                        // Should not happen
//	                        throw new SystemException("Unexpected error: " + e.getMessage(), e);
//	                    }
//	                }
//	            }
//            }
//
//            PrismReference resourceRef = containerWrapper.getObject().findReference(ShadowType.F_RESOURCE_REF);
//            PrismObject<ResourceType> resource = resourceRef.getValue().getObject();
//
//            // HACK. The revive should not be here. Revive is no good. The next use of the resource will
//            // cause parsing of resource schema. We need some centralized place to maintain live cached copies
//            // of resources.
//            try {
//                resource.revive(prismContext);
//            } catch (SchemaException e) {
//                throw new SystemException(e.getMessage(), e);
//            }
//            RefinedResourceSchema refinedSchema;
//            CompositeRefinedObjectClassDefinition rOcDef;
//            try {
//                refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
//                rOcDef = refinedSchema.determineCompositeObjectClassDefinition(containerWrapper.getObject());
//            } catch (SchemaException e) {
//                throw new SystemException(e.getMessage(), e);
//            }
//            // Make sure even empty associations have their wrappers so they can be displayed and edited
//            for (RefinedAssociationDefinition assocDef : rOcDef.getAssociationDefinitions()) {
//                QName name = assocDef.getName();
//                if (!assocMap.containsKey(name)) {
//                    PrismContainer<ShadowAssociationType> fractionalContainer = new PrismContainer<>(ShadowType.F_ASSOCIATION, ShadowAssociationType.class, prismContext);
//                    fractionalContainer.setDefinition(cWrapper.getItemDefinition());
//                    // HACK: set the name of the association as the element name so wrapper.getName() will return correct data.
//                    fractionalContainer.setElementName(name);
//                    assocMap.put(name, fractionalContainer);
//                }
//            }
//
//            for (Map.Entry<QName, PrismContainer<ShadowAssociationType>> assocEntry : assocMap.entrySet()) {
//            	RefinedAssociationDefinition assocRDef = rOcDef.findAssociationDefinition(assocEntry.getKey());
//                AssociationWrapper assocWrapper = new AssociationWrapper(cWrapper, assocEntry.getValue(),
//                		cWrapper.isReadonly(), ValueStatus.NOT_CHANGED, assocRDef);
//                properties.add(assocWrapper);
//            }
//
//        } else { // if not an assignment
    
                            // there's no point in showing properties for non-single-valued
                // parent containers,
                // so we continue only if the parent is single-valued

            	Collection<? extends ItemDefinition> propertyDefinitions = definition.getDefinitions();
            	List<PropertyOrReferenceWrapper> propertyOrReferenceWrappers = new ArrayList<>();
            	List<ContainerWrapper<C>> containerWrappers = new ArrayList<>();
            	propertyDefinitions.forEach( itemDef -> {
            	
            		
            		if (itemDef.isIgnored() || skipProperty(itemDef)) {
            			LOGGER.info("Skipping creating wrapper for: {}", itemDef);
            			return; 
            		}
//            		if (!cWrapper.isShowInheritedObjectAttributes() && INHERITED_OBJECT_ATTRIBUTES.contains(itemDef.getName())) {
//            			return;
//            		}
            		
            		
            		LOGGER.info("Creating wrapper for {}", itemDef);
            		createPropertyOrReferenceWrapper(itemDef, cWrapper, propertyOrReferenceWrappers);
            		createContainerWrapper(itemDef, cWrapper, containerWrappers);
            		
            	});
                
 
//        }

        Collections.sort(propertyOrReferenceWrappers, new ItemWrapperComparator());
        Collections.sort(containerWrappers, new ItemWrapperComparator());
        
        properties.addAll(propertyOrReferenceWrappers);
        properties.addAll(containerWrappers);

        result.recomputeStatus();

        return properties;
    }
    

    private <C extends Containerable> void createPropertyOrReferenceWrapper(ItemDefinition itemDef, ContainerValueWrapper<C> cWrapper, List<PropertyOrReferenceWrapper> properties) {
    	
//    	if (itemDef.isIgnored() || skipProperty(itemDef)) {
//			return;
//		}
//		if (!cWrapper.isShowInheritedObjectAttributes() && INHERITED_OBJECT_ATTRIBUTES.contains(itemDef.getName())) {
//			return;
//		}
    	
//    	PrismContainerValue<C> containerValue = cWrapper.getContainerValue();
    	
    	 //TODO temporary decision to hide adminGuiConfiguration attribute (MID-3305)
//    	if (itemDef != null && itemDef.getName() != null && itemDef.getName().getLocalPart() != null &&
//                itemDef.getName().getLocalPart().equals("adminGuiConfiguration")){
//            continue;
//        }
        if (itemDef instanceof PrismPropertyDefinition) {

           properties.add(createPropertyWrapper((PrismPropertyDefinition) itemDef, cWrapper));
        } else if (itemDef instanceof PrismReferenceDefinition) {
        	properties.add(createReferenceWrapper((PrismReferenceDefinition) itemDef, cWrapper));

        }
    }

    
    private <C extends Containerable> void createContainerWrapper(ItemDefinition itemDef, ContainerValueWrapper<C> cWrapper, List<ContainerWrapper<C>> properties) {
    	
    	if (itemDef instanceof PrismContainerDefinition) {
        	
        	if (cWrapper.isMain() && !ObjectType.F_EXTENSION.equals(itemDef.getName()) && !ObjectType.F_METADATA.equals(itemDef.getName())) {
        		return;
        	}
        	
        	ContainerWrapper<C> subContainerWrapper = createContainerWrapper((PrismContainerDefinition<C>) itemDef, cWrapper);
        	
        	
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
			PrismPropertyDefinition<T> def, ContainerValueWrapper<C> cWrapper) {
		PrismContainerValue<C> containerValue = cWrapper.getContainerValue();
 
		PrismProperty property = containerValue.findProperty(def.getName());
		boolean propertyIsReadOnly = isItemReadOnly(def, cWrapper);
		// decision is based on parent object status, not this
		// container's one (because container can be added also
		// to an existing object)
		
		if (property == null) {
			return new PropertyWrapper(cWrapper, def.instantiate(), propertyIsReadOnly, ValueStatus.ADDED);
		}
		return new PropertyWrapper(cWrapper, property, propertyIsReadOnly, ValueStatus.NOT_CHANGED);
	}

	private <C extends Containerable> ReferenceWrapper createReferenceWrapper(PrismReferenceDefinition def, ContainerValueWrapper<C> cWrapper) {
		
		PrismContainerValue<C> containerValue = cWrapper.getContainerValue();
//		
//        if (INHERITED_OBJECT_ATTRIBUTES.contains(def.getName())) {
//            return null;
//        }

        PrismReference reference = containerValue.findReference(def.getName());
        boolean propertyIsReadOnly = isItemReadOnly(def, cWrapper);
        // decision is based on parent object status, not this
        // container's one (because container can be added also
        // to an existing object)
        
        if (reference == null) {
            return new ReferenceWrapper(cWrapper, def.instantiate(), propertyIsReadOnly,
                    ValueStatus.ADDED);
        }
        
        return new ReferenceWrapper(cWrapper, reference, propertyIsReadOnly,
                    ValueStatus.NOT_CHANGED);
        
	}
	
	private <C extends Containerable> ContainerWrapper<C> createContainerWrapper(PrismContainerDefinition<C> def, ContainerValueWrapper<C> cWrapper) {
		
		PrismContainerValue<C> containerValue = cWrapper.getContainerValue();

		PrismContainer<C> container = containerValue.findContainer(def.getName());
    	
    	//TODO: hack, temporary because of recurcive dependecies
    	if (container == null && 
    			(PolicyConstraintPresentationType.COMPLEX_TYPE.equals(def.getTypeName()) || PolicyConstraintsType.COMPLEX_TYPE.equals(def.getTypeName()))) {
    		return null;
    	}
    	if (container == null) {
    		PrismContainer<C> newContainer;
			try {
				newContainer = (PrismContainer) def.instantiate();
			} catch (SchemaException e) {
				LoggingUtils.logException(LOGGER, "Cannot create container " + def.getName(), e);
				return null;
			}
			return createContainerWrapper(newContainer, ContainerStatus.ADDING, cWrapper.getPath().append(newContainer.getPath()));
    	} 
    		return createContainerWrapper(container, ContainerStatus.MODIFYING, container.getPath());
//    	List<ContainerValueWrapper> subContainerValues = createContainerValues(subContainerWrapper);
//    	subContainerWrapper.setProperties(subContainerValues);
//    	subContainerWrapper.computeStripes();
	}
	
	private <C extends Containerable> boolean isItemReadOnly(ItemDefinition def, ContainerValueWrapper<C> cWrapper) {
		if (cWrapper == null || cWrapper.getStatus() == ValueStatus.NOT_CHANGED) {

			return MetadataType.COMPLEX_TYPE.equals(cWrapper.getDefinition().getName()) || cWrapper.isReadonly() || !def.canModify();
		}
			
		return MetadataType.COMPLEX_TYPE.equals(cWrapper.getDefinition().getName()) || cWrapper.isReadonly() || !def.canAdd();
		
	}

private void createShadowContainer(PrismContainerDefinition containerDefinition, ContainerWrapper containerWrapper, PrismContainerDefinition definition) throws SchemaException {
	QName name = containerDefinition.getName();

	if (ShadowType.F_ATTRIBUTES.equals(name)) {
			definition = null;//containerWrapper.getObject().getRefinedAttributeDefinition();

			if (definition == null) {
				PrismReference resourceRef = containerWrapper.getItem().findReference(ShadowType.F_RESOURCE_REF);
				PrismObject<ResourceType> resource = resourceRef.getValue().getObject();

//				definition = modelServiceLocator
//						.getModelInteractionService()
//						.getEditObjectClassDefinition((PrismObject<ShadowType>) containerWrapper.getObject().getObject(), resource,
//								AuthorizationPhaseType.REQUEST)
//						.toResourceAttributeContainerDefinition();

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Refined account def:\n{}", definition.debugDump());
				}
			}
		
	
	} else {
		definition = containerDefinition;
	}
}

//    public <C extends Containerable> ContainerWrapper<C> createCustomContainerWrapper(C container, ContainerStatus status, ItemPath path,
//                                                                                      boolean readonly, boolean showInheritedAttributes) {
//
//		result = new OperationResult(CREATE_PROPERTIES);
//
//		PrismContainer<C> containerValue = container.asPrismContainerValue().getContainer();
//
//		ContainerWrapper cWrapper = new ContainerWrapper(containerValue, status, path, readonly, showInheritedAttributes);
//
//		List<ItemWrapper> properties = createProperties(container, containerValue.getDefinition(), cWrapper);
//		cWrapper.setProperties(properties);
//
//		cWrapper.computeStripes();
//
//		return cWrapper;
//    }

//private <C extends Containerable> List<ItemWrapper> createProperties(C container, PrismContainerDefinition definition, ContainerWrapper<Containerable> cWrapper) {
//	 Collection<ItemDefinition> propertyDefinitions = definition.getDefinitions();
//
//	 List<ItemWrapper> properties = new ArrayList<>();
//	 for (ItemDefinition itemDef : propertyDefinitions) {
//         //TODO temporary decision to hide adminGuiConfiguration attribute (MID-3305)
//         if (itemDef != null && itemDef.getName() != null && itemDef.getName().getLocalPart() != null &&
//                 itemDef.getName().getLocalPart().equals("adminGuiConfiguration")){
//             continue;
//         }
//         if (itemDef instanceof PrismPropertyDefinition) {
//
//             PrismPropertyDefinition def = (PrismPropertyDefinition) itemDef;
//             if (def.isIgnored() || skipProperty(def)) {
//                 continue;
//             }
//             if (!cWrapper.isShowInheritedObjectAttributes()
//                     && INHERITED_OBJECT_ATTRIBUTES.contains(def.getName())) {
//                 continue;
//             }
//
//             PrismProperty property = container.asPrismContainerValue().findProperty(def.getName());
//             boolean propertyIsReadOnly;
//             // decision is based on parent object status, not this
//             // container's one (because container can be added also
//             // to an existing object)
//             if (cWrapper.getStatus() == ContainerStatus.MODIFYING) {
//
//                 propertyIsReadOnly = cWrapper.isReadonly() || !def.canModify();
//             } else {
//                 propertyIsReadOnly = cWrapper.isReadonly() || !def.canAdd();
//             }
//             if (property == null) {
//                 properties.add(new PropertyWrapper(cWrapper, def.instantiate(), propertyIsReadOnly,
//                         ValueStatus.ADDED));
//             } else {
//                 properties.add(new PropertyWrapper(cWrapper, property, propertyIsReadOnly,
//                         ValueStatus.NOT_CHANGED));
//             }
//         } else if (itemDef instanceof PrismReferenceDefinition) {
//             PrismReferenceDefinition def = (PrismReferenceDefinition) itemDef;
//
//             if (INHERITED_OBJECT_ATTRIBUTES.contains(def.getName())) {
//                 continue;
//             }
//
//             PrismReference reference = container.asPrismContainerValue().findReference(def.getName());
//             boolean propertyIsReadOnly;
//             // decision is based on parent object status, not this
//             // container's one (because container can be added also
//             // to an existing object)
//             if (cWrapper.getStatus() == ContainerStatus.MODIFYING) {
//
//                 propertyIsReadOnly = !def.canModify();
//             } else {
//                 propertyIsReadOnly = !def.canAdd();
//             }
//             if (reference == null) {
//                 properties.add(new ReferenceWrapper(cWrapper, def.instantiate(), propertyIsReadOnly,
//                         ValueStatus.ADDED));
//             } else {
//                 properties.add(new ReferenceWrapper(cWrapper, reference, propertyIsReadOnly,
//                         ValueStatus.NOT_CHANGED));
//             }
//
//         }
//     }
//
//	 Collections.sort(properties, new ItemWrapperComparator());
//
//        return properties;
//
//}

	private boolean isShadowAssociation(ContainerWrapper cWrapper) {
//        ObjectWrapper oWrapper = cWrapper.getObject();
//		if (oWrapper == null) {
//			return false;
//		}
        PrismContainer container = cWrapper.getItem();
//
        if (!ShadowType.class.isAssignableFrom(container.getCompileTimeClass())) {
            return false;
        }

        if (!ShadowType.F_ASSOCIATION.equals(container.getElementName())) {
            return false;
        }

        return true;
    }

    private boolean isShadowActivation(ContainerWrapper cWrapper) {
//        ObjectWrapper oWrapper = cWrapper.getObject();
//		if (oWrapper == null) {
//			return false;
//		}
        PrismContainer oWrapper = cWrapper.getItem();
       
        if (!ShadowType.class.isAssignableFrom(oWrapper.getCompileTimeClass())) {
            return false;
        }

        if (!ShadowType.F_ACTIVATION.equals(oWrapper.getElementName())) {
            return false;
        }

        return true;
    }

    private boolean hasActivationCapability(ContainerWrapper cWrapper, PrismPropertyDefinition def) {
        PrismContainer oWrapper = cWrapper.getItem();
        if (!(oWrapper instanceof PrismObject)) {
        	return false;
        }
        ObjectType objectType = (ObjectType) ((PrismObject)oWrapper).asObjectable();
        if (!(objectType instanceof ShadowType)) {
        	return false;
        }

        ShadowType shadow = (ShadowType) objectType;
        ActivationCapabilityType cap = ResourceTypeUtil.getEffectiveCapability(shadow.getResource(),
                ActivationCapabilityType.class);

        if (ActivationType.F_VALID_FROM.equals(def.getName()) && CapabilityUtil.getEffectiveActivationValidFrom(cap) == null) {
            return false;
        }

        if (ActivationType.F_VALID_TO.equals(def.getName()) && CapabilityUtil.getEffectiveActivationValidTo(cap) == null) {
            return false;
        }

        if (ActivationType.F_ADMINISTRATIVE_STATUS.equals(def.getName()) && CapabilityUtil.getEffectiveActivationStatus(cap) == null) {
            return false;
        }

        return true;
    }

    // FIXME temporary - brutal hack - the following three methods are copied from
    // AddRoleAssignmentAspect - Pavol M.

    private String formatAssignmentBrief(AssignmentType assignment) {
        StringBuilder sb = new StringBuilder();
        if (assignment.getTarget() != null) {
            sb.append(assignment.getTarget().getName());
        } else {
            sb.append(assignment.getTargetRef().getOid());
        }
        if (assignment.getActivation() != null
                && (assignment.getActivation().getValidFrom() != null || assignment.getActivation()
                .getValidTo() != null)) {
            sb.append(" ");
            sb.append("(");
            sb.append(formatTimeIntervalBrief(assignment));
            sb.append(")");
        }
        if (assignment.getActivation() != null) {
            switch (assignment.getActivation().getEffectiveStatus()) {
                case ARCHIVED:
                    sb.append(", archived");
                    break; // TODO i18n
                case ENABLED:
                    sb.append(", enabled");
                    break;
                case DISABLED:
                    sb.append(", disabled");
                    break;
            }
        }
        return sb.toString();
    }

    public static String formatTimeIntervalBrief(AssignmentType assignment) {
        StringBuilder sb = new StringBuilder();
        if (assignment != null
                && assignment.getActivation() != null
                && (assignment.getActivation().getValidFrom() != null || assignment.getActivation()
                .getValidTo() != null)) {
            if (assignment.getActivation().getValidFrom() != null
                    && assignment.getActivation().getValidTo() != null) {
                sb.append(formatTime(assignment.getActivation().getValidFrom()));
                sb.append("-");
                sb.append(formatTime(assignment.getActivation().getValidTo()));
            } else if (assignment.getActivation().getValidFrom() != null) {
                sb.append("from ");
                sb.append(formatTime(assignment.getActivation().getValidFrom()));
            } else {
                sb.append("to ");
                sb.append(formatTime(assignment.getActivation().getValidTo()));
            }
        }
        return sb.toString();
    }

    private static String formatTime(XMLGregorianCalendar time) {
        DateFormat formatter = DateFormat.getDateInstance();
        return formatter.format(time.toGregorianCalendar().getTime());
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

        for (QName name : names) {
            if (name.equals(def.getName())) {
                return true;
            }
        }

        return false;
    }
}
