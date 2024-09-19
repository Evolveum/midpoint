/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.expression;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSynchronizationExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.wicket.markup.html.panel.Panel;

import java.util.List;

public abstract class AssociationReferenceAttributeExpressionPanelFactory extends ExpressionPanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper == null) {
            return false;
        }

        if (!QNameUtil.match(ExpressionType.COMPLEX_TYPE, wrapper.getTypeName())) {
            return false;
        }

        var evaluator = wrapper.getParentContainerValue(getEvaluatorClass());
        if (evaluator == null) {
            return false;
        }

        var parent = wrapper.getParentContainerValue(getMappingsClass());
        return parent != null && parent.getParent().getItemName().equivalent(AssociationSynchronizationExpressionEvaluatorType.F_OBJECT_REF);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ExpressionType> panelCtx) {
        return new ExpressionPanel(panelCtx.getComponentId(), panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel()) {
            @Override
            protected List<ExpressionPanel.RecognizedEvaluator> getChoices() {
                return AssociationReferenceAttributeExpressionPanelFactory.this.getChoices(super.getChoices());
            }
        };
    }

    protected abstract Class<? extends Containerable> getMappingsClass();

    protected abstract Class<? extends Containerable> getEvaluatorClass();

    protected List<ExpressionPanel.RecognizedEvaluator> getChoices(List<ExpressionPanel.RecognizedEvaluator> parentChoices) {
        parentChoices.removeIf(choice -> expressionEvaluatorForRemove() == choice);
        return parentChoices;
    }

    protected abstract ExpressionPanel.RecognizedEvaluator expressionEvaluatorForRemove();

    @Override
    public Integer getOrder() {
        return 99;
    }
}
