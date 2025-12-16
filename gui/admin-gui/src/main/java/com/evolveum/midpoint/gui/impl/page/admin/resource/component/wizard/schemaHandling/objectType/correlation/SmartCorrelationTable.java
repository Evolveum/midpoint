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
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.RequestDetailsRecordDto;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
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
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.events.Event;

import java.io.Serial;
import java.time.Duration;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.extractCorrelationItemListWrapper;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.*;
import static com.evolveum.midpoint.web.component.dialog.RequestDetailsRecordDto.initDummyCorrelationPermissionData;

/**
 * Multi-select tile table for correlation items.
 */
public class SmartCorrelationTable
        extends MultiSelectContainerActionTileTablePanel<PrismContainerValueWrapper<ItemsSubCorrelatorType>, ItemsSubCorrelatorType, TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> {

    private static final String CLASS_DOT = SmartCorrelationTable.class.getName() + ".";
    private static final String OP_SUGGEST_CORRELATION_RULES = CLASS_DOT + "suggestCorrelationRules";
    private static final String OP_DELETE_CORRELATION_RULES = CLASS_DOT + "deleteCorrelationRules";
    private static final String OP_SUSPEND_SUGGESTION = CLASS_DOT + "suspendSuggestion";

    private static final int MAX_TILE_COUNT = 4;

    private final String resourceOid;

    private static final Trace LOGGER = TraceManager.getTrace(SmartCorrelationTable.class);

    IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> correlationWrapper;

    public SmartCorrelationTable(
            @NotNull String id,
            @NotNull UserProfileStorage.TableId tableId,
            @NotNull IModel<ViewToggle> toggleView,
            @NotNull IModel<Boolean> switchToggleModel,
            IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> correlationWrapper,
            @NotNull String resourceOid) {
        super(id, tableId, toggleView, switchToggleModel);
        this.resourceOid = resourceOid;
        this.correlationWrapper = correlationWrapper;
        setDefaultPagingSize(tableId, MAX_TILE_COUNT);
        this.setOutputMarkupId(true);
    }

    @Override
    protected IModel<RequestDetailsRecordDto> buildSmartPermissionRecordDto() {
        return () -> new RequestDetailsRecordDto(null, initDummyCorrelationPermissionData());
    }

    @Override
    protected Class<? extends Containerable> getType() {
        return ItemsSubCorrelatorType.class;
    }

    @Contract(value = " -> new", pure = true)
    protected @NotNull LoadableModel<PrismContainerDefinition<ItemsSubCorrelatorType>> getCorrelationItemsDefinition() {
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
    protected void customizeNewRowItem(PrismContainerValueWrapper<ItemsSubCorrelatorType> value, Item<PrismContainerValueWrapper<ItemsSubCorrelatorType>> item) {
        super.customizeNewRowItem(value, item);
        applySuggestionAutoRefresh(value, item, Duration.ofSeconds(3), this::getStatusInfo, this::refreshAndDetach);
    }

    @Override
    protected void customizeTileItemCss(Component tile, @NotNull TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>> item) {
        super.customizeTileItemCss(tile, item);
        applySuggestionCss(tile, item.getValue(), "card rounded h-100", this::getStatusInfo);
    }

    @Override
    protected TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>> createTileObject(
            @NotNull PrismContainerValueWrapper<ItemsSubCorrelatorType> object) {
        StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(object);
        return new SmartCorrelationTileModel<>(object, resourceOid, statusInfo != null ? statusInfo.getToken() : null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Component createTile(String id, @NotNull IModel<TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> model) {
        PrismContainerValueWrapper<ItemsSubCorrelatorType> value = model.getObject().getValue();
        return new SmartCorrelationTilePanel(id, model) {
            @Override
            public List<InlineMenuItem> createMenuItems() {
                return getInlineMenuItems(value);
            }

            @Override
            protected void onFooterButtonClick(AjaxRequestTarget target) {
                StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(value);
                if (statusInfo != null) {
                    viewEditItemPerformed(target, () -> value, statusInfo);
                    return;
                }
                editItemPerformed(target, () -> value, false);
            }

            @Override
            protected void initSuggestionActionButton(@NotNull RepeatingView buttonsView) {
                buttonsView.add(createDiscardButton(buttonsView.newChildId(), () -> value));
                buttonsView.add(createAcceptButton(buttonsView.newChildId(), () -> value));
            }

            @Override
            protected void onFinishGeneration(AjaxRequestTarget target) {
                if (getProvider() instanceof MultivalueContainerListDataProvider provider) {
                    provider.getModel().detach();
                }
                refreshAndDetach(target);
            }
        };
    }

    @Override
    protected MultivalueContainerListDataProvider<ItemsSubCorrelatorType> createDataProvider() {
        var dto = StatusAwareDataFactory.createCorrelationModel(
                this,
                getSwitchToggleModel(),
                () -> getContainerModel().getObject(), //detach
                findResourceObjectTypeDefinition(),
                resourceOid);

        return new StatusAwareDataProvider<>(this, Model.of(), dto, false);
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
                PrismContainerValueWrapper<ItemsSubCorrelatorType> wrapper = rowModel.getObject();
                StatusInfo<CorrelationSuggestionsType> suggestionTypeStatusInfo = getStatusInfo(wrapper);

                if (suggestionTypeStatusInfo != null) {
                    ItemsSubCorrelatorType realValue = wrapper.getRealValue();
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
                PrismContainerValueWrapper<ItemsSubCorrelatorType> object = rowModel.getObject();
                List<PrismContainerValueWrapper<CorrelationItemType>> valueWrappers = extractCorrelationItemListWrapper(object);
                CorrelationItemTypePanel correlationItemTypePanel =
                        new CorrelationItemTypePanel(componentId, () -> valueWrappers, 2) {
                            @Override
                            protected boolean isIconStatusVisible() {
                                return getStatusInfo(object) != null;
                            }
                        };
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
                @NotNull String efficiency = extractEfficiencyFromSuggestedCorrelationItemWrapper(iModel.getObject());
                Label label = new Label(s, () -> efficiency);
                label.setOutputMarkupId(true);
                item.add(label);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_ENABLED,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> cellItem,
                    String componentId, IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel) {
                StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(rowModel.getObject());
                if (statusInfo != null && statusInfo.getStatus() == OperationResultStatusType.SUCCESS) {
                    RepeatingView buttonsView = new RepeatingView(componentId);
                    buttonsView.add(createDiscardButton(buttonsView.newChildId(), rowModel));
                    buttonsView.add(createAcceptButton(buttonsView.newChildId(), rowModel));
                    cellItem.add(AttributeModifier.append("class", " btn-group btn-group-sm gap-2 d-flex"));
                    cellItem.add(buttonsView);
                    return;
                }

                super.populateItem(cellItem, componentId, rowModel);
            }
        });
        return columns;
    }

    private void buildSuggestionNameColumnComponent(
            @NotNull Item<ICellPopulator<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> cellItem,
            String componentId,
            @NotNull StatusInfo<CorrelationSuggestionsType> statusInfo,
            ItemsSubCorrelatorType realValue) {

        OperationResultStatusType status = statusInfo.getStatus();
        LoadableModel<String> displayNameModel = new LoadableModel<>() {
            @Override
            protected String load() {
                if (status.equals(OperationResultStatusType.SUCCESS)) {
                    return realValue != null ? realValue.getName() : " - ";
                }

                String textKey = SmartIntegrationUtils.SuggestionUiStyle.from(statusInfo).textKey;
                return createStringResource(textKey).getString();
            }
        };

        LabelWithBadgePanel labelWithBadgePanel = buildSuggestionNameLabel(componentId, statusInfo, displayNameModel, status);
        cellItem.add(labelWithBadgePanel);
    }

    @Override
    protected String getDiscardButtonCssClass() {
        return "col p-2 btn btn-default rounded";
    }

    @Override
    protected String getAcceptButtonCssClass() {
        return "col p-2 btn btn-primary rounded";
    }

    @Override
    protected void onAcceptPerformed(AjaxRequestTarget target, @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> toAccept) {
        StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(toAccept.getObject());
        acceptSuggestionItemPerformed(target, toAccept, statusInfo);
    }

    @Override
    protected ButtonInlineMenuItem createEditInlineMenu(PrismContainerValueWrapper<ItemsSubCorrelatorType> tileModel) {
        ButtonInlineMenuItem editInlineMenu = super.createEditInlineMenu(tileModel);
        setVisibilityBySuggestion(editInlineMenu, false, this::getStatusInfo);
        return editInlineMenu;
    }

    @Override
    protected @NotNull InlineMenuItem createDuplicateInlineMenu(PrismContainerValueWrapper<ItemsSubCorrelatorType> tileModel) {
        InlineMenuItem duplicateInlineMenu = super.createDuplicateInlineMenu(tileModel);
        setVisibilityBySuggestion(duplicateInlineMenu, false, this::getStatusInfo);
        return duplicateInlineMenu;
    }

    @Override
    public @NotNull List<InlineMenuItem> getInlineMenuItems(PrismContainerValueWrapper<ItemsSubCorrelatorType> tileModel) {
        List<InlineMenuItem> inlineMenuItems = super.getInlineMenuItems(tileModel);
        inlineMenuItems.add(createViewRuleInlineMenu(tileModel));
        inlineMenuItems.add(createSuggestionOperationInlineMenu(getPageBase(), this::getStatusInfo, this::refreshAndDetach));
        inlineMenuItems.add(createSuggestionDetailsInlineMenu(getPageBase(), this::getStatusInfo));
        return inlineMenuItems;
    }

    protected ButtonInlineMenuItem createViewRuleInlineMenu(PrismContainerValueWrapper<ItemsSubCorrelatorType> tileModel) {
        ButtonInlineMenuItem buttonInlineMenuItem = new ButtonInlineMenuItem(
                createStringResource("SmartCorrelationTilePanel.viewRuleLink")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_PREVIEW);
            }

            @Override
            public @NotNull InlineMenuItemAction initAction() {
                ColumnMenuAction<PrismContainerValueWrapper<ItemsSubCorrelatorType>> columnMenuAction = new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() != null) {
                            viewEditItemPerformed(target, getRowModel(), getStatusInfo(getRowModel().getObject()));
                        }
                    }
                };

                if (tileModel != null) {
                    columnMenuAction.setRowModel(() -> tileModel);
                }
                return columnMenuAction;
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };

        setVisibilityBySuggestion(buttonInlineMenuItem, true, this::getStatusInfo);
        return buttonInlineMenuItem;
    }

    @Override
    protected void onSuggestNewPerformed(AjaxRequestTarget target) {
        PageBase pageBase = getPageBase();
        ResourceObjectTypeIdentification objectTypeIdentification = getResourceObjectTypeIdentification();
        SmartIntegrationService service = pageBase.getSmartIntegrationService();
        pageBase.taskAwareExecutor(target, OP_SUGGEST_CORRELATION_RULES)
                .runVoid((task, result) -> {
                    service.submitSuggestCorrelationOperation(resourceOid, objectTypeIdentification, task, result);
                    refreshAndDetach(target);
                });
    }

    @Override
    protected LoadableDetachableModel<PrismContainerWrapper<ItemsSubCorrelatorType>> getContainerModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerWrapper<ItemsSubCorrelatorType> load() {
                if (correlationWrapper == null || correlationWrapper.getObject() == null) {
                    return null;
                }

                try {
                    return correlationWrapper.getObject().findContainer(
                            ItemPath.create(CorrelationDefinitionType.F_CORRELATORS, CompositeCorrelatorType.F_ITEMS));
                } catch (SchemaException e) {
                    LOGGER.error("Cannot get correlation items container: {}", e.getMessage(), e);
                    return null;
                }
            }
        };
    }

    @Override
    public void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> toDelete, boolean refresh) {
        if (noSelectedItemsWarn(getPageBase(), target, toDelete)) {return;}

        Task task = getPageBase().createSimpleTask(OP_DELETE_CORRELATION_RULES);
        toDelete.forEach(value -> {
            StatusInfo<CorrelationSuggestionsType> status = getStatusInfo(value);
            if (status != null) {
                PrismContainerValueWrapper<CorrelationSuggestionType> parentContainerValue = value
                        .getParentContainerValue(CorrelationSuggestionType.class);
                if (parentContainerValue == null || parentContainerValue.getRealValue() == null) {
                    return;
                }
                removeCorrelationTypeSuggestionNew(getPageBase(), status, parentContainerValue.getRealValue(), task, task.getResult());
            } else {
                resolveDeletedItem(value);
            }
        });
        if (refresh) {
            refreshAndDetach(target);
        }
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

    @Override
    protected IModel<List<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> getSelectedItemsModel() {
        return getSelectedContainerItemsModel();
    }

    @Override
    protected void deselectItem(PrismContainerValueWrapper<ItemsSubCorrelatorType> entry) {
    }

    @Override
    protected IModel<String> getItemLabelModel(PrismContainerValueWrapper<ItemsSubCorrelatorType> entry) {
        return null;
    }

    private @Nullable ResourceObjectTypeIdentification getResourceObjectTypeIdentification() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentWrapper = findResourceObjectTypeDefinition();
        if (parentWrapper == null || parentWrapper.getRealValue() == null) {
            return null;
        }
        ResourceObjectTypeDefinitionType realValue = parentWrapper.getRealValue();
        return ResourceObjectTypeIdentification.of(realValue.getKind(), realValue.getIntent());
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttonsList = super.createToolbarButtonsList(idButton);
        return buttonsList;
    }

    @Override
    protected RepeatingView createTableActionToolbar(String id) {
        return super.createTableActionToolbar(id);
    }

    @Override
    protected void onCreateNewObjectPerform(AjaxRequestTarget target) {
        editItemPerformed(target, null, false);
    }

    protected PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> findResourceObjectTypeDefinition() {
        return correlationWrapper.getObject().getParentContainerValue(ResourceObjectTypeDefinitionType.class);
    }

    protected <C extends Containerable> @Nullable StatusInfo<CorrelationSuggestionsType> getStatusInfo(PrismContainerValueWrapper<C> value) {
        if (getProvider() instanceof StatusAwareDataProvider<ItemsSubCorrelatorType> provider) {
            //noinspection unchecked
            return (StatusInfo<CorrelationSuggestionsType>) provider.getSuggestionInfo(
                    (PrismContainerValueWrapper<ItemsSubCorrelatorType>) value);
        }
        return null;
    }

    public void viewEditItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
            StatusInfo<CorrelationSuggestionsType> statusInfo) {
    }

    public void acceptSuggestionItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
            StatusInfo<CorrelationSuggestionsType> statusInfo) {
    }

    protected boolean isHeaderPanelHeaderVisible() {
        return false;
    }

}


