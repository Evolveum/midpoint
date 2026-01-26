/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.createMappingsValueIfRequired;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeCorrelationTypeSuggestionNew;
import static com.evolveum.midpoint.web.session.UserProfileStorage.TableId.TABLE_SMART_CORRELATION;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;

import com.evolveum.midpoint.web.session.SuggestionsStorage;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartAlertGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingAlertDto;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.component.SimulationActionTaskButton;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.dialog.RequestDetailsRecordDto;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavor;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavors;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "rw-correlationRules")
@PanelInstance(identifier = "rw-correlationRules",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "CorrelationWizardPanelWizardPanel.headerLabel", icon = "fa fa-code-branch"))
public abstract class CorrelationItemsTableWizardPanel extends AbstractResourceWizardBasicPanel<CorrelationDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItemsTableWizardPanel.class);
    private static final String CLASS_DOT = CorrelationItemsTableWizardPanel.class.getName() + ".";
    private static final String OP_SUGGEST_CORRELATION_RULES = CLASS_DOT + "suggestCorrelationRules";

    private static final String ID_NOT_SHOWN_CONTAINER_INFO = "notShownContainerInfo";
    private static final String PANEL_TYPE = "rw-correlationRules";

    private static final String ID_AI_PANEL = "aiPanel";
    private static final String ID_TABLE = "table";

    IModel<Boolean> switchToggleModel = Model.of(Boolean.TRUE);

    public CorrelationItemsTableWizardPanel(
            String id,
            WizardPanelHelper<CorrelationDefinitionType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initSwitchSuggestionModel();
        initLayout();
    }

    private void initSwitchSuggestionModel() {
        switchToggleModel = SmartIntegrationUtils.createSuggestionSwitchModel(getPageBase(),
                SuggestionsStorage.SuggestionType.CORRELATION);
    }

    private void initLayout() {

        ResourceDetailsModel detailsModel = getHelper().getDetailsModel();
        ResourceType resource = detailsModel.getObjectType();
        String resourceOid = resource.getOid();

        initCorrelationPanel(resourceOid);

        Label info = new Label(
                ID_NOT_SHOWN_CONTAINER_INFO,
                getPageBase().createStringResource("CorrelationItemsTableWizardPanel.notShownContainer"));
        info.setOutputMarkupId(true);
        info.add(new VisibleBehaviour(this::isNotShownContainerInfo));
        add(info);
    }

    private void initCorrelationPanel(String resourceOid) {
        SmartAlertGeneratingPanel aiPanel = createSmartAlertGeneratingPanel(resourceOid, switchToggleModel);
        add(aiPanel);

        SmartCorrelationTable table = createSmartCorrelationTable(switchToggleModel);
        add(table);
    }

    protected LoadableModel<StatusInfo<?>> loadExistingSuggestion() {
        ResourceDetailsModel detailsModel = getHelper().getDetailsModel();
        ResourceType resource = detailsModel.getObjectType();
        String resourceOid = resource.getOid();

        Task task = getPageBase().createSimpleTask("Load generation statusInfo");
        OperationResult result = task.getResult();
        return new LoadableModel<>() {
            @Override
            protected StatusInfo<CorrelationSuggestionsType> load() {
                var resourceObjectTypeIdentification = getResourceObjectTypeIdentification();
                return loadCorrelationTypeSuggestion(getPageBase(), resourceOid, resourceObjectTypeIdentification, task, result);
            }
        };
    }

    private SmartAlertGeneratingPanel getAIPanel() {
        return (SmartAlertGeneratingPanel) get(ID_AI_PANEL);
    }

    private @NotNull SmartCorrelationTable createSmartCorrelationTable(IModel<Boolean> switchToggleModel) {
        SmartCorrelationTable table = new SmartCorrelationTable(
                ID_TABLE,
                TABLE_SMART_CORRELATION,
                Model.of(ViewToggle.TILE),
                switchToggleModel,
                getValueModel()) {

            @Override
            public void refreshAndDetach(AjaxRequestTarget target) {
                super.refreshAndDetach(target);

                if (getFeedback().hasErrorMessage()) {
                    target.add(CorrelationItemsTableWizardPanel.this);
                }

                if (displayNoValuePanel()) {
                    getSwitchToggleModel().setObject(Boolean.FALSE);
                }

                target.add(getAIPanel());
            }

            @Override
            public boolean displayNoValuePanel() {
                return super.displayNoValuePanel() && !switchToggleModel.getObject();
            }


            @Override
            protected @NotNull List<Component> createToolbarButtonsList(String idButton) {
                List<Component> toolbarButtonsList = super.createToolbarButtonsList(idButton);
                AjaxIconButton generateButton = new AjaxIconButton(idButton, new Model<>(GuiStyleConstants.CLASS_MAGIC_WAND),
                        () -> isSuggestionExists(loadExistingSuggestion().getObject())
                                ? createStringResource("Suggestion.button.showSuggest").getString()
                                : createStringResource("Suggestion.button.suggest").getString()) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (isSuggestionExists(loadExistingSuggestion().getObject())) {
                            getSwitchToggleModel().setObject(Boolean.TRUE);
                        } else {
                            onSuggestNewPerformed(target);
                        }

                        target.add(CorrelationItemsTableWizardPanel.this);
                        refreshAndDetach(target);
                    }
                };
                generateButton.add(new VisibleBehaviour(this::displayNoValuePanel));
                generateButton.add(AttributeModifier.append("class", "btn btn-default text-ai rounded"));
                generateButton.setOutputMarkupId(true);
                generateButton.showTitleAsLabel(true);

                toolbarButtonsList.add(generateButton);
                return toolbarButtonsList;
            }

            @Override
            public void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    boolean isDuplicate) {

                if (rowModel == null) {
                    PrismContainerValueWrapper<ItemsSubCorrelatorType> newValue = createNewItemsSubCorrelatorValue(
                            getPageBase(), null, target);
                    showTableForItemRefs(target, this::findResourceObjectTypeDefinition, () -> newValue, null);
                    return;
                }

                if (isDuplicate && rowModel.getObject() != null) {
                    PrismContainerValueWrapper<ItemsSubCorrelatorType> object = rowModel.getObject();
                    PrismContainerValueWrapper<ItemsSubCorrelatorType> newValue = createNewItemsSubCorrelatorValue(
                            getPageBase(), object.getNewValue(), target);
                    showTableForItemRefs(target, this::findResourceObjectTypeDefinition, () -> newValue, null);
                }

                showTableForItemRefs(target, this::findResourceObjectTypeDefinition, rowModel, null);
            }

            @Override
            public void viewEditItemPerformed(AjaxRequestTarget target,
                    @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    StatusInfo<CorrelationSuggestionsType> statusInfo) {
                if (rowModel.getObject() == null || rowModel.getObject().getRealValue() == null) {
                    return;
                }
                showTableForItemRefs(target, this::findResourceObjectTypeDefinition, rowModel, statusInfo);
            }

            @Override
            public void acceptSuggestionItemPerformed(AjaxRequestTarget target,
                    @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    StatusInfo<CorrelationSuggestionsType> statusInfo) {
                PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> resourceObjectTypeDefinition =
                        findResourceObjectTypeDefinition();
                CorrelationItemsTableWizardPanel.this.acceptSuggestionItemPerformed(
                        getPageBase(), target, rowModel, () -> resourceObjectTypeDefinition, statusInfo);
            }

            @Override
            protected ResourceType getResourceType() {
                return getAssignmentHolderDetailsModel().getObjectType();
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void newItemPerformed(
                    PrismContainerValue<ItemsSubCorrelatorType> value,
                    AjaxRequestTarget target,
                    AssignmentObjectRelation relationSpec,
                    boolean isDuplicate,
                    StatusInfo<?> statusInfo) {
                PrismContainerValueWrapper<ItemsSubCorrelatorType> newValue = createNewItemsSubCorrelatorValue(
                        getPageBase(), value, target);
                showTableForItemRefs(target, this::findResourceObjectTypeDefinition,
                        () -> newValue, (StatusInfo<CorrelationSuggestionsType>) statusInfo);
            }
        };

        table.setOutputMarkupId(true);
        return table;
    }

    private @NotNull SmartAlertGeneratingPanel createSmartAlertGeneratingPanel(
            String resourceOid,
            @NotNull IModel<Boolean> switchToggleModel) {
        SmartAlertGeneratingPanel aiPanel = new SmartAlertGeneratingPanel(ID_AI_PANEL,
                () -> new SmartGeneratingAlertDto(loadExistingSuggestion(), switchToggleModel, getPageBase())) {
            @Override
            protected void performSuggestOperation(AjaxRequestTarget target) {
                ResourceObjectTypeIdentification objectTypeIdentification = getResourceObjectTypeIdentification();
                SmartIntegrationService service = getPageBase().getSmartIntegrationService();
                getPageBase().taskAwareExecutor(target, OP_SUGGEST_CORRELATION_RULES)
                        .withOpResultOptions(OpResult.Options.create()
                                .withHideSuccess(true)
                                .withHideInProgress(true))
                        .runVoid((task, result) -> service
                                .submitSuggestCorrelationOperation(resourceOid, objectTypeIdentification, task, result));
            }

            @Override
            protected @NotNull IModel<RequestDetailsRecordDto> getPermissionRecordDtoIModel() {
                return () -> new RequestDetailsRecordDto(null,
                        RequestDetailsRecordDto.initDummyCorrelationPermissionData());
            }

            @Override
            protected void refreshAssociatedComponents(@NotNull AjaxRequestTarget target) {
                SmartCorrelationTable smartMappingTable = getTable();
                smartMappingTable.refreshAndDetach(target);
            }

            @Override
            protected void onFinishActionPerform(AjaxRequestTarget target) {
                getTable().refreshAndDetach(target);
            }
        };

        aiPanel.add(new VisibleBehaviour(switchToggleModel::getObject));
        aiPanel.setOutputMarkupId(true);
        aiPanel.setOutputMarkupPlaceholderTag(true);
        return aiPanel;
    }

    protected ResourceObjectTypeIdentification getResourceObjectTypeIdentification() {
        var valueWrapper = getValueModel().getObject();
        var parentContainerValue = valueWrapper.getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        if (parentContainerValue == null || parentContainerValue.getRealValue() == null) {
            return null;
        }
        ResourceObjectTypeDefinitionType resourceObjectTypeDef = parentContainerValue.getRealValue();
        return ResourceObjectTypeIdentification.of(resourceObjectTypeDef.getKind(), resourceObjectTypeDef.getIntent());
    }

    protected void acceptSuggestionItemPerformed(PageBase pageBase, AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
            StatusInfo<CorrelationSuggestionsType> statusInfo) {
        PrismContainerValueWrapper<CorrelationSuggestionType> parentSuggestionW = rowModel.getObject()
                .getParentContainerValue(CorrelationSuggestionType.class);

        if (statusInfo == null || parentSuggestionW == null || parentSuggestionW.getRealValue() == null) {
            pageBase.warn("Correlation suggestion is not available.");
            target.add(getPageBase().getFeedbackPanel().getParent());
            return;
        }

        List<ResourceAttributeDefinitionType> attributes = collectRequiredResourceAttributeDefs(pageBase, target, parentSuggestionW);

        if (attributes.isEmpty()) {
            performAddOperation(pageBase, target, resourceObjectTypeDefinition, attributes, rowModel, statusInfo);
            return;
        }

        CorrelationAddMappingConfirmationPanel confirmationPanel = new CorrelationAddMappingConfirmationPanel(
                pageBase.getMainPopupBodyId(), Model.of(), () -> attributes) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                performAddOperation(pageBase, target, resourceObjectTypeDefinition, attributes, rowModel, statusInfo);
            }
        };
        pageBase.showMainPopup(confirmationPanel, target);
    }

    protected void performAddOperation(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDef,
            @Nullable List<ResourceAttributeDefinitionType> attributes,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
            @NotNull StatusInfo<CorrelationSuggestionsType> statusInfo) {
        createMappingsValueIfRequired(pageBase, resourceObjectTypeDef, attributes, getAssignmentHolderDetailsModel());
        PrismContainerValueWrapper<ItemsSubCorrelatorType> object = valueModel.getObject();
        createNewItemsSubCorrelatorValue(pageBase, object.getNewValue().clone(), target);
        performDiscard(pageBase, target, valueModel, statusInfo);
        postProcessAddSuggestion(target);
    }

    protected void postProcessAddSuggestion(AjaxRequestTarget target) {
        getTable().refreshAndDetach(target);
    }

    protected void performDiscard(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
            @NotNull StatusInfo<CorrelationSuggestionsType> statusInfo) {
        Task task = pageBase.createSimpleTask("discardSuggestion");
        PrismContainerValueWrapper<CorrelationSuggestionType> parentContainerValue = valueModel.getObject()
                .getParentContainerValue(CorrelationSuggestionType.class);
        if (parentContainerValue != null && parentContainerValue.getRealValue() != null) {
            CorrelationSuggestionType suggestionToDelete = parentContainerValue.getRealValue();
            removeCorrelationTypeSuggestionNew(pageBase, statusInfo, suggestionToDelete, task, task.getResult());
        }
        target.add(this);
    }

    protected PrismContainerValueWrapper<ItemsSubCorrelatorType> createNewItemsSubCorrelatorValue(
            PageBase pageBase,
            PrismContainerValue<ItemsSubCorrelatorType> value,
            AjaxRequestTarget target) {
        return SmartIntegrationWrapperUtils.createNewItemsSubCorrelatorValue(
                pageBase, getValueModel(), value, target);
    }

    private @NotNull Boolean isNotShownContainerInfo() {
        PrismContainerValueWrapper<CorrelationDefinitionType> objectType = getValueModel().getObject();
        if (objectType != null) {
            try {
                PrismContainerWrapper<Containerable> correlators = objectType.findContainer(
                        ItemPath.create(CorrelationDefinitionType.F_CORRELATORS));
                if (correlators != null) {
                    PrismContainerValueWrapper<Containerable> correlatorsValue = correlators.getValue();
                    if (correlatorsValue != null) {
                        for (PrismContainerWrapper<? extends Containerable> container : correlatorsValue.getContainers()) {
                            if (container == null
                                    || container.isOperational()
                                    || container.getItemName().equivalent(CompositeCorrelatorType.F_ITEMS)
                                    || container.getItemName().equivalent(CompositeCorrelatorType.F_EXTENSION)) {
                                continue;
                            }

                            @SuppressWarnings("deprecation")
                            PrismContainer<? extends Containerable> cloneContainer = container.getItem().clone();
                            WebPrismUtil.cleanupEmptyContainers(cloneContainer);
                            if (!cloneContainer.isEmpty()) {
                                return true;
                            }
                        }
                    }
                }
            } catch (SchemaException e) {
                LOGGER.debug("Couldn't find correlators container in {}", objectType);
            }
        }
        return false;
    }

    protected abstract void showTableForItemRefs(
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
            @Nullable StatusInfo<CorrelationSuggestionsType> statusInfo);

    @Override
    protected String getSaveLabelKey() {
        return "CorrelationWizardPanelWizardPanel.saveButton";
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        PageBase pageBase = getPageBase();
        super.onSubmitPerformed(target);
        displaySynchronizationDialog(pageBase, target);
    }

    protected void displaySynchronizationDialog(@NotNull PageBase pageBase, AjaxRequestTarget target) {
        ResourceObjectTypeDefinitionType resourceObjectDefinition = getResourceObjectDefinition();
        if (resourceObjectDefinition == null) {
            return;
        }
        SynchronizationReactionsType synchronization = getResourceObjectDefinition().getSynchronization();
        if (synchronization == null
                || synchronization.getReaction() == null
                || synchronization.getReaction().isEmpty()) {

            ConfirmationPanel confirmPanel = new ConfirmationPanel(
                    pageBase.getMainPopupBodyId(),
                    pageBase.createStringResource("CorrelationWizardPanelWizardPanel.noSynchronization.info")) {
                @Override
                public void yesPerformed(AjaxRequestTarget target) {
                    navigateToSynchronizationPanel(target);
                }
            };
            pageBase.showMainPopup(confirmPanel, target);
        }
    }

    protected void navigateToSynchronizationPanel(AjaxRequestTarget target) {
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        SimulationActionTaskButton<?> simulationActionTaskButton = createSimulationMenuButton(buttons);
        buttons.add(simulationActionTaskButton);
    }

    private @NotNull SimulationActionTaskButton<SimulatedCorrelatorsType> createSimulationMenuButton(
            @NotNull RepeatingView buttons) {

        SimulationActionTaskButton<SimulatedCorrelatorsType> simulationActionTaskButton =
                new SimulationActionTaskButton<>(buttons.newChildId(), this::getResourceObjectDefinition,
                        () -> getAssignmentHolderDetailsModel().getObjectType()) {
                    @Override
                    protected boolean isSamplingEnabled() {
                        return true;
                    }

                    @Override
                    public void redirectToSimulationTasksWizard(AjaxRequestTarget target) {
                        CorrelationItemsTableWizardPanel.this.redirectToSimulationTasksWizard(target);
                    }

                    @Override
                    protected @NotNull ResourceTaskFlavor<SimulatedCorrelatorsType> getTaskFlavor() {
                        return ResourceTaskFlavors.CORRELATION_PREVIEW_ACIVITY;
                    }

                    @Override
                    protected SimulatedCorrelatorsType getWorkDefinitionConfiguration() {
                        return new SimulatedCorrelatorsType()
                                .includeExistingCorrelators(true);
                    }
                };

        simulationActionTaskButton.setRenderBodyOnly(true);
        return simulationActionTaskButton;
    }

    protected void redirectToSimulationTasksWizard(AjaxRequestTarget target) {

    }

    private @Nullable ResourceObjectTypeDefinitionType getResourceObjectDefinition() {
        var parentContainerValue = getResourceObjectTypeDefinitionWrapper();
        return parentContainerValue != null ? parentContainerValue.getRealValue() : null;
    }

    private PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> getResourceObjectTypeDefinitionWrapper() {
        var valueWrapper = getValueModel().getObject();
        return valueWrapper.getParentContainerValue(ResourceObjectTypeDefinitionType.class);
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.subText");
    }

    protected SmartCorrelationTable getTable() {
        return (SmartCorrelationTable) get(ID_TABLE);
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }
}
