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

    public <T extends PrismContainer> ContainerWrapper createContainerWrapper(ObjectWrapper objectWrapper,
                                                                              T container,
                                                                              ContainerStatus status,
                                                                              ItemPath path) {

        result = new OperationResult(CREATE_PROPERTIES);

        ContainerWrapper cWrapper = new ContainerWrapper(objectWrapper, container, status, path);

        List<ItemWrapper> properties = createProperties(cWrapper, result);
        cWrapper.setProperties(properties);

        cWrapper.computeStripes();
        
        return cWrapper;
    }

	public <T extends PrismContainer> ContainerWrapper createContainerWrapper(T container, ContainerStatus status, ItemPath path, boolean readonly) {

		result = new OperationResult(CREATE_PROPERTIES);

		ContainerWrapper cWrapper = new ContainerWrapper(container, status, path, readonly);

		List<ItemWrapper> properties = createProperties(cWrapper, result);
		cWrapper.setProperties(properties);
		
		cWrapper.computeStripes();

		return cWrapper;
	}

    private List<ItemWrapper> createProperties(ContainerWrapper cWrapper, OperationResult result) {
        ObjectWrapper objectWrapper = cWrapper.getObject();
        PrismContainer container = cWrapper.getItem();
        PrismContainerDefinition containerDefinition = cWrapper.getItemDefinition();

        List<ItemWrapper> properties = new ArrayList<>();

        PrismContainerDefinition definition;
		if (objectWrapper == null) {
			definition = containerDefinition;
		} else {
			PrismObject parent = objectWrapper.getObject();
			Class clazz = parent.getCompileTimeClass();
			if (ShadowType.class.isAssignableFrom(clazz)) {
				QName name = containerDefinition.getName();

				if (ShadowType.F_ATTRIBUTES.equals(name)) {
					try {
						definition = objectWrapper.getRefinedAttributeDefinition();

						if (definition == null) {
							PrismReference resourceRef = parent.findReference(ShadowType.F_RESOURCE_REF);
							PrismObject<ResourceType> resource = resourceRef.getValue().getObject();

							definition = modelServiceLocator
									.getModelInteractionService()
									.getEditObjectClassDefinition((PrismObject<ShadowType>) objectWrapper.getObject(), resource,
											AuthorizationPhaseType.REQUEST)
									.toResourceAttributeContainerDefinition();

							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Refined account def:\n{}", definition.debugDump());
							}
						}
					} catch (Exception ex) {
						LoggingUtils.logUnexpectedException(LOGGER,
								"Couldn't load definitions from refined schema for shadow", ex);
						result.recordFatalError(
								"Couldn't load definitions from refined schema for shadow, reason: "
										+ ex.getMessage(), ex);

						return properties;
					}
				} else {
					definition = containerDefinition;
				}
			} else if (ResourceType.class.isAssignableFrom(clazz)) {
				if (containerDefinition != null) {
					definition = containerDefinition;
				} else {
					definition = container.getDefinition();
				}
			} else {
				definition = containerDefinition;
			}
		}

        if (definition == null) {
            LOGGER.error("Couldn't get property list from null definition {}",
                    new Object[]{container.getElementName()});
            return properties;
        }

        // assignments are treated in a special way -- we display names of
        // org.units and roles
        // (but only if ObjectWrapper.isShowAssignments() is true; otherwise
        // they are filtered out by ObjectWrapper)
        if (container.getCompileTimeClass() != null
                && AssignmentType.class.isAssignableFrom(container.getCompileTimeClass())) {

            for (Object o : container.getValues()) {
                PrismContainerValue<AssignmentType> pcv = (PrismContainerValue<AssignmentType>) o;

                AssignmentType assignmentType = pcv.asContainerable();

                if (assignmentType.getTargetRef() == null) {
                    continue;
                }

                // hack... we want to create a definition for Name
                // PrismPropertyDefinition def = ((PrismContainerValue)
                // pcv.getContainer().getParent()).getContainer().findProperty(ObjectType.F_NAME).getDefinition();
                PrismPropertyDefinitionImpl def = new PrismPropertyDefinitionImpl(ObjectType.F_NAME,
                        DOMUtil.XSD_STRING, pcv.getPrismContext());

                if (OrgType.COMPLEX_TYPE.equals(assignmentType.getTargetRef().getType())) {
                    def.setDisplayName("Org.Unit");
                    def.setDisplayOrder(100);
                } else if (RoleType.COMPLEX_TYPE.equals(assignmentType.getTargetRef().getType())) {
                    def.setDisplayName("Role");
                    def.setDisplayOrder(200);
                } else {
                    continue;
                }

                PrismProperty<Object> temp = def.instantiate();

                String value = formatAssignmentBrief(assignmentType);

                temp.setValue(new PrismPropertyValue<Object>(value));
                // TODO: do this.isReadOnly() - is that OK? (originally it was the default behavior for all cases)
                properties.add(new PropertyWrapper(cWrapper, temp, cWrapper.isReadonly(), ValueStatus.NOT_CHANGED));
            }

        } else if (isShadowAssociation(cWrapper)) {
        	
        	// HACK: this should not be here. Find a better place.
        	cWrapper.setDisplayName("prismContainer.shadow.associations");
        	
            PrismContext prismContext = objectWrapper.getObject().getPrismContext();
            Map<QName, PrismContainer<ShadowAssociationType>> assocMap = new HashMap<>();
            PrismContainer<ShadowAssociationType> associationContainer = cWrapper.getItem();
        	if (associationContainer != null && associationContainer.getValues() != null) {
	            // Do NOT load shadows here. This will be huge overhead if there are many associations.
	        	// Load them on-demand (if necessary at all).
	            List<PrismContainerValue<ShadowAssociationType>> associations = associationContainer.getValues();
	            if (associations != null) {
	                for (PrismContainerValue<ShadowAssociationType> cval : associations) {
	                    ShadowAssociationType associationType = cval.asContainerable();
	                    QName assocName = associationType.getName();
	                    PrismContainer<ShadowAssociationType> fractionalContainer = assocMap.get(assocName);
	                    if (fractionalContainer == null) {
	                        fractionalContainer = new PrismContainer<>(ShadowType.F_ASSOCIATION, ShadowAssociationType.class, cval.getPrismContext());
	                        fractionalContainer.setDefinition(cval.getParent().getDefinition());
	                        // HACK: set the name of the association as the element name so wrapper.getName() will return correct data.
	                        fractionalContainer.setElementName(assocName);
	                        assocMap.put(assocName, fractionalContainer);
	                    }
	                    try {
	                        fractionalContainer.add(cval.clone());
	                    } catch (SchemaException e) {
	                        // Should not happen
	                        throw new SystemException("Unexpected error: " + e.getMessage(), e);
	                    }
	                }
	            }
            }

            PrismReference resourceRef = objectWrapper.getObject().findReference(ShadowType.F_RESOURCE_REF);
            PrismObject<ResourceType> resource = resourceRef.getValue().getObject();

            // HACK. The revive should not be here. Revive is no good. The next use of the resource will
            // cause parsing of resource schema. We need some centralized place to maintain live cached copies
            // of resources.
            try {
                resource.revive(prismContext);
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
            RefinedResourceSchema refinedSchema;
            CompositeRefinedObjectClassDefinition rOcDef;
            try {
                refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
                rOcDef = refinedSchema.determineCompositeObjectClassDefinition(objectWrapper.getObject());
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
            // Make sure even empty associations have their wrappers so they can be displayed and edited
            for (RefinedAssociationDefinition assocDef : rOcDef.getAssociationDefinitions()) {
                QName name = assocDef.getName();
                if (!assocMap.containsKey(name)) {
                    PrismContainer<ShadowAssociationType> fractionalContainer = new PrismContainer<>(ShadowType.F_ASSOCIATION, ShadowAssociationType.class, prismContext);
                    fractionalContainer.setDefinition(cWrapper.getItemDefinition());
                    // HACK: set the name of the association as the element name so wrapper.getName() will return correct data.
                    fractionalContainer.setElementName(name);
                    assocMap.put(name, fractionalContainer);
                }
            }

            for (Map.Entry<QName, PrismContainer<ShadowAssociationType>> assocEntry : assocMap.entrySet()) {
            	RefinedAssociationDefinition assocRDef = rOcDef.findAssociationDefinition(assocEntry.getKey());
                AssociationWrapper assocWrapper = new AssociationWrapper(cWrapper, assocEntry.getValue(), 
                		cWrapper.isReadonly(), ValueStatus.NOT_CHANGED, assocRDef);
                properties.add(assocWrapper);
            }

        } else { // if not an assignment
            //hack for CaseWorkItemType as a quick fix; in further versions there is already implemented
            //functionality for  multivalue containers
            if (((container.getValues().size() == 1 || container.getValues().isEmpty())
                    && (containerDefinition == null || containerDefinition.isSingleValue())) ||
                    (container.getValues().size() == 1 && CaseWorkItemType.class.isAssignableFrom(container.getCompileTimeClass()))){

                // there's no point in showing properties for non-single-valued
                // parent containers,
                // so we continue only if the parent is single-valued

                Collection<ItemDefinition> propertyDefinitions = definition.getDefinitions();
                for (ItemDefinition itemDef : propertyDefinitions) {
                    //TODO temporary decision to hide adminGuiConfiguration attribute (MID-3305)
                    if (itemDef != null && itemDef.getName() != null && itemDef.getName().getLocalPart() != null &&
                            itemDef.getName().getLocalPart().equals("adminGuiConfiguration")){
                        continue;
                    }
                    if (itemDef instanceof PrismPropertyDefinition) {

                        PrismPropertyDefinition def = (PrismPropertyDefinition) itemDef;
                        if (def.isIgnored() || skipProperty(def)) {
                            continue;
                        }
                        if (!cWrapper.isShowInheritedObjectAttributes()
                                && INHERITED_OBJECT_ATTRIBUTES.contains(def.getName())) {
                            continue;
                        }

                        // capability handling for activation properties
                        if (isShadowActivation(cWrapper) && !hasActivationCapability(cWrapper, def)) {
                            continue;
                        }

                        if (isShadowAssociation(cWrapper)) {
                            continue;
                        }

                        PrismProperty property = container.findProperty(def.getName());
                        boolean propertyIsReadOnly;
                        // decision is based on parent object status, not this
                        // container's one (because container can be added also
                        // to an existing object)
                        if (objectWrapper == null || objectWrapper.getStatus() == ContainerStatus.MODIFYING) {

                            propertyIsReadOnly = cWrapper.isReadonly() || !def.canModify();
                        } else {
                            propertyIsReadOnly = cWrapper.isReadonly() || !def.canAdd();
                        }
                        if (property == null) {
                            properties.add(new PropertyWrapper(cWrapper, def.instantiate(), propertyIsReadOnly,
                                    ValueStatus.ADDED));
                        } else {
                            properties.add(new PropertyWrapper(cWrapper, property, propertyIsReadOnly,
                                    ValueStatus.NOT_CHANGED));
                        }
                    } else if (itemDef instanceof PrismReferenceDefinition) {
                        PrismReferenceDefinition def = (PrismReferenceDefinition) itemDef;

                        if (INHERITED_OBJECT_ATTRIBUTES.contains(def.getName())) {
                            continue;
                        }

                        PrismReference reference = container.findReference(def.getName());
                        boolean propertyIsReadOnly;
                        // decision is based on parent object status, not this
                        // container's one (because container can be added also
                        // to an existing object)
                        if (objectWrapper == null || objectWrapper.getStatus() == ContainerStatus.MODIFYING) {

                            propertyIsReadOnly = !def.canModify();
                        } else {
                            propertyIsReadOnly = !def.canAdd();
                        }
                        if (reference == null) {
                            properties.add(new ReferenceWrapper(cWrapper, def.instantiate(), propertyIsReadOnly,
                                    ValueStatus.ADDED));
                        } else {
                            properties.add(new ReferenceWrapper(cWrapper, reference, propertyIsReadOnly,
                                    ValueStatus.NOT_CHANGED));
                        }

                    }
                }
            }
        }

        Collections.sort(properties, new ItemWrapperComparator());

        result.recomputeStatus();

        return properties;
    }

	private boolean isShadowAssociation(ContainerWrapper cWrapper) {
        ObjectWrapper oWrapper = cWrapper.getObject();
		if (oWrapper == null) {
			return false;
		}
        PrismContainer container = cWrapper.getItem();

        if (!ShadowType.class.isAssignableFrom(oWrapper.getObject().getCompileTimeClass())) {
            return false;
        }

        if (!ShadowType.F_ASSOCIATION.equals(container.getElementName())) {
            return false;
        }

        return true;
    }

    private boolean isShadowActivation(ContainerWrapper cWrapper) {
        ObjectWrapper oWrapper = cWrapper.getObject();
		if (oWrapper == null) {
			return false;
		}
        PrismContainer container = cWrapper.getItem();

        if (!ShadowType.class.isAssignableFrom(oWrapper.getObject().getCompileTimeClass())) {
            return false;
        }

        if (!ShadowType.F_ACTIVATION.equals(container.getElementName())) {
            return false;
        }

        return true;
    }

    private boolean hasActivationCapability(ContainerWrapper cWrapper, PrismPropertyDefinition def) {
        ObjectWrapper oWrapper = cWrapper.getObject();
        ShadowType shadow = (ShadowType) oWrapper.getObject().asObjectable();

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
            switch (assignment.getActivation().getAdministrativeStatus()) {
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
    private boolean skipProperty(PrismPropertyDefinition def) {
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
