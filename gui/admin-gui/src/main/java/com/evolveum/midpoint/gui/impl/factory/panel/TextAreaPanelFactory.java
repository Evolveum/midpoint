/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;

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
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return FocusType.F_DESCRIPTION.equals(wrapper.getItemName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<T> panelCtx) {
        int size = 10;
        if (FocusType.F_DESCRIPTION.equals(panelCtx.getDefinitionName())) {
            size = 2;
        }
        return new TextAreaPanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel(), size);
    }

    @Override
    public Integer getOrder() {
        return super.getOrder() - 2;
    }
}
