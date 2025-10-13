/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.enump;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.springframework.stereotype.Component;

@Component
public class ShadowKindPanelFactory extends EnumPanelFactory<ShadowKindType> {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!super.match(wrapper, valueWrapper)) {
            return false;
        }

        return ShadowKindType.class.equals(wrapper.getTypeClass());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<ShadowKindType> panelCtx) {
        return WebComponentUtil.createEnumPanel(
                panelCtx.getComponentId(),
                new ShadowKindTypeListModel(),
                panelCtx.getRealValueModel(),
                panelCtx.getParentComponent(),
                true);
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}
