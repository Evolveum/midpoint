/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeUtils;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismContainerPanelContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * Registers {@link AutoassignMappingContainerPanel} for exactly the autoassign mapping container - every other
 * mapping container still goes through the default factor.
 *
 * @author jjarabinec
 */
@Component
public class AutoassignMappingContainerPanelFactory implements GuiComponentFactory<PrismContainerPanelContext<MappingType>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return wrapper instanceof PrismContainerWrapper
                && !wrapper.isMetadata()
                && wrapper.getPath().namedSegmentsOnly().equivalent(MappingRangeUtils.AUTOASSIGN_MAPPING_PATH);
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismContainerPanelContext<MappingType> panelCtx) {
        return new AutoassignMappingContainerPanel(panelCtx.getComponentId(), panelCtx.getValueWrapper(), panelCtx.getSettings());
    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE - 10;
    }
}
