/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevGenerateArtifactResultType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.IOException;
import java.util.List;

/**
 * @author lskublik
 */
public abstract class ScriptConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String ID_PANEL = "panel";

    private static final String CLASS_DOT = ScriptConnectorStepPanel.class.getName() + ".";
    private static final String OP_LOAD_DOCS = CLASS_DOT + "loadDocumentations";
    private static final String OP_SAVE_AUTH_SCRIPT_DOCS = CLASS_DOT + "saveAuthScript";

    private LoadableModel<ConnDevArtifactType> valueModel;
    private boolean useOriginal = false;

    public ScriptConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
        createModels();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void createModels() {
        valueModel = new LoadableModel<>() {
            @Override
            protected ConnDevArtifactType load() {
//                if (useOriginal) {
//                    ConnDevArtifactType origValue = getOriginalContainerValue();
//                    if (origValue != null) {
//                        return origValue;
//                    }
//                }

                Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_LOAD_DOCS);
                OperationResult result = task.getResult();
                String token = getHelper().getVariable(getTokenForTaskForObtainResult());

                if (StringUtils.isEmpty(token)) {
                    return null;
                }

                StatusInfo<ConnDevGenerateArtifactResultType> statusInfo;
                try {
                    statusInfo = getDetailsModel().getServiceLocator().getConnectorService().getGenerateArtifactStatus(token, task, result);
                } catch (SchemaException | ObjectNotFoundException e) {
                    throw new RuntimeException(e);
                }
                ConnDevGenerateArtifactResultType artifactResultType = statusInfo.getResult();

                if (artifactResultType == null) {
                    return null;
                }

                return artifactResultType.getArtifact();
            }
        };
    }

    protected abstract ConnDevArtifactType getOriginalContainerValue();

    protected abstract String getTokenForTaskForObtainResult();

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        SimpleAceEditorPanel editorPanel = new SimpleAceEditorPanel(
                ID_PANEL, new PropertyModel<>(valueModel, ConnDevArtifactType.F_CONTENT.getLocalPart()), 450) {

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
        return true;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("ScriptConnectorStepPanel.submit");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        onNextPerformed(target);
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OP_SAVE_AUTH_SCRIPT_DOCS);
        try {
            saveScript(valueModel.getObject(), task, task.getResult());
            if (task.getResult() == null || task.getResult().isError()) {
                target.add(getFeedback());
                return false;
            }
            useOriginal = true;
            valueModel.detach();
        } catch (IOException | CommonException e) {
            throw new RuntimeException(e);
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

    protected final LoadableModel<ConnDevArtifactType> getValueModel() {
        return valueModel;
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        AjaxIconButton testResource = new AjaxIconButton(
                customButtons.newChildId(),
                Model.of("fa fa-refresh "),
                getPageBase().createStringResource("ScriptConnectorStepPanel.regenerate")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onRefreshPerformed(target);
            }
        };
        testResource.showTitleAsLabel(true);
        testResource.add(AttributeAppender.append("class", "ml-auto"));
        customButtons.add(testResource);
    }

    private void onRefreshPerformed(AjaxRequestTarget target) {
        if (getWizard() instanceof WizardModelWithParentSteps parentWizardModel) {
            List<WizardStep> steps = parentWizardModel.getActiveChildrenSteps();
            int activeStepIndex = parentWizardModel.getActiveStepIndex();
            String idOfFound = null;
            for (int i = activeStepIndex - 1; i >= 0; i--) {
                if (i < 0) {
                    return;
                }

                WizardStep step = steps.get(i);
                if (step instanceof WaitingScriptConnectorStepPanel waitingPanel) {
                    idOfFound = step.getStepId();
                    waitingPanel.resetScript(getPageBase());
                    if (i == 0) {
                        setActiveStepById(target, parentWizardModel, idOfFound);
                    }
                } else if (StringUtils.isNotEmpty(idOfFound)) {
                    setActiveStepById(target, parentWizardModel, idOfFound);
                    return;
                }
            }
            useOriginal = false;
            valueModel.detach();
        }
    }

    private void setActiveStepById(AjaxRequestTarget target, WizardModelWithParentSteps parentWizardModel, String idOfFound) {
        parentWizardModel.setActiveStepById(idOfFound);
        parentWizardModel.fireActiveStepChanged();
        target.add(getWizard().getPanel());
    }

    protected abstract void saveScript(ConnDevArtifactType object, Task task, OperationResult result) throws IOException, CommonException;
}
