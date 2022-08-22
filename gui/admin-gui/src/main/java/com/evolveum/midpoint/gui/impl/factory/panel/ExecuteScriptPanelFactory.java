/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

@Component
public class ExecuteScriptWrapperFactory extends AbstractInputGuiComponentFactory<ExecuteScriptType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return QNameUtil.match(ExecuteScriptType.COMPLEX_TYPE, wrapper.getTypeName());
    }

    @Override
    public Integer getOrder() {
        return super.getOrder() - 1;
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<ExecuteScriptType> panelCtx) {
        return new TextAreaPanel<>(panelCtx.getComponentId(), new ExecuteScriptModel(panelCtx.getRealValueModel(), panelCtx.getPageBase()), 20);
    }
}
