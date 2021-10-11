/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
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
import org.apache.commons.collections4.CollectionUtils;
import java.text.Collator;
import java.util.*;


/**
 * @author katka
 *
 */
public class PrismContainerValueWrapperImpl<C extends Containerable> extends PrismValueWrapperImpl<C, PrismContainerValue<C>> implements PrismContainerValueWrapper<C> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerValueWrapperImpl.class);

    private boolean expanded;
    private boolean showMetadata;
    private boolean sorted;
    private boolean showEmpty;
    private boolean readOnly;
    private boolean selected;
    private boolean heterogenous;

    private List<VirtualContainerItemSpecificationType> virtualItems;
    private List<ItemWrapper<?, ?, ?,?>> items = new ArrayList<>();

    private List<ItemWrapper<?, ?, ?, ?>> nonContainers = new ArrayList<>();

    public PrismContainerValueWrapperImpl(PrismContainerWrapper<C> parent, PrismContainerValue<C> pcv, ValueStatus status) {
        super(parent, pcv, status);
    }

    @Override
    public PrismContainerValue<C> getValueToAdd() throws SchemaException {
        Collection<ItemDelta> modifications = new ArrayList<>();
        for (ItemWrapper<?, ?, ?, ?> itemWrapper : items) {
            Collection<ItemDelta> subDelta =  itemWrapper.getDelta();

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

    @Override
    public boolean hasMetadata() {
        for (ItemWrapper<?,?,?,?> container : items) {
            if (container.getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
                return true;
            }
        }

        return false;
    }


    @Override
    public List<ItemWrapper<?, ?, ?,?>> getItems() {
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

            ContainerStatus objectStatus = findObjectStatus();

            boolean allowed = false;
            switch (objectStatus) {
                case ADDING:
                    allowed = def.canAdd();
                    break;
                case MODIFYING:
                case DELETING:
                    allowed = def.canModify();
            }

            //do not allow to add already existing singel value container
            if (def.isSingleValue() && findContainer(def.getItemName()) != null) {
                allowed = false;
            }


            if (allowed) {
                childContainers.add((PrismContainerDefinition<C>)def);
            }
        }

        return childContainers;
    }

    @Override
    public <T extends Containerable> List<PrismContainerWrapper<T>> getContainers() {
        List<PrismContainerWrapper<T>> containers = new ArrayList<>();
        for (ItemWrapper<?,?,?,?> container : items) {

            collectExtensionItems(container, true, containers);

            if (container instanceof PrismContainerWrapper) {
                containers.add((PrismContainerWrapper<T>) container);
            }
        }
        return containers;
    }

    @Override
    public List<? extends ItemWrapper<?,?,?,?>> getNonContainers() {
//        List<? extends ItemWrapper<?,?,?,?>> nonContainers = new ArrayList<>();
        if (!nonContainers.isEmpty()) {
            return nonContainers;
        }
        for (ItemWrapper<?,?,?,?> item : items) {

            collectExtensionItems(item, false, nonContainers);

            if (!(item instanceof PrismContainerWrapper)) {
                ((List)nonContainers).add(item);
            }
        }

        PrismObjectWrapper<?> objectWrapper = getObjectWrapperForVirtualContainers();
        if (objectWrapper != null) {

            for (VirtualContainerItemSpecificationType virtualItem : getVirtualItems()) {
                if (objectWrapper == null) {
                    //should not happen, if happens it means something veeery strange happened
                    continue;
                }
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

                    if (checkContainerInclusion(itemWrapper)) {
                        ((List) nonContainers).add(itemWrapper);
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Cannot find wrapper with path {}, error occured {}", virtualItem, e.getMessage(), e);
                }
            }
        }

        Locale locale = WebModelServiceUtils.getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.SECONDARY);       // e.g. "a" should be different from "á"
        collator.setDecomposition(Collator.FULL_DECOMPOSITION);
        ItemWrapperComparator<?> comparator = new ItemWrapperComparator<>(collator, sorted);
        if (CollectionUtils.isNotEmpty(nonContainers)) {
            nonContainers.sort((Comparator) comparator);
        }
        return nonContainers;
    }


    public boolean checkContainerInclusion(ItemWrapper<?, ?, ?, ?> itemWrapper) {
        return true;
    }

    private PrismObjectWrapper<?> getObjectWrapperForVirtualContainers() {
        if (getVirtualItems() == null) {
            LOGGER.trace("Skipping processing of virtual containers, no virtual container definition found");
            return null;
        }

        if (getParent() == null) {
            LOGGER.trace("Parent null, skipping virtual items");
            return null;
        }

        PrismObjectWrapper objectWrapper = getParent().findObjectWrapper();
        if (objectWrapper == null) {
            LOGGER.trace("No object wrapper found. Skipping virtual items.");
            return null;
        }
        return objectWrapper;
    }

    private ItemPath getVirtualItemPath(VirtualContainerItemSpecificationType virtualItem) throws SchemaException {
        ItemPathType itemPathType = virtualItem.getPath();
        if (itemPathType == null) {
            throw new SchemaException("Item path in virtual item definition cannot be null");
        }

        return itemPathType.getItemPath();
    }

    protected <IW extends ItemWrapper<?, ?, ?, ?>> void collectExtensionItems(ItemWrapper<?, ?, ?, ?> item, boolean containers, List<IW> itemWrappers) {
        if (!ObjectType.F_EXTENSION.equals(item.getItemName())) {
            return;
        }

        try {
            PrismContainerValueWrapper<ExtensionType> extenstion = (PrismContainerValueWrapper<ExtensionType>) item.getValue();
            List<IW> extensionItems = (List<IW>) extenstion.getItems();
            for (IW extensionItem : extensionItems) {
                if (extensionItem instanceof PrismContainerWrapper) {
                    if (containers) {
                        itemWrappers.add(extensionItem);
                    }
                    continue;
                }

                if (!containers) {
                    itemWrappers.add(extensionItem);
                }
            }
        } catch (SchemaException e) {
            //in this case we could ignore the error. extension is single value container so this error should not happened
            // but just to be sure we won't miss if something strange happened just throw runtime error
            LOGGER.error("Something unexpected happened. Please, check your schema");
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
     * @see com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper#findContainer(com.evolveum.midpoint.prism.path.ItemPath)
     */
    @Override
    public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException {
        PrismContainerWrapper<T> container = findItem(path, PrismContainerWrapper.class);
        return container;
    }

    @Override
    public <IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException {
        Object first = path.first();
        if (!ItemPath.isName(first)) {
            throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+path+" in "+this);
        }
        ItemName subName = ItemPath.toName(first);
        ItemPath rest = path.rest();
        IW item = findItemByQName(subName);
        if (item != null) {
            if (rest.isEmpty()) {
                if (type.isAssignableFrom(item.getClass())) {
                    return (IW) item;
                }
            } else {
                // Go deeper
                if (item instanceof PrismContainerWrapper) {
                    return ((PrismContainerWrapper<?>)item).findItem(rest, type);
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
            for (ItemWrapper<?, ?, ?, ?> item : items) {
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



    /* (non-Javadoc)
     * @see com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper#hasChanged()
     */
    @Override
    public boolean hasChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("Items:\n");
        for (ItemWrapper<?, ?, ?, ?> item: items) {
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

    public void addItem(ItemWrapper<?, ?, ?, ?> iw) {
        items.add(iw);
        nonContainers.clear();
    }

    @Override
    public void clearItems() {
        items.clear();
        nonContainers.clear();
    }

    @Override
    public void addItems(Collection<ItemWrapper<?,?,?,?>> allItems) {
        items.addAll(allItems);
        nonContainers.clear();
    }
}
