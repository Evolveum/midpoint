/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithBadgePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.ResourceContentSearchDto;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.ResourceContentStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadCorrelationSuggestionWrappers;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.getAiBadgeModel;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeCorrelationTypeSuggestion;

/**
 * Multi-select tile table for correlation items.
 */
public class SmartCorrelationTable
        extends MultiSelectContainerActionTileTablePanel<PrismContainerValueWrapper<ItemsSubCorrelatorType>, ItemsSubCorrelatorType> {

    private static final String CLASS_DOT = MultiSelectContainerActionTileTablePanel.class.getName() + ".";
    private static final String OP_SUGGEST_CORRELATION_RULES = CLASS_DOT + "suggestCorrelationRules";
    private static final String OP_DELETE_CORRELATION_RULES = CLASS_DOT + "deleteCorrelationRules";

    private static final int MAX_TILE_COUNT = 4;

    private final String resourceOid;

    PrismContainerValueWrapper<CorrelationDefinitionType> correlationWrapper;
    Map<ItemsSubCorrelatorType, StatusInfo<CorrelationSuggestionType>> suggestionByWrapper;

    public SmartCorrelationTable(
            @NotNull String id,
            @NotNull UserProfileStorage.TableId tableId,
            @NotNull IModel<ViewToggle> toggleView,
            PrismContainerValueWrapper<CorrelationDefinitionType> correlationWrapper,
            @NotNull String resourceOid) {
        super(id, tableId, toggleView);
        this.resourceOid = resourceOid;
        this.correlationWrapper = correlationWrapper;
        setDefaultPagingSize(tableId);
        this.setOutputMarkupId(true);
    }

    @Override
    protected void customizeNewRowItem(PrismContainerValueWrapper<ItemsSubCorrelatorType> value, Item<PrismContainerValueWrapper<ItemsSubCorrelatorType>> item) {
        super.customizeNewRowItem(value, item);

        OperationResultStatusType status = statusFor(item.getModelObject());
        if (status != null) {
            item.add(AttributeModifier.append("class", SmartIntegrationUtils.SuggestionUiStyle.from(status).rowClass));
        }
    }

    @Override
    protected void customizeTileItemCss(Component tile, @NotNull TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>> item) {
        super.customizeTileItemCss(tile, item);

        OperationResultStatusType status = statusFor(item.getValue());
        if (status != null) {
            tile.add(AttributeModifier.replace("class", "card rounded h-100 "
                    + SmartIntegrationUtils.SuggestionUiStyle.from(status).tileClass));
        }
    }

    @Override
    protected Class<? extends Containerable> getType() {
        return ItemsSubCorrelatorType.class;
    }

    @Override
    protected TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>> createTileObject(
            PrismContainerValueWrapper<ItemsSubCorrelatorType> object) {
        StatusInfo<CorrelationSuggestionType> statusInfo = suggestionByWrapper.get(object.getRealValue());
        return new SmartCorrelationTileModel<>(object, resourceOid, null, statusInfo != null ? statusInfo.getToken() : null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Component createTile(String id, @NotNull IModel<TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> model) {
        return new SmartCorrelationTilePanel(id, model) {
            @Override
            public List<InlineMenuItem> createMenuItems() {
                return getInlineMenuItems(model.getObject().getValue());
            }

            @Override
            protected void onFooterButtonClick(AjaxRequestTarget target) {
                PrismContainerValueWrapper<ItemsSubCorrelatorType> value = model.getObject().getValue();
                editItemPerformed(target, () -> value, false);
            }

            @Override
            protected void onAcceptButtonClick(AjaxRequestTarget target) {
                PrismContainerValueWrapper<ItemsSubCorrelatorType> value = model.getObject().getValue();
                editItemPerformed(target, () -> value, false);
            }

            @Override
            protected void onDiscardButtonClick(AjaxRequestTarget target) {
                deleteItemPerformed(target, Collections.singletonList(model.getObject().getValue()));
                refreshAndDetach(target);
                target.add(SmartCorrelationTable.this);
            }

            @Override
            protected void onFinishGeneration(AjaxRequestTarget target) {
                if (getProvider() instanceof MultivalueContainerListDataProvider provider) {
                    provider.getModel().detach();
                }
                refreshAndDetach(target);
            }

            @Override
            protected void onRefresh(AjaxRequestTarget target) {
                refreshAndDetach(target);
            }
        };
    }

    @Override
    protected MultivalueContainerListDataProvider<ItemsSubCorrelatorType> createProvider() {
        return super.createProvider();
    }

    @Override
    protected MultivalueContainerListDataProvider<ItemsSubCorrelatorType> createDataProvider() {
        LoadableDetachableModel<List<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> containerModel = new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> load() {
                List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> allValues = new ArrayList<>();
                try {
                    PrismContainerValueWrapper<CorrelationDefinitionType> object = correlationWrapper;
                    if (object == null) {
                        return List.of();
                    }
                    PrismContainerWrapper<ItemsSubCorrelatorType> container = object.findContainer(
                            ItemPath.create(
                                    CorrelationDefinitionType.F_CORRELATORS,
                                    CompositeCorrelatorType.F_ITEMS));

                    List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> values = container.getValues();
                    allValues.addAll(values);

                    if (getSwitchToggleModel().getObject().equals(Boolean.TRUE)) {
                        Task task = getPageBase().createSimpleTask("Loading correlation type suggestions");
                        OperationResult result = task.getResult();

                        SmartIntegrationStatusInfoUtils.@NotNull CorrelationSuggestionProviderResult suggestionProviderResult =
                                loadCorrelationSuggestionWrappers(getPageBase(), resourceOid, task, result);
                        allValues.addAll(suggestionProviderResult.wrappers());
                        suggestionByWrapper = suggestionProviderResult.suggestionByWrapper();
                    }

                    return allValues;
                } catch (SchemaException e) {
                    throw new RuntimeException("Error while loading items sub-correlator types", e);
                }
            }
        };

        return new MultivalueContainerListDataProvider<>(SmartCorrelationTable.this, Model.of(), containerModel);
    }

    @Contract(value = " -> new", pure = true)
    private @NotNull LoadableModel<PrismContainerDefinition<ItemsSubCorrelatorType>> getCorrelationItemsDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<ItemsSubCorrelatorType> load() {
                ComplexTypeDefinition resourceDef =
                        PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(ResourceType.class);
                return resourceDef.findContainerDefinition(
                        ItemPath.create(
                                ResourceType.F_SCHEMA_HANDLING,
                                SchemaHandlingType.F_OBJECT_TYPE,
                                ResourceObjectTypeDefinitionType.F_CORRELATION,
                                CorrelationDefinitionType.F_CORRELATORS,
                                CompositeCorrelatorType.F_ITEMS));
            }
        };
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ItemsSubCorrelatorType>, String>> createDomainColumns() {
        List<IColumn<PrismContainerValueWrapper<ItemsSubCorrelatorType>, String>> columns = new ArrayList<>();

        IModel<PrismContainerDefinition<ItemsSubCorrelatorType>> reactionDef = getCorrelationItemsDefinition();

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel) {
                ItemsSubCorrelatorType realValue = rowModel.getObject().getRealValue();
                StatusInfo<CorrelationSuggestionType> suggestionTypeStatusInfo = suggestionByWrapper.get(realValue);

                if (suggestionTypeStatusInfo != null) {
                    buildSuggestionNameColumnComponent(cellItem, componentId, suggestionTypeStatusInfo, realValue);
                    return;
                }

                super.populateItem(cellItem, componentId, rowModel);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_ITEM,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel) {
                List<CorrelationItemType> item = rowModel.getObject().getRealValue().getItem();
                CorrelationItemTypePanel correlationItemTypePanel =
                        new CorrelationItemTypePanel(componentId, () -> item, 2);
                correlationItemTypePanel.setOutputMarkupId(true);
                cellItem.add(correlationItemTypePanel);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemPath.create(
                        ItemsSubCorrelatorType.F_COMPOSITION,
                        CorrelatorCompositionDefinitionType.F_WEIGHT),
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemPath.create(
                        ItemsSubCorrelatorType.F_COMPOSITION,
                        CorrelatorCompositionDefinitionType.F_TIER),
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        columns.add(new AbstractColumn<>(createStringResource("ItemsSubCorrelatorType.efficiency")) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> item, String s,
                    IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> iModel) {
                Label label = new Label(s, Model.of("-"));
                label.setOutputMarkupId(true);
                item.add(label);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_ENABLED,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));
        return columns;
    }

    private void buildSuggestionNameColumnComponent(
            @NotNull Item<ICellPopulator<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> cellItem,
            String componentId,
            @NotNull StatusInfo<CorrelationSuggestionType> suggestionTypeStatusInfo,
            ItemsSubCorrelatorType realValue) {
        OperationResultStatusType status = suggestionTypeStatusInfo.getStatus();

        LoadableModel<String> displayNameModel = new LoadableModel<>() {
            @Override
            protected String load() {
                if (status.equals(OperationResultStatusType.IN_PROGRESS)) {
                    return createStringResource("ResourceObjectTypesPanel.suggestion.inProgress").getString();
                }
                return realValue != null ? realValue.getDisplayName() : " - ";
            }
        };

        LabelWithBadgePanel labelWithBadgePanel = new LabelWithBadgePanel(
                componentId, getAiBadgeModel(), displayNameModel) {
            @Override
            protected boolean isIconVisible() {
                return status.equals(OperationResultStatusType.IN_PROGRESS);
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCss() {
                return GuiStyleConstants.ICON_FA_SPINNER + " text-info";
            }

            @Contract(pure = true)
            @Override
            protected @Nullable String getLabelCss() {
                return status.equals(OperationResultStatusType.IN_PROGRESS)
                        ? " text-info"
                        : null;
            }

            @Override
            protected boolean isBadgeVisible() {
                return status.equals(OperationResultStatusType.SUCCESS);
            }
        };
        labelWithBadgePanel.setOutputMarkupId(true);
        cellItem.add(labelWithBadgePanel);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected IModel<Search> createSearchModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected Search<?> load() {
                SearchBuilder<?> searchBuilder = new SearchBuilder<>(ComplexTypeDefinitionType.class)
                        .modelServiceLocator(getPageBase());
                return searchBuilder.build();
            }
        };
    }

    protected void setDefaultPagingSize(UserProfileStorage.@NotNull TableId tableId) {
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        userProfile.setPagingSize(tableId, MAX_TILE_COUNT);
    }

    @Override
    protected String getAdditionalTableCssClasses() {
        return "table-td-middle";
    }

    @Override
    protected String getAdditionalFooterCss() {
        return "bg-white border-top";
    }

    @Override
    protected String getAdditionalBoxCssClasses() {
        return " m-0";
    }

    @Override
    protected String getTilesFooterCssClasses() {
        return "pt-1 border-0";
    }

    @Override
    protected String getTileCssStyle() {
        return "";
    }

    @Override
    protected String getTileCssClasses() {
        return "col-12 col-sm-12 col-md-6 col-lg-3 p-2";
    }

    @Override
    protected String getTileContainerCssClass() {
        return "row justify-content-left pt-2 ";
    }

    @Override
    protected String getTilesContainerAdditionalClass() {
        return "";
    }

    @Override
    protected void deselectItem(PrismContainerValueWrapper<ItemsSubCorrelatorType> entry) {
    }

    @Override
    protected IModel<String> getItemLabelModel(PrismContainerValueWrapper<ItemsSubCorrelatorType> entry) {
        return null;
    }

    @Override
    protected IModel<List<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> getSelectedItemsModel() {
        return getSelectedContainerItemsModel();
    }

    @Override
    protected boolean isSelectedItemsPanelVisible() {
        return false;
    }

    @Override
    protected boolean isClickableRow() {
        return false;
    }

    @Override
    protected boolean isTogglePanelVisible() {
        return true;
    }

    @Override
    protected String getAdditionalHeaderContainerCssClasses() {
        return isTileViewVisible() ? "border-0 p-0" : super.getAdditionalHeaderContainerCssClasses();
    }

    private ResourceContentStorage getResourceAccountContentStorage() {
        return getPageBase().getSessionStorage().getResourceContentStorage(ShadowKindType.ACCOUNT);
    }

    @Override
    protected void onSuggestNewPerformed(AjaxRequestTarget target) {
        PageBase pageBase = getPageBase();
        ResourceContentStorage pageStorage = getResourceAccountContentStorage();
        ResourceContentSearchDto contentSearch = pageStorage.getContentSearch();
        ShadowKindType kind = contentSearch.getKind();
        String intent = contentSearch.getIntent();

        ResourceObjectTypeIdentification objectTypeIdentification = ResourceObjectTypeIdentification.of(kind, intent);

        SmartIntegrationService service = pageBase.getSmartIntegrationService();
        pageBase.taskAwareExecutor(target, OP_SUGGEST_CORRELATION_RULES)
                .runVoid((task, result) -> {
                    var suggestion = service.submitSuggestCorrelationOperation(
                            resourceOid, objectTypeIdentification,
                            task, result);
                    target.add(this);
                });

        super.refreshAndDetach(target);
    }

    private @Nullable OperationResultStatusType statusFor(
            @NotNull PrismContainerValueWrapper<ItemsSubCorrelatorType> wrapper) {
        StatusInfo<CorrelationSuggestionType> info = suggestionByWrapper.get(wrapper.getRealValue());
        return info != null ? info.getStatus() : null;
    }

    @Override
    public boolean displayNoValuePanel() {
        return getProvider().size() == 0;
    }

    @Override
    public void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) {
            warn(createStringResource("SmartCorrelationTable.message.noItemsSelected").getString());
            target.add(getPageBase().getFeedbackPanel());
            target.add(getPageBase().getFeedbackPanel().getParent());
            return;
        }

        Task task = getPageBase().createSimpleTask(OP_DELETE_CORRELATION_RULES);
        toDelete.forEach(value -> {
            StatusInfo<CorrelationSuggestionType> status = suggestionByWrapper.get(value.getRealValue());
            if (status != null) {
                removeCorrelationTypeSuggestion(getPageBase(), status, task, task.getResult());

            } else {
                if (value.getStatus() == ValueStatus.ADDED) {
                    IModel<PrismContainerWrapper<ItemsSubCorrelatorType>> containerModel = getContainerModel(value, null);
                    PrismContainerWrapper<ItemsSubCorrelatorType> wrapper = containerModel != null ? containerModel.getObject() : null;
                    if (wrapper != null) {
                        wrapper.getValues().remove(value);
                    }
                } else {
                    value.setStatus(ValueStatus.DELETED);
                }
                value.setSelected(false);
            }
        });
        refreshAndDetach(target);
        refresh(target);
    }

    @Override
    protected void onToggleSwitchPerformed(@NotNull AjaxRequestTarget target) {
        super.onToggleSwitchPerformed(target);
    }
}


