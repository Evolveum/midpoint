/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.expression;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FilterExpressionEvaluatorType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Preview panel for filter expressions.
 */
public class FilterExpressionPreviewDetailsPanel extends BasePanel<ExpressionType> {

    private static final String ID_QUERY = "query";

    public FilterExpressionPreviewDetailsPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(new Label(ID_QUERY, getModel().map(FilterExpressionPreviewDetailsPanel::extractQuery)));
    }

    private static String extractQuery(ExpressionType expression) {
        FilterExpressionEvaluatorType evaluator = ExpressionUtil.getFilterExpressionValue(expression);
        if (evaluator == null || evaluator.getFilter() == null) {
            return null;
        }
        return evaluator.getFilter().getText();
    }
}
