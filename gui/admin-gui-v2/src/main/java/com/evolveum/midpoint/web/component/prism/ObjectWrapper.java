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
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

/**
 * @author lazyman
 */
public class ObjectWrapper implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectWrapper.class);
    private PrismObject object;
    private ContainerStatus status;
    private String displayName;
    private String description;
    private List<ContainerWrapper> containers;

    private boolean showEmpty;
    private boolean minimalized;
    private boolean selectable;
    private boolean selected;

    public ObjectWrapper(String displayName, String description, PrismObject object, ContainerStatus status) {
        Validate.notNull(object, "Object must not be null.");
        Validate.notNull(status, "Container status must not be null.");

        this.displayName = displayName;
        this.description = description;
        this.object = object;
        this.status = status;
    }

    public PrismObject getObject() {
        return object;
    }

    public String getDisplayName() {
        if (displayName == null) {
            return MiscUtil.getName(object);
        }
        return displayName;
    }

    public ContainerStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMinimalized() {
        return minimalized;
    }

    public void setMinimalized(boolean minimalized) {
        this.minimalized = minimalized;
    }

    public boolean isShowEmpty() {
        return showEmpty;
    }

    public void setShowEmpty(boolean showEmpty) {
        this.showEmpty = showEmpty;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<ContainerWrapper> getContainers() {
        if (containers == null) {
            containers = createContainers();
        }
        return containers;
    }

    public ContainerWrapper findContainerWrapper(PropertyPath path) {
        for (ContainerWrapper wrapper : getContainers()) {
            if (path != null && path.equals(wrapper.getPath())) {
                return wrapper;
            } else {
                if (wrapper.getPath() == null) {
                    return wrapper;
                }
            }
        }

        return null;
    }

    private List<ContainerWrapper> createCustomContainerWrapper(PrismObject object, QName name) {
        PrismContainer container = object.findContainer(name);
        ContainerStatus status = container == null ? ContainerStatus.ADDING : ContainerStatus.MODIFYING;
        if (container == null) {
            PrismContainerDefinition definition = object.getDefinition().findContainerDefinition(name);
            container = definition.instantiate();
        }

        List<ContainerWrapper> list = new ArrayList<ContainerWrapper>();
        list.add(new ContainerWrapper(this, container, status, new PropertyPath(name)));
        list.addAll(createContainerWrapper(container, new PropertyPath(name)));

        return list;
    }

    private List<ContainerWrapper> createContainers() {
        List<ContainerWrapper> containers = new ArrayList<ContainerWrapper>();

        try {
            Class clazz = object.getCompileTimeClass();
            if (ResourceObjectShadowType.class.isAssignableFrom(clazz)) {
                PrismContainer attributes = object.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
                ContainerStatus status = attributes != null ? getStatus() : ContainerStatus.ADDING;
                if (attributes == null) {
                    PrismContainerDefinition definition = object.getDefinition().findContainerDefinition(
                            ResourceObjectShadowType.F_ATTRIBUTES);
                    attributes = definition.instantiate();
                }

                ContainerWrapper container = new ContainerWrapper(this, attributes, status,
                        new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES));
                container.setMain(true);
                containers.add(container);

                containers.addAll(createCustomContainerWrapper(object, ResourceObjectShadowType.F_ACTIVATION));
                if (AccountShadowType.class.isAssignableFrom(clazz)) {
                    containers.addAll(createCustomContainerWrapper(object, AccountShadowType.F_CREDENTIALS));
                }
            } else {
                ContainerWrapper container = new ContainerWrapper(this, object, getStatus(), null);
                containers.add(container);

                containers.addAll(createContainerWrapper(object, null));
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Error occurred during container wrapping", ex);
            throw new SystemException(ex.getMessage(), ex);
        }

        for (ContainerWrapper c : containers) {
            System.out.println(c.getDisplayName() + " " + c.getItem().getDefinition().getDisplayOrder());
        }

        Collections.sort(containers, new ItemWrapperComparator());

        return containers;
    }

    private List<ContainerWrapper> createContainerWrapper(PrismContainer parent, PropertyPath path) {
        PrismContainerDefinition definition = parent.getDefinition();
        List<ContainerWrapper> wrappers = new ArrayList<ContainerWrapper>();

        List<PropertyPathSegment> segments = new ArrayList<PropertyPathSegment>();
        if (path != null) {
            segments.addAll(path.getSegments());
        }
        PropertyPath parentPath = new PropertyPath(segments);

        for (ItemDefinition def : (Collection<ItemDefinition>) definition.getDefinitions()) {
            if (!(def instanceof PrismContainerDefinition)) {
                continue;
            }

            PrismContainerDefinition containerDef = (PrismContainerDefinition) def;
            if (AssignmentType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {
                continue;
            }

            PropertyPath newPath = createPropertyPath(parentPath, containerDef.getName());
            PrismContainer prismContainer = object.findContainer(def.getName());
            if (prismContainer != null) {
                wrappers.add(new ContainerWrapper(this, prismContainer,
                        ContainerStatus.MODIFYING, newPath));
            } else {
                prismContainer = containerDef.instantiate();
                wrappers.add(new ContainerWrapper(this, prismContainer,
                        ContainerStatus.ADDING, newPath));
            }

            wrappers.addAll(createContainerWrapper(prismContainer, newPath));
        }

        return wrappers;
    }

    private PropertyPath createPropertyPath(PropertyPath path, QName element) {
        List<PropertyPathSegment> segments = new ArrayList<PropertyPathSegment>();
        segments.addAll(path.getSegments());
        segments.add(new PropertyPathSegment(element));

        return new PropertyPath(segments);
    }

    public void normalize() throws SchemaException {
        ObjectDelta delta = getObjectDelta();
        if (ChangeType.ADD.equals(delta.getChangeType())) {
            object = delta.getObjectToAdd();
        } else {
            delta.applyTo(object);
        }
    }

    public ObjectDelta getObjectDelta() throws SchemaException {
        if (ContainerStatus.ADDING.equals(getStatus())) {
            return createAddingObjectDelta();
        }

        ObjectDelta delta = new ObjectDelta(object.getCompileTimeClass(), ChangeType.MODIFY);
        delta.setOid(object.getOid());

        List<ContainerWrapper> containers = getContainers();
        //sort containers by path size
        Collections.sort(containers, new PathSizeComparator());

        for (ContainerWrapper containerWrapper : getContainers()) {
            if (!containerWrapper.hasChanged()) {
                continue;
            }

            for (PropertyWrapper propertyWrapper : (List<PropertyWrapper>) containerWrapper.getProperties()) {
                if (!propertyWrapper.hasChanged()) {
                    continue;
                }

                PrismPropertyDefinition propertyDef = propertyWrapper.getItem().getDefinition();

                PropertyDelta pDelta = new PropertyDelta(propertyDef);
                for (ValueWrapper valueWrapper : propertyWrapper.getValues()) {
                    if (!valueWrapper.hasValueChanged() && ValueStatus.NOT_CHANGED.equals(valueWrapper.getStatus())) {
                        continue;
                    }

                    PrismPropertyValue val = valueWrapper.getValue();
                    val.setType(SourceType.USER_ACTION);
                    switch (valueWrapper.getStatus()) {
                        case ADDED:
                            pDelta.addValueToAdd(val);
                            break;
                        case DELETED:
                            pDelta.addValueToDelete(val);
                            break;
                        case NOT_CHANGED:
                            //this is modify...
                            if (propertyDef.isSingleValue()) {
                                pDelta.setValuesToReplace(Arrays.asList(val));
                            } else {
                                pDelta.addValueToAdd(val);
                                pDelta.addValueToDelete(valueWrapper.getOldValue());
                            }
                            break;
                    }
                }
                if (!pDelta.isEmpty()) {
                    delta.addModification(pDelta);
                }
            }
        }

        return delta;
    }

    private ObjectDelta createAddingObjectDelta() throws SchemaException {
        PrismObject object = this.object.clone();

        List<ContainerWrapper> containers = getContainers();
        //sort containers by path size
        Collections.sort(containers, new PathSizeComparator());

        for (ContainerWrapper containerWrapper : getContainers()) {
            if (!containerWrapper.hasChanged()) {
                continue;
            }

            PrismContainer container = containerWrapper.getItem();
            PropertyPath path = containerWrapper.getPath();
            if (containerWrapper.getPath() != null) {
                container = container.clone();
                if (path.size() > 1) {
                    PropertyPath parentPath = path.allExceptLast();
                    PrismContainer parent = object.findOrCreateContainer(parentPath);
                    parent.add(container);
                } else {
                    PrismContainer existing = object.findContainer(container.getName());
                    if (existing == null) {
                        object.add(container);
                    } else {
                        continue;
                    }
                }
            } else {
                container = object;
            }

            for (PropertyWrapper propertyWrapper : (List<PropertyWrapper>) containerWrapper.getProperties()) {
                if (!propertyWrapper.hasChanged()) {
                    continue;
                }

                PrismProperty property = propertyWrapper.getItem().clone();
                if (container.findProperty(property.getName()) != null) {
                    continue;
                }
                container.add(property);
                for (ValueWrapper valueWrapper : propertyWrapper.getValues()) {
                    if (!valueWrapper.hasValueChanged() || ValueStatus.DELETED.equals(valueWrapper.getStatus())) {
                        continue;
                    }

                    if (property.hasRealValue(valueWrapper.getValue())) {
                        continue;
                    }
                    property.addValue(valueWrapper.getValue());
                }
            }
        }

        //cleanup empty containers
        cleanupEmptyContainers(object);

        ObjectDelta delta = new ObjectDelta(object.getCompileTimeClass(), ChangeType.ADD);
        delta.setObjectToAdd(object);

        return delta;
    }

    private void cleanupEmptyContainers(PrismContainer container) {
        List<PrismContainerValue> values = container.getValues();

        List<PrismContainerValue> valuesToBeRemoved = new ArrayList<PrismContainerValue>();
        for (PrismContainerValue value : values) {
            List<? extends Item> items = value.getItems();
            if (items != null) {
                Iterator<? extends Item> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Item item = iterator.next();

                    if (item instanceof PrismContainer) {
                        cleanupEmptyContainers((PrismContainer) item);

                        if (item.isEmpty()) {
                            iterator.remove();
                        }
                    }
                }
            }

            if (items == null || value.isEmpty()) {
                valuesToBeRemoved.add(value);
            }
        }

        container.removeAll(valuesToBeRemoved);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(ContainerWrapper.getDisplayNameFromItem(object));
        builder.append(", ");
        builder.append(status);
        builder.append("\n");
        for (ContainerWrapper wrapper : getContainers()) {
            builder.append("\t");
            builder.append(wrapper.toString());
            builder.append("\n");
        }
        return builder.toString();
    }

    private static class PathSizeComparator implements Comparator<ContainerWrapper> {

        @Override
        public int compare(ContainerWrapper c1, ContainerWrapper c2) {
            int size1 = c1.getPath() != null ? c1.getPath().size() : 0;
            int size2 = c2.getPath() != null ? c2.getPath().size() : 0;

            return size1 - size2;
        }
    }
}
