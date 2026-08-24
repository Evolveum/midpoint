/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import jakarta.annotation.PostConstruct;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

@Component
public class ConditionPanelFactory extends ExpressionPanelFactory implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ExpressionType> panelCtx) {
        return new ExpressionPanel(
                panelCtx.getComponentId(), panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel()) {

            @Override
            protected List<RecognizedEvaluator> getChoices() {
                return new ArrayList<>(List.of(RecognizedEvaluator.SCRIPT, RecognizedEvaluator.FILTER));
            }
        };
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return QNameUtil.match(ExpressionType.COMPLEX_TYPE, wrapper.getTypeName())
                && (wrapper.getItemName().getLocalPart().toLowerCase().contains(MappingType.F_CONDITION.getLocalPart())
                || (wrapper.getParent() != null
                    && wrapper.getParent().getParent() != null
                    && wrapper.getParent().getParent().getItemName().getLocalPart().toLowerCase().contains(MappingType.F_CONDITION.getLocalPart())));
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}
