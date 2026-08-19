/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.prism.Containerable;

/**
 * Drawer model that holds a single Prism container wrapper and shows it in the drawer
 * using VerticalFormPrismContainerPanel.
 */
public class ContainerDrawerInfoModel<C extends Containerable> implements DrawerDescriptor<ContainerDrawerInfoModel<C>> {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<List<CollapsedItem<ContainerDrawerInfoModel<C>>>> itemsModel;
    private final IModel<PrismContainerValueWrapper<C>> containerWrapperModel;

    public ContainerDrawerInfoModel(IModel<PrismContainerValueWrapper<C>> containerWrapperModel, ItemPanelSettings settings) {
        ContainerCollapsedItem<C> item = new ContainerCollapsedItem<>(containerWrapperModel, settings) {
            @Override
            public IModel<String> getTitle() {
                IModel<String> title = ContainerDrawerInfoModel.this.getTitle();
                if (title != null && title.getObject() != null) {
                    return title;
                }
                return super.getTitle();
            }

            @Override
            protected void customizePanel(ContainerDrawerPanel<?> components) {
                ContainerDrawerInfoModel.this.customizePanel(components);
            }

            @Override
            protected IModel<String> getDescription() {
                IModel<String> description = ContainerDrawerInfoModel.this.getDescription();
                if (description != null && description.getObject() != null) {
                    return description;
                }
                return super.getDescription();
            }
        };
        item.setSelected(true);
        List<CollapsedItem<ContainerDrawerInfoModel<C>>> list = new ArrayList<>();
        list.add(item);

        this.containerWrapperModel = containerWrapperModel;
        itemsModel = Model.ofList(list);
    }

    @Override
    public boolean isShowedCollapsedMenu() {
        return false;
    }

    public IModel<PrismContainerValueWrapper<C>> getContainerWrapperModel() {
        return containerWrapperModel;
    }

    @Override
    public IModel<List<CollapsedItem<ContainerDrawerInfoModel<C>>>> getCollapsedItems() {
        return itemsModel;
    }

    @Override
    public Optional<CollapsedItem<ContainerDrawerInfoModel<C>>> getSelectedCollapsedItem() {
        return itemsModel.getObject().stream()
                .filter(CollapsedItem::isSelected)
                .findFirst();
    }

    @Override
    public Component getFooter(String id, ContainerDrawerInfoModel<C> drawerModel) {
        return new DrawerFooterPanel(id) {
            @Override
            public void noPerformed(AjaxRequestTarget target) {
                PrismContainerValueWrapper<C> object = drawerModel.getContainerWrapperModel().getObject();
                WebPrismUtil.resetContainerValueWrapper(object);
                super.noPerformed(target);
                ContainerDrawerInfoModel.this.onNoPerformed(target, drawerModel);
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                super.yesPerformed(target);
                ContainerDrawerInfoModel.this.onYesPerformed(target);
            }
        };
    }

    @Override
    public boolean isFooterVisible() {
        return true;
    }

    protected IModel<String> getTitle() {
        return null;
    }

    protected void customizePanel(ContainerDrawerPanel<?> components) {
    }

    protected IModel<String> getDescription() {
        return null;
    }

    protected void onNoPerformed(AjaxRequestTarget target, ContainerDrawerInfoModel<C> drawerModel) {
    }

    protected void onYesPerformed(AjaxRequestTarget target) {
    }

}

