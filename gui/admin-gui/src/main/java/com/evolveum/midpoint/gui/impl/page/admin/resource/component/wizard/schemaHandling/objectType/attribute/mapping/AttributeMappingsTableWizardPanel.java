/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.createVirtualMappingContainerModel;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.isSuggestionExists;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectTypeMappingTypeSuggestion;

import java.util.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.FormWrapperValidator;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartAlertGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartSuggestButtonWithConfirmation;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingAlertDto;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.TabSeparatedTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationOption;
import com.evolveum.midpoint.web.component.dialog.privacy.DataAccessPermission;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.ButtonWithConfirmationOptionsDialog;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.SuggestionsStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author lskublik
 */
@PanelType(name = "rw-attribute-mappings")
@PanelInstance(identifier = "rw-attribute-inbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AttributeMappingsTableWizardPanel.inboundTable", icon = "fa fa-arrow-right-to-bracket"))
@PanelInstance(identifier = "rw-attribute-outbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AttributeMappingsTableWizardPanel.outboundTable", icon = "fa fa-arrow-right-from-bracket"))
public abstract class AttributeMappingsTableWizardPanel<P extends Containerable> extends AbstractResourceWizardBasicPanel<P> {

    private static final Trace LOGGER = TraceManager.getTrace(AttributeMappingsTableWizardPanel.class);
    private static final String CLASS_DOT = AttributeMappingsTableWizardPanel.class.getName() + ".";
    private static final String OP_SUGGEST_MAPPING = CLASS_DOT + "suggestMapping";
    private static final String OP_LOAD_SUGGESTION = CLASS_DOT + "loadSuggestion";

    private static final String ID_MAIN_FORM = "form";
    private static final String ID_AI_PANEL = "aiPanel";
    private static final String ID_TAB_TABLE = "panel";

    private final MappingDirection initialTab;
    IModel<Boolean> inboundSuggestionToggleModel = Model.of(Boolean.FALSE);
    IModel<Boolean> outboundSuggestionToggleModel = Model.of(Boolean.FALSE);
    boolean isInboundTabSelected = true;
    private SerializableConsumer<AjaxRequestTarget> restartTime;

    LoadableDetachableModel<SmartGeneratingAlertDto> suggestionModel = new LoadableDetachableModel<>() {
        @Override
        protected @NotNull SmartGeneratingAlertDto load() {
            if (!Boolean.TRUE.equals(getSwitchToggleModel().getObject())) {
                return new SmartGeneratingAlertDto(null, getSwitchToggleModel(), getPageBase());
            }

            ResourceType resource = getAssignmentHolderDetailsModel().getObjectType();
            return new SmartGeneratingAlertDto(loadSuggestion(resource.getOid()), getSwitchToggleModel(), getPageBase());
        }
    };

    public AttributeMappingsTableWizardPanel(
            String id,
            WizardPanelHelper<P, ResourceDetailsModel> superHelper,
            MappingDirection initialTab) {
        super(id, superHelper);
        this.initialTab = initialTab;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSwitchSuggestionModel();
        initLayout();
    }

    private void initSwitchSuggestionModel() {
        this.inboundSuggestionToggleModel = SmartIntegrationUtils.createSuggestionSwitchModel(getPageBase(),
                SuggestionsStorage.SuggestionType.INBOUND_MAPPING);
        this.outboundSuggestionToggleModel = SmartIntegrationUtils.createSuggestionSwitchModel(getPageBase(),
                SuggestionsStorage.SuggestionType.OUTBOUND_MAPPING);
    }

    private void initLayout() {

        MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM);
        form.setOutputMarkupId(true);

        //noinspection rawtypes,unchecked
        form.add(new FormWrapperValidator<>(getPageBase()) {

            @Override
            protected PrismObjectWrapper getObjectWrapper() {
                return getAssignmentHolderDetailsModel().getObjectWrapper();
            }
        });

        form.setMultiPart(true);
        add(form);

        add(form);

        String resourceOid = getResourceOid();
        SmartAlertGeneratingPanel aiPanel = createSmartAlertGeneratingPanel(resourceOid);
        this.restartTime = aiPanel::restartTimeBehavior;
        form.add(aiPanel);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(createInboundTableTab(resourceOid, inboundSuggestionToggleModel));
        tabs.add(createOutboundTableTab(resourceOid, outboundSuggestionToggleModel));

        TabSeparatedTabbedPanel<ITab> tabPanel = new TabSeparatedTabbedPanel<>(ID_TAB_TABLE, tabs) {
            @Override
            protected void onAjaxUpdate(@NotNull Optional<AjaxRequestTarget> optional) {
                optional.ifPresent(target -> {
                    target.add(getButtonsContainer());
                    target.add(getAiPanel());
                });
            }

            @Override
            protected void onClickTabPerformed(int index, @NotNull Optional<AjaxRequestTarget> target) {
                isInboundTabSelected = index == 0;
                if (getTable().isValidFormComponents(target.orElse(null))) {
                    super.onClickTabPerformed(index, target);
                }
            }
        };

        switchTabs(tabPanel);

        tabPanel.setOutputMarkupId(true);
        form.add(tabPanel);
    }

    private void switchTabs(TabSeparatedTabbedPanel<ITab> tabPanel) {
        switch (initialTab) {
            case INBOUND:
                tabPanel.setSelectedTab(0);
                break;
            case OUTBOUND:
                tabPanel.setSelectedTab(1);
                break;
        }
    }

    private @NotNull ITab createInboundTableTab(String resourceOid, IModel<Boolean> switchToggleModel) {
        return new IconPanelTab(
                getPageBase().createStringResource(
                        "AttributeMappingsTableWizardPanel.inboundTable"),
                new VisibleBehaviour(this::isInboundVisible)) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createSmartMappingTable(panelId, switchToggleModel, resourceOid, MappingDirection.INBOUND);
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-arrow-right-to-bracket");
            }
        };
    }

    @Contract("_, _ -> new")
    private @NotNull ITab createOutboundTableTab(String resourceOid, IModel<Boolean> switchToggleModel) {
        return new IconPanelTab(
                getPageBase().createStringResource(
                        "AttributeMappingsTableWizardPanel.outboundTable"),
                new VisibleBehaviour(this::isOutboundVisible)) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createSmartMappingTable(panelId, switchToggleModel, resourceOid, MappingDirection.OUTBOUND);
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-arrow-right-from-bracket");
            }
        };
    }

    protected boolean isOutboundVisible() {
        return true;
    }

    protected boolean isInboundVisible() {
        return true;
    }

    private @NotNull SmartMappingTable<P> createSmartMappingTable(
            String panelId,
            IModel<Boolean> switchToggleModel,
            String resourceOid,
            MappingDirection initialTab) {
        IModel<PrismContainerValueWrapper<P>> valueModel = getValueModel();
        SmartMappingTable<P> columnTileTable =
                new SmartMappingTable<>(
                        panelId,
                        () -> initialTab,
                        switchToggleModel,
                        valueModel,
                        resourceOid) {
                    @Override
                    protected void performOnEditMapping(
                            @NotNull AjaxRequestTarget target,
                            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        if (initialTab == MappingDirection.INBOUND) {
                            inEditInboundValue(rowModel, target);
                        } else {
                            inEditOutboundValue(rowModel, target);
                        }
                    }

                    @Override
                    protected void refreshAndDetach(AjaxRequestTarget target) {
                        suggestionModel.detach();
                        super.refreshAndDetach(target);

                        if (displayNoValuePanel()) {
                            switchToggleModel.setObject(Boolean.FALSE);
                        }

                        target.add(getAiPanel());
                    }

                    @Override
                    protected void addAdditionalNoValueToolbarButtons(@NotNull List<Component> toolbarButtonsList, String idButton) {
                        AjaxIconButton generateButton = SmartSuggestButtonWithConfirmation.create(idButton,
                                createStringResource("Suggestion.button.suggest"),
                                () -> GuiStyleConstants.CLASS_MAGIC_WAND,
                                ConfirmationOption.mappingPermissionsOptions(),
                                () -> new ButtonWithConfirmationOptionsDialog.ButtonHandlers<>(target -> {
                                },
                                        (target, confirmedOptions) -> {
                                            AttributeMappingsTableWizardPanel.this.performSuggestOperation(target, confirmedOptions, false);
                                            refreshAfterSuggestionOperationSubmitted(target);
                                        }),
                                getPageBase());

                        generateButton.add(new VisibleBehaviour(() -> this.displayNoValuePanel()
                                && !isSuggestionExists(loadSuggestion(resourceOid).getObject())));
                        generateButton.setOutputMarkupId(true);
                        generateButton.showTitleAsLabel(true);

                        toolbarButtonsList.add(generateButton);

                        AjaxIconButton showSuggestionsButton = new AjaxIconButton(idButton,
                                () -> GuiStyleConstants.CLASS_MAGIC_WAND,
                                () -> createStringResource("Suggestion.button.showSuggest").getString()) {

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                getSwitchToggleModel().setObject(Boolean.TRUE);
                                target.add(AttributeMappingsTableWizardPanel.this);
                                refreshAndDetach(target);
                            }
                        };
                        showSuggestionsButton.add(new VisibleBehaviour(() -> displayNoValuePanel()
                                && isSuggestionExists(loadSuggestion(resourceOid).getObject())));
                        showSuggestionsButton.add(AttributeModifier.append("class", "btn rounded bg-purple"));
                        showSuggestionsButton.setOutputMarkupId(true);
                        showSuggestionsButton.showTitleAsLabel(true);

                        toolbarButtonsList.add(showSuggestionsButton);
                    }

                    @Override
                    protected void buildSimulationResultPanel(AjaxRequestTarget target, IModel<SimulationResultType> simulationResultTypeIModel) {
                        AttributeMappingsTableWizardPanel.this.buildSimulationResultPanel(target, simulationResultTypeIModel);
                    }

                    @Override
                    protected ResourceType getResourceType() {
                        ResourceDetailsModel resourceDetailsModel = getAssignmentHolderDetailsModel();
                        PrismObjectWrapper<ResourceType> objectWrapper = resourceDetailsModel.getObjectWrapper();
                        PrismObject<ResourceType> objectApplyDelta;
                        try {
                            objectApplyDelta = objectWrapper.getObjectApplyDelta();
                        } catch (CommonException e) {
                            LOGGER.error("Couldn't get resource object with applied delta, returning the original object. Details: {}", e.getMessage(), e);
                            return null;
                        }

                        return objectApplyDelta.asObjectable();
                    }
                };

        columnTileTable.setOutputMarkupId(true);
        columnTileTable.add(AttributeAppender.append("class", "p-0"));

        return columnTileTable;
    }

    private @NotNull @Unmodifiable List<ItemPathType> getTargetPathsToIgnore() {
        boolean isInbound = getSelectedMappingType() == MappingDirection.INBOUND;
        Set<PrismContainerValueWrapper<MappingType>> accepted = getTable().getAcceptedSuggestionsCache();

        if (isInbound) {
            return accepted.stream()
                    .map(PrismContainerValueWrapper::getRealValue)
                    .filter(Objects::nonNull)
                    .map(MappingType::getTarget)
                    .filter(Objects::nonNull)
                    .map(VariableBindingDefinitionType::getPath)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }

        List<ItemPathType> targetPathsToIgnore = new ArrayList<>();
        for (PrismContainerValueWrapper<MappingType> wrapper : accepted) {
            try {
                PrismPropertyWrapper<ItemPathType> refProperty =
                        wrapper.findProperty(AbstractAttributeMappingsDefinitionType.F_REF);

                ItemPathType refPath = refProperty != null && refProperty.getValue() != null
                        ? refProperty.getValue().getRealValue()
                        : null;

                if (refPath != null) {
                    targetPathsToIgnore.add(refPath);
                }
            } catch (SchemaException e) {
                throw new RuntimeException("Error retrieving ref property from mapping", e);
            }
        }

        return targetPathsToIgnore.stream().distinct().toList();
    }

    private IModel<Boolean> getSwitchToggleModel() {
        return getSelectedMappingType() == MappingDirection.INBOUND
                ? inboundSuggestionToggleModel
                : outboundSuggestionToggleModel;
    }

    protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
        return createVirtualMappingContainerModel(
                getPageBase(),
                getValueModel(),
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_REF,
                getSelectedMappingType());
    }

    protected LoadableModel<StatusInfo<?>> loadSuggestion(String resourceOid) {
        Task task = getPageBase().createSimpleTask(OP_LOAD_SUGGESTION);
        OperationResult result = task.getResult();
        return new LoadableModel<>() {
            @Override
            protected StatusInfo<MappingsSuggestionType> load() {

                return loadObjectTypeMappingTypeSuggestion(getPageBase(),
                        resourceOid,
                        getResourceObjectTypeIdentification(),
                        getSelectedMappingType(),
                        task,
                        result);
            }
        };
    }

    private @NotNull SmartAlertGeneratingPanel createSmartAlertGeneratingPanel(
            @NotNull String resourceOid) {

        SmartAlertGeneratingPanel aiPanel = new SmartAlertGeneratingPanel(ID_AI_PANEL, suggestionModel) {
            @Override
            protected void performSuggestOperation(AjaxRequestTarget target,
                    IModel<List<ConfirmationOption<DataAccessPermission>>> confirmedOptions) {
                AttributeMappingsTableWizardPanel.this.performSuggestOperation(target, confirmedOptions, false);
                refreshAfterSuggestionOperationSubmitted(target);
            }

            @Override
            protected void performRegenerateSuggestOperation(AjaxRequestTarget target,
                    IModel<List<ConfirmationOption<DataAccessPermission>>> confirmedOptions) {
                AttributeMappingsTableWizardPanel.this.performSuggestOperation(target, confirmedOptions, true);
                refreshAfterSuggestionOperationSubmitted(target);
            }

            @Override
            protected void onSuggestionFinish(AjaxRequestTarget target) {
                Objects.requireNonNull(getTable()).refreshAndDetach(target);
            }

            @Override
            protected @NotNull IModel<List<ConfirmationOption<DataAccessPermission>>> getConfirmationOptions() {
                final List<ConfirmationOption<DataAccessPermission>> confirmationOptions =
                        ConfirmationOption.mappingPermissionsOptions();
                return () -> confirmationOptions;
            }

            @Override
            protected void onRefresh(@NotNull AjaxRequestTarget target) {
                SmartMappingTable<?> smartMappingTable = getTable();
                if (smartMappingTable != null) {
                    smartMappingTable.refreshAndDetach(target);
                }
            }
        };

        aiPanel.setOutputMarkupId(true);
        aiPanel.setOutputMarkupPlaceholderTag(true);
        aiPanel.add(new VisibleBehaviour(() -> getSwitchToggleModel().getObject().equals(Boolean.TRUE)));
        return aiPanel;
    }

    private void performSuggestOperation(AjaxRequestTarget target,
            IModel<List<ConfirmationOption<DataAccessPermission>>> confirmedOptions,
            boolean forceRecomputeSchemaMatch) {
        final List<DataAccessPermissionType> permissions = confirmedOptions.getObject().stream()
                .map(ConfirmationOption::option)
                .map(DataAccessPermission::toSchemaType)
                .toList();
        ResourceObjectTypeIdentification objectTypeIdentification = getResourceObjectTypeIdentification();
        if (objectTypeIdentification == null) {
            LOGGER.warn("Cannot perform suggest mapping operation - no resource object type definition found.");
            return;
        }

        SmartIntegrationService service = getPageBase().getSmartIntegrationService();
        String resourceOid = getResourceOid();
        getPageBase().taskAwareExecutor(target, OP_SUGGEST_MAPPING)
                .withOpResultOptions(OpResult.Options.create()
                        .withHideSuccess(true)
                        .withHideInProgress(true))
                .runVoid((task, result) -> {
                    boolean inbound = getSelectedMappingType() == MappingDirection.INBOUND;
                    service.submitSuggestMappingsOperation(
                            resourceOid,
                            objectTypeIdentification,
                            inbound,
                            getTargetPathsToIgnore(),
                            permissions,
                            forceRecomputeSchemaMatch,
                            task,
                            result);
                });

    }

    private void refreshAfterSuggestionOperationSubmitted(AjaxRequestTarget target) {
        getSwitchToggleModel().setObject(Boolean.TRUE);
        target.add(AttributeMappingsTableWizardPanel.this);
        getTable().refreshAndDetach(target);
        restartTime.accept(target);
    }

    private String getResourceOid() {
        return getAssignmentHolderDetailsModel().getObjectType().getOid();
    }

    public MappingDirection getSelectedMappingType() {
        return isInboundTabSelected ? MappingDirection.INBOUND : MappingDirection.OUTBOUND;
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        buttons.add(createShowOverridesButton(buttons));
    }

    private @NotNull AjaxIconButton createShowOverridesButton(@NotNull RepeatingView buttons) {
        AjaxIconButton showOverrides = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-shuffle"),
                getPageBase().createStringResource("AttributeMappingsTableWizardPanel.showOverrides")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getTable() != null && getTable().isValidFormComponents(target)) {
                    onShowOverrides(target, getSelectedMappingType());
                }
            }
        };
        showOverrides.showTitleAsLabel(true);
        showOverrides.add(AttributeAppender.append("class", "btn  btn-outline-primary"));
        return showOverrides;
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return Objects.requireNonNull(getTable()).isValidFormComponents(target);
    }

    protected abstract void onShowOverrides(AjaxRequestTarget target, MappingDirection selectedMappingType);

    @Override
    protected String getSaveLabelKey() {
        return "AttributeMappingsTableWizardPanel.saveButton";
    }

    @Override
    protected String getSubmitButtonCssClass() {
        return "btn-primary";
    }

    protected void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {
    }

    protected void inEditInboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AttributeMappingsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("AttributeMappingsTableWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    protected ContainerPanelConfigurationType getConfiguration(String panelType) {
        var objectDetailsPageConfiguration = getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration();
        return WebComponentUtil.getContainerConfiguration(objectDetailsPageConfiguration.getObject(), panelType);
    }

    private @Nullable ResourceObjectTypeIdentification getResourceObjectTypeIdentification() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentWrapper = getValueModel().getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        if (parentWrapper == null || parentWrapper.getRealValue() == null) {
            return null;
        }
        ResourceObjectTypeDefinitionType realValue = parentWrapper.getRealValue();
        return ResourceObjectTypeIdentification.of(realValue.getKind(), realValue.getIntent());
    }

    @SuppressWarnings("unchecked")
    public TabbedPanel<ITab> getTabPanel() {
        return (TabbedPanel<ITab>) get(createComponentPath(ID_MAIN_FORM, ID_TAB_TABLE));
    }

    @SuppressWarnings("unchecked")
    protected @Nullable SmartMappingTable<MappingType> getTable() {
        Component component = getTabPanel().get(TabbedPanel.TAB_PANEL_ID);
        return component instanceof SmartMappingTable<?>
                ? (SmartMappingTable<MappingType>) component
                : null;
    }

    protected SmartAlertGeneratingPanel getAiPanel() {
        return (SmartAlertGeneratingPanel) get(createComponentPath(ID_MAIN_FORM, ID_AI_PANEL));
    }

    protected void redirectToSimulationTasksWizard(AjaxRequestTarget target) {
    }

    protected void buildSimulationResultPanel(AjaxRequestTarget target, IModel<SimulationResultType> simulationResultTypeIModel) {

    }

    @Override
    protected String getButtonContainerAdditionalCssClass() {
        return "col-12";
    }

    @Override
    protected String getExitButtonCssClass() {
        return "btn btn-link";
    }
}
