/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.table;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.createNewVirtualMappingValue;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.createVirtualMappingContainerModel;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.isExcludedMapping;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.createToggleSuggestionVisibilityButton;
import static com.evolveum.midpoint.prism.PrismConstants.VARIABLE_BINDING_DEF_MATCHING_RULE_NAME;
import static com.evolveum.midpoint.web.session.UserProfileStorage.TableId.TABLE_SMART_MAPPINGS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.GroupedMappingDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.search.panel.SimpleCustomSearchPanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTile;
import com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable;
import com.evolveum.midpoint.gui.impl.component.tile.column.MappingSuggestionGroupColumnTilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUsedFor;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.MappingDataDto;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAttributeMappingsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Base component for displaying and managing mapping definitions and suggestion in a tile/table view.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Holds state (direction, search, suggestion toggle, models)</li>
 *   <li>Creates and wires the table, data provider, and search</li>
 *   <li>Handles refresh, grouping, and mapping lifecycle operations</li>
 *   <li>Provides extension hooks for subclasses (columns, provider, UI behavior)</li>
 * </ul>
 *
 * <p>Collaborators:
 * <ul>
 *   <li>{@link SmartMappingColumns} – column definitions</li>
 *   <li>{@link SmartMappingMenus} – inline menu composition</li>
 *   <li>{@link SmartMappingActions} – action logic</li>
 * </ul>
 */
public abstract class SmartMappingTable<P extends Containerable> extends BasePanel<String> {

    private static final String ID_TABLE = "table";
    private static final int MAX_TILE_COUNT = 10;

    private final String resourceOid;

    private final IModel<PrismContainerValueWrapper<P>> refAttributeDefValue;
    private final IModel<MappingDirection> mappingDirectionModel;
    private final IModel<MappingUsedFor> mappingUsedForModel = Model.of(MappingUsedFor.ALL);
    private final IModel<Boolean> suggestionToggleModel;
    private final IModel<String> searchTextModel = Model.of("");

    private final Set<PrismContainerValueWrapper<MappingType>> acceptedSuggestionsCache = new HashSet<>();

    private final SmartMappingColumns<P> columns;
    private final SmartMappingActions<P> actions;
    private final SmartMappingMenus<P> menus;

    private final LoadableModel<Boolean> noValuePanelModel = new LoadableModel<>(false) {
        @Override
        protected @NotNull Boolean load() {
            return !Boolean.TRUE.equals(suggestionToggleModel.getObject())
                    && mappingUsedForModel.getObject() == MappingUsedFor.ALL
                    && getTable().getProvider().size() == 0;
        }
    };

    public SmartMappingTable(
            @NotNull String id,
            @NotNull IModel<MappingDirection> mappingDirectionModel,
            @NotNull IModel<Boolean> suggestionToggleModel,
            @NotNull IModel<PrismContainerValueWrapper<P>> refAttributeDefValue,
            @NotNull String resourceOid) {
        super(id);
        this.mappingDirectionModel = mappingDirectionModel;
        this.suggestionToggleModel = suggestionToggleModel;
        this.refAttributeDefValue = refAttributeDefValue;
        this.resourceOid = resourceOid;

        this.columns = new SmartMappingColumns<>(this);
        this.actions = new SmartMappingActions<>(this);
        this.menus = new SmartMappingMenus<>(this, actions);
    }

    protected UserProfileStorage.TableId getTableId() {
        return TABLE_SMART_MAPPINGS;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(createSmartMappingTable());
    }

    @SuppressWarnings("rawtypes")
    public IModel<Search> getSearchModel() {
        return getTable().getSearchModel();
    }

    private @NotNull ColumnTileTable<MappingDataDto, PrismContainerValueWrapper<MappingType>> createSmartMappingTable() {
        ColumnTileTable<MappingDataDto, PrismContainerValueWrapper<MappingType>> table =
                new ColumnTileTable<>(
                        ID_TABLE,
                        Model.of(ViewToggle.TILE),
                        getTableId(),
                        this::getColumns) {

                    @Override
                    protected @NotNull Component createTile(
                            String id,
                            @NotNull IModel<ColumnTile<MappingDataDto, PrismContainerValueWrapper<MappingType>>> model) {

                        MappingDataDto dto = model.getObject().getValue();
                        List<PrismContainerValueWrapper<MappingType>> values = dto.getColumnsValues();

                        if (values.size() > 1) {
                            return new MappingSuggestionGroupColumnTilePanel<>(id, model) {

                                @Override
                                protected boolean isInbound() {
                                    return SmartMappingTable.this.isInbound();
                                }

                                @Override
                                protected void onDeletePerform(
                                        IModel<PrismContainerValueWrapper<MappingType>> selectedRowModel,
                                        AjaxRequestTarget target) {
                                    deleteItemPerform(selectedRowModel.getObject());
                                    refreshAndDetach(target);
                                }

                                @Override
                                protected void refresh(AjaxRequestTarget target) {
                                    super.refresh(target);
                                    refreshAndDetach(target);
                                }

                                @Override
                                protected void performOnAccept(
                                        AjaxRequestTarget target,
                                        PrismContainerValueWrapper<MappingType> selected) {
                                    actions.performAcceptFromGroup(selected, target);
                                }

                                @Override
                                protected void onAcceptSelected(
                                        @NotNull PrismContainerValueWrapper<MappingType> selected,
                                        @NotNull AjaxRequestTarget target) {
                                    actions.acceptGroupedSuggestion(selected, target);
                                }

                                @Override
                                public String getAdditionalDefaultTilePanelCss(
                                        PrismContainerValueWrapper<MappingType> rowValue) {
                                    return actions.resolveAdditionalTileCss(rowValue);
                                }
                            };
                        }

                        return new ColumnTilePanel<>(id, model, dto::getColumnValue) {
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
                        return SmartMappingTable.this.createSearchHeader(id);
                    }

                    @Override
                    public @NotNull List<InlineMenuItem> getInlineMenuItems() {
                        return menus.getInlineMenuItems();
                    }

                    @Override
                    protected void onCreateNewObjectPerform(AjaxRequestTarget target) {
                        createNewValue(null, target);
                        refreshAndDetach(target);
                    }

                    @Override
                    protected @NotNull List<Component> createToolbarButtonsList(String idButton) {
                        List<Component> buttons = super.createToolbarButtonsList(idButton);
                        buttons.add(createMappingTypeDropdownButton(idButton));
                        return buttons;
                    }

                    @Override
                    protected List<Component> createNoValueButtonToolbar(String id) {
                        List<Component> buttons = super.createNoValueButtonToolbar(id);
                        addAdditionalNoValueToolbarButtons(buttons, id);
                        return buttons;
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
                        return actions.defaultMenuVisibilityChecker();
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
                    protected org.apache.wicket.model.StringResourceModel getNewObjectButtonTitle() {
                        if (!isInbound() && !isOutbound()) {
                            return super.getNewObjectButtonTitle();
                        }

                        return isInbound()
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
                        return SmartMappingTable.this.createTableProvider();
                    }
                };

        table.setOutputMarkupId(true);
        table.add(AttributeAppender.append("class", "p-0"));
        table.setDefaultPagingSize(MAX_TILE_COUNT);
        return table;
    }

    //getColumns
    protected @NotNull List<IColumn<PrismContainerValueWrapper<MappingType>, String>> getColumns() {
        return columns.getColumns();
    }

    protected boolean displayNoValuePanel() {
        return Boolean.TRUE.equals(noValuePanelModel.getObject());
    }

    protected @NotNull Component createSearchHeader(String id) {
        return new SimpleCustomSearchPanel(id, getSearchTextModel()) {
            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                refreshAndDetach(target);
            }
        };
    }

    protected @NotNull GroupedMappingDataProvider createTableProvider() {
        MappingDirection mappingDirection = getMappingDirectionType();

        return new GroupedMappingDataProvider(this, createDataProvider(), isGroupedSuggestion()) {
            @Override
            protected @NotNull String resolveGroupingKey(@NotNull PrismContainerValueWrapper<MappingType> wrapper) {
                if (mappingDirection == MappingDirection.OUTBOUND) {
                    ItemPathType ref = getRefPath(wrapper);
                    return ref == null ? "__null_ref__" : String.valueOf(ref);
                }
                return super.resolveGroupingKey(wrapper);
            }

            @Override
            protected String getGroupName() {
                return isOutbound() ? "ref" : super.getGroupName();
            }
        };
    }

    protected boolean isGroupedSuggestion() {
        return true;
    }

    @SuppressWarnings("unchecked")
    protected ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> createDataProvider() {
        var dto = StatusAwareDataFactory.createMappingModel(
                this,
                getResourceOid(),
                getSuggestionToggleModel(),
                () -> getContainerModel().getObject(),
                findResourceObjectTypeDefinition(),
                getMappingDirectionType(),
                getAcceptedSuggestionsCache());

        return new StatusAwareDataProvider<>(
                this,
                (IModel<Search<MappingType>>) (IModel<?>) getTable().getSearchModel(),
                dto,
                MappingsSuggestionType.class,
                true) {

            @Override
            protected boolean matchItems(
                    @NotNull PrismContainerValueWrapper<MappingType> valueWrapper,
                    @NotNull ObjectQuery query) throws SchemaException {
                if (isInbound() && isExcludedMapping(getMappingUsedForModel().getObject(), valueWrapper)) {
                    return false;
                }

                return super.matchItems(valueWrapper, query) || matchRefAttribute(valueWrapper);
            }

            private boolean matchRefAttribute(@NotNull PrismContainerValueWrapper<MappingType> valueWrapper) {
                ItemPathType ref = getRefPath(valueWrapper);
                return ref != null && ref.toString().contains(getSearchText());
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getPageBase().getPrismContext().queryFor(MappingType.class)
                        .item(MappingType.F_NAME).contains(getSearchText())
                        .or()
                        .item(MappingType.F_TARGET).contains(getSearchText())
                        .matching(VARIABLE_BINDING_DEF_MATCHING_RULE_NAME)
                        .or()
                        .item(MappingType.F_SOURCE).contains(getSearchText())
                        .matching(VARIABLE_BINDING_DEF_MATCHING_RULE_NAME)
                        .build();
            }
        };
    }

    protected @Nullable ItemPathType getRefPath(@NotNull PrismContainerValueWrapper<MappingType> mappingWrapper) {
        try {
            PrismPropertyWrapper<ItemPathType> refProperty =
                    mappingWrapper.findProperty(AbstractAttributeMappingsDefinitionType.F_REF);
            return refProperty != null && refProperty.getValue() != null
                    ? refProperty.getValue().getRealValue()
                    : null;
        } catch (SchemaException e) {
            getPageBase().error("Couldn't get ref attribute path: " + e.getMessage());
            return null;
        }
    }

    protected String getNewObjectButtonCssClass() {
        return "btn btn-outline-primary ml-auto";
    }

    protected void initPanelToolbarButtons(@NotNull RepeatingView toolbar) {
        var toggleButton = createToggleSuggestionVisibilityButton(
                getPageBase(),
                toolbar.newChildId(),
                suggestionToggleModel,
                this::refreshAndDetach,
                null);

        toggleButton.add(new VisibleBehaviour(this::isSuggestionSwitchSupported));
        toolbar.add(toggleButton);
        toolbar.add(actions.createLegend(toolbar.newChildId()));
    }

    protected void performOnEditMapping(
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
        // extension hook
    }

    public boolean isValidFormComponents(@Nullable AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    protected IModel<PrismContainerValueWrapper<P>> getValueModel() {
        return refAttributeDefValue;
    }

    protected final @NotNull LoadableModel<PrismContainerDefinition<MappingType>> getMappingTypeDefinition() {
        return WebComponentUtil.getContainerDefinitionModel(MappingType.class);
    }

    protected @NotNull IModel<? extends PrismContainerDefinition<?>> getRefColumnDefinitionModel() {
        return Model.of(PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(ResourceAttributeDefinitionType.class));
    }

    protected MappingDirection getMappingDirectionType() {
        return mappingDirectionModel.getObject();
    }

    protected final boolean isInbound() {
        return getMappingDirectionType() == MappingDirection.INBOUND;
    }

    protected final boolean isOutbound() {
        return getMappingDirectionType() == MappingDirection.OUTBOUND;
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
            @Nullable PrismContainerValue<MappingType> value,
            @Nullable AjaxRequestTarget target) {
        return createNewVirtualMappingValue(
                value,
                getValueModel(),
                getMappingDirectionType(),
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                AbstractAttributeMappingsDefinitionType.F_REF,
                getPageBase(),
                target);
    }

    protected void createDuplicateValuePerform(PrismContainerValue<MappingType> value, AjaxRequestTarget target) {
        createNewValue(value, target);
        refreshAndDetach(target);
    }

    protected void resolveDeletedItem(@NotNull PrismContainerValueWrapper<MappingType> value) {
        actions.resolveMappingDeletedItem(value);
    }

    protected void deleteItemPerform(@NotNull PrismContainerValueWrapper<MappingType> value) {
        actions.deleteItemPerform(value);
    }

    public void refreshAndDetach(@Nullable AjaxRequestTarget target) {
        noValuePanelModel.reset();
        if (target != null) {
            getTable().refreshAndDetach(target);
        }
    }

    protected @Nullable com.evolveum.midpoint.smart.api.info.StatusInfo<?> getStatusInfo(
            PrismContainerValueWrapper<MappingType> value) {
        return getTable().getStatusInfo(value);
    }

    protected PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> findResourceObjectTypeDefinition() {
        return refAttributeDefValue.getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);
    }

    @NotNull
    public Set<PrismContainerValueWrapper<MappingType>> getAcceptedSuggestionsCache() {
        return acceptedSuggestionsCache;
    }

    protected IModel<Boolean> getSuggestionToggleModel() {
        return suggestionToggleModel;
    }

    protected IModel<MappingUsedFor> getMappingUsedForModel() {
        return mappingUsedForModel;
    }

    protected IModel<String> getSearchTextModel() {
        return searchTextModel;
    }

    protected @NotNull String getSearchText() {
        return searchTextModel.getObject() != null ? searchTextModel.getObject() : "";
    }

    protected @NotNull String getResourceOid() {
        return resourceOid;
    }

    @SuppressWarnings("unchecked")
    protected ColumnTileTable<MappingDataDto, PrismContainerValueWrapper<MappingType>> getTable() {
        return (ColumnTileTable<MappingDataDto, PrismContainerValueWrapper<MappingType>>) get(ID_TABLE);
    }

    protected @Nullable GroupedMappingDataProvider getProvider() {
        if (getTable().getProvider() instanceof GroupedMappingDataProvider provider) {
            return provider;
        }
        return null;
    }

    protected void addAdditionalNoValueToolbarButtons(List<Component> toolbarButtonsList, String idButton) {
        // extension hook
    }

    public PrismContainerValueWrapper<MappingType> acceptSuggestionItemPerformed(
            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            @NotNull AjaxRequestTarget target) {
        PrismContainerValueWrapper<MappingType> newValue = createNewValue(rowModel.getObject().getNewValue(), target);
        deleteItemPerform(rowModel.getObject());
        return newValue;
    }

    protected Component createMappingTypeDropdownButton(String idButton) {
        return actions.createMappingTypeDropdownButton(idButton);
    }

    protected abstract ResourceType getResourceType();

    protected void buildSimulationResultPanel(
            AjaxRequestTarget target,
            IModel<com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType> simulationResultTypeModel) {
        // extension hook
    }

    protected boolean isSuggestionSwitchSupported() {
        return true;
    }

    protected boolean isSimulationSupported() {
        return true;
    }

    SmartMappingActions<P> getActions() {
        return actions;
    }

}
