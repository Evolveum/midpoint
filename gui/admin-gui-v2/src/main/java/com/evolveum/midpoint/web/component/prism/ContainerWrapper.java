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
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
public class ContainerWrapper<T extends PrismContainer> implements ItemWrapper, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerWrapper.class);
    private ObjectWrapper object;
    private T container;
    private ContainerStatus status;

    private PropertyPath path;
    private List<PropertyWrapper> properties;

    public ContainerWrapper(ObjectWrapper object, T container, ContainerStatus status, PropertyPath path) {
        Validate.notNull(container, "Prism object must not be null.");
        Validate.notNull(status, "Container status must not be null.");

        this.object = object;
        this.container = container;
        this.status = status;
        this.path = path;
    }

    ObjectWrapper getObject() {
        return object;
    }

    ContainerStatus getStatus() {
        return status;
    }

    PropertyPath getPath() {
        return path;
    }

    T getContainer() {
        return container;
    }

    public List<PropertyWrapper> getProperties() {
        if (properties == null) {
            properties = createProperties();
        }
        return properties;
    }

    private List<PropertyWrapper> createProperties() {
        List<PropertyWrapper> properties = new ArrayList<PropertyWrapper>();

        PrismContainerDefinition definition = null;
        PrismObject parent = getObject().getObject();
        Class clazz = parent.getCompileTimeClass();
        if (ResourceObjectShadowType.class.isAssignableFrom(clazz)) {
            QName name = container.getDefinition().getName();
            if (ResourceObjectShadowType.F_ATTRIBUTES.equals(name)) {
                try {
                    PrismReference resourceRef = parent.findReference(AccountShadowType.F_RESOURCE_REF);
                    PrismObject<ResourceType> resource = resourceRef.getValue().getObject();
                    RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
                            parent.getPrismContext());

                    PrismProperty<QName> objectClassProp = parent.findProperty(AccountShadowType.F_OBJECT_CLASS);
                    QName objectClass = objectClassProp != null ? objectClassProp.getRealValue() : null;

                    definition = refinedSchema.findAccountDefinitionByObjectClass(objectClass);
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

        Set<PrismPropertyDefinition> propertyDefinitions = definition.getPropertyDefinitions();
        for (PrismPropertyDefinition def : propertyDefinitions) {
            if (skipProperty(def)) {
                continue;
            }
            PrismProperty property = container.findProperty(def.getName());
            if (property == null) {
                properties.add(new PropertyWrapper(this, def.instantiate(), ValueStatus.ADDED));
            } else {
                properties.add(new PropertyWrapper(this, property, ValueStatus.NOT_CHANGED));
            }
        }

        Collections.sort(properties, new PropertyWrapperComparator(definition));

        return properties;
    }

    boolean isPropertyVisible(PropertyWrapper property) {
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
        return getDisplayNameFromItem(container);
    }

    public boolean isMain() {
        return path == null;
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
        names.add(PasswordType.F_LAST_FAILED_LOGIN_TIMESTAMP);
        names.add(ObjectType.F_FETCH_RESULT);

        for (QName name : names) {
            if (name.equals(def.getName())) {
                return true;
            }
        }

        return false;
    }
}
