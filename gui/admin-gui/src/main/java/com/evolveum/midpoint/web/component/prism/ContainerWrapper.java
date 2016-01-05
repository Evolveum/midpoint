/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ContainerWrapper<C extends Containerable> implements ItemWrapper, Serializable, DebugDumpable {

    private String displayName;
    private ObjectWrapper<? extends ObjectType> objectWrapper;
    private PrismContainer<C> container;
    private ContainerStatus status;

    private boolean main;
    private ItemPath path;
    private List<ItemWrapper> properties;

    private boolean readonly;
    private boolean showInheritedObjectAttributes;

    private PrismContainerDefinition<C> containerDefinition;

    public ContainerWrapper(ObjectWrapper objectWrapper, PrismContainer<C> container, ContainerStatus status, ItemPath path) {
        Validate.notNull(container, "container must not be null.");
        Validate.notNull(status, "Container status must not be null.");

        this.objectWrapper = objectWrapper;
        this.container = container;
        this.status = status;
        this.path = path;
        main = path == null;
        readonly = objectWrapper.isReadonly(); // [pm] this is quite questionable
        showInheritedObjectAttributes = objectWrapper.isShowInheritedObjectAttributes();
        // have to be after setting "main" property
        containerDefinition = getItemDefinition();
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        if (container != null) {
            container.revive(prismContext);
        }
        if (containerDefinition != null) {
            containerDefinition.revive(prismContext);
        }
        if (properties != null) {
            for (ItemWrapper itemWrapper : properties) {
                itemWrapper.revive(prismContext);
            }
        }
    }

    @Override
    public PrismContainerDefinition<C> getItemDefinition() {
        if (main) {
            return objectWrapper.getDefinition();
        } else {
            return objectWrapper.getDefinition().findContainerDefinition(path);
        }
    }

    ObjectWrapper getObject() {
        return objectWrapper;
    }

    public ContainerStatus getStatus() {
        return status;
    }

    public ItemPath getPath() {
        return path;
    }

    public PrismContainer<C> getItem() {
        return container;
    }

    public List<ItemWrapper> getItems() {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        return properties;
    }

    public void setProperties(List<ItemWrapper> properties) {
        this.properties = properties;
    }

    public ItemWrapper findPropertyWrapper(QName name) {
        Validate.notNull(name, "QName must not be null.");
        for (ItemWrapper wrapper : getItems()) {
            if (name.equals(wrapper.getItem().getElementName())) {
                return wrapper;
            }
        }
        return null;
    }

    boolean isItemVisible(ItemWrapper item) {
        ItemDefinition def = item.getItemDefinition();
        if (def.isIgnored() || def.isOperational()) {
            return false;
        }

        if (def instanceof PrismPropertyDefinition && skipProperty((PrismPropertyDefinition) def)) {
            return false;
        }

        // we decide not according to status of this container, but according to
        // the status of the whole object
        if (objectWrapper.getStatus() == ContainerStatus.ADDING) {
            return def.canAdd();
        }

        // otherwise, object.getStatus() is MODIFYING

        if (def.canModify()) {
            return showEmpty(item);
        } else {
            if (def.canRead()) {
                return showEmpty(item);
            }
            return false;
        }
    }

    private boolean showEmpty(ItemWrapper item) {
        ObjectWrapper objectWrapper = getObject();
        List<ValueWrapper> valueWrappers = item.getValues();
        boolean isEmpty;
        if (valueWrappers == null) {
            isEmpty = true;
        } else {
            isEmpty = valueWrappers.isEmpty();
        }
        if (!isEmpty && valueWrappers.size() == 1) {
            ValueWrapper value = valueWrappers.get(0);
            if (ValueStatus.ADDED.equals(value.getStatus())) {
                isEmpty = true;
            }
        }
        return objectWrapper.isShowEmpty() || !isEmpty;
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

    @Override
    public QName getName() {
        return getItem().getElementName();
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

    public boolean hasChanged() {
        for (ItemWrapper item : getItems()) {
            if (item.hasChanged()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ContainerWrapper(");
        builder.append(getDisplayNameFromItem(container));
        builder.append(" (");
        builder.append(status);
        builder.append(") ");
        builder.append(getItems() == null ? null : getItems().size());
        builder.append(" items)");
        return builder.toString();
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
        final List<QName> names = new ArrayList<QName>();
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

    public boolean isReadonly() {
        PrismContainerDefinition def = getItemDefinition();
        if (def != null) {
            // todo take into account the containing object status (adding vs. modifying)
            return (def.canRead() && !def.canAdd() && !def.canModify());
        }
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    @Override
    public List<ValueWrapper> getValues() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isEmpty() {
        return getItem().isEmpty();
    }

    @Override
    public ContainerWrapper<C> getContainer() {
        // TODO Auto-generated method stub
        return null;
    }

    //TODO add new PrismContainerValue to association container
    public void addValue() {
        getItems().add(createItem());
    }

    public ItemWrapper createItem() {
        ValueWrapper wrapper = new ValueWrapper(this, new PrismPropertyValue(null), ValueStatus.ADDED);
        return wrapper.getItem();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ContainerWrapper: ").append(PrettyPrinter.prettyPrint(getName())).append("\n");
        DebugUtil.debugDumpWithLabel(sb, "displayName", displayName, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "status", status == null ? null : status.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "main", main, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "readonly", readonly, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "showInheritedObjectAttributes", showInheritedObjectAttributes, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "path", path == null ? null : path.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "containerDefinition", containerDefinition == null ? null : containerDefinition.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "container", container == null ? null : container.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "properties", indent + 1);
        sb.append("\n");
        DebugUtil.debugDump(sb, properties, indent + 2, false);
        return sb.toString();
    }
}
