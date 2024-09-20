/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.IconColorInputPanel;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.springframework.stereotype.Component;

import java.io.Serializable;

/***
 * Factory for Color of IconType container. Factory creating basic text input panel with insight.
 */
@Component
public class IconColorPanelFactory extends TextPanelFactory<String> implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper.getParentContainerValue(IconType.class) == null) {
            return false;
        }
        return QNameUtil.match(wrapper.getItemName(), IconType.F_COLOR);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        return new IconColorInputPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel()) {
            @Override
            protected InputPanel createPanel(String idPanel) {
                panelCtx.setComponentId(idPanel);
                return IconColorPanelFactory.super.getPanel(panelCtx);
            }
        };
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}
