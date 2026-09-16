/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.createVirtualMappingContainerModel;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.isSuggestionExists;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectTypeMappingTypeSuggestion;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.processSuggestedContainerValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.ContainerDrawerInfoModel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.ContainerDrawerPanel;
import com.evolveum.midpoint.gui.impl.page.admin.FormWrapperValidator;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.table.SmartMappingTable;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartAlertGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartSuggestButtonWithConfirmation;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingAlertDto;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.TabSeparatedTabbedPanel;
import com.evolveum.midpoint.web.component.dialog.AdditionalOperationConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationOption;
import com.evolveum.midpoint.web.component.dialog.SuggestionOption;
import com.evolveum.midpoint.web.component.dialog.privacy.DataAccessPermission;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.ButtonWithConfirmationOptionsDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemBuilder;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.SuggestionsStorage;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAttributeMappingsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DataAccessPermissionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * @author lskublik
 */
@PanelType(name = "rw-attribute-mappings")
@PanelInstance(
        identifier = "rw-attribute-inbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(
                label = "AttributeMappingsTableWizardPanel.inboundTable",
                icon = "fa fa-arrow-right-to-bracket"))
@PanelInstance(
        identifier = "rw-attribute-outbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(
                label = "AttributeMappingsTableWizardPanel.outboundTable",
                icon = "fa fa-arrow-right-from-bracket"))
public abstract class AttributeMappingsTableWizardPanel<P extends Containerable>
        extends AbstractResourceWizardBasicPanel<P> {

    private static final Trace LOGGER = TraceManager.getTrace(AttributeMappingsTableWizardPanel.class);
    private static final String CLASS_DOT = AttributeMappingsTableWizardPanel.class.getName() + ".";
    private static final String OP_SUGGEST_MAPPING = CLASS_DOT + "suggestMapping";
    private static final String OP_LOAD_SUGGESTION = CLASS_DOT + "loadSuggestion";

    private static final String ID_MAIN_FORM = "form";
    private static final String ID_AI_PANEL = "aiPanel";
    private static final String ID_TAB_TABLE = "panel";
    private static final String ID_MESSAGE_PANEL = "messagePanel";

    private final MappingDirection initialTab;

    private IModel<Boolean> inboundSuggestionToggleModel = Model.of(Boolean.FALSE);
    private IModel<Boolean> outboundSuggestionToggleModel = Model.of(Boolean.FALSE);
    private boolean isInboundTabSelected;
    private SerializableConsumer<AjaxRequestTarget> restartTime;

    private final LoadableDetachableModel<SmartGeneratingAlertDto> suggestionModel =
            new LoadableDetachableModel<>() {

                @Override
                protected @NotNull SmartGeneratingAlertDto load() {
                    if (!Boolean.TRUE.equals(getSwitchToggleModel().getObject())) {
                        return new SmartGeneratingAlertDto(null, getSwitchToggleModel(), getPageBase());
                    }

                    ResourceType resource = getAssignmentHolderDetailsModel().getObjectType();
                    return new SmartGeneratingAlertDto(
                            loadSuggestion(resource.getOid()),
                            getSwitchToggleModel(),
                            getPageBase());
                }
            };

    public AttributeMappingsTableWizardPanel(
            String id,
            WizardPanelHelper<P, ResourceDetailsModel> superHelper,
            MappingDirection initialTab) {
        super(id, superHelper);

        this.initialTab = initialTab;
        this.isInboundTabSelected = initialTab == MappingDirection.INBOUND;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initSwitchSuggestionModel();
        initLayout();
    }

    private void initSwitchSuggestionModel() {
        inboundSuggestionToggleModel = SmartIntegrationUtils.createSuggestionSwitchModel(
                getPageBase(),
                SuggestionsStorage.SuggestionType.INBOUND_MAPPING);

        outboundSuggestionToggleModel = SmartIntegrationUtils.createSuggestionSwitchModel(
                getPageBase(),
                SuggestionsStorage.SuggestionType.OUTBOUND_MAPPING);
    }

    private void initLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        form.setMultiPart(true);

        form.add(createFeedbackPanel());

        //noinspection rawtypes,unchecked
        form.add(new FormWrapperValidator<>(getPageBase()) {

            @Override
            protected PrismObjectWrapper getObjectWrapper() {
                return getAssignmentHolderDetailsModel().getObjectWrapper();
            }
        });

        add(form);

        String resourceOid = getResourceOid();

        SmartAlertGeneratingPanel aiPanel = createSmartAlertGeneratingPanel();
        restartTime = aiPanel::restartTimeBehavior;
        form.add(aiPanel);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(createInboundTableTab(resourceOid, inboundSuggestionToggleModel));
        tabs.add(createOutboundTableTab(resourceOid, outboundSuggestionToggleModel));

        TabSeparatedTabbedPanel<ITab> tabPanel = new TabSeparatedTabbedPanel<>(ID_TAB_TABLE, tabs) {

            @Override
            protected void onAjaxUpdate(@NotNull Optional<AjaxRequestTarget> optional) {
                optional.ifPresent(target -> {
                    SmartAlertGeneratingPanel aiPanel = getAiPanel();
                    aiPanel.stopTimeBehavior(target); //stop old polling
                    suggestionModel.detach(); // force reload for new tab (inbound/outbound)
                    target.add(getButtonsContainer());
                    target.add(aiPanel);
                    getTable().refreshAndDetach(target); // refresh table to update suggestions
                    aiPanel.restartTimeBehavior(target); // restart if needed
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

    private MessagePanel<?> createFeedbackPanel() {
        MessagePanel<String> warning = new MessagePanel<>(
                ID_MESSAGE_PANEL,
                MessagePanel.MessagePanelType.WARN,
                () -> createStringResource("AttributeMappingsTableWizardPanel.iteration.unused").getString(),
                false);

        warning.setOutputMarkupId(true);
        warning.setOutputMarkupPlaceholderTag(true);
        warning.setEscapeModelStrings(false);
        warning.add(new VisibleBehaviour(this::isIterationUnused));

        return warning;
    }

    private void switchTabs(TabSeparatedTabbedPanel<ITab> tabPanel) {
        switch (initialTab) {
            case INBOUND:
                isInboundTabSelected = true;
                tabPanel.setSelectedTab(0);
                break;
            case OUTBOUND:
                isInboundTabSelected = false;
                tabPanel.setSelectedTab(1);
                break;
        }
    }

    private @NotNull ITab createInboundTableTab(String resourceOid, IModel<Boolean> switchToggleModel) {
        return new IconPanelTab(getPageBase().createStringResource("AttributeMappingsTableWizardPanel.inboundTable"),
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
                getPageBase().createStringResource("AttributeMappingsTableWizardPanel.outboundTable"),
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

    private @NotNull SmartMappingTable<P> createSmartMappingTable(String panelId,
            IModel<Boolean> suggestionToggle,
            String resourceOid,
            MappingDirection direction) {

        SmartMappingTable<P> table = new SmartMappingTable<>(
                panelId,
                () -> direction,
                suggestionToggle,
                getValueModel(),
                resourceOid) {

            @Override
            protected void performOnEditMapping(
                    @NotNull AjaxRequestTarget target, @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                editMapping(direction, rowModel, target);
            }

            @Override
            public void acceptSuggestionItemPerformed(
                    @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel, @NotNull AjaxRequestTarget target) {
                processSuggestionAcceptance(this, rowModel.getObject(), target);
            }

            @Override
            public void refreshAndDetach(AjaxRequestTarget target) {
                suggestionModel.detach();
                super.refreshAndDetach(target);

                if (displayNoValuePanel()) {
                    suggestionToggle.setObject(false);
                }

                refreshAssociatedComponents(target);
            }

            public void refreshAssociatedComponents(AjaxRequestTarget target) {
                target.add(getAiPanel());
                target.add(getMessagePanel());
            }

            @Override
            protected @NotNull List<InlineMenuItem> getCustomSettingsMenuItems() {
                return createSettingsMenuItems(direction);
            }

            @Override
            protected void addAdditionalNoValueToolbarButtons(@NotNull List<Component> buttons, String buttonId) {
                addSuggestionButtons(this, buttons, buttonId, resourceOid);
            }

            @Override
            protected void buildSimulationResultPanel(AjaxRequestTarget target, IModel<SimulationResultType> resultModel) {
                AttributeMappingsTableWizardPanel.this.buildSimulationResultPanel(target, resultModel);
            }

            @Override
            protected ResourceType getResourceType() {
                return getResourceWithAppliedDelta();
            }
        };

        table.setOutputMarkupId(true);
        table.add(AttributeAppender.append("class", "p-0"));

        return table;
    }

    private void editMapping(
            MappingDirection direction,
            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            AjaxRequestTarget target) {

        if (direction == MappingDirection.INBOUND) {
            inEditInboundValue(rowModel, target);
        } else {
            inEditOutboundValue(rowModel, target);
        }
    }

    private @Nullable ResourceType getResourceWithAppliedDelta() {
        PrismObjectWrapper<ResourceType> wrapper = getAssignmentHolderDetailsModel().getObjectWrapper();

        try {
            PrismObject<ResourceType> resource = wrapper.getObjectApplyDelta();
            return resource != null ? resource.asObjectable() : null;
        } catch (CommonException e) {
            LOGGER.error("Couldn't get resource with applied delta. Returning the original resource.", e);
            return getAssignmentHolderDetailsModel().getObjectType();
        }
    }

    private void addSuggestionButtons(
            @NotNull SmartMappingTable<P> table,
            @NotNull List<Component> buttons,
            @NotNull String buttonId,
            @NotNull String resourceOid) {

        buttons.add(createGenerateSuggestionButton(table, buttonId, resourceOid));
        buttons.add(createShowSuggestionsButton(table, buttonId, resourceOid));
    }

    private @NotNull AjaxIconButton createGenerateSuggestionButton(
            @NotNull SmartMappingTable<P> table,
            @NotNull String id,
            @NotNull String resourceOid) {

        AjaxIconButton button = SmartSuggestButtonWithConfirmation.create(
                id,
                createStringResource("Suggestion.button.suggest"),
                () -> GuiStyleConstants.CLASS_MAGIC_WAND,
                SuggestionOption.of(ConfirmationOption.mappingPermissionsOptions()),
                () -> new ButtonWithConfirmationOptionsDialog.ButtonHandlers<>(
                        target -> {
                        },
                        (target, confirmedOptions) -> {
                            performSuggestOperation(target, confirmedOptions, false);
                            refreshAfterSuggestionOperationSubmitted(target);
                        }),
                getPageBase());

        button.add(new VisibleBehaviour(() ->
                table.displayNoValuePanel() && !hasSuggestion(resourceOid)));

        button.setOutputMarkupId(true);
        button.showTitleAsLabel(true);

        return button;
    }

    private @NotNull AjaxIconButton createShowSuggestionsButton(
            @NotNull SmartMappingTable<P> table,
            @NotNull String id,
            @NotNull String resourceOid) {

        AjaxIconButton button = new AjaxIconButton(
                id,
                () -> GuiStyleConstants.CLASS_MAGIC_WAND,
                () -> createStringResource("Suggestion.button.showSuggest").getString()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getSwitchToggleModel().setObject(Boolean.TRUE);

                target.add(AttributeMappingsTableWizardPanel.this);
                table.refreshAndDetach(target);
            }
        };

        button.add(new VisibleBehaviour(() ->
                table.displayNoValuePanel() && hasSuggestion(resourceOid)));

        button.add(AttributeModifier.append("class", "btn btn-purple"));
        button.setOutputMarkupId(true);
        button.showTitleAsLabel(true);

        return button;
    }

    private boolean hasSuggestion(@NotNull String resourceOid) {
        StatusInfo<?> status = loadSuggestion(resourceOid).getObject();
        return isSuggestionExists(status);
    }

    private void processSuggestionAcceptance(
            SmartMappingTable<P> table,
            PrismContainerValueWrapper<MappingType> suggestedMapping,
            AjaxRequestTarget target) {

        StatusInfo<?> status = table.getStatusInfo(suggestedMapping);

        if (status == null || !(status.getResult() instanceof MappingsSuggestionType suggestion)) {
            return;
        }

        IterationSpecificationType suggestedIteration = suggestion.getIterationSpecification();
        if (suggestedIteration != null) {
            //noinspection unchecked
            WebPrismUtil.cleanupEmptyContainerValue(suggestedIteration.asPrismContainerValue());
        }

        IterationSpecificationType currentIteration = getCurrentIteration();

        MappingType mapping = suggestedMapping.getRealValue();
        ExpressionType expression = mapping != null ? mapping.getExpression() : null;

        boolean iterationConfirmationRequired =
                ExpressionUtil.usesIterationVariables(expression)
                        && suggestedIteration != null
                        && !Objects.equals(currentIteration, suggestedIteration);

        if (iterationConfirmationRequired) {
            showIterationConfirmation(table, suggestedMapping, suggestedIteration, target);
            return;
        }

        acceptMappingSuggestion(table, suggestedMapping, target);
    }

    private void acceptMappingSuggestion(
            SmartMappingTable<P> table,
            PrismContainerValueWrapper<MappingType> suggestedMapping,
            AjaxRequestTarget target) {

        PrismContainerValueWrapper<MappingType> accepted =
                table.createNewValue(suggestedMapping.getNewValue(), target);

        if (accepted != null) {
            table.getAcceptedSuggestionsCache().add(accepted);
        }

        table.deleteItemPerform(suggestedMapping);
        table.refreshAndDetach(target);
    }

    private void showIterationConfirmation(
            SmartMappingTable<P> table,
            PrismContainerValueWrapper<MappingType> suggestedMapping,
            IterationSpecificationType suggestedIteration,
            AjaxRequestTarget target) {

        AdditionalOperationConfirmationPanel dialog =
                new AdditionalOperationConfirmationPanel(
                        getPageBase().getMainPopupBodyId(),
                        createStringResource("SmartMappingTable.confirmationMessage.iteration")) {

                    @Override
                    protected void performOnProcess(AjaxRequestTarget target) {
                        applySuggestedIteration(suggestedIteration, target);
                        finishAcceptance(table, suggestedMapping, target);
                    }

                    @Override
                    protected IModel<String> getProcessButtonLabel() {
                        return createStringResource(
                                "SmartMappingTable.confirmationMessage.iteration.apply");
                    }

                    @Override
                    protected IModel<String> createYesLabel() {
                        return createStringResource(
                                "SmartMappingTable.confirmationMessage.iteration.keep.existing");
                    }

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        finishAcceptance(table, suggestedMapping, target);
                    }

                    @Override
                    public int getWidth() {
                        return 40;
                    }

                    @Override
                    public String getWidthUnit() {
                        return "%";
                    }
                };

        getPageBase().showMainPopup(dialog, target);
    }

    private void finishAcceptance(
            SmartMappingTable<P> table,
            PrismContainerValueWrapper<MappingType> suggestedMapping,
            AjaxRequestTarget target) {

        acceptMappingSuggestion(table, suggestedMapping, target);
        getPageBase().hideMainPopup(target);
    }

    private boolean isIterationUnused() {
        if (!iterationExists()) {
            return false;
        }

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentWrapper = getParentWrapper();
        if (parentWrapper == null) {
            return false;
        }

        try {
            PrismContainerWrapper<ResourceAttributeDefinitionType> attributes =
                    parentWrapper.findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);

            if (attributes == null) {
                return true;
            }

            for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> attribute : attributes.getValues()) {
                if (containsIterationMapping(attribute.findContainer(ResourceAttributeDefinitionType.F_INBOUND))
                        || containsIterationMapping(attribute.findContainer(ResourceAttributeDefinitionType.F_OUTBOUND))) {
                    return false;
                }
            }

            return true;
        } catch (SchemaException e) {
            throw new SystemException("Couldn't check iteration usage", e);
        }
    }

    private boolean iterationExists() {
        PrismContainerWrapper<IterationSpecificationType> iterationContainer = loadIterationWrapper();

        if (iterationContainer == null) {
            return false;
        }

        return iterationContainer.getValues().stream()
                .anyMatch(this::isMeaningfulIterationValue);
    }

    private boolean isMeaningfulIterationValue(
            @NotNull PrismContainerValueWrapper<IterationSpecificationType> value) {

        if (value.getStatus() == ValueStatus.DELETED || value.getNewValue() == null) {
            return false;
        }

        PrismContainerValue<IterationSpecificationType> clone = value.getNewValue().clone();
        WebPrismUtil.cleanupEmptyContainerValue(clone);

        return !clone.isEmpty();
    }

    private boolean containsIterationMapping(@Nullable PrismContainerWrapper<MappingType> mappings) {
        if (mappings == null) {
            return false;
        }

        return mappings.getValues().stream()
                .filter(mapping -> mapping.getStatus() != ValueStatus.DELETED)
                .map(PrismContainerValueWrapper::getRealValue)
                .filter(Objects::nonNull)
                .map(MappingType::getExpression)
                .anyMatch(ExpressionUtil::usesIterationVariables);
    }

    private @Nullable IterationSpecificationType getCurrentIteration() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType = getParentWrapper();

        IterationSpecificationType iteration = objectType != null && objectType.getRealValue() != null
                ? objectType.getRealValue().getIteration()
                : null;

        if (iteration != null) {
            //noinspection unchecked
            WebPrismUtil.cleanupEmptyContainerValue(iteration.asPrismContainerValue());
        }

        return iteration;
    }

    private void applySuggestedIteration(
            IterationSpecificationType suggestedIteration,
            AjaxRequestTarget target) {

        PrismContainerWrapper<IterationSpecificationType> iterationContainer = loadIterationWrapper();
        if (iterationContainer == null) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            PrismContainerValue<IterationSpecificationType> newValue =
                    processSuggestedContainerValue(suggestedIteration.asPrismContainerValue());

            newValue.setId(null);
            WebPrismUtil.cleanupEmptyContainerValue(newValue);

            PrismContainerValueWrapper<IterationSpecificationType> newWrapper =
                    WebPrismUtil.createNewValueWrapper(
                            iterationContainer,
                            newValue,
                            getPageBase(),
                            target);

            newWrapper.setStatus(ValueStatus.ADDED);
            iterationContainer.getItem().setValue(newValue);
            iterationContainer.getValues().clear();
            iterationContainer.getValues().add(newWrapper);

        } catch (SchemaException e) {
            throw new SystemException("Couldn't apply suggested iteration", e);
        }
    }

    private @Nullable PrismContainerWrapper<IterationSpecificationType> loadIterationWrapper() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType = getParentWrapper();
        if (objectType == null) {
            return null;
        }

        try {
            return objectType.findContainer(ResourceObjectTypeDefinitionType.F_ITERATION);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't get iteration specification object.", e);
        }
    }

    protected @Nullable PrismContainerValueWrapper<IterationSpecificationType> loadIterationSettingValueWrapper() {
        PrismContainerWrapper<IterationSpecificationType> iterationContainer = loadIterationWrapper();
        try {
            return iterationContainer != null ? iterationContainer.getValue() : null;
        } catch (SchemaException e) {
            throw new SystemException("Couldn't get iteration specification object.", e);
        }
    }

    private void removeUnusedIteration() {
        PrismContainerWrapper<IterationSpecificationType> iterationContainer = loadIterationWrapper();

        if (iterationContainer == null) {
            return;
        }

        try {
            iterationContainer.removeAll(getPageBase());
        } catch (SchemaException e) {
            throw new SystemException("Couldn't remove unused iteration specification object.", e);
        }
    }

    private @NotNull List<InlineMenuItem> createSettingsMenuItems(MappingDirection direction) {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parent = getParentWrapper();

        if (parent == null || parent.getRealValue() == null) {
            return List.of();
        }

        return List.of(
                createIterationSettingsInlineMenu(),
                createAttributeOverridesSettingsMenu(direction));
    }

    @NotNull InlineMenuItem createIterationSettingsInlineMenu() {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-cogs")
                .label(createStringResource("SmartMappingTable.button.iterationSettings"))
                .action(new InlineMenuItemAction() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ContainerDrawerInfoModel<IterationSpecificationType> drawerModel =
                                new ContainerDrawerInfoModel<>(
                                        AttributeMappingsTableWizardPanel.this::loadIterationSettingValueWrapper,
                                        null) {

                                    @Override
                                    protected void onYesPerformed(AjaxRequestTarget target) {
                                        super.onYesPerformed(target);
                                        getTable().refreshAssociatedComponents(target);
                                    }

                                    @Override
                                    protected void customizePanel(ContainerDrawerPanel<?> components) {
                                        components.info(translate("IterationSettings.description"));
                                    }

                                    @Override
                                    protected IModel<String> getDescription() {
                                        return createStringResource("IterationSettings.definition.info");
                                    }

                                    @Override
                                    protected IModel<String> getTitle() {
                                        return createStringResource(
                                                "IterationSettings.button.iterationSettings");
                                    }
                                };

                        getPageBase().showDrawer(drawerModel, target);
                    }
                })
                .visibilityChecker((rowModel, isHeader) -> isHeader)
                .buildInlineMenu();
    }

    private @NotNull InlineMenuItem createAttributeOverridesSettingsMenu(
            @NotNull MappingDirection direction) {

        return InlineMenuItemBuilder.create()
                .icon("fa fa-shuffle")
                .label(createStringResource("AttributeMappingsTableWizardPanel.showOverrides"))
                .action(new InlineMenuItemAction() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SmartMappingTable<?> table = getTable();

                        if (table.isValidFormComponents(target)) {
                            onShowOverrides(target, direction);
                        }
                    }
                })
                .buildInlineMenu();
    }

    private PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> getParentWrapper() {
        return getValueModel().getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);
    }

    private void performSuggestOperation(
            AjaxRequestTarget target,
            IModel<List<ConfirmationOption<DataAccessPermission>>> confirmedOptions,
            boolean forceRecomputeSchemaMatch) {

        List<DataAccessPermissionType> permissions = confirmedOptions.getObject().stream()
                .map(ConfirmationOption::option)
                .map(DataAccessPermission::toSchemaType)
                .toList();

        ResourceObjectTypeIdentification objectTypeIdentification =
                getResourceObjectTypeIdentification();

        if (objectTypeIdentification == null) {
            LOGGER.warn(
                    "Cannot perform suggest mapping operation - no resource object type definition found.");
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

                    service.submitSuggestMappingsOperation(resourceOid, objectTypeIdentification,
                            inbound,
                            getTargetPathsToIgnore(),
                            permissions,
                            forceRecomputeSchemaMatch,
                            task, result);
                });
    }

    private void refreshAfterSuggestionOperationSubmitted(AjaxRequestTarget target) {
        getSwitchToggleModel().setObject(Boolean.TRUE);
        target.add(AttributeMappingsTableWizardPanel.this);
        getTable().refreshAndDetach(target);
        restartTime.accept(target);
    }

    protected LoadableModel<StatusInfo<?>> loadSuggestion(String resourceOid) {
        Task task = getPageBase().createSimpleTask(OP_LOAD_SUGGESTION);
        OperationResult result = task.getResult();

        return new LoadableModel<>() {

            @Override
            protected StatusInfo<MappingsSuggestionType> load() {
                return loadObjectTypeMappingTypeSuggestion(
                        getPageBase(),
                        resourceOid,
                        getResourceObjectTypeIdentification(),
                        getSelectedMappingType(),
                        task,
                        result);
            }
        };
    }

    private @NotNull SmartAlertGeneratingPanel createSmartAlertGeneratingPanel() {

        SmartAlertGeneratingPanel aiPanel = new SmartAlertGeneratingPanel(
                ID_AI_PANEL,
                suggestionModel) {

            @Override
            protected void performSuggestOperation(
                    AjaxRequestTarget target,
                    IModel<List<ConfirmationOption<DataAccessPermission>>> confirmedOptions) {

                AttributeMappingsTableWizardPanel.this.performSuggestOperation(target, confirmedOptions, false);
                refreshAfterSuggestionOperationSubmitted(target);
            }

            @Override
            protected void performRegenerateSuggestOperation(
                    AjaxRequestTarget target,
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

                List<ConfirmationOption<DataAccessPermission>> confirmationOptions =
                        ConfirmationOption.mappingPermissionsOptions();

                return () -> confirmationOptions;
            }

            @Override
            protected void onRefresh(@NotNull AjaxRequestTarget target) {
                getTable().refreshAndDetach(target);
            }
        };

        aiPanel.setOutputMarkupId(true);
        aiPanel.setOutputMarkupPlaceholderTag(true);
        aiPanel.add(new VisibleBehaviour(() ->
                Boolean.TRUE.equals(getSwitchToggleModel().getObject())));

        return aiPanel;
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
                throw new SystemException("Error retrieving ref property from mapping", e);
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

    private String getResourceOid() {
        return getAssignmentHolderDetailsModel().getObjectType().getOid();
    }

    public MappingDirection getSelectedMappingType() {
        return isInboundTabSelected ? MappingDirection.INBOUND : MappingDirection.OUTBOUND;
    }

    private @Nullable ResourceObjectTypeIdentification getResourceObjectTypeIdentification() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentWrapper =
                getParentWrapper();

        if (parentWrapper == null || parentWrapper.getRealValue() == null) {
            return null;
        }

        ResourceObjectTypeDefinitionType realValue = parentWrapper.getRealValue();
        return ResourceObjectTypeIdentification.of(realValue.getKind(), realValue.getIntent());
    }

    protected ContainerPanelConfigurationType getConfiguration(String panelType) {
        var configuration = getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration();
        return WebComponentUtil.getContainerConfiguration(configuration.getObject(), panelType);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return Objects.requireNonNull(getTable()).isValidFormComponents(target);
    }

    @Override
    protected void onSaveResourcePerformed(AjaxRequestTarget target) {
        if (isIterationUnused()) {
            removeUnusedIteration();
        }

        super.onSaveResourcePerformed(target);
    }

    protected abstract void onShowOverrides(
            AjaxRequestTarget target,
            MappingDirection selectedMappingType);

    protected void inEditOutboundValue(
            IModel<PrismContainerValueWrapper<MappingType>> value,
            AjaxRequestTarget target) {
    }

    protected void inEditInboundValue(
            IModel<PrismContainerValueWrapper<MappingType>> value,
            AjaxRequestTarget target) {
    }

    @SuppressWarnings("unchecked")
    public TabbedPanel<ITab> getTabPanel() {
        return (TabbedPanel<ITab>) get(
                createComponentPath(ID_MAIN_FORM, ID_TAB_TABLE));
    }

    @SuppressWarnings("unchecked")
    protected @NotNull SmartMappingTable<P> getTable() {
        Component component = getTabPanel().get(TabbedPanel.TAB_PANEL_ID);
        return (SmartMappingTable<P>) component;
    }

    protected SmartAlertGeneratingPanel getAiPanel() {
        return (SmartAlertGeneratingPanel) get(
                createComponentPath(ID_MAIN_FORM, ID_AI_PANEL));
    }

    protected MessagePanel<?> getMessagePanel() {
        return (MessagePanel<?>) get(
                createComponentPath(ID_MAIN_FORM, ID_MESSAGE_PANEL));
    }

    protected void buildSimulationResultPanel(
            AjaxRequestTarget target,
            IModel<SimulationResultType> simulationResultTypeIModel) {
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
    protected String getSaveLabelKey() {
        return "AttributeMappingsTableWizardPanel.saveButton";
    }

    @Override
    protected String getSubmitButtonCssClass() {
        return "btn-primary";
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
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
