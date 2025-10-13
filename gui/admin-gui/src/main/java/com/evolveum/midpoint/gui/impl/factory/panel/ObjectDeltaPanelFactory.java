/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class ObjectDeltaPanelFactory extends AbstractInputGuiComponentFactory<ObjectDeltaType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public Integer getOrder() {
        return super.getOrder() - 100;
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<ObjectDeltaType> panelCtx) {
        return new TextAreaPanel<>(panelCtx.getComponentId(), new ObjectDeltaModel(panelCtx.getRealValueModel(), panelCtx.getPageBase()), 20);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return QNameUtil.match(ObjectDeltaType.COMPLEX_TYPE, wrapper.getTypeName());
    }
}
