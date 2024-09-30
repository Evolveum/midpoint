/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import javax.xml.namespace.QName;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.AnyTypePanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;

@Component
public class AnyTypePanelFactory extends AbstractInputGuiComponentFactory<Object> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        QName type = wrapper.getTypeName();
        return DOMUtil.XSD_ANYTYPE.equals(type);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<Object> panelCtx) {
        AnyTypePanel panel = new AnyTypePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
        panel.setOutputMarkupId(true);

        return panel;
    }
}
