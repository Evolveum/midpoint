/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;

public class ScriptExpressionPanel extends EvaluatorExpressionPanel {

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExpressionPanel.class);

    private static final String ID_CODE_INPUT = "codeInput";
    private static final String ID_CODE_LABEL = "codeLabel";
    private static final String ID_LANGUAGE_INPUT = "languageInput";
    private static final String ID_LANGUAGE_LABEL = "languageLabel";

    public ScriptExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
        ScriptExpressionWrapper wrapper = getEvaluatorValue();
        if (wrapper == null || wrapper.isEmpty()) {
            updateEvaluatorValue((ExpressionUtil.Language) null);
        }
    }

    @Override
    public IModel<String> getValueContainerLabelModel() {
        return getPageBase().createStringResource("ScriptExpressionPanel.label");
    }

    protected void initLayout(MarkupContainer parent) {
        parent.add(new Label(ID_LANGUAGE_LABEL, createStringResource("ScriptExpressionEvaluatorType.language")));

        parent.add(createLanguageInputPanel());

        parent.add(new Label(ID_CODE_LABEL, createStringResource("ScriptExpressionEvaluatorType.code")));

        parent.add(createCodeInputPanel());
    }

    private Component createLanguageInputPanel() {
        ExpressionUtil.Language defaultLanguage = getEvaluatorValue().language;
        if (defaultLanguage == null) {
            defaultLanguage = ExpressionUtil.Language.GROOVY;
        }

        DropDownChoicePanel<ExpressionUtil.Language> languagePanel =
                WebComponentUtil.createEnumPanel(
                        ID_LANGUAGE_INPUT,
                        WebComponentUtil.createReadonlyModelFromEnum(ExpressionUtil.Language.class),
                        Model.of(defaultLanguage),
                        ScriptExpressionPanel.this,
                        false);
        languagePanel.setOutputMarkupId(true);

        languagePanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ExpressionUtil.Language languageValue = languagePanel.getBaseFormComponent().getConvertedInput();
                updateEvaluatorValue(languageValue);
                target.add(getFeedback());
            }
        });

        return languagePanel;
    }

    private WebMarkupContainer createCodeInputPanel() {

        IModel<String> model = new IModel<>() {
            @Override
            public String getObject() {
                return getEvaluatorValue().code;
            }

            @Override
            public void setObject(String value) {
                updateEvaluatorValue(value);
            }

            @Override
            public void detach() {
            }
        };

        SimpleAceEditorPanel editorPanel = new SimpleAceEditorPanel(ID_CODE_INPUT, model, 200) {
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

    private void updateEvaluatorValue(ExpressionUtil.Language language) {
        ScriptExpressionWrapper wrapper = getEvaluatorValue();
        if ((ExpressionUtil.Language.GROOVY.equals(language) && wrapper.language == null)
                || (language == null && wrapper.language == null)
                || language.equals(wrapper.language))  {
            return;
        }
        try {
            ScriptExpressionEvaluatorType evaluator = wrapper.language(language).toEvaluator();
            ExpressionUtil.updateScriptExpressionValue(getModelObject(), evaluator);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update generate expression values: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update generate expression values: " + ex.getLocalizedMessage());
        }
    }

    private void updateEvaluatorValue(String code) {
        try {
            ScriptExpressionEvaluatorType evaluator = getEvaluatorValue().code(code).toEvaluator();
            ExpressionUtil.updateScriptExpressionValue(getModelObject(), evaluator);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update generate expression values: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update generate expression values: " + ex.getLocalizedMessage());
        }
    }

    //don't remove it, used by class and method name
    public static String getInfoDescription(ExpressionType expression, PageBase pageBase) {
        return getEvaluatorCode(expression, pageBase);
    }

    private ScriptExpressionWrapper getEvaluatorValue() {
        try {
            ScriptExpressionEvaluatorType evaluator = ExpressionUtil.getScriptExpressionValue(getModelObject());
            if (evaluator == null) {
                return new ScriptExpressionWrapper();
            }
            return new ScriptExpressionWrapper(evaluator);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get script expression value: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't get script expression value: " + ex.getLocalizedMessage());
        }
        return null;
    }

    private static String getEvaluatorCode(ExpressionType expression, PageBase pageBase) {
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

    public class ScriptExpressionWrapper implements Serializable {

        private ExpressionUtil.Language language;
        private String code;

        private ScriptExpressionWrapper() {
        }

        private ScriptExpressionWrapper(ScriptExpressionEvaluatorType evaluator) {
            if (evaluator.getLanguage() != null) {
                this.language = ExpressionUtil.converLanguage(evaluator.getLanguage());
            }
            this.code = evaluator.getCode();
        }

        public ScriptExpressionEvaluatorType toEvaluator() {
            return new ScriptExpressionEvaluatorType().code(code).language(language.getLanguage());
        }

        public ScriptExpressionWrapper code(String code) {
            this.code = code;
            return ScriptExpressionPanel.ScriptExpressionWrapper.this;
        }

        public ScriptExpressionWrapper language(ExpressionUtil.Language language) {
            this.language = language;
            return ScriptExpressionPanel.ScriptExpressionWrapper.this;
        }

        public boolean isEmpty() {
            return code == null && language == null;
        }
    }
}
