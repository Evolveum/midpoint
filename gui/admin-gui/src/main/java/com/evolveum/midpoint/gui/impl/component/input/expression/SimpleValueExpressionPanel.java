/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContext;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

public class SimpleValueExpressionPanel extends EvaluatorExpressionPanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SimpleValueExpressionPanel.class);

    public SimpleValueExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    public IModel<String> getValueContainerLabelModel() {
        return getPageBase().createStringResource("AssociationExpressionValuePanel.literalValue");
    }

    @Override
    protected void updateEvaluatorValue(ExpressionType expression, List<String> values) {
        ExpressionUtil.updateLiteralExpressionValue(expression, values, PrismContext.get());
    }

    @Override
    protected List<String> getEvaluatorValues() {
        List<String> literalValueList = new ArrayList<>();
        try {
            return ExpressionUtil.getLiteralExpressionValues(getModelObject());
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get literal expression value: {}", ex.getLocalizedMessage());
        }
        return literalValueList;
    }
}
