/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
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
import org.apache.wicket.model.PropertyModel;
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
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
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

    /**
     * Token of the generation task whose suggested script the user rejected. Kept in memory only (a step panel
     * is reused across navigations, so it survives Next/Back but not a page reload). A newer generation has a
     * different token, so it is never treated as rejected.
     */
    private String rejectedSuggestionToken;

    /** Whether {@link #valueModel} currently holds the generated suggestion (as opposed to the deployed script or a blank one). */
    private boolean editorShowsSuggestion;

    /** The content the application last loaded into the editor - what the user's edits are compared against. */
    private String loadedContent;

    /** Snapshot of the latest completed generation's script, for the "Reload suggested script" action. */
    private boolean suggestionAvailable;
    private String suggestionContent;

    /** Set once the user confirmed deleting the deployed script with an empty editor, to let the Next that follows go through. */
    private boolean deleteDeployedConfirmed;

    private interface ConfirmedAction extends Serializable {
        void run(AjaxRequestTarget target);
    }

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
     * Dropdown next to the plain submit button: "Reject suggested script", "Reload suggested script",
     * "Regenerate" and, where it applies, "Repair object class".
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
                PrismContainerValueWrapper<ConnDevArtifactType> deployedWrapper = deployedScriptWrapper();
                boolean deployedActive = deployedWrapper != null
                        && !Boolean.TRUE.equals(deployedWrapper.getRealValue().isDisabled());
                String token = getTaskToken();
                boolean rejected = StringUtils.isNotEmpty(token) && token.equals(rejectedSuggestionToken);
                suggestionAvailable = false;
                suggestionContent = null;

                if (rejected || (!isReloaded && deployedActive)) {
                    rememberSuggestion(readSuggestion(token));
                    return loadedFrom(deployedOrBlank(deployedWrapper), false);
                }

                if (StringUtils.isEmpty(token)) {
                    return loadedFrom(deployedOrBlank(deployedWrapper), false);
                }

                ConnDevArtifactType artifact = readSuggestion(token);
                rememberSuggestion(artifact);
                if (artifact == null) {
                    return loadedFrom(deployedOrBlank(deployedWrapper), false);
                }

                if (!autoValidationDone) {
                    autoValidationDone = true;
                    Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_LOAD_SCRIPT);
                    OperationResult result = task.getResult();
                    boolean skipValidation = StringUtils.isBlank(artifact.getContent());
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

                return loadedFrom(artifact, true);
            }
        };
    }

    private void rememberSuggestion(ConnDevArtifactType artifact) {
        suggestionAvailable = artifact != null;
        suggestionContent = artifact == null ? null : artifact.getContent();
    }

    private ConnDevArtifactType loadedFrom(ConnDevArtifactType artifact, boolean suggestion) {
        editorShowsSuggestion = suggestion;
        loadedContent = artifact.getContent();
        return artifact;
    }

    /** The deployed script with its content read from the connector's file, or a blank script when none is deployed. */
    private ConnDevArtifactType deployedOrBlank(PrismContainerValueWrapper<ConnDevArtifactType> deployedWrapper) {
        if (deployedWrapper == null) {
            return getScriptType().create(getObjectClassName());
        }
        ConnDevArtifactType artifactType = deployedWrapper.getRealValue();
        Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_LOAD_SCRIPT);
        OperationResult result = task.getResult();
        try {
            String content = getDetailsModel().getConnectorDevelopmentOperation().getArtifactContent(artifactType, task, result);
            artifactType.setContent(content);
            return artifactType;
        } catch (IOException e) {
            // The script entry exists in the model, but its file cannot be read (e.g. it
            // was removed out-of-band). Don't fail the page render - show a blank script.
            getPageBase().error("Couldn't load " + artifactType.getFilename() + ": " + e.getMessage());
            artifactType.setContent(null);
            return artifactType;
        }
    }

    /** The script of the latest generation task, or null when there is no task or it produced none. */
    private ConnDevArtifactType readSuggestion(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_LOAD_SCRIPT);
        OperationResult result = task.getResult();
        try {
            StatusInfo<ConnDevGenerateArtifactResultType> statusInfo =
                    getDetailsModel().getServiceLocator().getConnectorService().getGenerateArtifactStatus(token, task, result);
            ConnDevGenerateArtifactResultType artifactResultType = statusInfo.getResult();
            return artifactResultType == null ? null : artifactResultType.getArtifact();
        } catch (CommonException e) {
            throw new RuntimeException(e);
        }
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
        boolean emptyScript = StringUtils.isBlank(currentLiveScriptContent());
        if (emptyScript && deployedScriptWrapper() != null && !deleteDeployedConfirmed) {
            showEditsConfirmation("ScriptConnectorStepPanel.deleteDeployed", confirmTarget -> {
                deleteDeployedConfirmed = true;
                onNextPerformed(confirmTarget);
            }, target);
            return false;
        }
        deleteDeployedConfirmed = false;

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
            if (StringUtils.isBlank(script.getContent())) {
                rejectedSuggestionToken = getTaskToken();
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
                    target.add(scriptActionsMenu);
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

    /** Whether the "Repair object class" action makes sense for this script (it fixes the object class the script belongs to). */
    protected boolean isRepairAvailable() {
        return true;
    }

    protected final LoadableModel<ConnDevArtifactType> getValueModel() {
        return valueModel;
    }

    /** Forces {@link #valueModel} to reload from disk, e.g. after {@link RepairObjectClassButton} saved a fixed script directly. */
    public void detachLoadedScript() {
        valueModel.detach();
        autoValidationDone = false;
        getHelper().removeVariable(VAR_LIVE_SCRIPT);
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
        List<InlineMenuItem> items = new ArrayList<>();

        InlineMenuItem rejectItem = new InlineMenuItem(createStringResource("ScriptConnectorStepPanel.rejectSuggestedScript")) {
            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRejectSuggestedScriptPerformed(target);
                    }
                };
            }
        };
        rejectItem.setVisible(this::isShowingSuggestedScript);
        items.add(rejectItem);

        InlineMenuItem reloadItem = new InlineMenuItem(createStringResource("ScriptConnectorStepPanel.reloadSuggestedScript")) {
            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onReloadSuggestedScriptPerformed(target);
                    }
                };
            }
        };
        reloadItem.setVisible(this::isReloadSuggestedScriptAvailable);
        items.add(reloadItem);

        InlineMenuItem regenerateItem = new InlineMenuItem(createStringResource("ScriptConnectorStepPanel.regenerate")) {
            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRegenerateClicked(target);
                    }
                };
            }
        };
        items.add(regenerateItem);

        if (isRepairAvailable()) {
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
            items.add(repairItem);
        }

        return items;
    }

    /** Whether the editor holds the generated suggestion (not the deployed script and not a blank script). */
    private boolean isShowingSuggestedScript() {
        valueModel.getObject();
        return editorShowsSuggestion;
    }

    /**
     * Whether the latest generation produced a suggestion. Deliberately independent of the editor content: the
     * menu is rendered on the server and is not refreshed while the user types, so a condition on the editor
     * would leave the item stale (e.g. hidden after the user clears the editor).
     */
    private boolean isReloadSuggestedScriptAvailable() {
        valueModel.getObject();
        return suggestionAvailable && StringUtils.isNotBlank(suggestionContent);
    }

    /** Whether the user changed the editor content compared to what the application loaded into it. */
    private boolean hasEditorEdits() {
        return !StringUtils.equals(StringUtils.defaultString(currentLiveScriptContent()), StringUtils.defaultString(loadedContent));
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

    private void onRegenerateClicked(AjaxRequestTarget target) {
        boolean edits = hasEditorEdits();
        if (edits) {
            showEditsConfirmation("ScriptConnectorStepPanel.regenerate", this::onRefreshPerformed, target);
            return;
        }
        onRefreshPerformed(target);
    }

    /** Takes back the suggested script: the editor shows the deployed script, or stays blank when none is deployed. */
    private void onRejectSuggestedScriptPerformed(AjaxRequestTarget target) {
        rejectedSuggestionToken = getTaskToken();
        resetEditorToLoadedState(target);
    }

    private void onReloadSuggestedScriptPerformed(AjaxRequestTarget target) {
        boolean edits = hasEditorEdits();
        if (edits) {
            showEditsConfirmation("ScriptConnectorStepPanel.reload", this::reloadSuggestedScript, target);
            return;
        }
        reloadSuggestedScript(target);
    }

    private void reloadSuggestedScript(AjaxRequestTarget target) {
        rejectedSuggestionToken = null;
        isReloaded = true;
        resetEditorToLoadedState(target);
    }

    /** Re-reads the script into the editor, dropping the user's edits and the validation errors reported for the previous content. */
    private void resetEditorToLoadedState(AjaxRequestTarget target) {
        autoValidationDone = false;
        scriptEditedSinceError = false;
        getHelper().removeVariable(VAR_LIVE_SCRIPT);
        ConnectorDevelopmentWizardUtil.clearScriptValidationErrors(this, getStepId());
        ConnectorDevelopmentWizardUtil.refreshDrawerPanel(this, target);
        valueModel.detach();
        languageModel.detach();
        String content = valueModel.getObject().getContent();
        target.add(this);
        if (scriptEditor != null) {
            scriptEditor.updateValue(target, StringUtils.defaultString(content));
        }
    }

    /** Asks before an action throws away the user's edits; {@code keyPrefix}.confirm.{title,message,yes} are the texts. */
    private void showEditsConfirmation(String keyPrefix, ConfirmedAction onConfirmed, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
                createStringResource(keyPrefix + ".confirm.message")) {

            @Override
            public IModel<String> getTitle() {
                return createStringResource(keyPrefix + ".confirm.title");
            }

            @Override
            protected IModel<String> createYesLabel() {
                return createStringResource(keyPrefix + ".confirm.yes");
            }

            @Override
            protected IModel<String> createNoLabel() {
                return createStringResource("ScriptConnectorStepPanel.confirm.cancel");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget confirmTarget) {
                onConfirmed.run(confirmTarget);
            }

            @Override
            public void noPerformed(AjaxRequestTarget cancelTarget) {
                super.noPerformed(cancelTarget);
            }
        };
        getPageBase().showMainPopup(dialog, target);
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
