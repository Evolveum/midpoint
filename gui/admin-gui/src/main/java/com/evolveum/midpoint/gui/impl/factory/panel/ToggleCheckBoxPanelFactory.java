/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import javax.annotation.PostConstruct;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import org.springframework.stereotype.Component;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;

/**
 * @author katka
 */
@Component
public class ToggleCheckBoxPanelFactory extends AbstractInputGuiComponentFactory<Boolean> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return DOMUtil.XSD_BOOLEAN.equals(wrapper.getTypeName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<Boolean> panelCtx) {
        return new ToggleCheckBoxPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
    }
}
