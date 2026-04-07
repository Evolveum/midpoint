/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.expression;

import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.web.component.AceEditor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Preview panel for script expressions. Shows the script language and
 * its code using a read-only {@link AceEditor}.
 */
public class ScriptExpressionPreviewDetailsPanel extends BasePanel<ExpressionType> {

    private static final String ID_LANGUAGE = "language";
    private static final String ID_EDITOR = "editor";

    public ScriptExpressionPreviewDetailsPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(new Label(ID_LANGUAGE, Model.of(getLanguage())));

        AceEditorPanel editorPanel = new AceEditorPanel(
                ID_EDITOR,
                null,
                Model.of(getCode()),
                400
        );

        add(editorPanel);

        AceEditor editor = editorPanel.getEditor();
        if (editor != null) {
            editor.setReadonly(true);
        }
    }

    private @NotNull String getLanguage() {
        ScriptExpressionEvaluatorType script = getScriptEvaluator();
        if (script == null || StringUtils.isBlank(script.getLanguage())) {
            return "Groovy";
        }

        String language = script.getLanguage();
        int lastSlash = language.lastIndexOf("#");
        return lastSlash >= 0 ? language.substring(lastSlash + 1) : language;
    }

    private String getCode() {
        ScriptExpressionEvaluatorType script = getScriptEvaluator();
        return script != null ? StringUtils.defaultString(script.getCode()) : "";
    }

    private @Nullable ScriptExpressionEvaluatorType getScriptEvaluator() {
        ExpressionType expression = getModelObject();
        if (expression == null || expression.getExpressionEvaluator() == null) {
            return null;
        }

        List<JAXBElement<?>> evaluators = expression.getExpressionEvaluator();
        for (Object evaluator : evaluators) {
            Object value = evaluator instanceof JAXBElement<?> jaxb ? jaxb.getValue() : evaluator;
            if (value instanceof ScriptExpressionEvaluatorType script) {
                return script;
            }
        }

        return null;
    }
}
