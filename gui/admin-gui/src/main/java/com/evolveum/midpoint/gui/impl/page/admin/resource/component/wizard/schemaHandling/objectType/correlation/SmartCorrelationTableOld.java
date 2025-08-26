/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanelOld;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
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
import java.util.List;
import java.util.Map;

/**
 * Multi-select tile table for correlation items.
 */
public class SmartCorrelationTableOld
        extends MultiSelectContainerActionTileTablePanelOld<PrismContainerValueWrapper<ItemsSubCorrelatorType>, ItemsSubCorrelatorType> {

    private static final String CLASS_DOT = MultiSelectContainerActionTileTablePanelOld.class.getName() + ".";
    private static final String OP_SUGGEST_CORRELATION_RULES = CLASS_DOT + "suggestCorrelationRules";

    private static final int MAX_TILE_COUNT = 4;

    private final String resourceOid;

    IModel<Map<PrismContainerValueWrapper<ItemsSubCorrelatorType>, StatusInfo<CorrelationSuggestionType>>> suggestionByWrapper;

    public SmartCorrelationTableOld(
            @NotNull String id,
            @NotNull UserProfileStorage.TableId tableId,
            @NotNull IModel<ViewToggle> toggleView,
            @NotNull IModel<List<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> model,
            IModel<Map<PrismContainerValueWrapper<ItemsSubCorrelatorType>, StatusInfo<CorrelationSuggestionType>>> suggestionByWrapper,
            @NotNull String resourceOid) {
        super(id, tableId, toggleView, model);
        this.resourceOid = resourceOid;
        this.suggestionByWrapper = suggestionByWrapper;
        setDefaultPagingSize(tableId);
    }


    @Override
    protected void customizeNewRowItem(PrismContainerValueWrapper<ItemsSubCorrelatorType> value, Item<PrismContainerValueWrapper<ItemsSubCorrelatorType>> item) {
        super.customizeNewRowItem(value, item);

        OperationResultStatusType status = statusFor(value);
        if (status != null) {
            item.add(AttributeModifier.append("class", SmartIntegrationUtils.SuggestionUiStyle.from(status).rowClass));
        }
    }

    @Override
    protected void customizeTileItemCss(Component tile, TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>> item) {
        super.customizeTileItemCss(tile, item);

        OperationResultStatusType status = statusFor(item.getValue());
        if (status != null) {
            tile.add(AttributeModifier.replace("class", "card rounded h-100 "
                    + SmartIntegrationUtils.SuggestionUiStyle.from(status).rowClass));
        }
    }

    @Override
    protected Class<? extends Containerable> getType() {
        return ItemsSubCorrelatorType.class;
    }

    @Override
    protected TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>> createTileObject(
            PrismContainerValueWrapper<ItemsSubCorrelatorType> object) {
        return new SmartCorrelationTileModel<>(object, resourceOid, null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Component createTile(String id, IModel<TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> model) {
        return new SmartCorrelationTilePanel(id, model, null) {
            @Override
            public List<InlineMenuItem> createMenuItems() {
                return getInlineMenuItems(model.getObject().getValue());
            }

            @Override
            protected void onFooterButtonClick(AjaxRequestTarget target) {
                PrismContainerValueWrapper<ItemsSubCorrelatorType> value = model.getObject().getValue();
                editItemPerformed(target, () -> value, false);
            }
        };
    }

    @Override
    protected MultivalueContainerListDataProvider<ItemsSubCorrelatorType> createProvider() {
        return super.createProvider();
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
                getPageBase()));

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
        return "border-0";
    }

    @Override
    protected void newItemPerformed(PrismContainerValue<ItemsSubCorrelatorType> value, AjaxRequestTarget target, AssignmentObjectRelation relationSpec, boolean isDuplicate) {
        super.newItemPerformed(value, target, relationSpec, isDuplicate);
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
        refresh(target);
    }

    private @Nullable OperationResultStatusType statusFor(
            PrismContainerValueWrapper<?> wrapper) {
        StatusInfo<CorrelationSuggestionType> info = suggestionByWrapper.getObject().get(wrapper);
        return info != null ? info.getStatus() : null;
    }

    @Override
    public boolean displayNoValuePanel() {
        return createProvider().size() == 0;
    }

}


