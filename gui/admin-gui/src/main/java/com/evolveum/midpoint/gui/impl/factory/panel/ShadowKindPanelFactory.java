/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        IModel<List<ShadowKindType>> choices = () -> {
            List<ShadowKindType> list = new ArrayList<>();
            Collections.addAll(list, ShadowKindType.class.getEnumConstants());
            list.removeIf(value -> ShadowKindType.ASSOCIATION == value);

            return list;
        };
        return WebComponentUtil.createEnumPanel(
                panelCtx.getComponentId(), choices, panelCtx.getRealValueModel(), panelCtx.getParentComponent(), true);
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}
