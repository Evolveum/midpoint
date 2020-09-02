/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.MetadataContainerValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.ValueMetadata;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MetadataPanelFactory<C extends Containerable> implements GuiComponentFactory<PrismContainerPanelContext<C>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
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
