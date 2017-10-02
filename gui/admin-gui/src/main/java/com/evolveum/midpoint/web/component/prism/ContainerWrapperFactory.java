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
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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

		ContainerWrapper<C> cWrapper = new ContainerWrapper<C>(container, status, path, readonly);

		List<ContainerValueWrapper<C>> containerValues = createContainerValues(cWrapper, path);
        cWrapper.setProperties(containerValues);
        cWrapper.computeStripes();

        
		return cWrapper;
	}

	  private <C extends Containerable> List<ContainerValueWrapper<C>> createContainerValues(ContainerWrapper<C> cWrapper, ItemPath path) {
	    	List<ContainerValueWrapper<C>> containerValueWrappers = new ArrayList<>();
	    	PrismContainer<C> container = cWrapper.getItem();
	    	
	    	if (container.isEmpty()) {
	    		PrismContainerValue<C> pcv = container.createNewValue();
	    		 ContainerValueWrapper<C> containerValueWrapper = createContainerValueWrapper(cWrapper, pcv, ValueStatus.ADDED, cWrapper.getPath());
	    		
	    		containerValueWrappers.add(containerValueWrapper);
	    		return containerValueWrappers;
	    	}
	    	
	    	container.getValues().forEach(pcv -> {
	    		ContainerValueWrapper<C> containerValueWrapper = createContainerValueWrapper(cWrapper, pcv, ValueStatus.NOT_CHANGED, pcv.getPath());
    			containerValueWrappers.add(containerValueWrapper);
	    	});
	    	
	    	
	    	return containerValueWrappers;
	    }
	  
	  public <C extends Containerable> ContainerValueWrapper<C> createContainerValueWrapper(ContainerWrapper cWrapper, PrismContainerValue<C> value, ValueStatus status, ItemPath path){
		  ContainerValueWrapper<C> containerValueWrapper = new ContainerValueWrapper<C>(cWrapper, value, status, path);
		    
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

    
    private <C extends Containerable> void createContainerWrapper(ItemDefinition itemDef, ContainerValueWrapper<C> cWrapper, List<ContainerWrapper<C>> properties, boolean onlyEmpty) {
    	
    	if (itemDef instanceof PrismContainerDefinition) {
        	
        	if (cWrapper.isMain() && !ObjectType.F_EXTENSION.equals(itemDef.getName()) && !ObjectType.F_METADATA.equals(itemDef.getName())) {
        		return;
        	}
        	
        	ContainerWrapper<C> subContainerWrapper = createContainerWrapper((PrismContainerDefinition<C>) itemDef, cWrapper, onlyEmpty);
        	
        	
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
		return new PropertyWrapper(cWrapper, property, propertyIsReadOnly, ValueStatus.NOT_CHANGED, property.getPath());
	}

	private <C extends Containerable> ReferenceWrapper createReferenceWrapper(PrismReferenceDefinition def, ContainerValueWrapper<C> cWrapper, boolean onlyEmpty) {
		
		PrismContainerValue<C> containerValue = cWrapper.getContainerValue();

        PrismReference reference = containerValue.findReference(def.getName());
        boolean propertyIsReadOnly = isItemReadOnly(def, cWrapper);
        
        if (reference != null && onlyEmpty) {
        	return null;
        }
          
        if (reference == null) {
        	PrismReference newReference = def.instantiate();
//        	try {
//				containerValue.add(newReference);
//			} catch (SchemaException e) {
//				LoggingUtils.logException(LOGGER, "Failed to create new reference " + def, e);
//				return null;
//			}
            return new ReferenceWrapper(cWrapper, newReference, propertyIsReadOnly,
                    ValueStatus.ADDED, cWrapper.getPath().append(newReference.getPath()));
        }
        
        return new ReferenceWrapper(cWrapper, reference, propertyIsReadOnly,
                    ValueStatus.NOT_CHANGED, reference.getPath());
        
	}
	
	private <C extends Containerable> ContainerWrapper<C> createContainerWrapper(PrismContainerDefinition<C> def,
			ContainerValueWrapper<C> cWrapper, boolean onlyEmpty) {

		PrismContainerValue<C> containerValue = cWrapper.getContainerValue();

		PrismContainer<C> container = containerValue.findContainer(def.getName());

		// TODO: hack, temporary because of recurcive dependecies
		if (PolicyConstraintsType.COMPLEX_TYPE.equals(def.getTypeName())) {
			return createPolicyConstraintsContainer(container, def, cWrapper);
		}
		
		if (container != null && onlyEmpty) {
			return null;
		}
		
		if (container == null) {
			PrismContainer<C> newContainer;
			try {
				newContainer = (PrismContainer) def.instantiate();
//				containerValue.add(newContainer);
			} catch (SchemaException e) {
				LoggingUtils.logException(LOGGER, "Cannot create container " + def.getName(), e);
				return null;
			}
			return createContainerWrapper(newContainer, ContainerStatus.ADDING, cWrapper.getPath().append(newContainer.getPath()));
		}
		return createContainerWrapper(container, ContainerStatus.MODIFYING, container.getPath());
	}
	
	private <C extends Containerable> ContainerWrapper<C> createPolicyConstraintsContainer(PrismContainer<C> policyConstraintsContainer, PrismContainerDefinition<C> def, ContainerValueWrapper<C> parentContainer) {
		if (policyConstraintsContainer != null) {
			return createContainerWrapper(policyConstraintsContainer, ContainerStatus.MODIFYING, policyConstraintsContainer.getPath());
		}
		
		return null;
		
	}
	
	private <C extends Containerable> boolean isItemReadOnly(ItemDefinition def, ContainerValueWrapper<C> cWrapper) {
		if (cWrapper == null || cWrapper.getStatus() == ValueStatus.NOT_CHANGED) {

			return MetadataType.COMPLEX_TYPE.equals(cWrapper.getDefinition().getName()) || cWrapper.isReadonly() || !def.canModify();
		}
			
		return MetadataType.COMPLEX_TYPE.equals(cWrapper.getDefinition().getName()) || cWrapper.isReadonly() || !def.canAdd();
		
	}

//private void createShadowContainer(PrismContainerDefinition containerDefinition, ContainerWrapper containerWrapper, PrismContainerDefinition definition) throws SchemaException {
//	QName name = containerDefinition.getName();
//
//	if (ShadowType.F_ATTRIBUTES.equals(name)) {
//			definition = null;//containerWrapper.getObject().getRefinedAttributeDefinition();
//
//			if (definition == null) {
//				PrismReference resourceRef = containerWrapper.getItem().findReference(ShadowType.F_RESOURCE_REF);
//				PrismObject<ResourceType> resource = resourceRef.getValue().getObject();
//
////				definition = modelServiceLocator
////						.getModelInteractionService()
////						.getEditObjectClassDefinition((PrismObject<ShadowType>) containerWrapper.getObject().getObject(), resource,
////								AuthorizationPhaseType.REQUEST)
////						.toResourceAttributeContainerDefinition();
//
//				if (LOGGER.isTraceEnabled()) {
//					LOGGER.trace("Refined account def:\n{}", definition.debugDump());
//				}
//			}
//		
//	
//	} else {
//		definition = containerDefinition;
//	}
//}

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

//	private boolean isShadowAssociation(ContainerWrapper cWrapper) {
////        ObjectWrapper oWrapper = cWrapper.getObject();
////		if (oWrapper == null) {
////			return false;
////		}
//        PrismContainer container = cWrapper.getItem();
////
//        if (!ShadowType.class.isAssignableFrom(container.getCompileTimeClass())) {
//            return false;
//        }
//
//        if (!ShadowType.F_ASSOCIATION.equals(container.getElementName())) {
//            return false;
//        }
//
//        return true;
//    }
//
//    private boolean isShadowActivation(ContainerWrapper cWrapper) {
////        ObjectWrapper oWrapper = cWrapper.getObject();
////		if (oWrapper == null) {
////			return false;
////		}
//        PrismContainer oWrapper = cWrapper.getItem();
//       
//        if (!ShadowType.class.isAssignableFrom(oWrapper.getCompileTimeClass())) {
//            return false;
//        }
//
//        if (!ShadowType.F_ACTIVATION.equals(oWrapper.getElementName())) {
//            return false;
//        }
//
//        return true;
//    }
//
//    private boolean hasActivationCapability(ContainerWrapper cWrapper, PrismPropertyDefinition def) {
//        PrismContainer oWrapper = cWrapper.getItem();
//        if (!(oWrapper instanceof PrismObject)) {
//        	return false;
//        }
//        ObjectType objectType = (ObjectType) ((PrismObject)oWrapper).asObjectable();
//        if (!(objectType instanceof ShadowType)) {
//        	return false;
//        }
//
//        ShadowType shadow = (ShadowType) objectType;
//        ActivationCapabilityType cap = ResourceTypeUtil.getEffectiveCapability(shadow.getResource(),
//                ActivationCapabilityType.class);
//
//        if (ActivationType.F_VALID_FROM.equals(def.getName()) && CapabilityUtil.getEffectiveActivationValidFrom(cap) == null) {
//            return false;
//        }
//
//        if (ActivationType.F_VALID_TO.equals(def.getName()) && CapabilityUtil.getEffectiveActivationValidTo(cap) == null) {
//            return false;
//        }
//
//        if (ActivationType.F_ADMINISTRATIVE_STATUS.equals(def.getName()) && CapabilityUtil.getEffectiveActivationStatus(cap) == null) {
//            return false;
//        }
//
//        return true;
//    }

    // FIXME temporary - brutal hack - the following three methods are copied from
    // AddRoleAssignmentAspect - Pavol M.

//    private String formatAssignmentBrief(AssignmentType assignment) {
//        StringBuilder sb = new StringBuilder();
//        if (assignment.getTarget() != null) {
//            sb.append(assignment.getTarget().getName());
//        } else {
//            sb.append(assignment.getTargetRef().getOid());
//        }
//        if (assignment.getActivation() != null
//                && (assignment.getActivation().getValidFrom() != null || assignment.getActivation()
//                .getValidTo() != null)) {
//            sb.append(" ");
//            sb.append("(");
//            sb.append(formatTimeIntervalBrief(assignment));
//            sb.append(")");
//        }
//        if (assignment.getActivation() != null) {
//            switch (assignment.getActivation().getEffectiveStatus()) {
//                case ARCHIVED:
//                    sb.append(", archived");
//                    break; // TODO i18n
//                case ENABLED:
//                    sb.append(", enabled");
//                    break;
//                case DISABLED:
//                    sb.append(", disabled");
//                    break;
//            }
//        }
//        return sb.toString();
//    }
//
//    public static String formatTimeIntervalBrief(AssignmentType assignment) {
//        StringBuilder sb = new StringBuilder();
//        if (assignment != null
//                && assignment.getActivation() != null
//                && (assignment.getActivation().getValidFrom() != null || assignment.getActivation()
//                .getValidTo() != null)) {
//            if (assignment.getActivation().getValidFrom() != null
//                    && assignment.getActivation().getValidTo() != null) {
//                sb.append(formatTime(assignment.getActivation().getValidFrom()));
//                sb.append("-");
//                sb.append(formatTime(assignment.getActivation().getValidTo()));
//            } else if (assignment.getActivation().getValidFrom() != null) {
//                sb.append("from ");
//                sb.append(formatTime(assignment.getActivation().getValidFrom()));
//            } else {
//                sb.append("to ");
//                sb.append(formatTime(assignment.getActivation().getValidTo()));
//            }
//        }
//        return sb.toString();
//    }
//
//    private static String formatTime(XMLGregorianCalendar time) {
//        DateFormat formatter = DateFormat.getDateInstance();
//        return formatter.format(time.toGregorianCalendar().getTime());
//    }

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
