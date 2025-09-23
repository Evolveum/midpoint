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
import com.evolveum.midpoint.gui.impl.component.data.provider.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
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
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.extractEfficiencyFromSuggestedCorrelationItemWrapper;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadCorrelationSuggestionWrappers;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.extractCorrelationItemListWrapper;

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
            IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> correlationWrapper,
            @NotNull String resourceOid) {
        super(id, tableId, toggleView);
        this.resourceOid = resourceOid;
        this.correlationWrapper = correlationWrapper;
        setDefaultPagingSize(tableId, MAX_TILE_COUNT);
        this.setOutputMarkupId(true);
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

        addAjaxTimeBehaviorIfRequested(value, item);
    }

    private void addAjaxTimeBehaviorIfRequested(
            PrismContainerValueWrapper<ItemsSubCorrelatorType> value,
            Item<PrismContainerValueWrapper<ItemsSubCorrelatorType>> item) {
        StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(value);
        if (statusInfo != null && statusInfo.getStatus() != null) {
            item.add(AttributeModifier.append("class", SuggestionUiStyle.from(statusInfo).rowClass));

            boolean executing = statusInfo.isExecuting() && statusInfo.getStatus() != OperationResultStatusType.FATAL_ERROR;
            if (executing) {
                AbstractAjaxTimerBehavior timer = new AbstractAjaxTimerBehavior(Duration.ofSeconds(3)) {
                    @Override
                    protected void onTimer(@NotNull AjaxRequestTarget target) {
                        target.add(item);
                        StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(value);

                        if (statusInfo == null || !statusInfo.isExecuting() || statusInfo.getStatus() == OperationResultStatusType.FATAL_ERROR) {
                            stop(target);
                            refreshAndDetach(target);
                            target.add(item);
                        }
                    }
                };
                item.add(timer);
            }
        }
    }

    @Override
    protected void customizeTileItemCss(Component tile, @NotNull TemplateTile<PrismContainerValueWrapper<ItemsSubCorrelatorType>> item) {
        super.customizeTileItemCss(tile, item);

        StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(item.getValue());
        if (statusInfo != null && statusInfo.getStatus() != null) {
            tile.add(AttributeModifier.replace("class", "card rounded h-100 "
                    + SmartIntegrationUtils.SuggestionUiStyle.from(statusInfo).tileClass));
        }
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
        final Map<PrismContainerValueWrapper<ItemsSubCorrelatorType>, StatusInfo<CorrelationSuggestionsType>> suggestionsIndex = new HashMap<>();

        LoadableDetachableModel<List<PrismContainerValueWrapper<ItemsSubCorrelatorType>>> containerModel =
                new LoadableDetachableModel<>() {
                    @Override
                    protected @NotNull List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> load() {
                        IModel<PrismContainerWrapper<ItemsSubCorrelatorType>> containerModel = getContainerModel();
                        PrismContainerWrapper<ItemsSubCorrelatorType> container = containerModel.getObject();

                        suggestionsIndex.clear();
                        List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> allValues = new ArrayList<>(container != null
                                ? container.getValues() : List.of());
                        if (Boolean.TRUE.equals(getSwitchToggleModel().getObject())) {
                            Task task = getPageBase().createSimpleTask("Loading correlation type suggestions");
                            OperationResult result = task.getResult();

                            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentWrapper = findResourceObjectTypeDefinition();
                            ResourceObjectTypeDefinitionType resourceObjectTypeDefinition = parentWrapper.getRealValue();
                            @NotNull SmartIntegrationStatusInfoUtils.CorrelationSuggestionProviderResult suggestionWrappers =
                                    loadCorrelationSuggestionWrappers(getPageBase(), resourceOid, resourceObjectTypeDefinition, task, result);

                            allValues.addAll(suggestionWrappers.wrappers());
                            suggestionsIndex.putAll(suggestionWrappers.suggestionByWrapper());
                        }

                        return allValues;
                    }
                };

        return new StatusAwareDataProvider<>(this, resourceOid, Model.of(), containerModel, suggestionsIndex::get);
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
                Double efficiency = extractEfficiencyFromSuggestedCorrelationItemWrapper(iModel.getObject());
                Label label = new Label(s, () -> efficiency != null ? String.format("%.2f%%", efficiency) : " - ");
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
            @NotNull StatusInfo<CorrelationSuggestionsType> suggestionTypeStatusInfo,
            ItemsSubCorrelatorType realValue) {
        OperationResultStatusType status = suggestionTypeStatusInfo.getStatus();

        LoadableModel<String> displayNameModel = new LoadableModel<>() {
            @Override
            protected String load() {
                if (status.equals(OperationResultStatusType.SUCCESS)) {
                    return realValue != null ? realValue.getDisplayName() : " - ";
                }

                String textKey = SmartIntegrationUtils.SuggestionUiStyle.from(suggestionTypeStatusInfo).textKey;
                return createStringResource(textKey).getString();
            }
        };

        LabelWithBadgePanel labelWithBadgePanel = new LabelWithBadgePanel(
                componentId, getAiBadgeModel(), displayNameModel) {
            @Override
            protected boolean isIconVisible() {
                return suggestionTypeStatusInfo.isExecuting();
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCss() {
                return GuiStyleConstants.ICON_FA_SPINNER + " fa-spin text-info";
            }

            @Contract(pure = true)
            @Override
            protected @Nullable String getLabelCss() {
                return switch (status) {
                    case IN_PROGRESS -> " text-info";
                    case FATAL_ERROR -> " text-danger";
                    default -> null;
                };
            }

            @Override
            protected boolean isBadgeVisible() {
                return status.equals(OperationResultStatusType.SUCCESS);
            }
        };
        labelWithBadgePanel.setOutputMarkupId(true);
        cellItem.add(labelWithBadgePanel);
    }

    protected AjaxIconButton createDiscardButton(String id, IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel) {
        AjaxIconButton discardButton = new AjaxIconButton(id, Model.of("fa fa-solid fa-x"),
                createStringResource("SmartCorrelationTilePanel.discardButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteItemPerformed(target, Collections.singletonList(rowModel.getObject()));
            }
        };
        discardButton.setOutputMarkupId(true);
        discardButton.add(new TooltipBehavior());
        discardButton.add(AttributeModifier.replace("class", "col p-2 btn btn-default rounded"));
        discardButton.showTitleAsLabel(true);
        return discardButton;
    }

    protected AjaxIconButton createAcceptButton(String id, IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel) {
        AjaxIconButton acceptButton = new AjaxIconButton(id, Model.of("fa fa-check"),
                createStringResource("SmartCorrelationTilePanel.acceptButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(rowModel.getObject());
                acceptSuggestionItemPerformed(target, rowModel, statusInfo);
            }
        };
        acceptButton.setOutputMarkupId(true);
        acceptButton.add(new TooltipBehavior());
        acceptButton.add(AttributeModifier.replace("class", "col p-2 btn btn-success rounded"));
        acceptButton.showTitleAsLabel(true);
        return acceptButton;
    }

    @Override
    protected ButtonInlineMenuItem createEditInlineMenu(PrismContainerValueWrapper<ItemsSubCorrelatorType> tileModel) {
        ButtonInlineMenuItem editInlineMenu = super.createEditInlineMenu(tileModel);
        setVisibilityBySuggestion(editInlineMenu, false);
        return editInlineMenu;
    }

    @Override
    protected @NotNull InlineMenuItem createDuplicateInlineMenu(PrismContainerValueWrapper<ItemsSubCorrelatorType> tileModel) {
        InlineMenuItem duplicateInlineMenu = super.createDuplicateInlineMenu(tileModel);
        setVisibilityBySuggestion(duplicateInlineMenu, false);
        return duplicateInlineMenu;
    }

    @Override
    public @NotNull List<InlineMenuItem> getInlineMenuItems(PrismContainerValueWrapper<ItemsSubCorrelatorType> tileModel) {
        List<InlineMenuItem> inlineMenuItems = super.getInlineMenuItems(tileModel);
        inlineMenuItems.add(createViewRuleInlineMenu(tileModel));
        inlineMenuItems.add(createSuggestionOperationInlineMenu());
        inlineMenuItems.add(createSuggestionDetailsInlineMenu());
        return inlineMenuItems;
    }

    protected ButtonInlineMenuItem createSuggestionDetailsInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.details.suggestion.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                        StatusInfo<CorrelationSuggestionsType> suggestionStatus = getStatusInfo(wrapper);
                        return suggestionStatus != null && suggestionStatus.getStatus() == OperationResultStatusType.FATAL_ERROR;
                    }

                    return false;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                            StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(valueWrapper);
                            HelpInfoPanel helpInfoPanel = new HelpInfoPanel(
                                    getPageBase().getMainPopupBodyId(),
                                    statusInfo != null ? statusInfo::getLocalizedMessage : null) {
                                @Override
                                public StringResourceModel getTitle() {
                                    return createStringResource("ResourceObjectTypesPanel.suggestion.details.title");
                                }

                                @Override
                                protected @NotNull Label initLabel(IModel<String> messageModel) {
                                    Label label = super.initLabel(messageModel);
                                    label.add(AttributeModifier.append("class", "alert alert-danger"));
                                    return label;
                                }

                                @Override
                                public @NotNull Component getFooter() {
                                    Component footer = super.getFooter();
                                    footer.add(new VisibleBehaviour(() -> false));
                                    return footer;
                                }
                            };

                            target.add(getPageBase().getMainPopup());

                            getPageBase().showMainPopup(
                                    helpInfoPanel, target);
                        }
                    }
                };
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }
        };
    }

    protected ButtonInlineMenuItem createSuggestionOperationInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.suspend.generating.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> getLabel() {
                ColumnMenuAction<?> action = (ColumnMenuAction<?>) getAction();
                IModel<?> rowModel = action.getRowModel();
                if (rowModel != null && rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                    StatusInfo<CorrelationSuggestionsType> s = getStatusInfo(wrapper);
                    if (s != null) {
                        if (s.isExecuting() && !s.isSuspended()) {
                            return createStringResource("ResourceObjectTypesPanel.suspend.generating.inlineMenu");
                        }
                        if (s.isSuspended()) {
                            return createStringResource("ResourceObjectTypesPanel.resume.generating.inlineMenu");
                        }
                    }
                }
                return super.getLabel();
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_STOP_MENU_ITEM);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                        StatusInfo<CorrelationSuggestionsType> suggestionStatus = getStatusInfo(wrapper);
                        if (suggestionStatus == null) {
                            return false;
                        }
                        OperationResultStatusType status = suggestionStatus.getStatus();
                        return !suggestionStatus.isComplete() && status != OperationResultStatusType.FATAL_ERROR;
                    }

                    return false;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Task task = getPageBase().createSimpleTask(OP_SUSPEND_SUGGESTION);
                        OperationResult result = task.getResult();

                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                            StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(valueWrapper);
                            if (statusInfo != null) {
                                if (statusInfo.isSuspended() && statusInfo.getStatus() != OperationResultStatusType.FATAL_ERROR) {
                                    resumeSuggestionTask(getPageBase(), statusInfo, task, result);
                                } else if (!statusInfo.isSuspended() && statusInfo.getStatus() != OperationResultStatusType.FATAL_ERROR) {
                                    suspendSuggestionTask(
                                            getPageBase(), statusInfo, task, result);
                                }
                                refreshAndDetach(target);
                            }
                        }
                    }
                };
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }
        };
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

        setVisibilityBySuggestion(buttonInlineMenuItem, true);
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
    protected IModel<PrismContainerWrapper<ItemsSubCorrelatorType>> getContainerModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerWrapper<ItemsSubCorrelatorType> load() {
                IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> object = correlationWrapper;
                if (object == null || object.getObject() == null) {
                    return null;
                }

                PrismContainerWrapper<ItemsSubCorrelatorType> container;
                try {
                    container = object.getObject().findContainer(
                            ItemPath.create(CorrelationDefinitionType.F_CORRELATORS, CompositeCorrelatorType.F_ITEMS));
                } catch (SchemaException e) {
                    LOGGER.error("Cannot get correlation items container: {}", e.getMessage(), e);
                    return null;
                }
                return container;
            }
        };
    }

    @Override
    public void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<ItemsSubCorrelatorType>> toDelete) {
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
        refreshAndDetach(target);
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

    private void setVisibilityBySuggestion(@NotNull InlineMenuItem item, boolean showWhenPresent) {
        item.setVisibilityChecker((rowModel, isHeader) -> {
            if (rowModel == null || rowModel.getObject() == null) {
                return false;
            }

            PrismContainerValueWrapper<?> wrapper = (PrismContainerValueWrapper<?>) rowModel.getObject();
            StatusInfo<CorrelationSuggestionsType> statusInfo = getStatusInfo(wrapper);

            boolean present = statusInfo != null;

            if (present && statusInfo.getStatus() != OperationResultStatusType.SUCCESS) {
                return false;
            }
            return present == showWhenPresent;
        });
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
    protected void onCreateNewObjectPerform(AjaxRequestTarget target) {
        editItemPerformed(target, null, false);
    }

    protected PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> findResourceObjectTypeDefinition() {
        return correlationWrapper.getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);
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
}


