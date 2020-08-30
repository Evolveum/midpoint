/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;

/**
 * @author katka
 */
public class PrismContainerValueWrapperImpl<C extends Containerable>
        extends PrismValueWrapperImpl<C> implements PrismContainerValueWrapper<C> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerValueWrapperImpl.class);

    private boolean expanded;
    private boolean showMetadata;
    private boolean sorted;
    private boolean showEmpty;
    private boolean readOnly;
    private boolean selected;
    private boolean heterogenous;
    private boolean metadata;

    private List<VirtualContainerItemSpecificationType> virtualItems;
    private List<ItemWrapper<?, ?>> items = new ArrayList<>();

    public PrismContainerValueWrapperImpl(PrismContainerWrapper<C> parent, PrismContainerValue<C> pcv, ValueStatus status) {
        super(parent, pcv, status);
    }

    @Override
    public PrismContainerValue<C> getValueToAdd() throws SchemaException {
        Collection<ItemDelta> modifications = new ArrayList<>();
        for (ItemWrapper<?, ?> itemWrapper : items) {
            Collection<ItemDelta> subDelta = itemWrapper.getDelta();

            if (subDelta != null && !subDelta.isEmpty()) {
                modifications.addAll(subDelta);
            }
        }

        PrismContainerValue<C> valueToAdd = getOldValue().clone();
        if (!modifications.isEmpty()) {
            for (ItemDelta delta : modifications) {
                delta.applyTo(valueToAdd);
            }
        }

        if (!valueToAdd.isEmpty()) {
            return valueToAdd;
        }

        return null;
    }

    @Override
    public <ID extends ItemDelta> void applyDelta(ID delta) throws SchemaException {
        if (delta == null) {
            return;
        }

        LOGGER.trace("Applying {} to {}", delta, getNewValue());
        delta.applyTo(getNewValue());
    }

    @Override
    public void setRealValue(C realValue) {
        LOGGER.info("######$$$$$$Nothing to do");
    }

    @Override
    public String getDisplayName() {
        if (isVirtual()) {
            return getContainerDefinition().getDisplayName();
        }

        if (getParent().isSingleValue()) {
            return getParent().getDisplayName();
        }

        if (getParent().isMultiValue() && ValueStatus.ADDED.equals(getStatus())) {
            String name;
            Class<C> cvalClass = getNewValue().getCompileTimeClass();
            if (cvalClass != null) {
                name = cvalClass.getSimpleName() + ".details.newValue";
            } else {
                name = "ContainerPanel.containerProperties";
            }
            return name;
        }

        return WebComponentUtil.getDisplayName(getNewValue());
    }

    @Override
    public String getHelpText() {
        return WebPrismUtil.getHelpText(getContainerDefinition());
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

//    @Override
//    public boolean hasMetadata() {
//        for (ItemWrapper<?,?> container : items) {
//            if (container.getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//

    @Override
    public List<ItemWrapper<?, ?>> getItems() {
        return items;
    }

    @Override
    public boolean isShowMetadata() {
        return showMetadata;
    }

    @Override
    public void setShowMetadata(boolean showMetadata) {
        this.showMetadata = showMetadata;
    }

    @Override
    public boolean isSorted() {
        return sorted;
    }

    @Override
    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    @Override
    public boolean isHeterogenous() {
        return heterogenous;
    }

    @Override
    public void setHeterogenous(boolean heterogenous) {
        this.heterogenous = heterogenous;
    }

    @Override
    public List<PrismContainerDefinition<C>> getChildContainers() throws SchemaException {
        List<PrismContainerDefinition<C>> childContainers = new ArrayList<>();
        for (ItemDefinition<?> def : getContainerDefinition().getDefinitions()) {
            if (!(def instanceof PrismContainerDefinition)) {
                continue;
            }

            @SuppressWarnings("unchecked") PrismContainerDefinition<C> containerDef = (PrismContainerDefinition<C>) def;

            ContainerStatus objectStatus = findObjectStatus();

            boolean allowed = false;
            switch (objectStatus) {
                case ADDING:
                    allowed = containerDef.canAdd();
                    break;
                case MODIFYING:
                case DELETING:
                    allowed = containerDef.canModify();
            }

            //do not allow to add already existing singel value container
            if (containerDef.isSingleValue() && findContainer(containerDef.getItemName()) != null) {
                allowed = false;
            }

            if (allowed) {
                childContainers.add(containerDef);
            }
        }

        return childContainers;
    }

    @Override
    public <T extends Containerable> List<PrismContainerWrapper<T>> getContainers() {
        List<PrismContainerWrapper<T>> containers = new ArrayList<>();
        for (ItemWrapper<?, ?> container : items) {

            collectExtensionItems(container, true, containers);

            if (container instanceof PrismContainerWrapper && !ObjectType.F_EXTENSION.equivalent(container.getItemName())) {
                //noinspection unchecked
                containers.add((PrismContainerWrapper<T>) container);
            }
        }
        return containers;
    }

    @Override
    public List<ItemWrapper<?,?>> getNonContainers() {
        List<ItemWrapper<?,?>> nonContainers = new ArrayList<>();
        for (ItemWrapper<?,?> item : items) {

            collectExtensionItems(item, false, nonContainers);

            if (!(item instanceof PrismContainerWrapper)) {
                (nonContainers).add(item);
            }
        }

        if (getVirtualItems() == null) {
            return nonContainers;
        }

        if (getParent() == null) {
            LOGGER.trace("Parent null, skipping virtual items");
            return nonContainers;
        }

        PrismObjectWrapper objectWrapper = getParent().findObjectWrapper();
        if (objectWrapper == null) {
            LOGGER.trace("No object wrapper found. Skipping virtual items.");
            return nonContainers;
        }

        for (VirtualContainerItemSpecificationType virtualItem : getVirtualItems()) {
            try {
                ItemPath virtualItemPath = getVirtualItemPath(virtualItem);
                ItemWrapper itemWrapper = objectWrapper.findItem(virtualItemPath, ItemWrapper.class);
                if (itemWrapper == null) {
                    LOGGER.warn("No wrapper found for {}", virtualItemPath);
                    continue;
                }

                if (itemWrapper instanceof PrismContainerWrapper) {
                    continue;
                }

                nonContainers.add(itemWrapper);

            } catch (SchemaException e) {
                LOGGER.error("Cannot find wrapper with path {}, error occurred {}", virtualItem, e.getMessage(), e);
            }
        }
        return nonContainers;
    }

    private ItemPath getVirtualItemPath(VirtualContainerItemSpecificationType virtualItem) throws SchemaException {
        ItemPathType itemPathType = virtualItem.getPath();
        if (itemPathType == null) {
            throw new SchemaException("Item path in virtual item definition cannot be null");
        }

        return itemPathType.getItemPath();
    }

    protected void collectExtensionItems(ItemWrapper<?, ?> item, boolean containers, List<? extends ItemWrapper<?, ?>> itemWrappers) {
        if (!ObjectType.F_EXTENSION.equals(item.getItemName())) {
            return;
        }

        try {
            PrismContainerValueWrapper<ExtensionType> extenstion = (PrismContainerValueWrapper<ExtensionType>) item.getValue();
            List<? extends ItemWrapper<?, ?>> extensionItems = extenstion.getItems();
            for (ItemWrapper<?, ?> extensionItem : extensionItems) {
                if (extensionItem instanceof PrismContainerWrapper) {
                    if (containers) {
                        ((List)itemWrappers).add(extensionItem);
                    }
                    continue;
                }

                if (!containers) {
                    ((List)itemWrappers).add(extensionItem);
                }
            }
        } catch (SchemaException e) {
            //in this case we could ignore the error. extension is single value container so this error should not happened
            // but just to be sure we won't miss if something strange happened just throw runtime error
            LOGGER.error("Something unexpected happened. Please, check your schema", e);
            throw new IllegalStateException(e.getMessage(), e);
        }

    }

    private PrismContainerDefinition<C> getContainerDefinition() {
        return getNewValue().getDefinition();
    }

    private ContainerStatus findObjectStatus() {
        return ContainerStatus.ADDING;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper#findContainer(com.evolveum.midpoint.prism.path.ItemPath)
     */
    @Override
    public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException {
        return findItem(path, PrismContainerWrapper.class);
    }

    @Override
    public <IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException {
        Object first = path.first();
        if (!ItemPath.isName(first)) {
            throw new IllegalArgumentException("Attempt to lookup item using a non-name path " + path + " in " + this);
        }
        ItemName subName = ItemPath.toName(first);
        ItemPath rest = path.rest();
        IW item = findItemByQName(subName);
        if (item != null) {
            if (rest.isEmpty()) {
                if (type.isAssignableFrom(item.getClass())) {
                    return item;
                }
            } else {
                // Go deeper
                if (item instanceof PrismContainerWrapper) {
                    return ((PrismContainerWrapper<?>) item).findItem(rest, type);
                }
            }
        }

        return null;
    }

    private <IW extends ItemWrapper> IW findItemByQName(QName subName) throws SchemaException {
        if (items == null) {
            return null;
        }
        IW matching = null;
        for (ItemWrapper<?, ?> item : items) {
            if (QNameUtil.match(subName, item.getItemName())) {
                if (matching != null) {
                    String containerName = getParent() != null ? DebugUtil.formatElementName(getParent().getItemName()) : "";
                    throw new SchemaException("More than one items matching " + subName + " in container " + containerName);
                } else {
                    matching = (IW) item;
                }
            }
        }
        return matching;
    }

    @Override
    public <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) throws SchemaException {
        return findItem(propertyPath, PrismPropertyWrapper.class);
    }

    @Override
    public <R extends Referencable> PrismReferenceWrapper<R> findReference(ItemPath path) throws SchemaException {
        return findItem(path, PrismReferenceWrapper.class);
    }

    @Override
    public ItemPath getPath() {
        return getNewValue().getPath();
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public boolean setSelected(boolean selected) {
        return this.selected = selected;
    }

    @Override
    public boolean hasChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("Items:\n");
        for (ItemWrapper<?, ?> item : items) {
            sb.append(item.debugDump(indent + 1)).append("\n");
        }

        return sb.toString();
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(boolean readOnly, boolean recursive) {
        this.readOnly = readOnly;
    }

    @Override
    public boolean isShowEmpty() {
        return showEmpty;
    }

    @Override
    public void setShowEmpty(boolean showEmpty) {
        this.showEmpty = showEmpty;
        //computeStripes();
    }

    @Override
    public void setVirtualContainerItems(List<VirtualContainerItemSpecificationType> virtualItems) {
        this.virtualItems = virtualItems;
    }

    public List<VirtualContainerItemSpecificationType> getVirtualItems() {
        return virtualItems;
    }

    @Override
    public boolean isVirtual() {
        return virtualItems != null;
    }

    @Override
    public boolean isMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean isVisible() {
        if (!super.isVisible()) {
            return false;
        }

        ItemWrapper parent = getParent();
        if (!PrismContainerWrapper.class.isAssignableFrom(parent.getClass())) {
            return false;
        }

        if (MetadataType.COMPLEX_TYPE.equals(parent.getTypeName()) && isShowMetadata()) {
            return false;
        }

        return ((PrismContainerWrapper) parent).isExpanded() || isHeterogenous();
    }

    @Override
    public PrismContainerValue<C> getNewValue() {
        return super.getNewValue();
    }

    @Override
    public PrismContainerValue<C> getOldValue() {
        return super.getOldValue();
    }

    public PrismContainerDefinition<C> getDefinition() {
        return getNewValue().getDefinition();
    }

    @Override
    public PrismContainerWrapper<Containerable> getSelectedChild() {
        for (PrismContainerWrapper<Containerable> child : getContainers()) {
            if (child.isShowMetadataDetails()) {
                return child;
            }
        }

        return null;
    }
}
