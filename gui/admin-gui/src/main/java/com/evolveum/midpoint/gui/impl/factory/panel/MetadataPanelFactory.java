/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.panel.MetadataContainerValuePanel;
import com.evolveum.midpoint.prism.Containerable;

@Component
public class MetadataPanelFactory<C extends Containerable> implements GuiComponentFactory<PrismContainerPanelContext<C>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!(wrapper instanceof PrismContainerWrapper)) {
            return false;
        }

        return wrapper.isMetadata();
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismContainerPanelContext<C> panelCtx) {
        return new MetadataContainerValuePanel<>(panelCtx.getComponentId(), panelCtx.getValueWrapper(), panelCtx.getSettings());
    }

    @Override
    public Integer getOrder() {
        return 10;
    }
}
