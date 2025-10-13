/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.expression;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class ExpressionPanelFactory extends AbstractGuiComponentFactory<ExpressionType> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ExpressionType> panelCtx) {
        return new ExpressionPanel(panelCtx.getComponentId(), (IModel)panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel());
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return QNameUtil.match(ExpressionType.COMPLEX_TYPE, wrapper.getTypeName());
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }
}
