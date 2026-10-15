/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.text.WordUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

/**
 * Review/editor step for an object class's scripts: reachable either right after {@link
 * WaitingFixObjectClassConnectorStepPanel} finishes a repair, or directly from the "Fix scripts"
 * menu action ({@code ConnectorDevelopmentController#editFixScripts}) with an empty batch, in
 * which case every script just defaults to its own deployed content. A single editor shows any
 * script of this object class (via {@link #ID_TYPE_SELECT}), not just the ones a repair
 * regenerated - for a type with both a deployed file and a fresh repair result, {@link
 * #ID_SOURCE_SELECT} picks which one populates the editor. Whichever content is on screen for a
 * type when "Confirm and save" is clicked is what gets saved for it (see {@link #workingSet}).
 * The confirm button validates the whole object class as one batch (via {@code validateArtifacts}),
 * not just whatever is currently selected (see {@link #allObjectClassArtifactsForValidation}), so
 * a cross-script issue surfaces here too. Every failing script gets its own banner above the
 * editor.
 *
 * <p>Hidden ({@link #isStepVisible()}) until {@link #resetArtifacts} is actually called.
 */
public class FixObjectClassReviewConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String ID_ERROR_BANNERS_CONTAINER = "errorBannersContainer";
    private static final String ID_ERROR_BANNERS = "errorBanners";
    private static final String ID_ERROR_BANNER_TEXT = "errorBannerText";
    private static final String ID_TYPE_SELECT = "typeSelect";
    private static final String ID_SOURCE_SELECT = "sourceSelect";
    private static final String ID_LANGUAGE_SELECT = "languageSelect";
    private static final String ID_ENABLED_TOGGLE = "enabledToggle";
    private static final String ID_EDITOR = "editor";

    private static final String CLASS_DOT = FixObjectClassReviewConnectorStepPanel.class.getName() + ".";
    private static final String OP_VALIDATE_AND_SAVE = CLASS_DOT + "validateAndSave";
    private static final String OP_LOAD_FILE_SCRIPT = CLASS_DOT + "loadFileScript";

    /** Which of a type's two possible contents is currently backing the editor. */
    private enum ScriptSource {
        FILE, MIDPILOT
    }

    /** One error banner shown above the editor - {@code type} is null if the error's source couldn't be matched to a known script kind. */
    private record ErrorBanner(ConnectorDevelopmentArtifacts.KnownArtifactType type, String filename, String message) {
    }

    /** Every object-class-scoped script kind - validation covers the whole object class, not just the repaired scripts. */
    private static final List<ConnectorDevelopmentArtifacts.KnownArtifactType> OBJECT_CLASS_ARTIFACT_TYPES = List.of(
            ConnectorDevelopmentArtifacts.KnownArtifactType.NATIVE_SCHEMA_DEFINITION,
            ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_ALL_DEFINITION,
            ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_BY_ID_DEFINITION,
            ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_FILTER_DEFINITION,
            ConnectorDevelopmentArtifacts.KnownArtifactType.CREATE,
            ConnectorDevelopmentArtifacts.KnownArtifactType.UPDATE,
            ConnectorDevelopmentArtifacts.KnownArtifactType.DELETE);

    /**
     * {@link #OBJECT_CLASS_ARTIFACT_TYPES}[i] is edited by the step whose PANEL_TYPE is {@link
     * RepairObjectClassButton#OBJECT_CLASS_SCRIPT_STEP_IDS}[i] (the two lists are kept in the same
     * order on purpose) - built once so a validation error here can be registered against the
     * underlying step that actually owns it, not this review step's own id - that's what {@code
     * RepairObjectClassButton} on that step (and this one) actually checks.
     */
    private static final Map<ConnectorDevelopmentArtifacts.KnownArtifactType, String> ARTIFACT_TYPE_TO_STEP_ID =
            buildArtifactTypeToStepId();

    private static Map<ConnectorDevelopmentArtifacts.KnownArtifactType, String> buildArtifactTypeToStepId() {
        Map<ConnectorDevelopmentArtifacts.KnownArtifactType, String> map = new LinkedHashMap<>();
        for (int i = 0; i < OBJECT_CLASS_ARTIFACT_TYPES.size(); i++) {
            map.put(OBJECT_CLASS_ARTIFACT_TYPES.get(i), RepairObjectClassButton.OBJECT_CLASS_SCRIPT_STEP_IDS.get(i));
        }
        return map;
    }

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel;
    private final String branchPanelType;

    private boolean triggered = false;

    /** The raw "repair" response batch, kept only to tell {@link #hasFreshVersion} what midpilot actually just returned. */
    private List<ConnDevArtifactType> fixedArtifacts = new ArrayList<>();

    /**
     * The actual editable content per visited script kind - whatever is here for a type is what
     * {@link #onNextPerformed} saves for it. Populated lazily (first visit to a type) or replaced
     * wholesale when {@link #ID_SOURCE_SELECT} is flipped for the currently selected type; switching
     * between already-visited types otherwise preserves each one's edits.
     */
    private final Map<ConnectorDevelopmentArtifacts.KnownArtifactType, ConnDevArtifactType> workingSet = new LinkedHashMap<>();

    /** Which source {@link #workingSet}'s current entry for a type came from - drives {@link #ID_SOURCE_SELECT}'s shown value. */
    private final Map<ConnectorDevelopmentArtifacts.KnownArtifactType, ScriptSource> sourceByType = new LinkedHashMap<>();

    private ConnectorDevelopmentArtifacts.KnownArtifactType selectedType;
    private List<ErrorBanner> errorBanners = new ArrayList<>();
    private RepairObjectClassButton repairObjectClassButton;
    private AceEditor scriptEditor;

    private final IModel<String> editorContentModel = new IModel<>() {
        @Override
        public String getObject() {
            ConnDevArtifactType artifact = currentWorkingArtifact();
            return artifact != null ? artifact.getContent() : null;
        }

        @Override
        public void setObject(String content) {
            ConnDevArtifactType artifact = currentWorkingArtifact();
            if (artifact != null) {
                artifact.setContent(content);
            }
        }
    };

    public FixObjectClassReviewConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel,
            String branchPanelType) {
        super(helper);
        this.objectClassModel = objectClassModel;
        this.branchPanelType = branchPanelType;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    public String getStepId() {
        return "cdw-connector-fix-review-" + branchPanelType + "-" + getObjectClassName();
    }

    private String getObjectClassName() {
        return objectClassModel.getObject().getRealValue().getName();
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> triggered;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("FixObjectClassReviewConnectorStepPanel.title", getObjectClassName());
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("FixObjectClassReviewConnectorStepPanel.text", getObjectClassName());
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("FixObjectClassReviewConnectorStepPanel.subText");
    }

    /**
     * Starts (or restarts) the review for a fresh fix result - called by
     * {@link WaitingFixObjectClassConnectorStepPanel#onNextPerformed} right after its fix task
     * completes, before the wizard navigates here. Navigation is the caller's job.
     */
    public void resetArtifacts(List<ConnDevArtifactType> newFixedArtifacts) {
        this.fixedArtifacts = new ArrayList<>(newFixedArtifacts);
        this.workingSet.clear();
        this.sourceByType.clear();
        this.errorBanners = new ArrayList<>();
        for (ConnDevArtifactType artifact : newFixedArtifacts) {
            var type = ConnectorDevelopmentArtifacts.classify(artifact);
            if (type != null) {
                workingSet.put(type, artifact);
                sourceByType.put(type, ScriptSource.MIDPILOT);
            }
        }
        this.selectedType = newFixedArtifacts.stream()
                .map(ConnectorDevelopmentArtifacts::classify)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> availableTypes().stream().findFirst().orElse(null));
        this.triggered = true;
    }

    private boolean hasFileVersion(ConnectorDevelopmentArtifacts.KnownArtifactType type) {
        return ConnectorDevelopmentWizardUtil.existScript(getDetailsModel(), type, getObjectClassName());
    }

    private boolean hasFreshVersion(ConnectorDevelopmentArtifacts.KnownArtifactType type) {
        return fixedArtifacts.stream().anyMatch(a -> ConnectorDevelopmentArtifacts.classify(a) == type);
    }

    /** Every script kind with at least one of a deployed file or a fresh "repair" result - nothing to show otherwise. */
    private List<ConnectorDevelopmentArtifacts.KnownArtifactType> availableTypes() {
        return OBJECT_CLASS_ARTIFACT_TYPES.stream()
                .filter(t -> hasFileVersion(t) || hasFreshVersion(t))
                .toList();
    }

    /** {@link #workingSet}'s entry for {@link #selectedType}, lazily initialized on first visit. */
    private ConnDevArtifactType currentWorkingArtifact() {
        if (selectedType == null) {
            return null;
        }
        return workingSet.computeIfAbsent(selectedType, this::loadDefaultArtifactFor);
    }

    /** First visit to {@code type}: prefer the fresh "repair" result, falling back to the deployed file. */
    private ConnDevArtifactType loadDefaultArtifactFor(ConnectorDevelopmentArtifacts.KnownArtifactType type) {
        if (hasFreshVersion(type)) {
            sourceByType.put(type, ScriptSource.MIDPILOT);
            return freshArtifactFor(type);
        }
        sourceByType.put(type, ScriptSource.FILE);
        return loadFileArtifact(type);
    }

    private ConnDevArtifactType freshArtifactFor(ConnectorDevelopmentArtifacts.KnownArtifactType type) {
        return fixedArtifacts.stream()
                .filter(a -> ConnectorDevelopmentArtifacts.classify(a) == type)
                .findFirst().orElse(null);
    }

    private ConnDevArtifactType loadFileArtifact(ConnectorDevelopmentArtifacts.KnownArtifactType type) {
        PrismContainerValueWrapper<ConnDevArtifactType> wrapper =
                ConnectorDevelopmentWizardUtil.getScript(getDetailsModel(), type, getObjectClassName());
        if (wrapper == null || wrapper.getRealValue() == null) {
            return type.create(getObjectClassName());
        }
        // Never clone/reuse the live wrapper's real value directly - it carries a full
        // PrismContext-bound definition that then fails to marshal later ("confirm" required but
        // unset - see RepairObjectClassButton#toFreshOverride for the same gotcha). A fresh,
        // unwrapped artifact with just the filename and disabled flag copied over is safe to
        // clone/save/marshal.
        ConnDevArtifactType deployed = wrapper.getRealValue();
        ConnDevArtifactType artifact = type.create(getObjectClassName())
                .filename(deployed.getFilename())
                .disabled(Boolean.TRUE.equals(deployed.isDisabled()));
        Task task = getPageBase().createSimpleTask(OP_LOAD_FILE_SCRIPT);
        try {
            String content = getDetailsModel().getConnectorDevelopmentOperation()
                    .getArtifactContent(artifact, task, task.getResult());
            artifact.setContent(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return artifact;
    }

    /** Explicit source switch for {@code type} - replaces its {@link #workingSet} entry wholesale, discarding any edit since the last switch. */
    private void switchSource(ConnectorDevelopmentArtifacts.KnownArtifactType type, ScriptSource newSource) {
        ConnDevArtifactType artifact = newSource == ScriptSource.FILE ? loadFileArtifact(type) : freshArtifactFor(type);
        if (artifact != null) {
            workingSet.put(type, artifact);
            sourceByType.put(type, newSource);
        }
    }

    /**
     * Every currently deployed script of this object class, with {@link #workingSet} (whatever is
     * currently shown/edited for a visited type) substituted in place of its stale deployed version
     * - so confirming here validates the whole object class as a set, not just whatever is selected.
     * A type not yet visited is loaded fresh via {@link #loadFileArtifact} rather than taken from
     * its plain deployed wrapper - the wrapper's real value never carries content (only the file
     * does; the model stores metadata only), and {@code validateArtifacts} now builds every
     * artifact in this list for real, so a content-less one would fail with a bare "scriptText
     * must be specified" instead of a real validation result.
     */
    private List<ConnDevArtifactType> allObjectClassArtifactsForValidation() {
        Map<String, ConnDevArtifactType> byFilename = new LinkedHashMap<>();
        for (var type : OBJECT_CLASS_ARTIFACT_TYPES) {
            if (workingSet.containsKey(type) || !hasFileVersion(type)) {
                continue;
            }
            ConnDevArtifactType artifact = loadFileArtifact(type);
            if (artifact.getFilename() != null && !Boolean.TRUE.equals(artifact.isDisabled())) {
                byFilename.put(artifact.getFilename(), artifact);
            }
        }
        for (ConnDevArtifactType artifact : workingSet.values()) {
            if (artifact.getFilename() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(artifact.isDisabled())) {
                // Disabled overrides whatever the deployed-fallback loop above added for the same
                // filename - excluded from validation entirely, exactly like conndev's manifest
                // treats a disabled script as not bundled at all.
                byFilename.remove(artifact.getFilename());
            } else {
                byFilename.put(artifact.getFilename(), artifact);
            }
        }
        return new ArrayList<>(byFilename.values());
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        WebMarkupContainer errorBannersContainer = new WebMarkupContainer(ID_ERROR_BANNERS_CONTAINER);
        errorBannersContainer.setOutputMarkupId(true);
        ListView<ErrorBanner> bannersView = new ListView<>(ID_ERROR_BANNERS, (IModel<List<ErrorBanner>>) () -> errorBanners) {
            @Override
            protected void populateItem(ListItem<ErrorBanner> item) {
                ErrorBanner banner = item.getModelObject();
                item.add(AttributeAppender.append("class", "alert alert-danger mb-2"));
                if (banner.type() != null) {
                    item.add(AttributeAppender.append("style", "cursor: pointer;"));
                    item.add(new AjaxEventBehavior("click") {
                        @Override
                        protected void onEvent(AjaxRequestTarget target) {
                            selectedType = banner.type();
                            refreshEditor(target);
                        }
                    });
                }
                item.add(new Label(ID_ERROR_BANNER_TEXT, Model.of(banner.filename() + ": " + banner.message())));
            }
        };
        errorBannersContainer.add(bannersView);
        add(errorBannersContainer);

        add(createTypeSelect());
        add(createSourceSelect());
        add(createLanguageSelect());
        add(createEnabledToggle());

        SimpleAceEditorPanel editorPanel = new SimpleAceEditorPanel(ID_EDITOR, editorContentModel, 300) {
            @Override
            protected AceEditor createEditor(String id, IModel<String> model, int minSize) {
                AceEditor editor = new AceEditor(id, model);
                editor.setReadonly(false);
                editor.setMinHeight(minSize);
                editor.setHeight(300);
                editor.setResizeToMaxHeight(false);
                editor.setMode(currentEditorMode());
                add(editor);
                editor.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                scriptEditor = editor;
                return editor;
            }
        };
        editorPanel.getEditor().setConvertEmptyInputStringToNull(false);
        editorPanel.setOutputMarkupId(true);
        editorPanel.add(AttributeAppender.append("class", "d-flex flex-column w-100 border rounded"));
        add(editorPanel);
    }

    private DropDownChoicePanel<ConnectorDevelopmentArtifacts.KnownArtifactType> createTypeSelect() {
        IModel<List<ConnectorDevelopmentArtifacts.KnownArtifactType>> choices =
                (IModel<List<ConnectorDevelopmentArtifacts.KnownArtifactType>>) this::availableTypes;
        DropDownChoicePanel<ConnectorDevelopmentArtifacts.KnownArtifactType> typeSelect = new DropDownChoicePanel<>(
                ID_TYPE_SELECT,
                new IModel<>() {
                    @Override
                    public ConnectorDevelopmentArtifacts.KnownArtifactType getObject() {
                        return selectedType;
                    }

                    @Override
                    public void setObject(ConnectorDevelopmentArtifacts.KnownArtifactType object) {
                        selectedType = object;
                    }
                },
                choices,
                new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(ConnectorDevelopmentArtifacts.KnownArtifactType type) {
                        return WordUtils.capitalizeFully(type.name().replace('_', ' '));
                    }
                }, false);
        typeSelect.setOutputMarkupId(true);
        typeSelect.getBaseFormComponent().add(AttributeAppender.append("class", "form-select form-select-sm"));
        typeSelect.getBaseFormComponent().add(AttributeAppender.append("style", "width: 16rem;"));
        typeSelect.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                selectedType = typeSelect.getBaseFormComponent().getConvertedInput();
                refreshEditor(target);
            }
        });
        return typeSelect;
    }

    private DropDownChoicePanel<ScriptSource> createSourceSelect() {
        IModel<List<ScriptSource>> choices = Model.ofList(List.of(ScriptSource.values()));
        DropDownChoicePanel<ScriptSource> sourceSelect = new DropDownChoicePanel<>(
                ID_SOURCE_SELECT,
                new IModel<>() {
                    @Override
                    public ScriptSource getObject() {
                        return selectedType != null ? sourceByType.get(selectedType) : null;
                    }

                    @Override
                    public void setObject(ScriptSource object) {
                        // Handled explicitly in the "change" handler below - switching source
                        // reloads content, which a plain field assignment here can't do.
                    }
                },
                choices,
                new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(ScriptSource source) {
                        return getString(source == ScriptSource.FILE
                                ? "FixObjectClassReviewConnectorStepPanel.source.file"
                                : "FixObjectClassReviewConnectorStepPanel.source.midpilot");
                    }
                }, false);
        sourceSelect.setOutputMarkupId(true);
        sourceSelect.setOutputMarkupPlaceholderTag(true);
        sourceSelect.add(new VisibleBehaviour(() ->
                selectedType != null && hasFileVersion(selectedType) && hasFreshVersion(selectedType)));
        sourceSelect.getBaseFormComponent().add(AttributeAppender.append("class", "form-select form-select-sm"));
        sourceSelect.getBaseFormComponent().add(AttributeAppender.append("style", "width: 12rem;"));
        sourceSelect.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ScriptSource newSource = sourceSelect.getBaseFormComponent().getConvertedInput();
                if (selectedType != null && newSource != null) {
                    switchSource(selectedType, newSource);
                }
                refreshEditor(target);
            }
        });
        return sourceSelect;
    }

    private DropDownChoicePanel<ConnDevScriptFormat> createLanguageSelect() {
        IModel<List<ConnDevScriptFormat>> choices = Model.ofList(List.of(ConnDevScriptFormat.values()));
        DropDownChoicePanel<ConnDevScriptFormat> languageSelect = new DropDownChoicePanel<>(
                ID_LANGUAGE_SELECT,
                new IModel<>() {
                    @Override
                    public ConnDevScriptFormat getObject() {
                        ConnDevArtifactType artifact = currentWorkingArtifact();
                        return ConnDevScriptFormat.fromFilename(artifact != null ? artifact.getFilename() : null);
                    }

                    @Override
                    public void setObject(ConnDevScriptFormat object) {
                        // Handled explicitly in the "change" handler below - it also has to rename
                        // the working artifact's filename, which a plain field assignment can't do.
                    }
                },
                choices,
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
                ConnDevArtifactType artifact = currentWorkingArtifact();
                if (artifact != null && selectedFormat != null) {
                    artifact.setFilename(selectedFormat.withExtension(artifact.getFilename()));
                }
                if (scriptEditor != null) {
                    scriptEditor.updateMode(target, toAceMode(selectedFormat));
                }
            }
        });
        return languageSelect;
    }

    /**
     * Lets the user deactivate the currently selected script (e.g. one they can't fix) instead of
     * blocking "Confirm and save" on it forever - mirrors the drawer's "Disable operation" action
     * for a broken sibling, but reachable directly from the editor for whichever script is shown.
     * A disabled script is excluded from validation entirely (see {@link
     * #allObjectClassArtifactsForValidation}) and, on save, its content is never written - only
     * the manifest's {@code disabled} flag is set (see {@link #onNextPerformed}).
     */
    private CheckBox createEnabledToggle() {
        CheckBox enabledToggle = new CheckBox(ID_ENABLED_TOGGLE, new IModel<>() {
            @Override
            public Boolean getObject() {
                ConnDevArtifactType artifact = currentWorkingArtifact();
                return artifact == null || !Boolean.TRUE.equals(artifact.isDisabled());
            }

            @Override
            public void setObject(Boolean enabled) {
                ConnDevArtifactType artifact = currentWorkingArtifact();
                if (artifact != null) {
                    artifact.setDisabled(!Boolean.TRUE.equals(enabled));
                }
            }
        });
        enabledToggle.setOutputMarkupId(true);
        enabledToggle.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Boolean enabled = enabledToggle.getConvertedInput();
                ConnDevArtifactType artifact = currentWorkingArtifact();
                if (artifact != null) {
                    artifact.setDisabled(!Boolean.TRUE.equals(enabled));
                }
            }
        });
        return enabledToggle;
    }

    private AceEditor.Mode currentEditorMode() {
        ConnDevArtifactType artifact = currentWorkingArtifact();
        return toAceMode(ConnDevScriptFormat.fromFilename(artifact != null ? artifact.getFilename() : null));
    }

    /** Pushes the newly selected type/source's content and mode into the (reused) editor, and re-renders the selects around it. */
    private void refreshEditor(AjaxRequestTarget target) {
        currentWorkingArtifact();
        if (scriptEditor != null) {
            scriptEditor.setMode(currentEditorMode());
        }
        target.add(get(ID_EDITOR));
        target.add(get(ID_SOURCE_SELECT));
        target.add(get(ID_LANGUAGE_SELECT));
        target.add(get(ID_ENABLED_TOGGLE));
    }

    /** Mirrors {@code ScriptConnectorStepPanel#toAceMode} - see there for why this stays an exhaustive switch. */
    private static AceEditor.Mode toAceMode(ConnDevScriptFormat format) {
        return switch (format) {
            case GROOVY -> AceEditor.Mode.GROOVY;
            case YAML -> AceEditor.Mode.YAML;
        };
    }

    /**
     * Same "Repair object class" action every script step of this object class embeds - without
     * it, a failed "Confirm and save" leaves no way to trigger another repair attempt without
     * first navigating away to one of the underlying script steps.
     */
    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        repairObjectClassButton = new RepairObjectClassButton(
                customButtons.newChildId(), this, this::getObjectClassName, () -> new ArrayList<>(workingSet.values()));
        customButtons.add(repairObjectClassButton);
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
        return createStringResource("FixObjectClassReviewConnectorStepPanel.submit");
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
        Task task = getPageBase().createSimpleTask(OP_VALIDATE_AND_SAVE);
        OperationResult result = task.getResult();

        // Drop whatever is currently registered for the WHOLE object class - this step's own
        // previous attempt, but also any pre-existing error a script step reported on its own
        // (e.g. a direct edit, before "repair" ever ran) - before re-validating, so a source this
        // validation now finds clean doesn't keep reporting a stale error forever (and keeping
        // RepairObjectClassButton visible) just because nothing else ever clears it. Whatever is
        // still actually broken gets re-registered fresh right below, from this validation's own
        // results - nothing is silently lost, only recomputed.
        ConnectorDevelopmentWizardUtil.clearScriptValidationErrors(this, getStepId());
        if (getWizard() instanceof WizardModelWithParentSteps parentWizardModelForClear) {
            parentWizardModelForClear.removeOperationResultsForFixSteps(RepairObjectClassButton.OBJECT_CLASS_SCRIPT_STEP_IDS);
        }

        List<ConnDevArtifactType> allArtifacts = allObjectClassArtifactsForValidation();

        ConnDevArtifactValidationResult validation;
        try {
            validation = getDetailsModel().getConnectorDevelopmentOperation().validateArtifacts(allArtifacts, task, result);
        } catch (RuntimeException e) {
            getPageBase().error("Couldn't validate the object class's scripts: " + e.getMessage());
            target.add(getFeedback());
            return false;
        }

        errorBanners = new ArrayList<>();
        if (!validation.ok()) {
            // Every object-class script is reachable via the select now, so every error gets its
            // own banner here (grouped by source) - nothing needs routing to the drawer anymore.
            var bySource = validation.errors().stream()
                    .collect(Collectors.groupingBy(
                            e -> e.source() != null ? e.source() : "?",
                            LinkedHashMap::new, Collectors.toList()));
            for (var entry : bySource.entrySet()) {
                String source = entry.getKey();
                String filename = source.startsWith("/") ? source.substring(1) : source;
                String message = entry.getValue().stream()
                        .map(e -> e.line() != null
                                ? e.message() + " (line " + e.line() + (e.column() != null ? ", column " + e.column() : "") + ")"
                                : e.message())
                        .collect(Collectors.joining("; "));
                var matchingArtifact = allArtifacts.stream()
                        .filter(a -> source.equals("/" + a.getFilename()))
                        .findFirst().orElse(null);
                var knownType = matchingArtifact != null ? ConnectorDevelopmentArtifacts.classify(matchingArtifact) : null;
                errorBanners.add(new ErrorBanner(knownType, filename, message));

                String fixPanelId = knownType != null ? ARTIFACT_TYPE_TO_STEP_ID.get(knownType) : null;
                if (fixPanelId != null) {
                    ConnectorDevelopmentWizardUtil.reportScriptValidationErrorsForStep(
                            this, getStepId(), fixPanelId, entry.getValue(), source);
                }
            }
            ConnectorDevelopmentWizardUtil.refreshDrawerPanel(this, target);
            getPageBase().error(createStringResource(
                    "FixObjectClassReviewConnectorStepPanel.validation.summary", validation.errors().size()).getString());
            target.add(getFeedback());
            target.add(get(ID_ERROR_BANNERS_CONTAINER));
            target.add(repairObjectClassButton);
            return false;
        }

        try {
            for (var entry : workingSet.entrySet()) {
                ConnectorDevelopmentArtifacts.KnownArtifactType type = entry.getKey();
                ConnDevArtifactType artifact = entry.getValue();
                if (Boolean.TRUE.equals(artifact.isDisabled())) {
                    if (hasFileVersion(type)) {
                        // Only flips the manifest's disabled flag - never writes content, so any
                        // edit made before deciding to disable this script is simply dropped.
                        getDetailsModel().getConnectorDevelopmentOperation().disableArtifact(artifact.getFilename(), task, result);
                    }
                    // Not yet deployed and disabled: nothing to disable, nothing to save either.
                } else {
                    getDetailsModel().getConnectorDevelopmentOperation().saveArtifact(artifact, task, result);
                }
            }
        } catch (IOException | CommonException e) {
            getPageBase().error("Couldn't save the repaired scripts: " + e.getMessage());
            target.add(getFeedback());
            return false;
        }
        // saveArtifact/disableArtifact save through the backend's own, separate object instance -
        // the GUI's own prism object wrapper (what every other step, including the object class
        // tile overview, actually renders from) is stale until explicitly reloaded, same as
        // ScriptConnectorStepPanel does after its own save.
        getDetailsModel().reloadPrismObjectByOid();

        if (getWizard() instanceof WizardModelWithParentSteps parentWizardModel) {
            parentWizardModel.removeOperationResultsForFixSteps(RepairObjectClassButton.OBJECT_CLASS_SCRIPT_STEP_IDS);
            for (var step : parentWizardModel.getActiveChildrenSteps()) {
                if (step instanceof ScriptConnectorStepPanel scriptStep) {
                    scriptStep.detachLoadedScript();
                }
            }
        }

        triggered = false;
        fixedArtifacts = new ArrayList<>();
        workingSet.clear();
        sourceByType.clear();
        errorBanners = new ArrayList<>();
        selectedType = null;
        getPageBase().success(createStringResource("FixObjectClassReviewConnectorStepPanel.success").getString());
        return super.onNextPerformed(target);
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
