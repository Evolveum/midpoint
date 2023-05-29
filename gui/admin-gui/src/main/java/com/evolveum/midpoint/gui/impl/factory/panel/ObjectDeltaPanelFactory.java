/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

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
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return QNameUtil.match(ObjectDeltaType.COMPLEX_TYPE, wrapper.getTypeName());
    }
}
