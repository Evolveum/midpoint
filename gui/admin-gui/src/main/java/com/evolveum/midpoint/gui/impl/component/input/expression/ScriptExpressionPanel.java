/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class ScriptExpressionPanel extends EvaluatorExpressionPanel {

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExpressionPanel.class);

    private static final String ID_VALUE_INPUT = "valueInput";
    private static final String ID_VALUE_CONTAINER_LABEL = "valueContainerLabel";

    public ScriptExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (StringUtils.isEmpty(getEvaluatorValue())) {
            updateEvaluatorValue("");
        }
    }

    @Override
    public IModel<String> getValueContainerLabelModel() {
        return getPageBase().createStringResource("ScriptExpressionPanel.label");
    }

    public static String getInfoDescription(ExpressionType expression, PageBase pageBase) {
        return getEvaluatorValue(expression, pageBase);
    }

    protected void initLayout(MarkupContainer parent) {
        parent.add(new Label(ID_VALUE_CONTAINER_LABEL, getValueContainerLabelModel()));

        parent.add(createDefaultInputPanel());
    }

    private WebMarkupContainer createDefaultInputPanel() {

        IModel<String> model = new IModel<>() {
            @Override
            public String getObject() {
                return getEvaluatorValue();
            }

            @Override
            public void setObject(String value) {
                updateEvaluatorValue(value);
            }

            @Override
            public void detach() {
            }
        };

        SimpleAceEditorPanel editorPanel = new SimpleAceEditorPanel(ID_VALUE_INPUT, model, 200) {
            protected AceEditor createEditor(String id, IModel<String> model, int minSize) {
                AceEditor editor = new AceEditor(id, model);
                editor.setReadonly(false);
                editor.setMinHeight(minSize);
                editor.setResizeToMaxHeight(false);
                editor.setMode("ace/mode/groovy");
                add(editor);
                return editor;
            }
        };
        ((AceEditor) editorPanel.getBaseFormComponent()).setConvertEmptyInputStringToNull(false);
        editorPanel.add(AttributeAppender.append("class", "d-flex flex-column w-100 border rounded"));

        editorPanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String updatedValue = ((AceEditor) editorPanel.getBaseFormComponent()).getConvertedInput();
                updateEvaluatorValue(updatedValue);
                target.add(getFeedback());
            }
        });

        return editorPanel;
    }

    private void updateEvaluatorValue(String value) {
        try {
            if (StringUtils.isNotEmpty(value) && value.contains(ExpressionUtil.ELEMENT_SCRIPT)) {
                getFeedbackMessages().add(
                        new FeedbackMessage(
                                getFeedback(),
                                getPageBase().createStringResource("ScriptExpressionPanel.warning.parse").getString(),
                                300));
            }
            ExpressionUtil.updateScriptExpressionValue(getModelObject(), value);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update script expression values: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update script expression values: " + ex.getLocalizedMessage());
        }
    }

    private String getEvaluatorValue() {
        return getEvaluatorValue(getModelObject(), getPageBase());
    }

    private static String getEvaluatorValue(ExpressionType expression, PageBase pageBase) {
        try {
            ScriptExpressionEvaluatorType evaluator = ExpressionUtil.getScriptExpressionValue(expression);
            if (evaluator == null) {
                return "";
            }
            return evaluator.getCode();
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get script expression value: {}", ex.getLocalizedMessage());
            pageBase.error("Couldn't get script expression value: " + ex.getLocalizedMessage());
        }
        return "";
    }
}
