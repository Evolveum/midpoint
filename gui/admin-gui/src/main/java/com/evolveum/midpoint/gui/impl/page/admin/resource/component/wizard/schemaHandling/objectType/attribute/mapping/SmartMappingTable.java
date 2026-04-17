/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.AbstractMappingsTable.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundAttributeMappingsTable.getMappingUsedIconColumn;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.loadSimulationResult;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard.ResourceSimulationTaskWizardPanel.getSimulationResultReference;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.*;
import static com.evolveum.midpoint.prism.PrismConstants.VARIABLE_BINDING_DEF_MATCHING_RULE_NAME;
import static com.evolveum.midpoint.web.component.menu.cog.MenuDividerPanel.createSectionDivider;
import static com.evolveum.midpoint.web.session.UserProfileStorage.TableId.TABLE_SMART_MAPPINGS;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.GroupedMappingDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoiceColumnPanel;
import com.evolveum.midpoint.gui.impl.component.search.panel.SimpleCustomSearchPanel;
import com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTile;
import com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable;
import com.evolveum.midpoint.gui.impl.component.tile.column.MappingSuggestionGroupColumnTilePanel;
import com.evolveum.midpoint.gui.impl.duplication.DuplicationProcessHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUsedFor;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.PreviewMappingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action.FocusStatisticsActions;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action.ObjectTypeStatisticsActions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.component.SimulationActionFlow;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.component.SimulationParams;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemBuilder;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavors;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * Multi-select tile table for mappings items.
 */
public abstract class SmartMappingTable<P extends Containerable> extends BasePanel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(SmartMappingTable.class);

    private static final String CLASS_DOT = SmartMappingTable.class.getName() + ".";
    private static final String OP_DELETE_MAPPING = CLASS_DOT + "deleteMapping";

    private static final String ID_TABLE = "table";
    private static final int MAX_TILE_COUNT = 10;

    private final String resourceOid;

    IModel<PrismContainerValueWrapper<P>> refAttributeDefValue;
    IModel<MappingDirection> mappingDirectionIModel;
    IModel<MappingUsedFor> mappingUsedForIModel = new Model<>(MappingUsedFor.ALL);
    IModel<Boolean> suggestionToggleModel;

    // Cache of accepted suggestions to keep them shown on the same place after refresh
    private final Set<PrismContainerValueWrapper<MappingType>> acceptedSuggestionsCache = new HashSet<>();
    private final IModel<String> searchTextModel = Model.of("");

    public SmartMappingTable(
            @NotNull String id,
            @NotNull IModel<MappingDirection> mappingDirection,
            @NotNull IModel<Boolean> suggestionToggleModel,
            IModel<PrismContainerValueWrapper<P>> refAttributeDefValue,
            String resourceOid) {
        super(id);
        this.suggestionToggleModel = suggestionToggleModel;
        this.resourceOid = resourceOid;
        this.refAttributeDefValue = refAttributeDefValue;
        this.mappingDirectionIModel = mappingDirection;
    }

    protected UserProfileStorage.TableId getTableId() {
        return TABLE_SMART_MAPPINGS;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        ColumnTileTable<MappingDataDto, PrismContainerValueWrapper<MappingType>> smartMappingTable = createSmartMappingTable();
        add(smartMappingTable);
    }

    @SuppressWarnings("rawtypes")
    public IModel<Search> getSearchModel() {
        return getTable().getSearchModel();
    }

    private @NotNull ColumnTileTable<MappingDataDto, PrismContainerValueWrapper<MappingType>> createSmartMappingTable() {

        ColumnTileTable<MappingDataDto, PrismContainerValueWrapper<MappingType>> columnTileTable =
                new ColumnTileTable<>(
                        ID_TABLE,
                        Model.of(ViewToggle.TILE),
                        getTableId(),
                        this::getColumns) {

                    @Override
                    protected @NotNull Component createTile(String id,
                            @NotNull IModel<ColumnTile<MappingDataDto, PrismContainerValueWrapper<MappingType>>> model) {
                        MappingDataDto tileModel = model.getObject().getValue();

                        List<PrismContainerValueWrapper<MappingType>> columnsValues = model.getObject().getValue().getColumnsValues();
                        if (columnsValues.size() > 1) {
                            return new MappingSuggestionGroupColumnTilePanel<>(id, model) {

                                @Override
                                protected boolean isInbound() {
                                    return getMappingDirectionType() == MappingDirection.INBOUND;
                                }

                                @Override
                                protected void onDeletePerform(IModel<PrismContainerValueWrapper<MappingType>> selectedRowModel, AjaxRequestTarget target) {
                                    deleteItemPerform(selectedRowModel.getObject());
                                    SmartMappingTable.this.refreshAndDetach(target);
                                }

                                @Override
                                protected void refresh(AjaxRequestTarget target) {
                                    super.refresh(target);
                                    SmartMappingTable.this.refreshAndDetach(target);
                                }

                                @Override
                                protected void onAcceptSelected(@NotNull PrismContainerValueWrapper<MappingType> selected, @NotNull AjaxRequestTarget target) {
                                    var accepted = SmartMappingTable.this.acceptSuggestionItemPerformed(() -> selected, target);
                                    getAcceptedSuggestionsCache().add(accepted);
                                    tileModel.getColumnsValues().forEach(SmartMappingTable.this::deleteItemPerform);
                                    SmartMappingTable.this.refreshAndDetach(target);
                                }

                                @Override
                                public String getAdditionalDefaultTilePanelCss(PrismContainerValueWrapper<MappingType> rowValue) {
                                    StatusInfo<?> statusInfo = getStatusInfo(rowValue);
                                    if (statusInfo != null && statusInfo.getStatus() != null) {
                                        SmartIntegrationUtils.SuggestionUiStyle uiStyle =
                                                SmartIntegrationUtils.SuggestionUiStyle.from(statusInfo, rowValue);
                                        return "border-large-left " + uiStyle.tileClass;
                                    }
                                    return "";
                                }
                            };
                        }

                        return new ColumnTilePanel<>(id, model, tileModel::getColumnValue) {
                            @Override
                            protected boolean isCheckboxVisible() {
                                return isCheckboxSelectionEnabled();
                            }
                        };

                    }

                    @Override
                    public boolean displayNoValuePanel() {
                        return SmartMappingTable.this.displayNoValuePanel();
                    }

                    @Override
                    protected @NotNull String getNoValuePanelAdditionalCssClass() {
                        return super.getNoValuePanelAdditionalCssClass() + " border-top-none";
                    }

                    @Override
                    protected @NotNull Component createHeader(String id) {
                        return new SimpleCustomSearchPanel(id, searchTextModel) {
                            @Override
                            protected void searchPerformed(AjaxRequestTarget target) {
                                refresh(target);
                            }
                        };
                    }

                    @Override
                    public @NotNull List<InlineMenuItem> getInlineMenuItems() {
                        List<InlineMenuItem> inlineMenuItems = new ArrayList<>();

                        InlineMenuItem acceptSuggestionInlineMenu = createAcceptSuggestionInlineMenu();
                        inlineMenuItems.add(acceptSuggestionInlineMenu);
                        inlineMenuItems.add(createDiscardSuggestionInlineMenu());
                        inlineMenuItems.add(createSectionDivider(acceptSuggestionInlineMenu.getVisibilityChecker()));

                        InlineMenuItem changeLifecycleButtonInlineMenu = createChangeLifecycleButtonInlineMenu();
                        inlineMenuItems.add(createChangeLifecycleButtonInlineMenu());
                        inlineMenuItems.add(createEditInlineMenu());
                        InlineMenuItem duplicateInlineMenu = createDuplicateInlineMenu();
                        inlineMenuItems.add(duplicateInlineMenu);
                        InlineMenuItem changeMappingNameInlineMenu = createChangeMappingNameInlineMenu();
                        inlineMenuItems.add(changeMappingNameInlineMenu);
                        inlineMenuItems.add(createSectionDivider(
                                changeLifecycleButtonInlineMenu.getVisibilityChecker(),
                                changeMappingNameInlineMenu.getVisibilityChecker(),
                                duplicateInlineMenu.getVisibilityChecker()));

                        if (isSimulationSupported()) {
                            InlineMenuItem simulationInlineMenu = createSimulationInlineMenu();
                            inlineMenuItems.add(simulationInlineMenu);
                            inlineMenuItems.add(createSectionDivider(simulationInlineMenu.getVisibilityChecker()));
                        }

                        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> resourceObjectTypeDefinition = findResourceObjectTypeDefinition();
                        if (resourceObjectTypeDefinition != null && resourceObjectTypeDefinition.getRealValue() != null) {
                            inlineMenuItems.add(createResourceAttributeStatisticsMenu(resourceObjectTypeDefinition.getRealValue()));
                            inlineMenuItems.add(createFocusAttributeStatisticsMenu(resourceObjectTypeDefinition.getRealValue()));
                            inlineMenuItems.add(createSectionDivider());
                        }

                        inlineMenuItems.add(createDeleteItemMenu());
                        inlineMenuItems.add(createPreviewInlineMenu());

                        //buttons
                        inlineMenuItems.add(createSuggestionOperationInlineMenu(getPageBase(), this::getStatusInfo, this::refreshAndDetach));
                        inlineMenuItems.add(createSuggestionDetailsInlineMenu(getPageBase(), this::getStatusInfo));
                        inlineMenuItems.add(createAcceptItemMenu());
                        inlineMenuItems.add(createDiscardItemMenu());

                        return inlineMenuItems;
                    }

                    @Override
                    protected void onCreateNewObjectPerform(AjaxRequestTarget target) {
                        createNewValue(null, target);
                        refreshAndDetach(target);
                    }

                    @Override
                    protected @NotNull List<Component> createToolbarButtonsList(String idButton) {
                        List<Component> toolbarButtonsList = super.createToolbarButtonsList(idButton);
                        toolbarButtonsList.add(createMappingTypeDropdownButton(idButton));
                        return toolbarButtonsList;
                    }

                    @Override
                    protected List<Component> createNoValueButtonToolbar(String id) {
                        List<Component> noValueButtonToolbar = super.createNoValueButtonToolbar(id);
                        addAdditionalNoValueToolbarButtons(noValueButtonToolbar, id);
                        return noValueButtonToolbar;
                    }

                    @Override
                    protected void initPanelToolbarButtons(@NotNull RepeatingView toolbar) {
                        SmartMappingTable.this.initPanelToolbarButtons(toolbar);
                        super.initPanelToolbarButtons(toolbar);
                    }

                    @Override
                    protected String getNewObjectButtonCssClass() {
                        return SmartMappingTable.this.getNewObjectButtonCssClass();
                    }

                    @Override
                    public @NotNull InlineMenuItem.VisibilityChecker getDefaultMenuVisibilityChecker() {
                        return bySuggestion(false, this::getStatusInfo);
                    }

                    @SuppressWarnings("rawtypes")
                    @Override
                    protected @NotNull IModel<Search> createSearchModel() {
                        return new LoadableDetachableModel<>() {

                            @Override
                            protected Search<?> load() {
                                return new SearchBuilder<>(MappingType.class)
                                        .modelServiceLocator(getPageBase())
                                        .build();
                            }
                        };
                    }

                    @Override
                    protected StringResourceModel getNewObjectButtonTitle() {
                        if (getMappingDirectionType() != MappingDirection.INBOUND && getMappingDirectionType() != MappingDirection.OUTBOUND) {
                            return super.getNewObjectButtonTitle();
                        }

                        return getMappingDirectionType() == MappingDirection.INBOUND
                                ? createStringResource("SmartMappingTable.addInboundMapping")
                                : createStringResource("SmartMappingTable.addOutboundMapping");
                    }

                    @Override
                    public void editItemPerformed(AjaxRequestTarget target, IModel<MappingDataDto> rowModel) {
                        performOnEditMapping(target, () -> rowModel.getObject().getPrimaryMapping());
                    }

                    @Override
                    protected void deleteItemPerformed(@NotNull PrismContainerValueWrapper<MappingType> value) {
                        SmartMappingTable.this.deleteItemPerform(value);
                    }

                    @Override
                    protected @NotNull ISortableDataProvider<MappingDataDto, String> createProvider() {
                        return createTableProvider();
                    }

                };

        columnTileTable.setOutputMarkupId(true);
        columnTileTable.add(AttributeAppender.append("class", "p-0"));
        columnTileTable.setDefaultPagingSize(MAX_TILE_COUNT);

        return columnTileTable;
    }

    protected String getNewObjectButtonCssClass() {
        return "btn btn-outline-primary ml-auto";
    }

    protected void initPanelToolbarButtons(@NotNull RepeatingView toolbar) {
        var toggleSuggestionVisibilityButton = createToggleSuggestionVisibilityButton(getPageBase(),
                toolbar.newChildId(),
                suggestionToggleModel,
                SmartMappingTable.this::refreshAndDetach,
                null);

        toggleSuggestionVisibilityButton.add(new VisibleBehaviour(this::isSuggestionSwitchSupported));

        toolbar.add(toggleSuggestionVisibilityButton);
        toolbar.add(createLegend(toolbar.newChildId()));
    }

    private @NotNull WebMarkupContainer createLegend(String id) {
        Fragment fragment = new Fragment(id, "legendFragment", this);

        WebMarkupContainer legend = new WebMarkupContainer("legend");
        legend.setOutputMarkupPlaceholderTag(true);
        fragment.add(legend);
        Label label = new Label("legendTitle",
                createStringResource("MappingSuggestionGroupColumnTilePanel.legendTitle"));
        legend.add(label);
        IconWithLabel aiLegend = new IconWithLabel("aiLegend",
                createStringResource("MappingSuggestionGroupColumnTilePanel.legendAi")) {
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-circle text-purple mr-1";
            }
        };
        legend.add(aiLegend);

        IconWithLabel systemLegend = new IconWithLabel("systemLegend",
                createStringResource("MappingSuggestionGroupColumnTilePanel.legendSystem")) {
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-circle text-primary mr-1";
            }
        };
        legend.add(systemLegend);

        fragment.add(new VisibleBehaviour(() -> isSuggestionSwitchSupported()
                && getSuggestionToggleModel().getObject().equals(Boolean.TRUE)));
        return fragment;
    }

    private @NotNull GroupedMappingDataProvider createTableProvider() {
        MappingDirection mappingDirection = getMappingDirectionType();
        return new GroupedMappingDataProvider(this, createDataProvider(), isGroupedSuggestion()) {
            @Override
            protected @NotNull String resolveGroupingKey(@NotNull PrismContainerValueWrapper<MappingType> wrapper) {
                if (mappingDirection == MappingDirection.OUTBOUND) {
                    ItemPathType refAttributePath = getRefAttributePath(wrapper);
                    return refAttributePath == null ? "__null_ref__" : String.valueOf(refAttributePath);
                }

                return super.resolveGroupingKey(wrapper);
            }

            @Override
            protected String getGroupName() {
                if (mappingDirection == MappingDirection.OUTBOUND) {
                    return "ref";
                }
                return super.getGroupName();
            }
        };
    }

    protected boolean isGroupedSuggestion() {
        return true;
    }

    @SuppressWarnings("unchecked")
    protected ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> createDataProvider() {
        var dto = StatusAwareDataFactory.createMappingModel(this, resourceOid, suggestionToggleModel,
                () -> getContainerModel().getObject(), findResourceObjectTypeDefinition(), getMappingDirectionType(),
                getAcceptedSuggestionsCache());
        return new StatusAwareDataProvider<>(
                this,
                (IModel<Search<MappingType>>) (IModel<?>) getTable().getSearchModel(),
                dto,
                true) {

            @Override
            protected boolean matchItems(
                    @NotNull PrismContainerValueWrapper<MappingType> valueWrapper,
                    @NotNull ObjectQuery query) throws SchemaException {
                if (getMappingDirectionType() == MappingDirection.INBOUND
                        && isExcludedMapping(mappingUsedForIModel.getObject(), valueWrapper)) {
                    return false;
                }

                boolean defaultMatch = super.matchItems(valueWrapper, query);
                return defaultMatch || matchRefAttribute(valueWrapper);
            }

            /* Check if the ref attribute contains the search text */
            private boolean matchRefAttribute(
                    @NotNull PrismContainerValueWrapper<MappingType> valueWrapper) {
                ItemPathType refAttributePath = getRefAttributePath(valueWrapper);
                if (refAttributePath != null) {
                    return refAttributePath.toString().contains(getSearchTextModelObject());
                }
                return false;
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getPageBase().getPrismContext().queryFor(MappingType.class)
                        .item(MappingType.F_NAME).contains(getSearchTextModelObject())
                        .or()
                        .item(MappingType.F_TARGET).contains(getSearchTextModelObject())
                        .matching(VARIABLE_BINDING_DEF_MATCHING_RULE_NAME)
                        .or()
                        .item(MappingType.F_SOURCE).contains(getSearchTextModelObject())
                        .matching(VARIABLE_BINDING_DEF_MATCHING_RULE_NAME)
                        .build();
            }
        };
    }

    private @Nullable ItemPathType getRefAttributePath(@NotNull PrismContainerValueWrapper<MappingType> valueWrapper) {
        PrismPropertyWrapper<ItemPathType> refProperty;
        try {
            refProperty = valueWrapper.findProperty(AbstractAttributeMappingsDefinitionType.F_REF);
            if (refProperty != null && refProperty.getValue() != null) {
                return refProperty.getValue().getRealValue();
            }
        } catch (SchemaException e) {
            getPageBase().error("Couldn't get ref attribute path: " + e.getMessage());
        }
        return null;
    }

    protected boolean displayNoValuePanel() {
        return getTable().getProvider().size() == 0
                && !suggestionToggleModel.getObject()
                && mappingUsedForIModel.getObject().equals(MappingUsedFor.ALL);
    }

    @SuppressWarnings("unchecked")
    protected @NotNull List<IColumn<PrismContainerValueWrapper<MappingType>, String>> getColumns() {
        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        boolean isInbound = getMappingDirectionType() == MappingDirection.INBOUND;

        if (isInbound) {
            columns.add(getMappingUsedIconColumn("tile-column-icon"));
        }

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return GuiDisplayTypeUtil.getDisplayTypeForStrengthOfMapping(rowModel, "text-muted");
            }

            @Override
            public String getCssClass() {
                return "px-0 tile-column-icon";
            }
        });

        IColumn<PrismContainerValueWrapper<MappingType>, String> nameCol =
                new PrismPropertyWrapperColumn<>(
                        getMappingTypeDefinition(),
                        MappingType.F_NAME,
                        AbstractItemWrapperColumn.ColumnType.VALUE,
                        getPageBase()) {
                    @Override
                    public @NotNull String getCssClass() {
                        return "col-2 header-border-right";
                    }

                    @Override
                    public String getSortProperty() {
                        return MappingType.F_NAME.getLocalPart();
                    }
                };

        @SuppressWarnings({ "unchecked", "rawtypes" })
        IColumn refCol = new PrismPropertyWrapperColumn(
                getRefColumnDefinitionModel(),
                ResourceAttributeDefinitionType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {

            @SuppressWarnings("rawtypes")
            @Override
            protected Component createHeader(String componentId, IModel mainModel) {
                return new PrismPropertyHeaderPanel<ItemPathType>(
                        componentId,
                        new PrismPropertyWrapperHeaderModel<>(mainModel, itemName, getPageBase())) {

                    @Override
                    protected boolean isAddButtonVisible() {
                        return false;
                    }

                    @Override
                    protected boolean isButtonEnabled() {
                        return false;
                    }

                    @Override
                    protected Component createTitle(IModel<String> label) {
                        return super.createTitle(getPageBase().createStringResource(
                                getMappingDirectionType().name() + "." + ResourceAttributeDefinitionType.F_REF));
                    }
                };
            }

            @Override
            public @NotNull String getCssClass() {
                return "col-2 header-border-right";
            }
        };

        IColumn<PrismContainerValueWrapper<MappingType>, String> expressionCol =
                new PrismPropertyWrapperColumn<>(
                        getMappingTypeDefinition(),
                        MappingType.F_EXPRESSION,
                        AbstractItemWrapperColumn.ColumnType.VALUE,
                        getPageBase()) {
                    @Override
                    public @NotNull String getCssClass() {
                        return "col-2 header-border-right";
                    }

                    @SuppressWarnings("rawtypes")
                    @Override
                    protected <IW extends ItemWrapper> @NotNull Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                        Component columnPanel = super.createColumnPanel(componentId, rowModel);
                        columnPanel.setOutputMarkupId(true);

                        if (rowModel != null
                                && rowModel.getObject() != null
                                && rowModel.getObject().getParent() != null
                                && rowModel.getObject().getParent().getRealValue() instanceof MappingType) {

                            PrismContainerValueWrapper<MappingType> mappingWrapper =
                                    (PrismContainerValueWrapper<MappingType>) rowModel.getObject().getParent();

                            if (getStatusInfo(mappingWrapper) != null) {

                                columnPanel.add(AttributeModifier.append("class", "btn-link cursor-pointer"));

                                columnPanel.add(new AjaxEventBehavior("click") {
                                    @Override
                                    protected void onEvent(AjaxRequestTarget target) {
                                        PreviewMappingPanel panel = buildPreviewMappingPanelPopup(() -> mappingWrapper);
                                        getPageBase().showMainPopup(panel, target);
                                    }
                                });
                            }
                        }

                        return columnPanel;
                    }
                };

        IColumn<PrismContainerValueWrapper<MappingType>, String> sourceOrTargetCol;
        if (getMappingDirectionType() == MappingDirection.OUTBOUND) {
            sourceOrTargetCol = new PrismPropertyWrapperColumn<MappingType, String>(
                    getMappingTypeDefinition(),
                    MappingType.F_SOURCE,
                    AbstractItemWrapperColumn.ColumnType.VALUE,
                    getPageBase()) {

                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                    if (rowModel.getObject().isReadOnly()) {
                        return super.createColumnPanel(componentId, rowModel);
                    }

                    IModel<Collection<VariableBindingDefinitionType>> multiselectModel =
                            createSourceMultiselectModel(rowModel, SmartMappingTable.this.getPageBase());
                    var provider = new FocusDefinitionsMappingProvider(
                            (IModel<PrismPropertyWrapper<VariableBindingDefinitionType>>) rowModel);
                    return new Select2MultiChoiceColumnPanel<>(componentId, multiselectModel, provider);
                }

                @Override
                public String getCssClass() {
                    return "col-2 header-border-right";
                }
            };
        } else {
            sourceOrTargetCol = new PrismPropertyWrapperColumn<MappingType, String>(
                    getMappingTypeDefinition(),
                    MappingType.F_TARGET,
                    AbstractItemWrapperColumn.ColumnType.VALUE,
                    getPageBase()) {
                @Override
                public String getCssClass() {
                    return "col-2 header-border-right";
                }
            };
        }

        columns.add(nameCol);

        if (getMappingDirectionType() == MappingDirection.OUTBOUND) {
            columns.add(sourceOrTargetCol);
            columns.add(expressionCol);
            columns.add(refCol);
        } else {
            columns.add(refCol);
            columns.add(expressionCol);
            columns.add(sourceOrTargetCol);
        }

        columns.add(new LifecycleStateColumn<>(getMappingTypeDefinition(), getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-auto";
            }
        });

        return columns;
    }

    protected final @NotNull LoadableModel<PrismContainerDefinition<MappingType>> getMappingTypeDefinition() {
        return WebComponentUtil.getContainerDefinitionModel(MappingType.class);
    }

    protected void performOnEditMapping(
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {

    }

    public boolean isValidFormComponents(@Nullable AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    protected IModel<PrismContainerValueWrapper<P>> getValueModel() {
        return refAttributeDefValue;
    }

    private void removeFromAcceptedSuggestionsCache(PrismContainerValueWrapper<MappingType> value) {
        acceptedSuggestionsCache.remove(value);
    }

    private void deleteItemPerform(@NotNull PrismContainerValueWrapper<MappingType> value) {
        Task task = getPageBase().createSimpleTask(OP_DELETE_MAPPING);
        @Nullable StatusInfo<?> status = getStatusInfo(value);
        if (status != null) {
            PrismContainerValueWrapper<AttributeMappingsSuggestionType> parentContainerValue = value
                    .getParentContainerValue(AttributeMappingsSuggestionType.class);
            if (parentContainerValue == null || parentContainerValue.getRealValue() == null) {
                return;
            }

            PrismValue oldValue = parentContainerValue.getOldValue();
            removeMappingTypeSuggestionNew(getPageBase(), status, oldValue.getRealValue(), task, task.getResult());
        } else {
            removeFromAcceptedSuggestionsCache(value);
            resolveDeletedItem(value);
        }
    }

    private ItemName getPathBaseOnMappingType() {
        return getMappingDirectionType().getContainerName();
    }

    protected MappingDirection getMappingDirectionType() {
        return mappingDirectionIModel.getObject();
    }

    protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
        return createVirtualMappingContainerModel(
                getPageBase(),
                getValueModel(),
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                AbstractAttributeMappingsDefinitionType.F_REF,
                getMappingDirectionType());
    }

    protected @Nullable PrismContainerValueWrapper<MappingType> createNewValue(
            PrismContainerValue<MappingType> value,
            AjaxRequestTarget target) {
        return createNewVirtualMappingValue(
                value,
                getValueModel(),
                getMappingDirectionType(),
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                AbstractAttributeMappingsDefinitionType.F_REF,
                getPageBase(),
                target);
    }

    protected IModel<? extends PrismContainerDefinition<?>> getRefColumnDefinitionModel() {
        return Model.of(PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(ResourceAttributeDefinitionType.class));
    }

    protected void resolveDeletedItem(@NotNull PrismContainerValueWrapper<MappingType> value) {
        try {
            if (value.getStatus() == ValueStatus.ADDED) {
                PrismContainerWrapper<ResourceAttributeDefinitionType> container = getValueModel().getObject()
                        .findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);

                for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> valueR : container.getValues()) {
                    PrismContainerWrapper<MappingType> mappingR = valueR.findContainer(getPathBaseOnMappingType());
                    mappingR.getValues().removeIf(v -> v.equals(value));
                }
            } else {
                value.setStatus(ValueStatus.DELETED);
            }
        } catch (SchemaException e) {
            getPageBase().error("Couldn't delete mapping: " + e.getMessage());
        } finally {
            value.setSelected(false);
        }
    }

    protected @NotNull InlineMenuItem createDuplicateInlineMenu() {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-copy")
                .label(createStringResource("DuplicationProcessHelper.menu.duplicate"))
                .action(DuplicationProcessHelper.createDuplicateColumnAction(getPageBase(),
                        this::createDuplicateValuePerform))
                .headerMenuItem(false)
                .visibilityChecker(bySuggestion(false, this::getStatusInfo))
                .buildInlineMenu();
    }

    protected void createDuplicateValuePerform(PrismContainerValue<MappingType> value, AjaxRequestTarget target) {
        createNewValue(value, target);
        refreshAndDetach(target);
    }

    private @NotNull InlineMenuItem createChangeLifecycleButtonInlineMenu() {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-sync")
                .label(createStringResource("AttributeMappingsTable.button.changeLifecycle"))
                .action(new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        List<PrismContainerValueWrapper<MappingType>> selectedMappings =
                                new ArrayList<>(getSelectedMappings());

                        if (selectedMappings.isEmpty()) {
                            getPageBase().warn(createStringResource(
                                    "MultivalueContainerListPanel.message.noItemsSelected").getString());
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        ChangeLifecycleSelectedMappingsPopup changePopup =
                                new ChangeLifecycleSelectedMappingsPopup(
                                        getPageBase().getMainPopupBodyId(),
                                        Model.ofList(selectedMappings)) {
                                    @Override
                                    protected void applyChanges(AjaxRequestTarget target) {
                                        super.applyChanges(target);
                                        refreshAndDetach(target);
                                    }
                                };

                        getPageBase().showMainPopup(changePopup, target);
                    }
                })
                .visibilityChecker((rowModel, isHeader) -> isHeader)
                .buildInlineMenu();
    }

    private @NotNull List<PrismContainerValueWrapper<MappingType>> getSelectedMappings() {
        List<PrismContainerValueWrapper<MappingType>> selectedContainerValues = new ArrayList<>();
        List<MappingDataDto> selectedContainerItems = getTable().getSelectedContainerItems();
        for (MappingDataDto selectedContainerItem : selectedContainerItems) {
            PrismContainerValueWrapper<MappingType> primaryMapping = selectedContainerItem.getPrimaryMapping();
            if (primaryMapping != null) {
                selectedContainerValues.add(primaryMapping);
            }
        }
        return selectedContainerValues;
    }

    private @NotNull InlineMenuItem createChangeMappingNameInlineMenu() {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-sync")
                .label(createStringResource("AttributeMappingsTable.button.changeMappingName"))
                .action(createChangeNameColumnAction(getPageBase(), this::refreshAndDetach))
                .headerMenuItem(false)
                .visibilityChecker(changeNameVisibilityChecker())
                .buildInlineMenu();
    }

    private InlineMenuItem.@NotNull VisibilityChecker changeNameVisibilityChecker() {
        return (rowModel, isHeader) -> {
            InlineMenuItem.VisibilityChecker visibilityChecker = bySuggestion(false, this::getStatusInfo);
            boolean visible = visibilityChecker.isVisible(rowModel, isHeader);
            if (!visible) {
                return false;
            }
            try {
                if (rowModel == null || rowModel.getObject() == null) {
                    return false;
                }
                @SuppressWarnings("unchecked") PrismPropertyWrapper<String> property =
                        ((PrismContainerValueWrapper<MappingType>) rowModel.getObject()).findProperty(MappingType.F_NAME);
                if (property == null) {
                    return false;
                }
                return property.isReadOnly();
            } catch (SchemaException e) {
                return false;
            }
        };
    }

    protected void refreshAndDetach(AjaxRequestTarget target) {
        getTable().refreshAndDetach(target);
    }

    protected @Nullable StatusInfo<?> getStatusInfo(PrismContainerValueWrapper<MappingType> value) {
        return getTable().getStatusInfo(value);
    }

    protected PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> findResourceObjectTypeDefinition() {
        return refAttributeDefValue.getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);
    }

    protected DropDownChoicePanel<MappingUsedFor> createMappingTypeDropdownButton(String idButton) {
        DropDownChoicePanel<MappingUsedFor> dropdown = WebComponentUtil.createEnumPanel(
                idButton,
                WebComponentUtil.createReadonlyModelFromEnum(MappingUsedFor.class),
                mappingUsedForIModel,
                SmartMappingTable.this,
                true,
                getString("InboundAttributeMappingsTable.allMappings"));
        dropdown.getBaseFormComponent().add(AttributeAppender.append("style", "width: 220px;"));
        dropdown.getBaseFormComponent().add(AttributeModifier.replace("class", "form-control"));
        dropdown.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                mappingUsedForIModel.setObject(dropdown.getModel().getObject());
                getTable().refreshAndDetach(target);
            }
        });
        dropdown.add(new VisibleBehaviour(() -> MappingDirection.INBOUND == getMappingDirectionType() && !getTable().displayNoValuePanel()));
        return dropdown;
    }

    @SuppressWarnings("unchecked")
    protected ColumnTileTable<MappingDataDto, PrismContainerValueWrapper<MappingType>> getTable() {
        return (ColumnTileTable<MappingDataDto, PrismContainerValueWrapper<MappingType>>) get(ID_TABLE);
    }

    public PrismContainerValueWrapper<MappingType> acceptSuggestionItemPerformed(
            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            @NotNull AjaxRequestTarget target) {
        PrismContainerValueWrapper<MappingType> newValue = createNewValue(rowModel.getObject().getNewValue(), target);
        deleteItemPerform(rowModel.getObject());
        return newValue;
    }

    protected InlineMenuItem createAcceptItemMenu() {
        return createSuggestActionMenuBuilder()
                .label(createStringResource("SmartMappingTable.apply"))
                .icon("fa fa-check mr-2")
                .action(createAcceptSuggestionColumnAction())
                .additionalCssClass("btn-link text-primary rounded border-primary mr-2")
                .buildButtonMenu();
    }

    protected InlineMenuItem createDiscardItemMenu() {
        return createSuggestActionMenuBuilder()
                .label(createStringResource("SmartMappingTable.dismiss"))
                .icon("fa fa-times mr-2")
                .action(createDiscardColumnAction())
                .additionalCssClass("btn-link text-danger")
                .buildButtonMenu();
    }

    private InlineMenuItemBuilder createSuggestActionMenuBuilder() {
        return InlineMenuItemBuilder.create()
                .labelVisible(true)
                .menuLinkVisible(false)
                .headerMenuItem(false)
                .visible(Model.of(isSuggestionInlineMenuVisible()))
                .visibilityChecker(bySuggestion(true, this::getStatusInfo));
    }

    private @NotNull ColumnMenuAction<Serializable> createDiscardColumnAction() {
        return new ColumnMenuAction<>() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null) {
                    return;
                }

                if (getRowModel().getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                    //noinspection unchecked
                    deleteItemPerform((PrismContainerValueWrapper<MappingType>) valueWrapper);
                }

                refreshAndDetach(target);
            }
        };
    }

    private @NotNull InlineMenuItem createResourceAttributeStatisticsMenu(ResourceObjectTypeDefinitionType objectTypeDefinitionType) {
        return InlineMenuItemBuilder.create()
                .label(createStringResource("SmartMappingTable.objectTypeStatistics.resourceAttribute"))
                .icon("fa fa-bar-chart")
                .action(new ColumnMenuAction<>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        ItemPathType ref = null;
                        if (getRowModel() != null) {
                            PrismContainerValueWrapper<MappingType> valueWrapper = (PrismContainerValueWrapper<MappingType>) getRowModel().getObject();
                            ref = getRefAttributePath(valueWrapper);
                        }

                        ResourceObjectTypeIdentification id = ResourceObjectTypeIdentification.of(objectTypeDefinitionType);
                        ObjectTypeStatisticsActions.handleClick(
                                target,
                                getPageBase(),
                                getPageBase().getSmartIntegrationService(),
                                resourceOid,
                                id,
                                ref,
                                false
                        );
                    }
                })
                .headerMenuItem(true)
                .buildInlineMenu();
    }

    private @NotNull InlineMenuItem createFocusAttributeStatisticsMenu(ResourceObjectTypeDefinitionType objectTypeDef) {
        boolean isOutbound = getMappingDirectionType() == MappingDirection.OUTBOUND;

        return InlineMenuItemBuilder.create()
                .label(createStringResource("SmartMappingTable.objectTypeStatistics.focusAttribute.outbound." + isOutbound))
                .icon("fa fa-line-chart")
                .action(new ColumnMenuAction<>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        QName focusTypeName = objectTypeDef.getFocus().getType();

                        if (focusTypeName == null) {
                            getPageBase().warn("Focus type is not specified for the object type. Cannot show statistics.");
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        String resourceOid = SmartMappingTable.this.resourceOid;
                        ShadowKindType kind = objectTypeDef.getKind();
                        String intent = objectTypeDef.getIntent();

                        if (resourceOid == null || kind == null || intent == null) {
                            getPageBase().warn("Resource, kind, and intent must be specified for focus statistics.");
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        ItemPathType targetPath = null;
                        if (getRowModel() != null) {
                            PrismContainerValueWrapper<MappingType> valueWrapper =
                                    (PrismContainerValueWrapper<MappingType>) getRowModel().getObject();
                            MappingType mapping = valueWrapper.getRealValue();
                            if (getMappingDirectionType() == MappingDirection.INBOUND) {
                                if (mapping != null && mapping.getTarget() != null && mapping.getTarget().getPath() != null) {
                                    targetPath = mapping.getTarget().getPath();
                                }
                            } else {
                                if (mapping != null && mapping.getSource() != null && !mapping.getSource().isEmpty()) {
                                    //TODO handle multiple sources
                                    if (mapping.getSource().get(0) != null && mapping.getSource().get(0).getPath() != null) {
                                        targetPath = mapping.getSource().get(0).getPath();
                                    }
                                }
                            }
                        }

                        FocusStatisticsActions.handleClick(
                                target,
                                getPageBase(),
                                getPageBase().getSmartIntegrationService(),
                                focusTypeName,
                                resourceOid,
                                kind,
                                intent,
                                targetPath,
                                false);
                    }
                })
                .headerMenuItem(true)
                .buildInlineMenu();
    }

    protected boolean isSuggestionInlineMenuVisible() {
        return suggestionToggleModel.getObject().equals(Boolean.TRUE);
    }

    public ColumnMenuAction<Serializable> createAcceptSuggestionColumnAction() {
        return new ColumnMenuAction<>() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null) {
                    return;
                }

                if (getRowModel().getObject() instanceof MappingDataDto dto) {
                    for (PrismContainerValueWrapper<MappingType> value : dto.getMappings()) {
                        StatusInfo<?> status = getStatusInfo(value);
                        if (status != null) {
                            PrismContainerValueWrapper<MappingType> newValue =
                                    acceptSuggestionItemPerformed(() -> value, target);
                            getAcceptedSuggestionsCache().add(newValue);
                        }
                    }
                } else if (getRowModel().getObject() instanceof PrismContainerValueWrapper) {
                    @SuppressWarnings("unchecked")
                    PrismContainerValueWrapper<MappingType> value =
                            (PrismContainerValueWrapper<MappingType>) getRowModel().getObject();
                    StatusInfo<?> status = getStatusInfo(value);
                    if (status != null) {
                        PrismContainerValueWrapper<MappingType> newValue =
                                acceptSuggestionItemPerformed(() -> value, target);
                        getAcceptedSuggestionsCache().add(newValue);
                    }
                }

                refreshAndDetach(target);
            }
        };
    }

    protected IModel<Boolean> getSuggestionToggleModel() {
        return suggestionToggleModel;
    }

    protected InlineMenuItem createAcceptSuggestionInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(createStringResource("SmartMappingTable.accept.suggestion"))
                .icon("fa fa-check")
                .additionalCssClass("text-success")
                .headerMenuItem(true)
                .visibilityChecker((rowModel, isHeader) -> isHeader && getSuggestionToggleModel().getObject().equals(Boolean.TRUE))
                .action(createAcceptSuggestionBulkAction())
                .buildInlineMenu();
    }

    protected StringResourceModel discardConfirmationTitle(int selectedCount, int allCount) {
        if (selectedCount == 0 && allCount == 0) {
            return createStringResource("ColumnTileTable.discard.title.noItems");
        }

        return selectedCount == 0
                ? createStringResource("ColumnTileTable.discard.title.empty", allCount)
                : createStringResource("ColumnTileTable.discard.title", selectedCount);
    }

    protected StringResourceModel acceptSuggestionTitle(int selectedCount, int allCount) {
        if (selectedCount == 0 && allCount == 0) {
            return createStringResource("ColumnTileTable.accept.title.noItems");
        }

        return selectedCount == 0
                ? createStringResource("ColumnTileTable.accept.title.empty", allCount)
                : createStringResource("ColumnTileTable.accept.title", selectedCount);
    }

    protected InlineMenuItem createDiscardSuggestionInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(createStringResource("SmartMappingTable.discard.suggestion"))
                .icon("fa fa-times")
                .additionalCssClass("text-danger")
                .headerMenuItem(true)
                .visibilityChecker((rowModel, isHeader) -> isHeader && getSuggestionToggleModel().getObject().equals(Boolean.TRUE))
                .action(createDiscardSuggestionBulkAction())
                .buildInlineMenu();
    }

    public ColumnMenuAction<Serializable> createDiscardSuggestionBulkAction() {
        return new ColumnMenuAction<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                final List<PrismContainerValueWrapper<MappingType>> selectedSuggestions =
                        new ArrayList<>(getAllSelectedItemsWithStatus());

                final List<PrismContainerValueWrapper<MappingType>> allSuggestions =
                        new ArrayList<>(getAllItemsWithStatus());

                final int selectedCount = selectedSuggestions.size();
                final int allCount = allSuggestions.size();

                ConfirmationPanel dialog = new ConfirmationPanel(
                        getPageBase().getMainPopupBodyId(),
                        discardConfirmationTitle(selectedCount, allCount)) {

                    @Override
                    protected IModel<String> createNoLabel() {
                        return selectedCount == 0
                                ? createStringResource("MultiSelectContainerActionTileTablePanel.deleteConfirmation.cancel")
                                : super.createNoLabel();
                    }

                    @Override
                    protected boolean isYesButtonVisible() {
                        return selectedCount > 0 || allCount > 0;
                    }

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        if (selectedSuggestions.isEmpty()) {
                            allSuggestions.forEach(SmartMappingTable.this::deleteItemPerform);
                        } else {
                            selectedSuggestions.forEach(SmartMappingTable.this::deleteItemPerform);
                        }
                        getPageBase().hideMainPopup(target);
                        refreshAndDetach(target);
                    }
                };
                getPageBase().showMainPopup(dialog, target);
            }
        };
    }

    public ColumnMenuAction<Serializable> createAcceptSuggestionBulkAction() {
        return new ColumnMenuAction<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                final List<PrismContainerValueWrapper<MappingType>> selectedSuggestions =
                        new ArrayList<>(getAllSelectedItemsWithStatus());

                final List<PrismContainerValueWrapper<MappingType>> allSuggestions =
                        new ArrayList<>(getAllItemsWithStatus());

                final int selectedCount = selectedSuggestions.size();
                final int allCount = allSuggestions.size();

                ConfirmationPanel dialog = new ConfirmationPanel(
                        getPageBase().getMainPopupBodyId(),
                        acceptSuggestionTitle(selectedCount, allCount)) {

                    @Override
                    protected IModel<String> createNoLabel() {
                        return selectedCount == 0
                                ? createStringResource("MultiSelectContainerActionTileTablePanel.deleteConfirmation.cancel")
                                : super.createNoLabel();
                    }

                    @Override
                    protected boolean isYesButtonVisible() {
                        return selectedCount > 0 || allCount > 0;
                    }

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        if (selectedSuggestions.isEmpty()) {
                            allSuggestions.forEach(v -> acceptSuggestionItemPerformed(() -> v, target));
                        } else {
                            selectedSuggestions.forEach(v -> acceptSuggestionItemPerformed(() -> v, target));
                        }
                        getPageBase().hideMainPopup(target);
                        refreshAndDetach(target);
                    }
                };
                getPageBase().showMainPopup(dialog, target);
            }
        };
    }

    private @NotNull List<PrismContainerValueWrapper<MappingType>> getAllSelectedItemsWithStatus() {
        List<PrismContainerValueWrapper<MappingType>> selectedItemsWithStatus = new ArrayList<>();
        List<MappingDataDto> selectedContainerItems = getTable().getSelectedContainerItems();
        for (MappingDataDto selectedContainerItem : selectedContainerItems) {
            List<PrismContainerValueWrapper<MappingType>> mappings = selectedContainerItem.getMappings();
            for (PrismContainerValueWrapper<MappingType> mapping : mappings) {
                if (getStatusInfo(mapping) != null) {
                    selectedItemsWithStatus.add(mapping);
                }
            }
        }
        return selectedItemsWithStatus;
    }

    private @NotNull List<PrismContainerValueWrapper<MappingType>> getAllItemsWithStatus() {
        List<PrismContainerValueWrapper<MappingType>> itemsWithStatus = new ArrayList<>();
        for (MappingDataDto item : getTable().getAllItems()) {
            List<PrismContainerValueWrapper<MappingType>> mappings = item.getMappings();
            for (PrismContainerValueWrapper<MappingType> mapping : mappings) {
                if (getStatusInfo(mapping) != null) {
                    itemsWithStatus.add(mapping);
                }
            }
        }
        return itemsWithStatus;
    }

    protected @Nullable GroupedMappingDataProvider getProvider() {
        if (getTable().getProvider() instanceof GroupedMappingDataProvider provider) {
            return provider;
        }
        return null;
    }

    protected @NotNull String getSearchTextModelObject() {
        return searchTextModel != null && searchTextModel.getObject() != null ? searchTextModel.getObject() : "";
    }

    protected void addAdditionalNoValueToolbarButtons(List<Component> toolbarButtonsList, String idButton) {
        // empty implementation, can be overridden in subclasses
    }

    protected Set<PrismContainerValueWrapper<MappingType>> getAcceptedSuggestionsCache() {
        return acceptedSuggestionsCache;
    }

    protected InlineMenuItem createSimulationInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(createStringResource("SmartMappingPanel.simulate"))
                .icon("fa fa-flask")
                .headerMenuItem(false)
                //only for inbound
                .visibilityChecker((rowModel, isHeader) -> !isHeader && getMappingDirectionType() == MappingDirection.INBOUND)
                .action(new ColumnMenuAction<PrismContainerValueWrapper<MappingType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            return;
                        }

                        InlineMappingDefinitionType mappingToSimulate = new InlineMappingDefinitionType();
                        ItemPathType refPath = getRefPath(getRowModel().getObject());
                        if (refPath == null) {
                            return;
                        }

                        mappingToSimulate.setRef(refPath);

                        MappingType mappingRealValue = getRowModel().getObject().getRealValue();
                        //noinspection unchecked
                        WebPrismUtil.cleanupEmptyContainerValue(mappingRealValue.asPrismContainerValue());

                        if (mappingRealValue instanceof InboundMappingType inbound) {
                            mappingToSimulate.getInbound().add(inbound.clone());
                        } else if (mappingRealValue instanceof OutboundMappingType outbound) {
                            mappingToSimulate.getOutbound().add(outbound.clone());
                        }

                        SimulationParams<?> params = new SimulationParams<>(
                                getPageBase(),
                                getResourceType(),
                                findResourceObjectTypeDefinition().getRealValue(),
                                ResourceTaskFlavors.MAPPING_PREVIEW_ACTIVITY,
                                mappingToSimulate.clone(),
                                ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW
                        );

                        SimulationActionFlow<?> flow = getSimulationActionFlow(params);
                        flow.start(target);
                    }
                })
                .buildInlineMenu();
    }

    private @NotNull SimulationActionFlow<?> getSimulationActionFlow(SimulationParams<?> params) {
        SimulationActionFlow<?> flow = new SimulationActionFlow(params) {
            @Override
            public void onShowResultProcess(AjaxRequestTarget target, TaskType task, PageBase pageBase) {
                ObjectReferenceType simulationResultReference = getSimulationResultReference(task);
                if (simulationResultReference == null || simulationResultReference.getOid() == null) {
                    LOGGER.error("Simulation result reference or OID is null for task {}", task.getName());
                    return;
                }
                SimulationResultType simulationResultType = loadSimulationResult(pageBase, simulationResultReference.getOid());
                buildSimulationResultPanel(target, Model.of(simulationResultType));

            }
        };
        flow.enableSampling();
        flow.showProgressPopup();
        return flow;
    }

    protected InlineMenuItem createPreviewInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(createStringResource("SmartMappingPanel.preview.mapping"))
                .icon("fa fa-search")
                .headerMenuItem(false)
                .action(new ColumnMenuAction<PrismContainerValueWrapper<MappingType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            return;
                        }

                        IModel<PrismContainerValueWrapper<MappingType>> mappingWrapper = getRowModel();
                        PreviewMappingPanel previewMappingPanel = buildPreviewMappingPanelPopup(mappingWrapper);
                        getPageBase().showMainPopup(previewMappingPanel, target);
                    }
                })
                .visibilityChecker(bySuggestion(true, this::getStatusInfo))
                .buildInlineMenu();
    }

    private @NotNull PreviewMappingPanel buildPreviewMappingPanelPopup(IModel<PrismContainerValueWrapper<MappingType>> mappingWrapper) {
        return new PreviewMappingPanel(
                getPageBase().getMainPopupBodyId(),
                mappingWrapper,
                getMappingDirectionType() == MappingDirection.INBOUND) {

            @Override
            public void customizeFooterButtons(@NotNull RepeatingView repeater) {
                super.customizeFooterButtons(repeater);

                StatusInfo<?> statusInfo = getStatusInfo(mappingWrapper.getObject());
                if (statusInfo != null) {
                    AjaxIconButton discardSuggestionButton = buildDiscardButton(repeater, mappingWrapper);
                    discardSuggestionButton.add(AttributeModifier.replace("class",
                            "btn-link text-danger ml-auto"));
                    repeater.add(discardSuggestionButton);

                    AjaxIconButton acceptSuggestionButton =
                            buildAcceptButton(repeater, mappingWrapper);
                    acceptSuggestionButton.add(AttributeModifier.replace("class", "btn btn-primary"));
                    repeater.add(acceptSuggestionButton);
                }
            }

        };
    }

    private @NotNull AjaxIconButton buildAcceptButton(
            @NotNull RepeatingView repeater,
            IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
        AjaxIconButton acceptSuggestionButton = new AjaxIconButton(
                repeater.newChildId(),
                Model.of("fa fa-check mr-2"),
                createStringResource("SmartMappingTable.apply.suggestion")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                var accepted = acceptSuggestionItemPerformed(rowModel, target);
                getAcceptedSuggestionsCache().add(accepted);
                refreshAndDetach(target);
                getPageBase().hideMainPopup(target);
            }
        };
        acceptSuggestionButton.setOutputMarkupId(true);
        acceptSuggestionButton.showTitleAsLabel(true);
        return acceptSuggestionButton;
    }

    private @NotNull AjaxIconButton buildDiscardButton(
            @NotNull RepeatingView repeater,
            IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
        AjaxIconButton discardSuggestionButton = new AjaxIconButton(
                repeater.newChildId(),
                Model.of("fa fa-times mr-2"),
                createStringResource("SmartMappingTable.dismiss")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteItemPerform(rowModel.getObject());
                refreshAndDetach(target);
                getPageBase().hideMainPopup(target);
            }
        };
        discardSuggestionButton.setOutputMarkupId(true);
        discardSuggestionButton.showTitleAsLabel(true);
        return discardSuggestionButton;
    }

    private @Nullable ItemPathType getRefPath(@NotNull PrismContainerValueWrapper<MappingType> mappingWrapper) {
        PrismPropertyWrapper<ItemPathType> refProperty;
        try {
            refProperty = mappingWrapper.findProperty(AbstractAttributeMappingsDefinitionType.F_REF);

            return refProperty != null && refProperty.getValue() != null
                    ? refProperty.getValue().getRealValue()
                    : null;
        } catch (SchemaException e) {
            throw new RuntimeException("Error retrieving ref property from mapping", e);
        }
    }

    protected abstract ResourceType getResourceType();

    protected void buildSimulationResultPanel(AjaxRequestTarget target, IModel<SimulationResultType> simulationResultTypeIModel) {
    }

    protected boolean isSuggestionSwitchSupported() {
        return true;
    }

    protected boolean isSimulationSupported() {
        return true;
    }
}


