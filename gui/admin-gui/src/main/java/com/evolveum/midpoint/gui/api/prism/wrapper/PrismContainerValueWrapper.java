/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;

/**
 * @author katka
 *
 */
public interface PrismContainerValueWrapper<C extends Containerable> extends PrismValueWrapper<C>, SelectableRow {

    String getDisplayName();
    String getHelpText();

    boolean isExpanded();

    void setExpanded(boolean expanded);

    boolean isSorted();
    void setSorted(boolean sorted);

    List<PrismContainerDefinition<C>> getChildContainers() throws SchemaException;

    ValueStatus getStatus();
    void setStatus(ValueStatus status);

    List<PrismContainerWrapper<? extends Containerable>> getContainers();
    List<PrismContainerWrapper<? extends Containerable>> getContainers(ContainerPanelConfigurationType config, ModelServiceLocator modelServiceLocator);

    List<ItemWrapper<?, ?>> getNonContainers();

    @Deprecated
    List<? extends ItemWrapper<?, ?>> getItems();

    <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException;
    <T extends Containerable> PrismContainerWrapper<T> findContainer(String identifier);
    <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) throws SchemaException;
    <R extends Referencable> PrismReferenceWrapper<R> findReference(ItemPath path) throws SchemaException;
    <IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException;
    <IW extends ItemWrapper> IW findItem(ItemPath path) throws SchemaException;

    ItemPath getPath();

    boolean isSelected();
    void setSelected(boolean selected);

    boolean isReadOnly();
    void setReadOnly(boolean readOnly, boolean recursive);

    @Deprecated
    boolean hasChanged();

    boolean isShowEmpty();
    void setShowEmpty(boolean setShowEmpty);

    //void sort();

    <ID extends ItemDelta> void applyDelta(ID delta) throws SchemaException;
    PrismContainerValue<C> getValueToAdd() throws SchemaException;

    boolean isHeterogenous();
    void setHeterogenous(boolean heterogenous);

    void setVirtualContainerItems(List<VirtualContainerItemSpecificationType> virtualItems);

    List<VirtualContainerItemSpecificationType> getVirtualItems();
    boolean isVirtual();

    boolean isMetadata();
    void setMetadata(boolean metadata);

    PrismContainerDefinition<C> getDefinition();

    @Override
    PrismContainerValue<C> getNewValue();

    PrismContainerWrapper<? extends Containerable> getSelectedChild();

    void addItem(ItemWrapper<?, ?> newItem);

    void clearItems();
    void addItems(Collection<ItemWrapper<?, ?>> items);
    int size();
}

