/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

public interface DrawerDescriptor<M extends DrawerDescriptor<M>>
        extends Serializable {

    IModel<List<CollapsedItem<M>>> getCollapsedItems();

    Optional<CollapsedItem<M>> getSelectedCollapsedItem();

    default boolean isCollapsedItemsVisible() {
        return getCollapsedItems().getObject().stream()
                .anyMatch(CollapsedItem::isSelected);
    }

    default boolean isShowedCollapsedMenu() {
        return true;
    }

    default void clearSelection() {
        getCollapsedItems().getObject()
                .forEach(item -> item.setSelected(false));
    }

    default void select(CollapsedItem<M> selectedItem) {
        getCollapsedItems().getObject()
                .forEach(item -> item.setSelected(item == selectedItem));
    }

    default Component getFooter(String id, M model) {
        return new DrawerFooterPanel(id);
    }

    default boolean isFooterVisible() {
        return false;
    }

}
