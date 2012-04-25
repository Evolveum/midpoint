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

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class ObjectWrapper implements Serializable {

    private PrismObject object;
    private ContainerStatus status;
    private String displayName;
    private String description;
    private List<ContainerWrapper> containers;

    private boolean showEmpty;
    private boolean minimalized;

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
            PrismProperty<String> name = object.findProperty(ObjectType.F_NAME);
            if (name == null) {
                return null;
            }

            return name.getRealValue();
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

    public List<ContainerWrapper> getContainers() {
        if (containers == null) {
            containers = createContainers();
        }
        return containers;
    }

    private List<ContainerWrapper> createContainers() {
        List<ContainerWrapper> containers = new ArrayList<ContainerWrapper>();

        Collection<ItemDefinition> definitions = new ArrayList<ItemDefinition>();
        if (AccountShadowType.class.equals(object.getCompileTimeClass())) {
            ContainerWrapper container = new ContainerWrapper(this, object.findContainer(AccountShadowType.F_ATTRIBUTES),
                    getStatus(), true);
            containers.add(container);
            //todo credentials, activation fix for accounts as well as for user (or other objects)
        } else {
            ContainerWrapper container = new ContainerWrapper(this, object, getStatus(), true);
            containers.add(container);
            PrismObjectDefinition definition = object.getDefinition();
            definitions.addAll(definition.getDefinitions());
        }

        for (ItemDefinition def : definitions) {
            if (!(def instanceof PrismContainerDefinition)) {
                continue;
            }

            PrismContainerDefinition containerDef = (PrismContainerDefinition) def;
            if (AssignmentType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {
                continue;
            }

            PrismContainer prismContainer = object.findContainer(def.getName());
            if (prismContainer == null) {
                containers.add(new ContainerWrapper(this, containerDef.instantiate(),
                        ContainerStatus.MODIFYING, false));
            } else {
                containers.add(new ContainerWrapper(this, containerDef.instantiate(),
                        ContainerStatus.ADDING, false));
            }
        }

        return containers;
    }

    public ObjectDelta getObjectDelta() {
        if (ContainerStatus.ADDING.equals(getStatus())) {
            return createAddingObjectDelta();
        }

        ObjectDelta delta = new ObjectDelta(object.getCompileTimeClass(), ChangeType.MODIFY);
        delta.setOid(object.getOid());
        for (ContainerWrapper containerWrapper : getContainers()) {
            if (!containerWrapper.hasChanged()) {
                //todo handle container changes
                continue;
            }

            for (PropertyWrapper propertyWrapper : (List<PropertyWrapper>) containerWrapper.getProperties()) {
                if (!propertyWrapper.hasChanged()) {
                    continue;
                }

                PropertyDelta pDelta = new PropertyDelta(propertyWrapper.getProperty().getDefinition());
                delta.addModification(pDelta);
                for (ValueWrapper valueWrapper : propertyWrapper.getValues()) {
                    if (!valueWrapper.hasValueChanged() && ValueStatus.NOT_CHANGED.equals(valueWrapper.getStatus())) {
                        continue;
                    }

                    PrismPropertyValue val = valueWrapper.getValue();
                    switch (valueWrapper.getStatus()) {
                        case ADDED:
                            val.setType(SourceType.USER_ACTION);
                            pDelta.addValueToAdd(val);
                            break;
                        case DELETED:
                            pDelta.addValueToDelete(val);
                            break;
                        case NOT_CHANGED:
                            //this is modify...
                            val.setType(SourceType.USER_ACTION);

                            pDelta.addValueToAdd(val);
                            pDelta.addValueToDelete(valueWrapper.getOldValue());
                            break;
                    }
                }
            }
        }

        return delta;
    }

    private ObjectDelta createAddingObjectDelta() {
        ObjectDelta delta = new ObjectDelta(object.getCompileTimeClass(), ChangeType.ADD);
        //todo implement

        return delta;
    }
}
