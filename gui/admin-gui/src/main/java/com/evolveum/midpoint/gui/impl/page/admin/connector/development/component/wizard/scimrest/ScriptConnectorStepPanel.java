/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnDevArtifactValidationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnDevScriptFormat;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevGenerateArtifactResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

/**
 * @author lskublik
 */
public abstract class ScriptConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String ID_PANEL = "panel";
    private static final String ID_LANGUAGE_SELECT = "languageSelect";
    private static final String ID_SUGGESTED_SCRIPT_ALERT = "suggestedScriptAlert";

    /** {@code getHelper().putVariable} key for the editor's live content - survives valueModel's own detach. */
    private static final String VAR_LIVE_SCRIPT = "liveScript";

    private static final String CLASS_DOT = ScriptConnectorStepPanel.class.getName() + ".";
    private static final String OP_LOAD_SCRIPT = CLASS_DOT + "loadScript";
    private static final String OP_SAVE_SCRIPT = CLASS_DOT + "saveScript";

    private LoadableModel<ConnDevArtifactType> valueModel;
    private boolean isReloaded = false;

    /** Guards the auto-validate-on-arrival check in {@link #createModels()} to run once per fresh script. */
    private boolean autoValidationDone = false;

    /** Script format; derived from the artifact's file extension, overridable via the language selector. */
    private final LoadableModel<ConnDevScriptFormat> languageModel = new LoadableModel<>() {
        @Override
        protected ConnDevScriptFormat load() {
            ConnDevArtifactType artifact = valueModel != null ? valueModel.getObject() : null;
            return ConnDevScriptFormat.fromFilename(artifact != null ? artifact.getFilename() : null);
        }
    };
    private AceEditor scriptEditor;

    /** Only built for mandatory scripts - for an optional script, "Regenerate" lives in {@link #scriptActionsMenu} instead. */
    private AjaxIconButton regenerateButton;

    /** Kept so a freshly-recorded validation error can refresh its visibility (see {@link #onNextPerformed}). */
    private RepairObjectClassButton repairObjectClassButton;

    /**
     * For an optional script: dropdown with "Discard suggested script" / "Regenerate" /
     * "Discard script", next to the plain submit button. Built in {@link #createCustomButtonsAnchor}
     * rather than {@link #initCustomButtons}'s repeater, since that wraps every child in an
     * {@code <a>} tag - illegal for a {@code <button>}+{@code <div class="dropdown-menu">} panel.
     */
    private DropdownButtonPanel scriptActionsMenu;

    private WebMarkupContainer suggestedScriptAlert;

    /** Set once the user edits after a validation error was recorded, to hide "Regenerate" via {@link #hasPendingValidationError()}. */
    private boolean scriptEditedSinceError = false;

    /** Mode last pushed to the live editor - a step panel is reused across navigations, so a plain re-render alone never pushes a {@link #languageModel} change. */
    private AceEditor.Mode lastPushedMode;

    public ScriptConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);
        createModels();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
        valueModel.getObject();
        initLayout();
    }

    /**
     * Re-syncs the live editor's mode with {@link #languageModel} on every render - the editor is a
     * stateful JS widget that only changes mode when explicitly told to via {@link AceEditor#updateMode}.
     */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (scriptEditor != null) {
            languageModel.detach();
            AceEditor.Mode currentMode = toAceMode(languageModel.getObject());
            var target = RequestCycle.get().find(AjaxRequestTarget.class);
            target.ifPresentOrElse(
                    t -> scriptEditor.updateMode(t, currentMode),
                    () -> scriptEditor.setMode(currentMode));
            lastPushedMode = currentMode;
        }
    }

    private void createModels() {
        valueModel = new LoadableModel<>() {
            @Override
            protected ConnDevArtifactType load() {
                PrismContainerValueWrapper<ConnDevArtifactType> deployedWrapper = deployedScriptWrapper();
                boolean deployedActive = deployedWrapper != null
                        && !Boolean.TRUE.equals(deployedWrapper.getRealValue().isDisabled());

                if (!isReloaded && deployedActive) {
                    ConnDevArtifactType artifactType = deployedWrapper.getRealValue();
                    Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_LOAD_SCRIPT);
                    OperationResult result = task.getResult();
                    try {
                        String content = getDetailsModel().getConnectorDevelopmentOperation().getArtifactContent(artifactType, task, result);
                        artifactType.setContent(content);
                        return artifactType;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                String token = getTaskToken();

                if (StringUtils.isEmpty(token)) {
                    return getScriptType().create(getObjectClassName());
                }

                Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_LOAD_SCRIPT);
                OperationResult result = task.getResult();

                StatusInfo<ConnDevGenerateArtifactResultType> statusInfo;
                try {
                    statusInfo = getDetailsModel().getServiceLocator().getConnectorService().getGenerateArtifactStatus(token, task, result);
                } catch (CommonException e) {
                    throw new RuntimeException(e);
                }
                ConnDevGenerateArtifactResultType artifactResultType = statusInfo.getResult();

                if (artifactResultType == null || artifactResultType.getArtifact() == null) {
                    return getScriptType().create(getObjectClassName());
                }

                ConnDevArtifactType artifact = artifactResultType.getArtifact();

                if (!autoValidationDone) {
                    autoValidationDone = true;
                    boolean skipValidation = isScriptOptional() && StringUtils.isBlank(artifact.getContent());
                    ConnDevArtifactValidationResult validation = skipValidation
                            ? ConnDevArtifactValidationResult.success()
                            : getDetailsModel().getConnectorDevelopmentOperation().validateArtifact(artifact, task, result);

                    if (validation.ok()) {
                        if (hasPendingValidationError()) {
                            ConnectorDevelopmentWizardUtil.clearScriptValidationErrors(
                                    ScriptConnectorStepPanel.this, getStepId());
                        }
                    } else {
                        getPageBase().error(ConnectorDevelopmentWizardUtil.scriptValidationErrorMessage(
                                validation, artifact.getFilename(), getPageBase()));
                        scriptEditedSinceError = false;
                        ConnectorDevelopmentWizardUtil.reportScriptValidationErrors(
                                ScriptConnectorStepPanel.this, getStepId(), validation, artifact.getFilename());
                    }
                }

                return artifact;
            }
        };
    }

    private String getTaskToken() {
        try {
            return ConnectorDevelopmentWizardUtil.getTaskToken(
                    WorkDefinitionsType.F_GENERATE_CONNECTOR_ARTIFACT,
                    getObjectClassName(),
                    getScriptType(),
                    getDetailsModel().getObjectWrapper().getOid(),
                    getDetailsModel().getPageAssignmentHolder());
        } catch (CommonException e) {
            throw new RuntimeException(e);
        }
    }

    abstract protected ConnectorDevelopmentArtifacts.KnownArtifactType getScriptType();

    protected String getObjectClassName() {
        return null;
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        add(createLanguageSelect());

        suggestedScriptAlert = new WebMarkupContainer(ID_SUGGESTED_SCRIPT_ALERT);
        suggestedScriptAlert.setOutputMarkupId(true);
        suggestedScriptAlert.add(new VisibleBehaviour(this::isShowingSuggestedScript));
        add(suggestedScriptAlert);

        SimpleAceEditorPanel editorPanel = new SimpleAceEditorPanel(
                ID_PANEL, new PropertyModel<>(valueModel, ConnDevArtifactType.F_CONTENT.getLocalPart()), 400) {

            protected AceEditor createEditor(String id, IModel<String> model, int minSize) {
                AceEditor editor = new AceEditor(id, model);
                editor.setReadonly(false);
                editor.setMinHeight(minSize);
                editor.setHeight(400);
                editor.setResizeToMaxHeight(false);
                lastPushedMode = toAceMode(languageModel.getObject());
                editor.setMode(lastPushedMode);
                add(editor);
                return editor;
            }
        };

        scriptEditor = (AceEditor) editorPanel.getBaseFormComponent();
        scriptEditor.setConvertEmptyInputStringToNull(false);
        editorPanel.add(AttributeAppender.append("class", "d-flex flex-column w-100 border rounded"));

        editorPanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getHelper().putVariable(VAR_LIVE_SCRIPT, scriptEditor.getModelObject());
                target.add(getFeedback());
            }
        });
        editorPanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(
                        new ThrottlingSettings(getComponent().getMarkupId() + "-scriptChange", Duration.ofMillis(500), true));
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getHelper().putVariable(VAR_LIVE_SCRIPT, scriptEditor.getModelObject());
                scriptEditedSinceError = true;
            }
        });
        add(editorPanel);
    }

    @Override
    protected Component createCustomButtonsAnchor(String id) {
        if (!isScriptOptional()) {
            return super.createCustomButtonsAnchor(id);
        }
        DropdownButtonDto model = new DropdownButtonDto(null, null, null, buildScriptActionsMenuItems());
        scriptActionsMenu = new DropdownButtonPanel(id, model) {
            @Override
            protected String getSpecialButtonClass() {
                return "btn btn-light border";
            }
        };
        return scriptActionsMenu;
    }

    private DropDownChoicePanel<ConnDevScriptFormat> createLanguageSelect() {
        IModel<List<ConnDevScriptFormat>> choices = Model.ofList(List.of(ConnDevScriptFormat.values()));

        DropDownChoicePanel<ConnDevScriptFormat> languageSelect = new DropDownChoicePanel<>(
                ID_LANGUAGE_SELECT, languageModel, choices,
                new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(ConnDevScriptFormat format) {
                        return format == ConnDevScriptFormat.YAML ? "YAML" : WordUtils.capitalizeFully(format.name());
                    }
                }, false);
        languageSelect.setOutputMarkupId(true);
        languageSelect.getBaseFormComponent().add(AttributeAppender.append("class", "form-select form-select-sm"));
        languageSelect.getBaseFormComponent().add(AttributeAppender.append("style", "width: 10rem;"));

        languageSelect.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ConnDevScriptFormat selectedFormat = languageSelect.getBaseFormComponent().getConvertedInput();
                languageModel.setObject(selectedFormat);
                if (scriptEditor != null) {
                    AceEditor.Mode selectedMode = toAceMode(selectedFormat);
                    scriptEditor.updateMode(target, selectedMode);
                    lastPushedMode = selectedMode;
                }
            }
        });
        return languageSelect;
    }

    /** Bridges {@link ConnDevScriptFormat} to {@link AceEditor.Mode}; exhaustive so a new format fails to compile until mapped. */
    private static AceEditor.Mode toAceMode(ConnDevScriptFormat format) {
        return switch (format) {
            case GROOVY -> AceEditor.Mode.GROOVY;
            case YAML -> AceEditor.Mode.YAML;
        };
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
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
        Task task = getPageBase().createSimpleTask(OP_SAVE_SCRIPT);
        ConnectorDevelopmentWizardUtil.clearScriptValidationErrors(this, getStepId());
        ConnectorDevelopmentWizardUtil.refreshDrawerPanel(this, target);
        String scriptFileToDelete = null;
        try {
            ConnDevArtifactType script = valueModel.getObject().clone();
            String liveScript = currentLiveScriptContent();
            if (liveScript != null) {
                script.setContent(liveScript);
            }
            script.setFilename(languageModel.getObject().withExtension(script.getFilename()));
            WebPrismUtil.cleanupEmptyContainerValue(script.asPrismContainerValue());
            if (isScriptOptional() && StringUtils.isBlank(script.getContent())) {
                scriptFileToDelete = removeDeployedScriptEntry();
                valueModel.detach();
                languageModel.detach();
                getHelper().removeVariable(VAR_LIVE_SCRIPT);
            } else {
                ConnDevArtifactValidationResult validation = getDetailsModel().getConnectorDevelopmentOperation()
                        .validateArtifact(script, task, task.getResult());
                if (!validation.ok()) {
                    getPageBase().error(ConnectorDevelopmentWizardUtil.scriptValidationErrorMessage(
                            validation, script.getFilename(), getPageBase()));
                    target.add(getFeedback());
                    ConnectorDevelopmentWizardUtil.reportScriptValidationErrors(
                            this, getStepId(), validation, script.getFilename(), target);
                    scriptEditedSinceError = false;
                    target.add(isScriptOptional() ? scriptActionsMenu : regenerateButton);
                    target.add(repairObjectClassButton);
                    return false;
                }
                saveScript(script, task, task.getResult());
                getDetailsModel().reloadPrismObjectByOid();
                if (task.getResult() == null || task.getResult().isError()) {
                    target.add(getFeedback());
                    return false;
                }
                valueModel.detach();
                languageModel.detach();
                getHelper().removeVariable(VAR_LIVE_SCRIPT);
            }
        } catch (IOException | CommonException e) {
            throw new RuntimeException(e);
        }

        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();

        if (result != null && !result.isError() && scriptFileToDelete != null) {
            try {
                getDetailsModel().getConnectorDevelopmentOperation().deleteArtifactFile(scriptFileToDelete, task, task.getResult());
                getDetailsModel().getConnectorDevelopmentOperation().recomputeConnectorManifest(task, task.getResult());
            } catch (IOException | CommonException e) {
                getPageBase().error("Couldn't delete " + scriptFileToDelete + ": " + e.getMessage());
                target.add(getFeedback());
            }
        }

        onAfterSave(target);
        if (result != null && !result.isError()) {
            isReloaded = false;
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }

    protected void onAfterSave(AjaxRequestTarget target) {
    }

    /** True only where the connector can work without this script (see {@link AuthScriptsConnectorStepPanel}). */
    protected boolean isScriptOptional() {
        return false;
    }

    protected final LoadableModel<ConnDevArtifactType> getValueModel() {
        return valueModel;
    }

    /** Forces {@link #valueModel} to reload from disk, e.g. after {@link RepairObjectClassButton} saved a fixed script directly. */
    public void detachLoadedScript() {
        valueModel.detach();
        autoValidationDone = false;
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        if (!isScriptOptional()) {
            regenerateButton = new AjaxIconButton(
                    customButtons.newChildId(),
                    Model.of("fa fa-refresh "),
                    getPageBase().createStringResource("ScriptConnectorStepPanel.regenerate")) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    onRefreshPerformed(target);
                }
            };
            regenerateButton.showTitleAsLabel(true);
            regenerateButton.add(AttributeAppender.append("class", "ms-auto"));
            regenerateButton.add(new VisibleBehaviour(this::hasPendingValidationError));
            regenerateButton.setOutputMarkupPlaceholderTag(true);
            customButtons.add(regenerateButton);
        }

        repairObjectClassButton = new RepairObjectClassButton(customButtons.newChildId(), this, this::getObjectClassName,
                this::currentScriptsForRepair);
        customButtons.add(repairObjectClassButton);
    }

    private List<InlineMenuItem> buildScriptActionsMenuItems() {
        InlineMenuItem discardItem = new InlineMenuItem(createStringResource("ScriptConnectorStepPanel.discardSuggestedScript")) {
            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onDiscardSuggestedScriptPerformed(target);
                    }
                };
            }
        };
        discardItem.setVisible(this::isShowingSuggestedScript);

        InlineMenuItem regenerateItem = new InlineMenuItem(createStringResource("ScriptConnectorStepPanel.regenerate")) {
            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefreshPerformed(target);
                    }
                };
            }
        };

        InlineMenuItem deleteItem = new InlineMenuItem(createStringResource("ScriptConnectorStepPanel.discardScript")) {
            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onDeleteScriptPerformed(target);
                    }
                };
            }
        };

        return List.of(discardItem, regenerateItem, deleteItem);
    }

    /** Whether {@link #valueModel} is showing a generated suggestion rather than the deployed script. */
    private boolean isShowingSuggestedScript() {
        if (hasActiveDeployedScript()) {
            return false;
        }
        ConnDevArtifactType current = valueModel.getObject();
        boolean result = current != null && StringUtils.isNotBlank(current.getContent());
        return result;
    }

    private boolean hasActiveDeployedScript() {
        PrismContainerValueWrapper<ConnDevArtifactType> deployed = deployedScriptWrapper();
        boolean result = deployed != null && !Boolean.TRUE.equals(deployed.getRealValue().isDisabled());
        return result;
    }

    /** The deployed script's wrapper, or null - filters out the empty placeholder value an as-yet-nonexistent container gets. */
    private PrismContainerValueWrapper<ConnDevArtifactType> deployedScriptWrapper() {
        if (!ConnectorDevelopmentWizardUtil.existScript(getDetailsModel(), getScriptType(), getObjectClassName())) {
            return null;
        }
        return ConnectorDevelopmentWizardUtil.getScript(getDetailsModel(), getScriptType(), getObjectClassName());
    }

    /** Removes the deployed script's model entry, if any; the file itself is deleted separately by the caller. */
    private String removeDeployedScriptEntry() {
        PrismContainerValueWrapper<ConnDevArtifactType> existing = deployedScriptWrapper();
        if (existing == null) {
            return null;
        }
        String filename = existing.getRealValue() != null ? existing.getRealValue().getFilename() : null;
        try {
            existing.getParent().remove(existing, getDetailsModel().getPageAssignmentHolder());
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return filename;
    }

    /** Fully removes an already-deployed optional script: model entry, file and manifest record. */
    private void onDeleteScriptPerformed(AjaxRequestTarget target) {
        String filename = removeDeployedScriptEntry();
        if (filename != null) {
            OperationResult result = getHelper().onSaveObjectPerformed(target);
            if (result == null || result.isError()) {
                target.add(getFeedback());
                return;
            }
            Task task = getPageBase().createSimpleTask(CLASS_DOT + "deleteScript");
            try {
                getDetailsModel().getConnectorDevelopmentOperation().deleteArtifactFile(filename, task, task.getResult());
                getDetailsModel().getConnectorDevelopmentOperation().recomputeConnectorManifest(task, task.getResult());
            } catch (IOException | CommonException e) {
                getPageBase().error("Couldn't delete " + filename + ": " + e.getMessage());
                target.add(getFeedback());
                return;
            }
            getDetailsModel().reloadPrismObjectByOid();
        }
        autoValidationDone = false;
        valueModel.detach();
        languageModel.detach();
        getHelper().removeVariable(VAR_LIVE_SCRIPT);
        isReloaded = false;
        super.onNextPerformed(target);
    }

    /** Marks a deployed script disabled (no-op if nothing deployed, or already disabled) - see {@link #onRefreshPerformed}. */
    private void disableDeployedScriptIfPresent() {
        PrismContainerValueWrapper<ConnDevArtifactType> deployed = deployedScriptWrapper();
        if (deployed == null || deployed.getRealValue() == null || deployed.getRealValue().getFilename() == null
                || Boolean.TRUE.equals(deployed.getRealValue().isDisabled())) {
            return;
        }
        Task task = getPageBase().createSimpleTask(CLASS_DOT + "disableDeployedScript");
        try {
            getDetailsModel().getConnectorDevelopmentOperation().disableArtifact(deployed.getRealValue().getFilename(), task, task.getResult());
            getDetailsModel().reloadPrismObjectByOid();
        } catch (IOException | CommonException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reverts to the deployed script if there is one, or clears the editor to blank otherwise.
     * The blank case sets {@link #valueModel} directly rather than detaching it: load() would
     * just re-fetch the same completed generation task's result again.
     */
    private void onDiscardSuggestedScriptPerformed(AjaxRequestTarget target) {
        PrismContainerValueWrapper<ConnDevArtifactType> deployed = deployedScriptWrapper();
        if (deployed == null || deployed.getRealValue() == null) {
            autoValidationDone = false;
            valueModel.setObject(getScriptType().create(getObjectClassName()));
            languageModel.detach();
            getHelper().removeVariable(VAR_LIVE_SCRIPT);
            target.add(this);
            if (scriptEditor != null) {
                scriptEditor.updateValue(target, "");
            }
            return;
        }

        Task task = getPageBase().createSimpleTask(CLASS_DOT + "discardSuggestedScript");
        String content;
        try {
            ConnDevArtifactType deployedArtifact = deployed.getRealValue().clone();
            content = getDetailsModel().getConnectorDevelopmentOperation()
                    .getArtifactContent(deployedArtifact, task, task.getResult());
            deployedArtifact.setContent(content);
            WebPrismUtil.cleanupEmptyContainerValue(deployedArtifact.asPrismContainerValue());
            saveScript(deployedArtifact, task, task.getResult());
            getDetailsModel().reloadPrismObjectByOid();
        } catch (IOException | CommonException e) {
            throw new RuntimeException(e);
        }
        if (task.getResult() != null && task.getResult().isError()) {
            target.add(getFeedback());
            return;
        }
        autoValidationDone = false;
        valueModel.detach();
        languageModel.detach();
        target.add(this);
        if (scriptEditor != null) {
            scriptEditor.updateValue(target, content);
        }
    }

    private boolean hasPendingValidationError() {
        if (scriptEditedSinceError) {
            return false;
        }
        if (!(getWizard() instanceof WizardModelWithParentSteps parentWizardModel)) {
            return false;
        }
        return !parentWizardModel.getOperationResultsForFixStep(getStepId()).isEmpty();
    }

    /** The script content currently in the editor, not necessarily what was last saved - read from {@link #VAR_LIVE_SCRIPT}. */
    private String currentLiveScriptContent() {
        String liveScript = getHelper().getVariable(VAR_LIVE_SCRIPT);
        return liveScript != null
                ? liveScript
                : (valueModel.getObject() != null ? valueModel.getObject().getContent() : null);
    }

    private List<ConnDevArtifactType> currentScriptsForRepair() {
        ConnDevArtifactType artifact = valueModel.getObject();
        if (artifact == null) {
            return List.of();
        }
        String currentScript = currentLiveScriptContent();
        if (currentScript == null || currentScript.equals(artifact.getContent())) {
            return List.of(artifact);
        }
        return List.of(artifact.clone().content(currentScript));
    }

    private void onRefreshPerformed(AjaxRequestTarget target) {
        if (isScriptOptional()) {
            disableDeployedScriptIfPresent();
        }
        if (getWizard() instanceof WizardModelWithParentSteps parentWizardModel) {
            var pendingResults = parentWizardModel.getOperationResultsForFixStep(getStepId());
            List<String> errorMessages = ConnectorDevelopmentWizardUtil.collectErrorMessages(pendingResults);
            String currentScript = currentLiveScriptContent();
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
                    waitingPanel.resetScript(getPageBase(), currentScript, errorMessages);
                    if (i == 0) {
                        setActiveStepById(target, parentWizardModel, idOfFound);
                    }
                } else if (StringUtils.isNotEmpty(idOfFound)) {
                    setActiveStepById(target, parentWizardModel, idOfFound);
                    isReloaded = true;
                    autoValidationDone = false;
                    valueModel.detach();
                    languageModel.detach();
                    return;
                }
            }
            autoValidationDone = false;
            valueModel.detach();
            languageModel.detach();
        }
    }

    private void setActiveStepById(AjaxRequestTarget target, WizardModelWithParentSteps parentWizardModel, String idOfFound) {
        parentWizardModel.setActiveStepById(idOfFound);
        parentWizardModel.fireActiveStepChanged();
        target.add(getWizard().getPanel());
    }

    protected abstract void saveScript(ConnDevArtifactType object, Task task, OperationResult result) throws IOException, CommonException;

    @Override
    public boolean isCompleted() {
        return ConnectorDevelopmentWizardUtil.existScript(getDetailsModel(), getScriptType(), getObjectClassName());
    }
    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
