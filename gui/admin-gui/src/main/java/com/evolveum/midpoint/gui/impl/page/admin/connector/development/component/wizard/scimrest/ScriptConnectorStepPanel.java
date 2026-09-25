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
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;

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
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxIconButton;
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

    /**
     * {@code getHelper().putVariable} key for the editor's live content. {@code valueModel} gets
     * detached (and its cached edit discarded) at the end of every request, so a later, separate
     * request (e.g. clicking "Regenerate" some time after the last edit) can't see it there -
     * stashing it here instead carries it across, the same way the wizard already does for task
     * tokens.
     */
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

    /**
     * "Regenerate" is only meaningful while there's a still-relevant validation error to react to
     * (see {@link #initCustomButtons}) - kept as a field so the editor's change handler can hide it
     * again the moment the user edits the script, since editing makes the recorded error stale.
     */
    private AjaxIconButton regenerateButton;

    /** Kept so a freshly-recorded validation error can refresh its visibility (see {@link #onNextPerformed}). */
    private RepairObjectClassButton repairObjectClassButton;

    /**
     * Set the moment the user edits the script after a validation error was recorded - makes
     * {@link #hasPendingValidationError()} hide "Regenerate" without touching the drawer's error
     * entry itself (see the "change" handler in {@link #initLayout}): the error detail stays
     * visible for reference, only the button (tied to that exact, now-superseded content) goes.
     */
    private boolean scriptEditedSinceError = false;

    /**
     * Language last pushed to the live editor via {@link AceEditor#updateMode}. The editor's own
     * mode field only changes when explicitly told to (see {@link AceEditor#updateMode}) - a step
     * panel is reused across navigations (regenerate, "finish" after waiting, ...), so a plain
     * re-render never picks up a {@link #languageModel} change on its own.
     */
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
        // Force valueModel to resolve now, before any component's VisibleBehaviour gets a chance
        // to evaluate hasPendingValidationError(): load()'s task-result branch clears a stale
        // error as a side effect (see createModels()), and that decision must already be final by
        // the time regenerateButton/RepairObjectClassButton compute their visibility later in this
        // same render pass - relying on markup order to make that happen "naturally" would be
        // fragile, whereas onInitialize() runs, once, before any of this instance's descendants
        // are actually rendered.
        valueModel.getObject();
        initLayout();
    }

    /**
     * Re-syncs the live editor's mode with {@link #languageModel} on every render. The editor is a
     * stateful JS widget (see {@link AceEditor#updateMode}) that only changes mode when explicitly
     * told to - a plain markup re-render (e.g. after regenerate, or the wizard moving on to this
     * step once background generation finishes) never picks that up on its own.
     *
     * <p>Uses {@code onBeforeRender} rather than {@code onConfigure}: an Ajax partial update only
     * walks the subtree actually added to the target, and {@code onConfigure} isn't guaranteed to
     * run on a component outside that subtree, whereas {@code onBeforeRender} only fires on a
     * component that is actually about to be rendered into the response.
     *
     * <p>During an Ajax request the push always happens, even if {@link #languageModel} already
     * matches {@link #lastPushedMode}: that equality only says the *server* thinks nothing changed
     * - it says nothing about what the *browser's* already-initialized JS widget is currently
     * showing (e.g. a freshly (re)constructed panel instance sets {@link #lastPushedMode} to its
     * own just-computed mode at construction time, which trivially "matches" without the browser
     * ever having received that mode). A full page render has no such gap (the markup is generated
     * from scratch), so there {@link AceEditor#setMode} alone is enough.
     */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (scriptEditor != null) {
            // Force a fresh recompute from valueModel right now: languageModel may still be
            // holding a value cached from earlier in this same request (e.g. from this
            // instance's own construction), predating a background generation job that only
            // just finished - relying on languageModel's own cache here would race the content
            // binding below, which re-reads valueModel fresh (uncached) at actual render time.
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
                if (!isReloaded && ConnectorDevelopmentWizardUtil.existScript(getDetailsModel(), getScriptType(), getObjectClassName())) {

                    PrismContainerValueWrapper<ConnDevArtifactType> artifactTypeValueWrapper = ConnectorDevelopmentWizardUtil.getScript(
                            getDetailsModel(), getScriptType(), getObjectClassName());
                    ConnDevArtifactType artifactType = artifactTypeValueWrapper.getRealValue();
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

                // Fresh content from a (just-)completed generation task - whether this is the very
                // first landing here or the result of clicking "Regenerate", any validation error
                // still on record described an earlier version of the script and no longer applies.
                // onInitialize() forces this load() to run before regenerateButton/
                // RepairObjectClassButton compute their visibility, so validating (or clearing a
                // stale error) here is enough - no separate step is needed to tell the browser.
                //
                // Also validates immediately, so a still-invalid result (after the generation
                // service's own retries) shows up without the user clicking "Yes" first. Guarded
                // by autoValidationDone since load() re-runs on every later, unrelated render.
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
                        // No AjaxRequestTarget here - drawer refresh happens on the next request.
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
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        add(createLanguageSelect());

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
        // Editing the script makes "Regenerate" stale (it would act on a version of the script the
        // user has since changed) - hide it, but leave the drawer's error entry alone: it stays
        // visible for reference regardless of edits. Ace's "change" event fires on every keystroke,
        // hence the throttle; the update is idempotent once already flagged, so a coalesced,
        // slightly-delayed firing is fine.
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
                if (!scriptEditedSinceError) {
                    scriptEditedSinceError = true;
                    target.add(regenerateButton);
                }
            }
        });
        add(editorPanel);

        // The "change" Ajax roundtrip above (needed to actually clear the server-side error and
        // keep it in sync) is throttled and network-bound, so it visibly lags a keystroke behind.
        // Hiding the button is a purely client-side concern - do it immediately, instead of
        // waiting on that roundtrip. Delegated (not bound directly) so it survives the editor's
        // underlying textarea being replaced across an Ajax re-render (see modeForFilename's
        // callers); namespaced so a re-render doesn't stack duplicate bindings.
        add(new Behavior() {
            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                super.renderHead(component, response);
                response.render(OnDomReadyHeaderItem.forScript(
                        "$(document).off('change.scriptStepRegenerate', '#" + scriptEditor.getMarkupId() + "')"
                                + ".on('change.scriptStepRegenerate', '#" + scriptEditor.getMarkupId() + "', function() {"
                                + " $('#" + regenerateButton.getMarkupId() + "').hide(); });"));
            }
        });
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

    /**
     * Bridges {@link ConnDevScriptFormat} (the script-format model, in {@code smart-api}, shared
     * with the backend) to {@link AceEditor.Mode} (a GUI-only concept, with modes - XML, JSON - that
     * have no {@link ConnDevScriptFormat} counterpart). An exhaustive {@code switch}, so a future
     * format added to {@link ConnDevScriptFormat} fails to compile here until given an Ace mode.
     */
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
        try {
            ConnDevArtifactType script = valueModel.getObject().clone();
            script.setFilename(languageModel.getObject().withExtension(script.getFilename()));
            WebPrismUtil.cleanupEmptyContainerValue(script.asPrismContainerValue());
            if (isScriptOptional() && StringUtils.isBlank(script.getContent())) {
                // Nothing to validate or save - an optional script left empty (e.g. an
                // authentication script for an auth type the connector already implements
                // natively, like OAuth2 client credentials) just means "don't customize this",
                // not "invalid input". Unlike mandatory scripts (schema, search, ...), this is a
                // legitimate end state, not a validation failure.
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
                    target.add(regenerateButton);
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

    /**
     * Whether an empty script is a legitimate end state for this step, rather than something to
     * validate/save. False for every mandatory script (schema, search, create, ...) - true only
     * where the connector can work without one (see {@link AuthScriptsConnectorStepPanel}).
     */
    protected boolean isScriptOptional() {
        return false;
    }

    protected final LoadableModel<ConnDevArtifactType> getValueModel() {
        return valueModel;
    }

    /**
     * Forces {@link #valueModel} to reload from disk on next access, e.g. after
     * {@link RepairObjectClassButton} saved a fixed script directly (bypassing this step's own
     * submit) - mirrors what {@link #onRefreshPerformed} already does for its own "Regenerate" flow.
     */
    public void detachLoadedScript() {
        valueModel.detach();
        autoValidationDone = false;
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
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
        // Only meaningful while there's a still-relevant validation error to react to - editing
        // the script (see the "change" handler in initLayout) hides it again, since the error no
        // longer describes what's currently in the editor.
        regenerateButton.add(new VisibleBehaviour(() -> hasPendingValidationError()));
        regenerateButton.setOutputMarkupPlaceholderTag(true);
        customButtons.add(regenerateButton);

        repairObjectClassButton = new RepairObjectClassButton(customButtons.newChildId(), this, this::getObjectClassName,
                this::currentScriptsForRepair);
        customButtons.add(repairObjectClassButton);
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

    /**
     * The script content to send to the generation service for this step - whatever the user is
     * actually looking at right now, not necessarily what was last saved. {@link #valueModel} only
     * ever reflects what a *previous, separate* request last synced into it (Wicket detaches every
     * component model at the end of each request) - by the time a later click's own request starts,
     * that's already gone unless it was actually saved. {@link #VAR_LIVE_SCRIPT} is what the
     * "blur"/"change" handlers durably stashed for exactly this. Shared by "Regenerate" ({@link
     * #onRefreshPerformed}) and "Repair object class" ({@link RepairObjectClassButton}) so both act
     * on the same content.
     */
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
            // Read pending errors *before* touching valueModel: valueModel.load()'s task-result
            // branch clears any stale error the moment it resolves fresh content (see
            // createModels()) - reading the model first would let that same call wipe the very
            // error this click is trying to send along.
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
