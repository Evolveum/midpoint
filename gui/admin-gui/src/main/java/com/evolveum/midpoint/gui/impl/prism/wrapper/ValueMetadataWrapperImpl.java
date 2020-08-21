/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;

import java.util.List;

public class ValueMetadataWrapperImpl implements PrismContainerValueWrapper<Containerable> {

    private boolean sorted;
    private PrismContainerValueWrapper<Containerable> metadataValueWrapper;

    public ValueMetadataWrapperImpl(PrismContainerValueWrapper<Containerable> metadataValueWrapper) {
        this.metadataValueWrapper = metadataValueWrapper;
    }

    @Override
    public String getDisplayName() {
        if (getDefinition() == null) {
            return "MetadataMock";
        }
        return getDefinition().getDisplayName();
    }

    @Override
    public String getHelpText() {
        return metadataValueWrapper.getHelpText();
    }

    @Override
    public boolean isExpanded() {
        return true;
    }

    @Override
    public void setExpanded(boolean expanded) {

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
    public List<PrismContainerDefinition<Containerable>> getChildContainers() {
        throw new UnsupportedOperationException("Cannot create child containers for value metadata");
    }

    @Override
    public Containerable getRealValue() {
        return getOldValue().getRealValue();
    }

    @Override
    public void setRealValue(Containerable realValue) {
        throw new UnsupportedOperationException("Cannot set real value for value metadata");
    }

    @Override
    public ValueStatus getStatus() {
        return ValueStatus.NOT_CHANGED;
    }

    @Override
    public void setStatus(ValueStatus status) {
        throw new UnsupportedOperationException("Cannot set value status for value metadata");
    }

    @Override
    public PrismContainerValue  getNewValue() {
        return null;
        // todo adapt
//        return (ValueMetadata) metadataValueWrapper.getOldValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public PrismContainerValue getOldValue() {
        return null;
        // todo adapt
//        return (ValueMetadata) metadataValueWrapper.getOldValue();
    }

    @Override
    public <IW extends ItemWrapper> IW getParent() {
        return null;
    }

    @Override
    public <D extends ItemDelta<PrismValue, ? extends ItemDefinition>> void addToDelta(D delta) {
        throw new UnsupportedOperationException("Cannot compute delta for valueMetadata");
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public ValueMetadataWrapperImpl getValueMetadata() {
        return null;
    }

    @Override
    public void setValueMetadata(ValueMetadataWrapperImpl valueMetadata) {

    }

    @Override
    public boolean isShowMetadata() {
        return false;
    }

    @Override
    public void setShowMetadata(boolean showMetadata) {

    }

    @Override
    public <T extends Containerable> List<PrismContainerWrapper<T>> getContainers() {
        return metadataValueWrapper.getContainers();
    }

    @Override
    public List<ItemWrapper<?, ?>> getNonContainers() {
        return metadataValueWrapper.getNonContainers();
    }

    @Override
    public List<? extends ItemWrapper<?, ?>> getItems() {
        return metadataValueWrapper.getItems();
    }

    @Override
    public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException {
        return metadataValueWrapper.findContainer(path);
    }

    @Override
    public <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) throws SchemaException {
        return metadataValueWrapper.findProperty(propertyPath);
    }

    @Override
    public <R extends Referencable> PrismReferenceWrapper<R> findReference(ItemPath path) throws SchemaException {
        return metadataValueWrapper.findReference(path);
    }

    @Override
    public <IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException {
        return metadataValueWrapper.findItem(path, type);
    }

    @Override
    public ItemPath getPath() {
        return null;
    }

    @Override
    public boolean isSelected() {
        return false;
    }

    @Override
    public boolean setSelected(boolean selected) {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void setReadOnly(boolean readOnly, boolean recursive) {

    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public boolean isShowEmpty() {
        return false;
    }

    @Override
    public void setShowEmpty(boolean setShowEmpty) {

    }

    @Override
    public <ID extends ItemDelta> void applyDelta(ID delta) {
        throw new UnsupportedOperationException("apply delta not supported");
    }

    @Override
    public PrismContainerValue<Containerable> getValueToAdd() {
        throw new UnsupportedOperationException("getValueToAdd not supported");
    }

    @Override
    public boolean isHeterogenous() {
        return false;
    }

    @Override
    public void setHeterogenous(boolean heterogenous) {

    }

    @Override
    public void setVirtualContainerItems(List<VirtualContainerItemSpecificationType> virtualItems) {

    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public boolean isMetadata() {
        return metadataValueWrapper.isMetadata();
    }

    @Override
    public void setMetadata(boolean metadata) {
        metadataValueWrapper.setMetadata(metadata);
    }

    @Override
    public PrismContainerDefinition<Containerable> getDefinition() {
        return metadataValueWrapper.getDefinition();
    }

    @Override
    public String debugDump(int indent) {
        return null;
    }

    @Override
    public String toShortString() {
        return "";
    }
}
