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

import com.evolveum.midpoint.prism.*;
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

    private ObjectWrapper object;
    private T container;
    private ContainerStatus status;

    private boolean main;
    private List<PropertyWrapper> properties;

    public ContainerWrapper(ObjectWrapper object, T container, ContainerStatus status, boolean main) {
        Validate.notNull(container, "Prism object must not be null.");
        Validate.notNull(status, "Container status must not be null.");

        this.object = object;
        this.container = container;
        this.status = status;
        this.main = main;
    }

    ObjectWrapper getObject() {
        return object;
    }

    ContainerStatus getStatus() {
        return status;
    }

    public List<PropertyWrapper> getProperties() {
        if (properties == null) {
            properties = createProperties();
        }
        return properties;
    }

    private List<PropertyWrapper> createProperties() {
        List<PropertyWrapper> properties = new ArrayList<PropertyWrapper>();

        PrismContainerDefinition definition = container.getDefinition();
        Set<PrismPropertyDefinition> propertyDefinitions = definition.getPropertyDefinitions();
        for (PrismPropertyDefinition def : propertyDefinitions) {
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
        return main;
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
}
