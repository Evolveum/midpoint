/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.web.component.input.TriStateComboPanel;

import org.springframework.stereotype.Component;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;

/**
 * @author katka
 */
@Component
public class ThreeStateComboPanelFactory extends AbstractInputGuiComponentFactory<Boolean> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return DOMUtil.XSD_BOOLEAN.equals(wrapper.getTypeName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<Boolean> panelCtx) {
        return new TriStateComboPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
    }
}
