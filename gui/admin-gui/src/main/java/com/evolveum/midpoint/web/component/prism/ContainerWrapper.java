/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

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

    private static final List<QName> INHERITED_OBJECT_ATTRIBUTES = Arrays.asList(ObjectType.F_NAME, ObjectType.F_DESCRIPTION, ObjectType.F_FETCH_RESULT, ObjectType.F_PARENT_ORG, ObjectType.F_PARENT_ORG_REF);

    private String displayName;
    private ObjectWrapper object;
    private T container;
    private ContainerStatus status;

    private boolean main;
    private ItemPath path;
    private List<PropertyWrapper> properties;

    private boolean readonly;
    private boolean showInheritedObjectAttributes;

    public ContainerWrapper(ObjectWrapper object, T container, ContainerStatus status, ItemPath path) {
        Validate.notNull(container, "Prism object must not be null.");
        Validate.notNull(status, "Container status must not be null.");

        this.object = object;
        this.container = container;
        this.status = status;
        this.path = path;
        main = path == null;
        readonly = object.isReadonly();
        showInheritedObjectAttributes = object.isShowInheritedObjectAttributes();
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
        if (properties == null) {
            properties = createProperties();
        }
        return properties;
    }

    public PropertyWrapper findPropertyWrapper(QName name) {
        Validate.notNull(name, "QName must not be null.");
        for (PropertyWrapper wrapper : getProperties()) {
            if (name.equals(wrapper.getItem().getName())) {
                return wrapper;
            }
        }
        return null;
    }

    private List<PropertyWrapper> createProperties() {
        List<PropertyWrapper> properties = new ArrayList<PropertyWrapper>();

        PrismContainerDefinition definition = null;
        PrismObject parent = getObject().getObject();
        Class clazz = parent.getCompileTimeClass();
        if (ShadowType.class.isAssignableFrom(clazz)) {
            QName name = container.getDefinition().getName();
            if (ShadowType.F_ATTRIBUTES.equals(name)) {
                try {
                    PrismReference resourceRef = parent.findReference(ShadowType.F_RESOURCE_REF);
                    PrismObject<ResourceType> resource = resourceRef.getValue().getObject();
                    RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
                            LayerType.PRESENTATION, parent.getPrismContext());

                    PrismProperty<QName> objectClassProp = parent.findProperty(ShadowType.F_OBJECT_CLASS);
                    QName objectClass = objectClassProp != null ? objectClassProp.getRealValue() : null;
                    
                    definition = refinedSchema.findRefinedDefinitionByObjectClassQName(ShadowKindType.ACCOUNT, objectClass)
                    		.toResourceAttributeContainerDefinition();
                    if (LOGGER.isTraceEnabled()) {
                    	LOGGER.trace("Refined account def:\n{}", definition.dump());
                    }
                } catch (Exception ex) {
                    throw new SystemException(ex.getMessage(), ex);
                }
            } else {
                definition = container.getDefinition();
            }
        } else {
            definition = container.getDefinition();
        }

        if (definition == null) {
            LOGGER.error("Couldn't get property list from null definition {}", new Object[]{container.getName()});
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
                PrismPropertyDefinition def = new PrismPropertyDefinition(ObjectType.F_NAME, ObjectType.F_NAME, DOMUtil.XSD_STRING, pcv.getPrismContext());

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
//                if (assignmentType.getTarget() != null) {
//                    value = assignmentType.getTarget().getName().getOrig();
//                } else {
//                    PrismReference targetRef = pcv.findReference(AssignmentType.F_TARGET_REF);
//                    value = targetRef.getValue().getOid();
//                }

                temp.setValue(new PrismPropertyValue<Object>(value));
                properties.add(new PropertyWrapper(this, temp, ValueStatus.NOT_CHANGED));
            }

        } else {            // if not an assignment

           if (container.getValues().size() == 1 ||
                    (container.getValues().isEmpty() && (container.getDefinition() == null || container.getDefinition().isSingleValue()))) {

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

                    PrismProperty property = container.findProperty(def.getName());
                    if (property == null) {
                        properties.add(new PropertyWrapper(this, def.instantiate(), ValueStatus.ADDED));
                    } else {
                        properties.add(new PropertyWrapper(this, property, ValueStatus.NOT_CHANGED));
                    }
                }
            }
        }

        Collections.sort(properties, new ItemWrapperComparator());

        return properties;
    }

    // temporary - brutal hack - the following three methods are copied from AddRoleAssignmentWrapper - Pavol M.

    private String formatAssignmentBrief(AssignmentType assignment) {
        StringBuilder sb = new StringBuilder();
        if (assignment.getTarget() != null) {
            sb.append(assignment.getTarget().getName());
        } else {
            sb.append(assignment.getTargetRef().getOid());
        }
        if (assignment.getActivation() != null) {
            if (Boolean.TRUE.equals(assignment.getActivation().isEnabled())) {
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
        PrismPropertyDefinition def = property.getItem().getDefinition();
        if (!def.canRead() || def.isIgnored()) {
            return false;
        }

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
            QName name = item.getName();
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

        if (ShadowType.class.isAssignableFrom(getObject().getObject().getCompileTimeClass())) {
            names.add(CredentialsType.F_ALLOWED_IDM_ADMIN_GUI_ACCESS);
        }

        for (QName name : names) {
            if (name.equals(def.getName())) {
                return true;
            }
        }

        return false;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
