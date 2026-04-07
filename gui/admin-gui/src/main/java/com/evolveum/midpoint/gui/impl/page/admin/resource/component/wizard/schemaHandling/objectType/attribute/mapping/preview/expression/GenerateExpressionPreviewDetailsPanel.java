/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.expression;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

/**
 * Preview panel for generate expressions. Shows generation mode
 * and associated value policy reference.
 */
public class GenerateExpressionPreviewDetailsPanel extends BasePanel<ExpressionType> {

    private static final Trace LOGGER = TraceManager.getTrace(GenerateExpressionPreviewDetailsPanel.class);

    private static final String ID_MODE = "mode";
    private static final String ID_VALUE_POLICY = "valuePolicy";

    public GenerateExpressionPreviewDetailsPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(new Label(ID_MODE, Model.of(getModeLabel())));
        add(new Label(ID_VALUE_POLICY, Model.of(getValuePolicyLabel())));
    }

    private String getModeLabel() {
        GenerateExpressionEvaluatorType evaluator = getGenerateEvaluator();
        if (evaluator == null || evaluator.getMode() == null) {
            return "";
        }

        return getPageBase().getString(evaluator.getMode());
    }

    private String getValuePolicyLabel() {
        GenerateExpressionEvaluatorType evaluator = getGenerateEvaluator();
        if (evaluator == null || evaluator.getValuePolicyRef() == null) {
            return "";
        }

        return StringUtils.defaultString(
                WebComponentUtil.getReferencedObjectDisplayNameAndName(
                        evaluator.getValuePolicyRef(), true, getPageBase()));
    }

    private @Nullable GenerateExpressionEvaluatorType getGenerateEvaluator() {
        ExpressionType expression = getModelObject();
        if (expression == null) {
            return null;
        }

        try {
            return ExpressionUtil.getGenerateExpressionValue(expression);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get generate expression value: {}", ex.getLocalizedMessage(), ex);
            return null;
        }
    }
}
