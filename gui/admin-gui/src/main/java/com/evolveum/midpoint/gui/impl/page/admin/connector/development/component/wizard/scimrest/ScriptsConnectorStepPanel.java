/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevGenerateArtifactResultType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public abstract class ScriptsConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String ID_PANEL = "panel";

    private static final String CLASS_DOT = ScriptsConnectorStepPanel.class.getName() + ".";
    private static final String OP_LOAD_DOCS = CLASS_DOT + "loadDocumentations";
    private static final String OP_SAVE_AUTH_SCRIPT_DOCS = CLASS_DOT + "saveAuthScript";

    private LoadableModel<List<ConnDevArtifactType>> valueModel;

    public ScriptsConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createModels();
        initLayout();
    }

    private void createModels() {
        valueModel = new LoadableModel<>() {
            @Override
            protected List<ConnDevArtifactType> load() {
                List<String> tokens = getTokensForTasksForObtainResults();
                List<ConnDevArtifactType> list = new ArrayList<>();
                tokens.forEach(token -> {
                    Task task = getPageBase().createSimpleTask(OP_LOAD_DOCS);
                    OperationResult result = task.getResult();

                    StatusInfo<ConnDevGenerateArtifactResultType> statusInfo;
                    try {
                        statusInfo = getDetailsModel().getServiceLocator().getConnectorService().getGenerateArtifactStatus(token, task, result);
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    ConnDevGenerateArtifactResultType artifactResultType = statusInfo.getResult();

                    if (artifactResultType == null) {
                        return;
                    }

                    list.add(artifactResultType.getArtifact());
                });
                return list;
            }
        };
    }

    protected abstract List<String> getTokensKeys();

    private List<String> getTokensForTasksForObtainResults() {
        return getTokensKeys().stream().map(key -> getHelper().getVariable(key)).toList();
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        TabbedPanel<ITab> panel = WebComponentUtil.createTabPanel(ID_PANEL, getPageBase(), createScriptsTabs());
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private List<ITab> createScriptsTabs() {
        return valueModel.getObject().stream()
                .map(scriptArtifact ->
                        (ITab) new AbstractTab(() -> scriptArtifact.getFilename()) {
                            @Override
                            public WebMarkupContainer getPanel(String id) {
                                SimpleAceEditorPanel editorPanel = new SimpleAceEditorPanel(
                                        id,
                                        new PropertyModel<>(scriptArtifact, ConnDevArtifactType.F_CONTENT.getLocalPart()),
                                        450) {

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
                                return editorPanel;
                            }
                        }
                ).toList();
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

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        for (ConnDevArtifactType scriptArtifact : valueModel.getObject()) {
            Task task = getPageBase().createSimpleTask(OP_SAVE_AUTH_SCRIPT_DOCS);
            try {
                saveScript(scriptArtifact, task, task.getResult());
                if (task.getResult() == null || task.getResult().isError()) {
                    target.add(getFeedback());
                    return false;
                }
            } catch (IOException | CommonException e) {
                throw new RuntimeException(e);
            }
        }

        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }

    protected abstract void saveScript(ConnDevArtifactType object, Task task, OperationResult result) throws IOException, CommonException;
}
