/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class ScriptConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String ID_PANEL = "panel";

    private LoadableModel<String> scriptModel;

    public ScriptConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createValuesModel();
        initLayout();
    }

    private void createValuesModel() {
        scriptModel = new LoadableModel<>() {
            @Override
            protected String load() {
                return "";
            }
        };
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        SimpleAceEditorPanel editorPanel = new SimpleAceEditorPanel(ID_PANEL, scriptModel, 250) {

            protected AceEditor createEditor(String id, IModel<String> model, int minSize) {
                AceEditor editor = new AceEditor(id, model);
                editor.setReadonly(false);
                editor.setMinHeight(minSize);
                editor.setResizeToMaxHeight(false);
                editor.setMode(AceEditor.Mode.GROOVY);
                add(editor);
                return editor;
            }
        };

        ((AceEditor) editorPanel.getBaseFormComponent()).setConvertEmptyInputStringToNull(false);
        editorPanel.add(AttributeAppender.append("class", "d-flex flex-column w-100 border rounded"));

        editorPanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getFeedback());
            }
        });
        add(editorPanel);
    }

    @Override
    public String appendCssToWizard() {
        return "col-10";
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }
}
