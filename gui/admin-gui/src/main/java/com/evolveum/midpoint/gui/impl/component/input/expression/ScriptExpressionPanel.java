/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptExpressionPanel extends EvaluatorExpressionPanel {

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExpressionPanel.class);

    public ScriptExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    public IModel<String> getValueContainerLabelModel() {
        return getPageBase().createStringResource("ScriptExpressionPanel.label");
    }

    @Override
    protected void updateEvaluatorValue(ExpressionType expression, List<String> values) {
        try {
            if (values.stream().anyMatch(value -> StringUtils.isNotEmpty(value) && value.contains(ExpressionUtil.ELEMENT_SCRIPT))) {
                getFeedbackMessages().add(
                        new FeedbackMessage(
                                getFeedback(),
                                getPageBase().createStringResource("ScriptExpressionPanel.warning.parse").getString(),
                                300));
            }
            ExpressionUtil.updateScriptExpressionValue(expression, values);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update script expression values: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update script expression values: " + ex.getLocalizedMessage());
        }
    }

    @Override
    protected List<String> getEvaluatorValues() {
        List<String> literalValueList = new ArrayList<>();
        try {
            return ExpressionUtil.getScriptExpressionValues(getModelObject()).stream()
                    .map(ScriptExpressionEvaluatorType::getCode)
                    .collect(Collectors.toList());
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get script expression value: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't get script expression value: " + ex.getLocalizedMessage());
        }
        return literalValueList;
    }

    @Override
    protected InputPanel createTextPanel(String id, IModel<String> model) {
        SimpleAceEditorPanel editorPanel = new SimpleAceEditorPanel(id, model, 200){
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
        editorPanel.add(AttributeAppender.append("class", "d-flex flex-column w-100 border rounded"));
        return editorPanel;
    }
}
