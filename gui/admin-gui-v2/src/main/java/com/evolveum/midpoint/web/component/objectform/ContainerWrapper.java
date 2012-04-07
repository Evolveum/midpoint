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

package com.evolveum.midpoint.web.component.objectform;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

/**
 * @author lazyman
 */
public class ContainerWrapper implements Serializable {

    private PrismContainer container;
    private ContainerStatus status;
    private List<PropertyWrapper> properties;

    public ContainerWrapper(PrismContainer container, ContainerStatus status) {
        Validate.notNull(container, "Item must not be null.");
        Validate.notNull(status, "Status must not be null.");

        this.container = container;
        this.status = status;
    }

    public PrismContainer getContainer() {
        return container;
    }

    public ContainerStatus getStatus() {
        return status;
    }

    public List<PropertyWrapper> getPropertyWrappers() {
        if (properties != null) {
            return properties;
        }

        properties = new ArrayList<PropertyWrapper>();

        PrismContainerDefinition definition = container.getDefinition();
        properties.addAll(createPropertyWrappers(null, definition));

        Collections.sort(properties, new PropertyWrapperComparator(definition));

        return properties;
    }

    private List<PropertyWrapper> createPropertyWrappers(PropertyPath path,
            PrismContainerDefinition definition) {
        List<PropertyWrapper> properties = new ArrayList<PropertyWrapper>();

        Set<PrismPropertyDefinition> propertyDefinitions = definition.getPropertyDefinitions();
        for (PrismPropertyDefinition def : propertyDefinitions) {
            PrismProperty property = container.findProperty(def.getName());
            if (property == null) {
                properties.add(new PropertyWrapper(path, def.instantiate(), ValueStatus.ADDED));
            } else {
                properties.add(new PropertyWrapper(path, property, ValueStatus.NOT_CHANGED));
            }
        }

        for (ItemDefinition def : (Collection<ItemDefinition>) definition.getDefinitions()) {
            if (!(def instanceof PrismContainerDefinition)) {
                continue;
            }
            
            PrismContainerDefinition containerDef = (PrismContainerDefinition) def;
            if (AssignmentType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {
                continue;
            }
            PropertyPath newPath;
            if (path != null) {
                List<PropertyPathSegment> segments = new ArrayList<PropertyPathSegment>();
                segments.addAll(path.getSegments());
                segments.add(new PropertyPathSegment(def.getName()));
                newPath = new PropertyPath(segments);
            } else {
                newPath = new PropertyPath(def.getName());
            }

            properties.addAll(createPropertyWrappers(newPath, (PrismContainerDefinition) def));
        }

        return properties;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getDisplayNameFromItem(container));
        builder.append(", ");
        builder.append(status);
        builder.append("\n");
        for (PropertyWrapper wrapper : getPropertyWrappers()) {
            builder.append("\t");
            builder.append(wrapper.toString());
            builder.append("\n");
        }

        return builder.toString();
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

    public void cleanup() {
        Collection<PrismProperty> propertiesToDelete = new ArrayList<PrismProperty>();

        for (PropertyWrapper property : getPropertyWrappers()) {
            property.cleanup();

            if (property.getProperty().isEmpty()) {
                propertiesToDelete.add(property.getProperty());
            }
        }

        getContainer().getValue().getItems().removeAll(propertiesToDelete);
        properties = null;
    }

    public ObjectDelta getObjectDelta() {
        if (ContainerStatus.ADDING.equals(getStatus())) {
            return createAddingObjectDelta();
        }

        for (PropertyWrapper property : getPropertyWrappers()) {
            for (PropertyValueWrapper value : property.getPropertyValueWrappers()) {
                if (!value.hasValueChanged()) {
                    continue;
                }

                switch (value.getStatus()) {
                    case ADDED:
                        //todo create property delta value add
                        break;
                    case DELETED:
                        //todo create property delta value delete
                        break;
                }
            }
        }

        return null;
    }

    private ObjectDelta createAddingObjectDelta() {
        //todo implement
        return null;
    }
}
