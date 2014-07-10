/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationValidityCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.*;

/**
 * @author lazyman
 */
public class ContainerWrapper<T extends PrismContainer> implements ItemWrapper, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerWrapper.class);

    private static final List<QName> INHERITED_OBJECT_ATTRIBUTES = Arrays.asList(ObjectType.F_NAME,
            ObjectType.F_DESCRIPTION, ObjectType.F_FETCH_RESULT, ObjectType.F_PARENT_ORG, ObjectType.F_PARENT_ORG_REF);

    private static final String DOT_CLASS = ContainerWrapper.class.getName() + ".";
    private static final String CREATE_PROPERTIES = DOT_CLASS + "createProperties";

    private String displayName;
    private ObjectWrapper object;
    private T container;
    private ContainerStatus status;

    private boolean main;
    private ItemPath path;
    private List<PropertyWrapper> properties;

    private boolean readonly;
    private boolean showInheritedObjectAttributes;

    private OperationResult result;
    
    private PrismContainerDefinition containerDefinition;

    public ContainerWrapper(ObjectWrapper object, T container, ContainerStatus status, ItemPath path, PageBase pageBase) {
        Validate.notNull(container, "Prism object must not be null.");
        Validate.notNull(status, "Container status must not be null.");
        Validate.notNull(pageBase, "pageBase must not be null.");

        this.object = object;
        this.container = container;
        this.status = status;
        this.path = path;
        main = path == null;
        readonly = object.isReadonly();     // [pm] this is quite questionable
        showInheritedObjectAttributes = object.isShowInheritedObjectAttributes();
        //have to be after setting "main" property
        containerDefinition = getContainerDefinition();
        properties = createProperties(pageBase);
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        if (container != null) {
            container.revive(prismContext);
        }
        if (containerDefinition != null) {
            containerDefinition.revive(prismContext);
        }
        if (properties != null) {
            for (PropertyWrapper propertyWrapper : properties) {
                propertyWrapper.revive(prismContext);
            }
        }
    }


    protected PrismContainerDefinition getContainerDefinition() {
    	if (main) {
    		return object.getDefinition();
    	} else {
        	return object.getDefinition().findContainerDefinition(path);
        }
    }

    OperationResult getResult() {
        return result;
    }

    void clearResult() {
        result = null;
    }

    ObjectWrapper getObject() {
        return object;
    }

    ContainerStatus getStatus() {
        return status;
    }

    public ItemPath getPath() {
        return path;
    }

    public T getItem() {
        return container;
    }

    public List<PropertyWrapper> getProperties() {
        return properties;
    }

    public PropertyWrapper findPropertyWrapper(QName name) {
        Validate.notNull(name, "QName must not be null.");
        for (PropertyWrapper wrapper : getProperties()) {
            if (name.equals(wrapper.getItem().getElementName())) {
                return wrapper;
            }
        }
        return null;
    }

    private List<PropertyWrapper> createProperties(PageBase pageBase) {
        result = new OperationResult(CREATE_PROPERTIES);

        List<PropertyWrapper> properties = new ArrayList<PropertyWrapper>();

        PrismContainerDefinition definition = null;
        PrismObject parent = getObject().getObject();
        Class clazz = parent.getCompileTimeClass();
        if (ShadowType.class.isAssignableFrom(clazz)) {
        	QName name = containerDefinition.getName();

            if (ShadowType.F_ATTRIBUTES.equals(name)) {
                try {
                	definition = object.getRefinedAttributeDefinition();
                	
                	if (definition == null) {
	                    PrismReference resourceRef = parent.findReference(ShadowType.F_RESOURCE_REF);
	                    PrismObject<ResourceType> resource = resourceRef.getValue().getObject();

                        definition = pageBase.getModelInteractionService()
                                .getEditObjectClassDefinition(object.getObject(), resource).toResourceAttributeContainerDefinition();

	                    if (LOGGER.isTraceEnabled()) {
	                        LOGGER.trace("Refined account def:\n{}", definition.debugDump());
	                    }
                	}
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't load definitions from refined schema for shadow", ex);
                    result.recordFatalError("Couldn't load definitions from refined schema for shadow, reason: "
                            + ex.getMessage(), ex);

                    return properties;
                }
            } else {
            	definition = containerDefinition;
            }
        } else {
        	definition = containerDefinition;
        }

        if (definition == null) {
            LOGGER.error("Couldn't get property list from null definition {}", new Object[]{container.getElementName()});
            return properties;
        }

        // assignments are treated in a special way -- we display names of org.units and roles
        // (but only if ObjectWrapper.isShowAssignments() is true; otherwise they are filtered out by ObjectWrapper)
        if (container.getCompileTimeClass() != null && container.getCompileTimeClass().isAssignableFrom(AssignmentType.class)) {

            for (Object o : container.getValues()) {
                PrismContainerValue<AssignmentType> pcv = (PrismContainerValue<AssignmentType>) o;

                AssignmentType assignmentType = pcv.asContainerable();

                if (assignmentType.getTargetRef() == null) {
                    continue;
                }

                // hack... we want to create a definition for Name
                //PrismPropertyDefinition def = ((PrismContainerValue) pcv.getContainer().getParent()).getContainer().findProperty(ObjectType.F_NAME).getDefinition();
                PrismPropertyDefinition def = new PrismPropertyDefinition(ObjectType.F_NAME, DOMUtil.XSD_STRING, pcv.getPrismContext());

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
                properties.add(new PropertyWrapper(this, temp, this.isReadonly(), ValueStatus.NOT_CHANGED));        // todo this.isReadOnly() - is that OK? (originally it was the default behavior for all cases)
            }

        } else if (isShadowAssociation()) {
        	if (object.getAssociations() != null) {
        		for (PrismProperty property : object.getAssociations()) {
        			//TODO: fix this -> for now, read only is supported..
        			PropertyWrapper propertyWrapper = new PropertyWrapper(this, property, true, ValueStatus.NOT_CHANGED);
                	properties.add(propertyWrapper);
        		}
        	}
        	
                	
       } else {            // if not an assignment

            if (container.getValues().size() == 1 ||
                    (container.getValues().isEmpty() && (containerDefinition== null || containerDefinition.isSingleValue()))) {

                // there's no point in showing properties for non-single-valued parent containers,
                // so we continue only if the parent is single-valued

                Collection<PrismPropertyDefinition> propertyDefinitions = definition.getPropertyDefinitions();
                for (PrismPropertyDefinition def : propertyDefinitions) {
                    if (def.isIgnored() || skipProperty(def)) {
                        continue;
                    }
                    if (!showInheritedObjectAttributes && INHERITED_OBJECT_ATTRIBUTES.contains(def.getName())) {
                        continue;
                    }

                    //capability handling for activation properties
                    if (isShadowActivation() && !hasCapability(def)) {
                        continue;
                    }
                    
                    if (isShadowAssociation()) {
                    	continue;
                    }

                    PrismProperty property = container.findProperty(def.getName());
                    boolean propertyIsReadOnly;
                    if (object.getStatus() == ContainerStatus.MODIFYING) {  // decision is based on parent object status, not this container's one (because container can be added also to an existing object)
                        propertyIsReadOnly = !def.canModify();
                    } else {
                        propertyIsReadOnly = !def.canAdd();
                    }
                    if (property == null) {
                        properties.add(new PropertyWrapper(this, def.instantiate(), propertyIsReadOnly, ValueStatus.ADDED));
                    } else {
                        properties.add(new PropertyWrapper(this, property, propertyIsReadOnly, ValueStatus.NOT_CHANGED));
                    }

                }
            } 
        }

        Collections.sort(properties, new ItemWrapperComparator());

        result.recomputeStatus();

        return properties;
    }

    private boolean isPassword(PrismPropertyDefinition def) {
        return CredentialsType.F_PASSWORD.equals(container.getElementName()) ||
                CredentialsType.F_PASSWORD.equals(def.getName());           // in the future, this option could apply as well
    }

    private boolean isShadowAssociation() {
    	 if (!ShadowType.class.isAssignableFrom(getObject().getObject().getCompileTimeClass())) {
             return false;
         }

         if (!ShadowType.F_ASSOCIATION.equals(container.getElementName())) {
             return false;
         }

         return true;
	}

	private boolean isShadowActivation() {
        if (!ShadowType.class.isAssignableFrom(getObject().getObject().getCompileTimeClass())) {
            return false;
        }

        if (!ShadowType.F_ACTIVATION.equals(container.getElementName())) {
            return false;
        }

        return true;
    }

    private boolean hasCapability(PrismPropertyDefinition def) {
        ShadowType shadow = (ShadowType) getObject().getObject().asObjectable();

        ActivationCapabilityType cap = ResourceTypeUtil.getEffectiveCapability(shadow.getResource(),
                ActivationCapabilityType.class);

        if (ActivationType.F_VALID_FROM.equals(def.getName()) && cap.getValidFrom() == null) {
            return false;
        }

        if (ActivationType.F_VALID_TO.equals(def.getName()) && cap.getValidTo() == null) {
            return false;
        }

        if (ActivationType.F_ADMINISTRATIVE_STATUS.equals(def.getName()) && cap.getStatus() == null) {
            return false;
        }

        return true;
    }

    // temporary - brutal hack - the following three methods are copied from AddRoleAssignmentAspect - Pavol M.

    private String formatAssignmentBrief(AssignmentType assignment) {
        StringBuilder sb = new StringBuilder();
        if (assignment.getTarget() != null) {
            sb.append(assignment.getTarget().getName());
        } else {
            sb.append(assignment.getTargetRef().getOid());
        }
        if (assignment.getActivation() != null) {
            if (assignment.getActivation().getAdministrativeStatus() == ActivationStatusType.ENABLED) {
                sb.append(", active");
            }
        }
        if (assignment.getActivation() != null && (assignment.getActivation().getValidFrom() != null || assignment.getActivation().getValidTo() != null)) {
            sb.append(" ");
            sb.append("(");
            sb.append(formatTimeIntervalBrief(assignment));
            sb.append(")");
        }
        return sb.toString();
    }

    public static String formatTimeIntervalBrief(AssignmentType assignment) {
        StringBuilder sb = new StringBuilder();
        if (assignment != null && assignment.getActivation() != null &&
                (assignment.getActivation().getValidFrom() != null || assignment.getActivation().getValidTo() != null)) {
            if (assignment.getActivation().getValidFrom() != null && assignment.getActivation().getValidTo() != null) {
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

    boolean isPropertyVisible(PropertyWrapper property) {
        PrismPropertyDefinition def = property.getItemDefinition();
        if (skipProperty(def) || def.isIgnored() || def.isOperational()) {
            return false;
        }

        // we decide not according to status of this container, but according to the status of the whole object
        if (object.getStatus() == ContainerStatus.ADDING) {
        	return def.canAdd();
        }

        // otherwise, object.getStatus() is MODIFYING
        
        if (def.canModify()) {
        	return showEmpty(property);
        } else {
        	if (def.canRead()) {
        		return showEmpty(property);
        	} 
        	return false;
        }
    }

    private boolean showEmpty(PropertyWrapper property) {
    	ObjectWrapper object = getObject();

        List<ValueWrapper> values = property.getValues();
        boolean isEmpty = values.isEmpty();
        if (values.size() == 1) {
            ValueWrapper value = values.get(0);
            if (ValueStatus.ADDED.equals(value.getStatus())) {
                isEmpty = true;
            }
        }

        return object.isShowEmpty() || !isEmpty;
    }
    
    @Override
    public String getDisplayName() {
        if (StringUtils.isNotEmpty(displayName)) {
            return displayName;
        }
        return getDisplayNameFromItem(container);
    }

    @Override
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public boolean isMain() {
        return main;
    }

    public void setMain(boolean main) {
        this.main = main;
    }

    static String getDisplayNameFromItem(Item item) {
        Validate.notNull(item, "Item must not be null.");

        String displayName = item.getDisplayName();
        if (StringUtils.isEmpty(displayName)) {
            QName name = item.getElementName();
            if (name != null) {
                displayName = name.getLocalPart();
            } else {
                displayName = item.getDefinition().getTypeName().getLocalPart();
            }
        }

        return displayName;
    }

    boolean hasChanged() {
        for (PropertyWrapper property : getProperties()) {
            if (property.hasChanged()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getDisplayNameFromItem(container));
        builder.append(", ");
        builder.append(status);
        builder.append("\n");
        for (PropertyWrapper wrapper : getProperties()) {
            builder.append("\t");
            builder.append(wrapper.toString());
            builder.append("\n");
        }
        return builder.toString();
    }

    /**
     * This methods check if we want to show property in form (e.g. failedLogins, fetchResult,
     * lastFailedLoginTimestamp must be invisible)
     *
     * @return
     * @deprecated will be implemented through annotations in schema
     */
    @Deprecated
    private boolean skipProperty(PrismPropertyDefinition def) {
        final List<QName> names = new ArrayList<QName>();
        names.add(PasswordType.F_FAILED_LOGINS);
        names.add(PasswordType.F_LAST_FAILED_LOGIN);
        names.add(PasswordType.F_LAST_SUCCESSFUL_LOGIN);
        names.add(PasswordType.F_PREVIOUS_SUCCESSFUL_LOGIN);
        names.add(ObjectType.F_FETCH_RESULT);
        //activation
        names.add(ActivationType.F_EFFECTIVE_STATUS);
        names.add(ActivationType.F_VALIDITY_STATUS);
        //user
        names.add(UserType.F_RESULT);

        for (QName name : names) {
            if (name.equals(def.getName())) {
                return true;
            }
        }

        return false;
    }

    public boolean isReadonly() {
    	PrismContainerDefinition def = getContainerDefinition();
    	if (def != null) {
    		return (def.canRead() && !def.canAdd() && !def.canModify());        // todo take into account the containing object status (adding vs. modifying)
    	}
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
