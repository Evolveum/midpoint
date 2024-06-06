/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationFromLinkExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AssociationFromLinkPanel extends EvaluatorExpressionPanel {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationFromLinkPanel.class);

    public AssociationFromLinkPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
        updateEvaluatorValue();
    }

    @Override
    public IModel<String> getValueContainerLabelModel() {
        return Model.of("");
    }

    protected void initLayout(MarkupContainer parent) {
    }

    private void updateEvaluatorValue() {
        ExpressionType expressionType = getModelObject();
        try {
            AssociationFromLinkExpressionEvaluatorType evaluator = new AssociationFromLinkExpressionEvaluatorType();
            expressionType = ExpressionUtil.updateAssociationFromLinkExpressionValue(expressionType, evaluator);
            getModel().setObject(expressionType);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update associationFromLink expression value: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update associationFromLink expression value: " + ex.getLocalizedMessage());
        }
    }

    //don't remove it, used by class and method name
    public static String getInfoDescription(ExpressionType expression, PageBase pageBase) {
        return null;
    }
}
