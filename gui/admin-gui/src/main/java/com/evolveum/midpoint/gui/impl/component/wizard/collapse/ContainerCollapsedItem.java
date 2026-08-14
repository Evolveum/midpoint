/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class ContainerCollapsedItem<C extends Containerable> extends CollapsedItem<ContainerDrawerInfoModel<C>> {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<PrismContainerValueWrapper<C>> wrapperModel;
    private final ItemPanelSettings settings;

    public ContainerCollapsedItem(IModel<PrismContainerValueWrapper<C>> wrapperModel, ItemPanelSettings settings) {
        this.wrapperModel = wrapperModel;
        this.settings = settings;
    }

    @Override
    public @NotNull IModel<String> getIcon() {
        return Model.of("fa fa-sort-numeric-asc");
    }

    @Override
    @NotNull
    IModel<String> getTitleIconCss() {
        return Model.of("fa fa-sort-numeric-asc");
    }

    @Override
    public IModel<String> getTitle() {
        return LoadableDetachableModel.of(() -> {
            PrismContainerValueWrapper<C> w = wrapperModel.getObject();
            if (w == null) {
                return "";
            }
            String displayName = w.getDisplayName();
            return displayName != null ? displayName : "";
        });
    }

    @Override
    public @NotNull Component getPanel(String id, ContainerDrawerInfoModel<C> drawerModel) {
        ContainerDrawerPanel<C> components = new ContainerDrawerPanel<>(id,
                drawerModel.getContainerWrapperModel(),
                settings,
                getDescription());
        customizePanel(components);
        return components;
    }

    protected void customizePanel(ContainerDrawerPanel<?> components) {

    }

    protected IModel<String> getDescription() {
        return null;
    }

}
