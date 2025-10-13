/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

@Component
public class TextAreaPanelFactory<T extends Serializable> extends AbstractInputGuiComponentFactory<T> {


    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return FocusType.F_DESCRIPTION.equals(wrapper.getItemName()) || FocusType.F_DOCUMENTATION.equals(wrapper.getItemName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<T> panelCtx) {
        int size = 10;
        if (FocusType.F_DESCRIPTION.equals(panelCtx.getDefinitionName())) {
            size = 2;
        }
        if (FocusType.F_DOCUMENTATION.equals(panelCtx.getDefinitionName())) {
            size = 3;
        }
        return new TextAreaPanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel(), size);
    }

    @Override
    public Integer getOrder() {
        return super.getOrder() - 2;
    }
}
