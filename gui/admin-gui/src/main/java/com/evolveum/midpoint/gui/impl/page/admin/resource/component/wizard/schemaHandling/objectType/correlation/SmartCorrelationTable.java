/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.SmartSuggestConfirmationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-select tile table for correlation items.
 */
public class SmartCorrelationTable
        extends MultiSelectContainerTileTablePanel<PrismContainerValueWrapper<ItemsSubCorrelatorType>, ItemsSubCorrelatorType> {

    private static final int MAX_TILE_COUNT = 4;

    private final String resourceOid;

    public SmartCorrelationTable(
            @NotNull String id,
            @NotNull UserProfileStorage.TableId tableId,
            @NotNull IModel<ViewToggle> toggleView,
            @NotNull IModel<List<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> model,
            @NotNull String resourceOid) {
        super(id, tableId, toggleView, model);
        this.resourceOid = resourceOid;
        setDefaultPagingSize(tableId);
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
        return new SmartCorrelationTilePanel(id, model);
    }

    @Override
    protected void togglePanelItemSelectPerformed(
            AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
        ViewToggle value = item.getObject().getValue();
        add(AttributeModifier.replace("class",
                java.util.Objects.equals(value, ViewToggle.TABLE) ? "card" : ""));
        super.togglePanelItemSelectPerformed(target, item);
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
    protected List<IColumn<PrismContainerValueWrapper<ItemsSubCorrelatorType>, String>> createColumns() {
        List<IColumn<PrismContainerValueWrapper<ItemsSubCorrelatorType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

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

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_ENABLED,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        return columns;
    }

    @Override
    protected IModel<List<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> getSelectedItemsModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> load() {
                List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> all = getListModel().getObject();
                if (all == null || all.isEmpty()) {
                    return List.of();
                }
                return all.stream()
                        .filter(PrismContainerValueWrapper::isSelected)
                        .toList();
            }
        };
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
        return "col-12 col-sm-6 col-md-4 col-lg-3 p-2";
    }

    @Override
    protected String getTileContainerCssClass() {
        return "justify-content-left pt-2 ";
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

    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttonsList = new ArrayList<>();
        buttonsList.add(createNewObjectPerformButton(idButton));
        buttonsList.add(createSuggestObjectButton(idButton));
        return buttonsList;
    }

    private @NotNull AjaxIconButton createNewObjectPerformButton(String idButton) {
        AjaxIconButton newObjectButton = new AjaxIconButton(idButton,
                Model.of(GuiStyleConstants.CLASS_ADD_NEW_OBJECT),
                createStringResource("SmartCorrelationTable.button.addNewCorrelationItem")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // TODO
            }
        };
        newObjectButton.showTitleAsLabel(true);
        newObjectButton.add(AttributeAppender.replace("class", "btn btn-primary mr-2"));
        return newObjectButton;
    }

    private AjaxIconButton createSuggestObjectButton(String idButton) {
        AjaxIconButton suggestObjectButton = new AjaxIconButton(idButton,
                Model.of(GuiStyleConstants.CLASS_ICON_WIZARD),
                createStringResource("SmartIntegration.suggestNew")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                SmartSuggestConfirmationPanel dialog = new SmartSuggestConfirmationPanel(getPageBase().getMainPopupBodyId(),
                        createStringResource("SmartIntegration.suggestNew")) {

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
//                        onSuggestValue(
//                                null, createContainerModel(), target);
                    }
                };
                getPageBase().showMainPopup(dialog, target);
            }
        };
        suggestObjectButton.showTitleAsLabel(true);
        suggestObjectButton.add(AttributeAppender.replace("class", "btn btn-default mr-2"));
        suggestObjectButton.add(new VisibleBehaviour(this::isSuggestButtonVisible));
        suggestObjectButton.setOutputMarkupId(true);
        return suggestObjectButton;
    }

    protected boolean isSuggestButtonVisible() {
        return true;
    }

}


