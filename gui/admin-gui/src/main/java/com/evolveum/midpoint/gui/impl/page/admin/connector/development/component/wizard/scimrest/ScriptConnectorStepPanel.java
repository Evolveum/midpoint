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
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
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
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevGenerateArtifactResultType;

/**
 * @author lskublik
 */
public abstract class ScriptConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String ID_PANEL = "panel";
    private static final String ID_LANGUAGE_SELECT = "languageSelect";
    private static final String ID_SUGGESTED_SCRIPT_ALERT = "suggestedScriptAlert";

    /** {@code getHelper().putVariable} key prefix for the editor's live content - survives valueModel's own detach. */
    private static final String VAR_LIVE_SCRIPT_PREFIX = "liveScript:";

    private static final String CLASS_DOT = ScriptConnectorStepPanel.class.getName() + ".";
    private static final String OP_LOAD_SCRIPT = CLASS_DOT + "loadScript";
    private static final String OP_SAVE_SCRIPT = CLASS_DOT + "saveScript";

    private LoadableModel<ConnDevArtifactType> valueModel;
    private LoadableModel<String> savedContentModel;

    /** Token of the finished generation task whose result is waiting for the user to save or discard it. */
    private String generatedScriptToken;
    private String generatedScriptObjectClass;

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

    /** Logic holder for the "Repair object class" dropdown item - never added to the component tree. */
    private RepairObjectClassButton repairObjectClassButton;

    /**
     * Dropdown next to the plain submit button: "Regenerate" + "Repair object class" for a mandatory
     * script, or "Discard suggested script" / "Regenerate" / "Discard script" for an optional one.
     * Built in {@link #createCustomButtonsAnchor} rather than {@link #initCustomButtons}'s repeater,
     * since that wraps every child in an {@code <a>} tag - illegal for a {@code <button>}+
     * {@code <div class="dropdown-menu">} panel.
     */
    private DropdownButtonPanel scriptActionsMenu;

    private WebMarkupContainer suggestedScriptAlert;

    /** Set once the user edits after a validation error was recorded, to hide "Regenerate" via {@link #hasPendingValidationError()}. */
    private boolean scriptEditedSinceError = false;

    /** Mode last pushed to the live editor - a step panel is reused across navigations, so a plain re-render alone never pushes a {@link #languageModel} change. */
    private AceEditor.Mode lastPushedMode;

    /** The editor's raw submitted value for the current request - see {@link #rawEditedScriptContent()}. */
    private String pendingEditorValue;

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
            AceEditor.Mode currentMode = AceEditor.Mode.forScriptFormat(languageModel.getObject());
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
                ConnDevArtifactType generated = loadGeneratedScript();
                if (generated != null) {
                    return generated;
                }

                PrismContainerValueWrapper<ConnDevArtifactType> deployedWrapper = deployedScriptWrapper();
                if (deployedWrapper != null && !Boolean.TRUE.equals(deployedWrapper.getRealValue().isDisabled())) {
                    ConnDevArtifactType artifactType = deployedWrapper.getRealValue();
                    Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_LOAD_SCRIPT);
                    try {
                        artifactType.setContent(getDetailsModel().getConnectorDevelopmentOperation()
                                .getArtifactContent(artifactType, task, task.getResult()));
                    } catch (IOException e) {
                        // The script entry exists in the model, but its file cannot be read (e.g. it
                        // was removed out-of-band). Don't fail the page render - show a blank script.
                        getPageBase().error("Couldn't load " + artifactType.getFilename() + ": " + e.getMessage());
                        artifactType.setContent(null);
                    }
                    return artifactType;
                }

                return getScriptType().create(getObjectClassName());
            }
        };

        savedContentModel = new LoadableModel<>() {
            @Override
            protected String load() {
                return savedScriptContent();
            }
        };
    }

    private ConnDevArtifactType loadGeneratedScript() {
        String token = generatedScriptToken();
        if (token == null) {
            return null;
        }

        Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_LOAD_SCRIPT);
        OperationResult result = task.getResult();

        StatusInfo<ConnDevGenerateArtifactResultType> statusInfo;
        try {
            statusInfo = getDetailsModel().getServiceLocator().getConnectorService().getGenerateArtifactStatus(token, task, result);
        } catch (CommonException e) {
            throw new RuntimeException(e);
        }
        ConnDevGenerateArtifactResultType generatedResult = statusInfo.getResult();
        if (generatedResult == null || generatedResult.getArtifact() == null) {
            return null;
        }

        ConnDevArtifactType artifact = generatedResult.getArtifact();
        String saved = savedScriptContent();
        if (saved != null && StringUtils.equals(saved, artifact.getContent())) {
            generatedScriptToken = null;
            generatedScriptObjectClass = null;
            return null;
        }

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

    private String generatedScriptToken() {
        return StringUtils.equals(generatedScriptObjectClass, getObjectClassName()) ? generatedScriptToken : null;
    }

    private String savedScriptContent() {
        PrismContainerValueWrapper<ConnDevArtifactType> deployed = deployedScriptWrapper();
        if (deployed == null || Boolean.TRUE.equals(deployed.getRealValue().isDisabled())) {
            return null;
        }
        Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_LOAD_SCRIPT);
        try {
            return getDetailsModel().getConnectorDevelopmentOperation()
                    .getArtifactContent(deployed.getRealValue().clone(), task, task.getResult());
        } catch (IOException e) {
            return null;
        }
    }

    private String liveScriptKey() {
        return VAR_LIVE_SCRIPT_PREFIX + getStepId() + ":" + getObjectClassName();
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

        IModel<String> editorContentModel = new IModel<>() {
            @Override
            public String getObject() {
                return currentLiveScriptContent();
            }

            @Override
            public void setObject(String value) {
                pendingEditorValue = value;
            }

            @Override
            public void detach() {
                valueModel.detach();
                savedContentModel.detach();
            }
        };

        SimpleAceEditorPanel editorPanel = new SimpleAceEditorPanel(ID_PANEL, editorContentModel, 400) {

            protected AceEditor createEditor(String id, IModel<String> model, int minSize) {
                AceEditor editor = new AceEditor(id, model);
                editor.setReadonly(false);
                editor.setMinHeight(minSize);
                editor.setHeight(400);
                editor.setResizeToMaxHeight(false);
                lastPushedMode = AceEditor.Mode.forScriptFormat(languageModel.getObject());
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
                getHelper().putVariable(liveScriptKey(), rawEditedScriptContent());
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
                getHelper().putVariable(liveScriptKey(), rawEditedScriptContent());
                scriptEditedSinceError = true;
            }
        });
        add(editorPanel);
    }

    @Override
    protected Component createCustomButtonsAnchor(String id) {
        DropdownButtonDto model = new DropdownButtonDto(null, null, null, buildScriptActionsMenuItems());
        scriptActionsMenu = new DropdownButtonPanel(id, model) {
            @Override
            protected String getSpecialButtonClass() {
                return "btn btn-light border";
            }
        };
        return scriptActionsMenu;
    }

    private ScriptFormatSelectPanel createLanguageSelect() {
        ScriptFormatSelectPanel languageSelect = new ScriptFormatSelectPanel(ID_LANGUAGE_SELECT, languageModel) {
            @Override
            protected void onFormatChanged(AjaxRequestTarget target, ConnDevScriptFormat format) {
                if (scriptEditor != null) {
                    AceEditor.Mode selectedMode = AceEditor.Mode.forScriptFormat(format);
                    scriptEditor.updateMode(target, selectedMode);
                    lastPushedMode = selectedMode;
                }
            }
        };
        languageSelect.setOutputMarkupId(true);
        return languageSelect;
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
        if (!saveUnsavedStepChanges(target)) {
            return false;
        }
        onAfterSave(target);
        super.onNextPerformed(target);
        return false;
    }

    /**
     * Commits the live script (and the rest of this object's pending changes - this does the
     * generic object save itself too, the caller doesn't need to repeat it) exactly like clicking
     * Next would, without actually advancing to the next step. Used by {@link #onNextPerformed}
     * itself, and by the "Save" choice on an unsaved-changes confirmation when leaving via Back or
     * the navigation menu (plain {@code getHelper().onSaveObjectPerformed} alone wouldn't do, since
     * the live script lives in a wizard variable, not any Prism wrapper's delta).
     *
     * @return false if validation or saving failed - an error is already shown, and the caller
     * should NOT navigate away.
     */
    @Override
    public boolean saveUnsavedStepChanges(AjaxRequestTarget target) {
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
                dropGeneratedScript();
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
                    target.add(scriptActionsMenu);
                    return false;
                }
                saveScript(script, task, task.getResult());
                getDetailsModel().reloadPrismObjectByOid();
                if (task.getResult() == null || task.getResult().isError()) {
                    target.add(getFeedback());
                    return false;
                }
                dropGeneratedScript();
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

        if (result == null || result.isError()) {
            target.add(getFeedback());
            return false;
        }
        return true;
    }

    /**
     * Forgets a generated script nobody has saved yet, together with the live editor content, so the
     * step shows the saved script (or a blank editor) next time. Called when the user discards the
     * generated script, saves, or when a new generation replaces it.
     */
    public void dropGeneratedScript() {
        generatedScriptToken = null;
        generatedScriptObjectClass = null;
        autoValidationDone = false;
        valueModel.detach();
        savedContentModel.detach();
        languageModel.detach();
        getHelper().removeVariable(liveScriptKey());
    }

    /** Remembers the finished generation task whose result this step shows until it is saved or discarded. */
    public void setGeneratedScriptToken(String token) {
        dropGeneratedScript();
        generatedScriptToken = token;
        generatedScriptObjectClass = getObjectClassName();
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
        dropGeneratedScript();
    }

    /** Triggers the same regeneration as the "Regenerate" dropdown item, for a caller outside this step (e.g. the drawer's "Fix it"). */
    public void triggerRegenerate(AjaxRequestTarget target) {
        onRefreshPerformed(target);
    }

    /** Triggers the same object class repair as {@link #repairObjectClassButton}, for a caller outside this step (e.g. the drawer's "Fix it"). */
    public void triggerRepair(AjaxRequestTarget target) {
        repairObjectClassButton.onClick(target);
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        repairObjectClassButton = new RepairObjectClassButton(customButtons.newChildId(), this, this::getObjectClassName,
                this::currentScriptsForRepair);
    }

    private List<InlineMenuItem> buildScriptActionsMenuItems() {
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

        if (!isScriptOptional()) {
            InlineMenuItem repairItem = new InlineMenuItem(createStringResource("NextStepsConnectorStepPanel.repairObjectClass")) {
                @Override
                public InlineMenuItemAction initAction() {
                    return new InlineMenuItemAction() {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            if (!repairObjectClassButton.isApplicable()) {
                                getPageBase().info(getString("ScriptConnectorStepPanel.repairObjectClass.noErrors"));
                                target.add(getFeedback());
                                return;
                            }
                            repairObjectClassButton.onClick(target);
                        }
                    };
                }
            };
            return List.of(regenerateItem, repairItem);
        }

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

    /** Whether the editor content differs from the saved script (or there is no saved script and the editor is not blank). */
    private boolean isShowingSuggestedScript() {
        return differsFromSavedScript();
    }

    private boolean differsFromSavedScript() {
        String current = StringUtils.defaultString(currentLiveScriptContent());
        String saved = savedContentModel.getObject();
        return saved == null ? StringUtils.isNotBlank(current) : !saved.equals(current);
    }

    /** The deployed script's wrapper, or null - filters out the empty placeholder value an as-yet-nonexistent container gets. */
    protected PrismContainerValueWrapper<ConnDevArtifactType> deployedScriptWrapper() {
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
        dropGeneratedScript();
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

    /** Reverts to the deployed script if there is one, or clears the editor to blank otherwise. */
    private void onDiscardSuggestedScriptPerformed(AjaxRequestTarget target) {
        PrismContainerValueWrapper<ConnDevArtifactType> deployed = deployedScriptWrapper();
        if (deployed == null || deployed.getRealValue() == null) {
            dropGeneratedScript();
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
        dropGeneratedScript();
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

    /** The script content currently in the editor, not necessarily what was last saved. */
    private String currentLiveScriptContent() {
        String liveScript = getHelper().getVariable(liveScriptKey());
        return liveScript != null
                ? liveScript
                : (valueModel.getObject() != null ? valueModel.getObject().getContent() : null);
    }

    /**
     * The value the browser just submitted for the editor - set by the editor's own model
     * ({@code setObject}) moments earlier in this same request. Deliberately NOT written into
     * {@link #valueModel}: that artifact stays untouched by the editor.
     */
    private String rawEditedScriptContent() {
        return pendingEditorValue;
    }

    @Override
    public boolean hasUnsavedStepChanges() {
        return differsFromSavedScript();
    }

    @Override
    public void discardUnsavedStepChanges() {
        dropGeneratedScript();
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
                    dropGeneratedScript();
                    return;
                }
            }
            dropGeneratedScript();
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
