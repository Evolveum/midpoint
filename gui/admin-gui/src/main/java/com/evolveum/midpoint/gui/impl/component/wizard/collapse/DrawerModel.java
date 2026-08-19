/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

public class DrawerModel implements DrawerDescriptor<DrawerModel> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final IModel<List<CollapsedItem<DrawerModel>>> collapsedItemsModel;
    private final boolean collapsedMenuVisible;


    public DrawerModel(IModel<List<CollapsedItem<DrawerModel>>> collapsedItemsModel) {
        this.collapsedItemsModel = collapsedItemsModel;
        this.collapsedMenuVisible = collapsedItemsModel.getObject().size() > 1;
    }

    public DrawerModel(IModel<List<CollapsedItem<DrawerModel>>> collapsedItemsModel, boolean collapsedMenuVisible) {
        this.collapsedItemsModel = collapsedItemsModel;
        this.collapsedMenuVisible = collapsedMenuVisible;
    }

    @Override
    public IModel<List<CollapsedItem<DrawerModel>>> getCollapsedItems() {
        return collapsedItemsModel;
    }

    @Override
    public Optional<CollapsedItem<DrawerModel>> getSelectedCollapsedItem() {
        List<CollapsedItem<DrawerModel>> items =
                collapsedItemsModel.getObject();

        if (items == null) {
            return Optional.empty();
        }

        return items.stream()
                .filter(CollapsedItem::isSelected)
                .findFirst();
    }

    @Override
    public Component getFooter(String id, DrawerModel model) {
        return DrawerDescriptor.super.getFooter(id, model);
    }

    @Override
    public boolean isFooterVisible() {
        return true;
    }

    @Override
    public boolean isShowedCollapsedMenu() {
        return collapsedMenuVisible;
    }
}
