/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.mapping;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadMappingSuggestionWrappers;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUsedFor;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

/**
 * Multi-select tile table for mappings items.
 */
public abstract class SmartMappingTable<P extends Containerable>
        extends MultiSelectContainerActionTileTablePanel<PrismContainerValueWrapper<MappingType>, MappingType, SmartMappingTileModel<PrismContainerValueWrapper<MappingType>>> {

    private static final String CLASS_DOT = SmartMappingTable.class.getName() + ".";
    private static final String OP_SUGGEST_MAPPING = CLASS_DOT + "suggestMapping";
    private static final String OP_DELETE_MAPPING = CLASS_DOT + "deleteMapping";
    private static final String OP_SUSPEND_SUGGESTION = CLASS_DOT + "suspendSuggestion";

    private static final int MAX_TILE_COUNT = 4;

    private final String resourceOid;

    IModel<PrismContainerValueWrapper<P>> refAttributeDefValue;

    IModel<MappingDirection> mappingDirectionIModel;

    IModel<MappingUsedFor> mappingUsedForIModel = new Model<>(MappingUsedFor.ALL);

    public SmartMappingTable(
            @NotNull String id,
            @NotNull UserProfileStorage.TableId tableId,
            @NotNull IModel<ViewToggle> toggleView,
            @NotNull IModel<MappingDirection> mappingDirection,
            IModel<PrismContainerValueWrapper<P>> refAttributeDefValue,
            @NotNull String resourceOid) {
        super(id, tableId, toggleView);
        this.resourceOid = resourceOid;
        this.refAttributeDefValue = refAttributeDefValue;
        this.mappingDirectionIModel = mappingDirection;
        setDefaultPagingSize(tableId, MAX_TILE_COUNT);
        this.setOutputMarkupId(true);
    }

    @Override
    protected boolean isTogglePanelVisible() {
        return false;
    }

    public boolean isValidFormComponents(AjaxRequestTarget target) {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<P>> getValueModel() {
        return refAttributeDefValue;
    }

    @Override
    protected Class<? extends Containerable> getType() {
        return MappingType.class;
    }

    @Override
    protected void customizeTileItemCss(Component tile, @NotNull SmartMappingTileModel<PrismContainerValueWrapper<MappingType>> item) {
        super.customizeTileItemCss(tile, item);

        StatusInfo<MappingsSuggestionType> statusInfo = getStatusInfo(item.getValue());
        if (statusInfo != null && statusInfo.getStatus() != null) {
            tile.add(AttributeModifier.replace("class", "card rounded m-0 "
                    + SuggestionUiStyle.from(statusInfo).tileClass));
        }
    }

    @Override
    protected SmartMappingTileModel<PrismContainerValueWrapper<MappingType>> createTileObject(
            @NotNull PrismContainerValueWrapper<MappingType> object) {
        StatusInfo<MappingsSuggestionType> statusInfo = getStatusInfo(object);
        return new SmartMappingTileModel<>(object, resourceOid, statusInfo != null ? statusInfo.getToken() : null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Component createTile(String id, @NotNull IModel<SmartMappingTileModel<PrismContainerValueWrapper<MappingType>>> model) {
        PrismContainerValueWrapper<MappingType> value = model.getObject().getValue();
        return new SmartMappingTilePanel(id, model) {
            @Override
            public List<InlineMenuItem> createMenuItems() {
                return getInlineMenuItems(value);
            }

            @Override
            protected void initSuggestionActionButton(@NotNull RepeatingView buttonsView) {
                buttonsView.add(createDiscardButton(buttonsView.newChildId(), () -> value));
                buttonsView.add(createAcceptButton(buttonsView.newChildId(), () -> value));
            }

            @Override
            protected void onFinishGeneration(AjaxRequestTarget target) {
                refreshAndDetach(target);
            }

            @Override
            protected void onEditButton(AjaxRequestTarget target) {
                editItemPerformed(target, () -> value, false);
            }
        };
    }

    protected AjaxIconButton createDiscardButton(String id, IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
        AjaxIconButton discardButton = new AjaxIconButton(id, Model.of("fa fa-solid fa-x"),
                createStringResource("SmartCorrelationTilePanel.discardButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteItemPerformed(target, Collections.singletonList(rowModel.getObject()));
            }
        };
        discardButton.setOutputMarkupId(true);
        discardButton.add(new TooltipBehavior());
        discardButton.add(AttributeModifier.replace("class", "col-auto px-4 btn btn-default btn-sm rounded ml-auto"));
        discardButton.showTitleAsLabel(true);
        return discardButton;
    }

    protected AjaxIconButton createAcceptButton(String id, IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
        AjaxIconButton acceptButton = new AjaxIconButton(id, Model.of("fa fa-check"),
                createStringResource("SmartCorrelationTilePanel.acceptButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                @Nullable StatusInfo<MappingsSuggestionType> statusInfo = getStatusInfo(rowModel.getObject());
                acceptSuggestionItemPerformed(rowModel, statusInfo, target);
            }
        };
        acceptButton.setOutputMarkupId(true);
        acceptButton.add(new TooltipBehavior());
        acceptButton.add(AttributeModifier.replace("class", "col-auto px-4 btn btn-success btn-sm bg-purple rounded"));
        acceptButton.showTitleAsLabel(true);
        return acceptButton;
    }

    protected void excludeMappings(@NotNull List<PrismContainerValueWrapper<MappingType>> list, MappingUsedFor usedFor) {
        list.removeIf(valueWrapper -> {
            InboundMappingType realValue = (InboundMappingType) valueWrapper.getRealValue();
            InboundMappingUseType valueUse = realValue.getUse();
            if (valueUse == null) {
                valueUse = InboundMappingUseType.ALL;
            }
            MappingUsedFor valueUsedFor = MappingUsedFor.valueOf(valueUse.name());

            return !usedFor.equals(valueUsedFor);
        });
    }

    protected ItemName getItemNameOfRefAttribute() {
        return ResourceAttributeDefinitionType.F_REF;
    }

    protected ItemName getItemNameOfContainerWithMappings() {
        return ResourceObjectTypeDefinitionType.F_ATTRIBUTE;
    }

    private ItemName getPathBaseOnMappingType() {
        return getMappingType().getContainerName();
    }

    protected MappingDirection getMappingType() {
        return mappingDirectionIModel.getObject();
    }

    @Override
    protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
        return createVirtualMappingContainerModel(
                getPageBase(),
                getValueModel(),
                getItemNameOfContainerWithMappings(),
                getItemNameOfRefAttribute(),
                getMappingType());
    }

    @Override
    protected void resolveDeletedItem(@NotNull PrismContainerValueWrapper<MappingType> value) {
        PrismContainerValueWrapper<P> valueWrapper = getValueModel().getObject();

        try {
            if (value.getStatus() == ValueStatus.ADDED) {
                PrismContainerWrapper<ResourceAttributeDefinitionType> container = valueWrapper.findContainer(
                        getItemNameOfContainerWithMappings());

                List<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> values = container.getValues();

                for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> valueR : values) {
                    PrismContainerWrapper<MappingType> mappingR = valueR.findContainer(getPathBaseOnMappingType());
                    mappingR.getValues().removeIf(v -> v.equals(value));
                }

            } else {
                value.setStatus(ValueStatus.DELETED);
            }
        } catch (SchemaException e) {
            getPageBase().error("Couldn't delete mapping: " + e.getMessage());
        }

        value.setSelected(false);
    }

    @Override
    protected MultivalueContainerListDataProvider<MappingType> createDataProvider() {
        final Map<PrismContainerValueWrapper<MappingType>, StatusInfo<MappingsSuggestionType>> suggestionsIndex = new HashMap<>();

        LoadableDetachableModel<List<PrismContainerValueWrapper<MappingType>>> valuesModel =
                new LoadableDetachableModel<>() {
                    @Override
                    protected @NotNull List<PrismContainerValueWrapper<MappingType>> load() {
                        PrismContainerWrapper<MappingType> container = getContainerModel().getObject();
                        suggestionsIndex.clear();

                        List<PrismContainerValueWrapper<MappingType>> allValues = new ArrayList<>(
                                container != null ? container.getValues() : List.of());

                        if (Boolean.TRUE.equals(getSwitchToggleModel().getObject())) {
                            Task task = getPageBase().createSimpleTask("Loading mappings type suggestions");
                            OperationResult result = task.getResult();

                            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentWrapper = findResourceObjectTypeDefinition();
                            ResourceObjectTypeDefinitionType resourceObjectTypeDefinition = parentWrapper.getRealValue();
                            SmartIntegrationStatusInfoUtils.@NotNull MappingSuggestionProviderResult suggestionWrappers =
                                    loadMappingSuggestionWrappers(getPageBase(), resourceOid, resourceObjectTypeDefinition,
                                            getMappingType(), task, result);

                            suggestionWrappers.wrappers().forEach(WebPrismUtil::setReadOnlyRecursively);

                            allValues.addAll(suggestionWrappers.wrappers());
                            suggestionsIndex.putAll(suggestionWrappers.suggestionByWrapper());
                        }
                        return allValues;
                    }
                };

        return new StatusAwareDataProvider<>(
                this,
                resourceOid,
                Model.of(),
                valuesModel,
                suggestionsIndex::get
        ) {
            @Override
            protected List<PrismContainerValueWrapper<MappingType>> searchThroughList() {
                if (getMappingType() != MappingDirection.INBOUND) {
                    super.searchThroughList();
                }
                List<PrismContainerValueWrapper<MappingType>> list = super.searchThroughList();

                if (list == null || list.isEmpty()) {
                    return null;
                }

                if (getMappingType() != MappingDirection.INBOUND) {
                    return list;
                }

                MappingUsedFor usedFor = getSelectedTypeOfMappings();
                excludeMappings(list, usedFor);
                return list;
            }
        };
    }

    protected MappingUsedFor getSelectedTypeOfMappings() {
        return mappingUsedForIModel.getObject();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<MappingType>, String>> createDomainColumns() {
        return List.of();
    }

    @Override
    protected ButtonInlineMenuItem createEditInlineMenu(PrismContainerValueWrapper<MappingType> tileModel) {
        ButtonInlineMenuItem editInlineMenu = super.createEditInlineMenu(tileModel);
        setVisibilityBySuggestion(editInlineMenu, false);
        return editInlineMenu;
    }

    @Override
    protected @NotNull InlineMenuItem createDuplicateInlineMenu(PrismContainerValueWrapper<MappingType> tileModel) {
        InlineMenuItem duplicateInlineMenu = super.createDuplicateInlineMenu(tileModel);
        setVisibilityBySuggestion(duplicateInlineMenu, false);
        return duplicateInlineMenu;
    }

    @Override
    public @NotNull List<InlineMenuItem> getInlineMenuItems(PrismContainerValueWrapper<MappingType> tileModel) {
        List<InlineMenuItem> inlineMenuItems = super.getInlineMenuItems(tileModel);
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
                        StatusInfo<MappingsSuggestionType> suggestionStatus = getStatusInfo(wrapper);
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
                            StatusInfo<MappingsSuggestionType> statusInfo = getStatusInfo(valueWrapper);
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

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttonsList = new ArrayList<>();
        buttonsList.add(createTableActionToolbar(idButton));
        buttonsList.add(createToggleMappingDirectionButton(idButton));
        buttonsList.add(createMappingTypeDropdownButton(idButton));
        return buttonsList;
    }

    @Override
    protected final PrismContainerValueWrapper<MappingType> createNewValue(PrismContainerValue<MappingType> value, AjaxRequestTarget target) {
        return createNewVirtualMappingValue(
                value,
                getValueModel(),
                getMappingType(),
                getItemNameOfContainerWithMappings(),
                getItemNameOfRefAttribute(),
                getPageBase(),
                target);
    }

    @Override
    protected String getTileContainerCssClass() {
        return "row justify-content-left pt-2 gap-2";
    }

    @Override
    protected void onCreateNewObjectPerform(AjaxRequestTarget target) {
        PrismContainerValueWrapper<MappingType> newValue = createNewValue(null, target);
        refreshAndDetach(target);
    }

    protected TogglePanel<MappingDirection> createToggleMappingDirectionButton(String idButton) {

        IModel<List<Toggle<MappingDirection>>> toggleModel = () -> {
            List<Toggle<MappingDirection>> list = new ArrayList<>();

            Toggle<MappingDirection> t = new Toggle<>(null, getString(MappingDirection.INBOUND));
            t.setActive(MappingDirection.INBOUND == mappingDirectionIModel.getObject());
            t.setValue(MappingDirection.INBOUND);
            t.setBadgeCss(t.isActive() ? Badge.State.PRIMARY.getCss() : Badge.State.SECONDARY.getCss());
            list.add(t);

            Toggle<MappingDirection> t2 = new Toggle<>(null, getString(MappingDirection.OUTBOUND));
            t2.setActive(MappingDirection.OUTBOUND == mappingDirectionIModel.getObject());
            t2.setValue(MappingDirection.OUTBOUND);
            t2.setBadgeCss(t2.isActive() ? Badge.State.PRIMARY.getCss() : Badge.State.SECONDARY.getCss());
            list.add(t2);

            return list;
        };

        return new TogglePanel<>(idButton, toggleModel) {

            @Override
            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<MappingDirection>> item) {
                super.itemSelected(target, item);
                mappingDirectionIModel.setObject(item.getObject().getValue());
                refreshAndDetach(target);
                target.add(SmartMappingTable.this);
            }

            @Override
            protected String getDefaultCssClass() {
                return "badge bg-white border mr-2";
            }

            @Override
            protected String getButtonCssClass() {
                return null;
            }

            @Override
            public @NotNull String getActiveCssClass() {
                return "btn btn-default btn-sm px-3 active";
            }

            @Override
            public String getInactiveCssClass() {
                return "btn btn-sm px-3";
            }
        };

    }

    @Override
    protected String getAdditionalHeaderContainerCssClasses() {
        return "bg-white p-3 rounded";
    }

    protected ButtonInlineMenuItem createSuggestionOperationInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.suspend.generating.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> getLabel() {
                ColumnMenuAction<?> action = (ColumnMenuAction<?>) getAction();
                IModel<?> rowModel = action.getRowModel();
                if (rowModel != null && rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                    StatusInfo<MappingsSuggestionType> s = getStatusInfo(wrapper);
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
                        StatusInfo<MappingsSuggestionType> suggestionStatus = getStatusInfo(wrapper);
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
                            StatusInfo<MappingsSuggestionType> statusInfo = getStatusInfo(valueWrapper);
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

    @Override
    protected void onSuggestNewPerformed(AjaxRequestTarget target) {
        PageBase pageBase = getPageBase();
        ResourceObjectTypeIdentification objectTypeIdentification = getResourceObjectTypeIdentification();
        SmartIntegrationService service = pageBase.getSmartIntegrationService();
        pageBase.taskAwareExecutor(target, OP_SUGGEST_MAPPING)
                .runVoid((task, result) -> {
                    service.submitSuggestMappingsOperation(resourceOid, objectTypeIdentification, task, result);
                    target.add(this);
                });
        super.refreshAndDetach(target);
        target.add(getFeedbackPanel().getParent());
    }

    @Override
    public void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<MappingType>> toDelete) {
        if (noSelectedItemsWarn(getPageBase(), target, toDelete)) {return;}

        Task task = getPageBase().createSimpleTask(OP_DELETE_MAPPING);
        toDelete.forEach(value -> {
            StatusInfo<MappingsSuggestionType> status = getStatusInfo(value);
            if (status != null) {
                PrismContainerValueWrapper<AttributeMappingsSuggestionType> parentContainerValue = value
                        .getParentContainerValue(AttributeMappingsSuggestionType.class);
                if (parentContainerValue == null || parentContainerValue.getRealValue() == null) {
                    return;
                }
                removeMappingTypeSuggestionNew(getPageBase(), status, parentContainerValue.getRealValue(), task, task.getResult());
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
    protected IModel<List<PrismContainerValueWrapper<MappingType>>> getSelectedContainerItemsModel() {
        return super.getSelectedContainerItemsModel();
    }

    @Override
    protected void deselectItem(PrismContainerValueWrapper<MappingType> entry) {

    }

    @Override
    protected IModel<String> getItemLabelModel(PrismContainerValueWrapper<MappingType> entry) {
        return null;
    }

    @Override
    protected IModel<List<PrismContainerValueWrapper<MappingType>>> getSelectedItemsModel() {
        return getSelectedContainerItemsModel();
    }

    private void setVisibilityBySuggestion(@NotNull InlineMenuItem item, boolean showWhenPresent) {
        item.setVisibilityChecker((rowModel, isHeader) -> {
            if (rowModel == null || rowModel.getObject() == null) {
                return false;
            }

            PrismContainerValueWrapper<?> wrapper = (PrismContainerValueWrapper<?>) rowModel.getObject();
            StatusInfo<MappingsSuggestionType> statusInfo = getStatusInfo(wrapper);

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

    protected PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> findResourceObjectTypeDefinition() {
        return refAttributeDefValue.getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);
    }

    protected <C extends Containerable> @Nullable StatusInfo<MappingsSuggestionType> getStatusInfo(PrismContainerValueWrapper<C> value) {
        if (getProvider() instanceof StatusAwareDataProvider<MappingType> provider) {
            //noinspection unchecked
            return (StatusInfo<MappingsSuggestionType>) provider.getSuggestionInfo(
                    (PrismContainerValueWrapper<MappingType>) value);
        }
        return null;
    }

    //TODO any view activity over suggestion?
    public void viewEditItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            StatusInfo<MappingsSuggestionType> statusInfo) {
    }

    protected DropDownChoicePanel<MappingUsedFor> createMappingTypeDropdownButton(String idButton) {
        DropDownChoicePanel<MappingUsedFor> dropdown = WebComponentUtil.createEnumPanel(
                idButton,
                WebComponentUtil.createReadonlyModelFromEnum(MappingUsedFor.class),
                Model.of(),
                SmartMappingTable.this,
                true,
                getString("InboundAttributeMappingsTable.allMappings"));
        dropdown.getBaseFormComponent().add(AttributeAppender.append("style", "width: 220px;"));
        dropdown.getBaseFormComponent().add(AttributeModifier.replace("class", "form-control"));
        dropdown.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                mappingUsedForIModel.setObject(dropdown.getModel().getObject());
                refreshAndDetach(target);
            }
        });
        dropdown.add(new VisibleBehaviour(() -> MappingDirection.INBOUND == getMappingType()));
        return dropdown;
    }

    @Override
    protected Component createHeader(String headerId) {
        RepeatingView headerButtons = new RepeatingView(headerId);
        headerButtons.add(createNewObjectPerformButton(headerButtons.newChildId(), getModelObject()));
        headerButtons.add(createSuggestObjectButton(headerButtons.newChildId()));
//        headerButtons.add(super.createHeader(headerButtons.newChildId()));
        return headerButtons;
    }

    @Override
    protected @NotNull AjaxIconButton createNewObjectPerformButton(String idButton, PrismContainerValueWrapper<MappingType> modelObject) {
        AjaxIconButton newObjectPerformButton = super.createNewObjectPerformButton(idButton, modelObject);
        newObjectPerformButton.add(AttributeAppender.replace("class", "btn btn-primary rounded mr-2 ml-auto"));
        return newObjectPerformButton;
    }

    public abstract void acceptSuggestionItemPerformed(
            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            StatusInfo<MappingsSuggestionType> statusInfo,
            @NotNull AjaxRequestTarget target);

    @Override
    protected String getTileCssStyle() {
        return null;
    }

    @Override
    protected String getTileCssClasses() {
        return "col-12";
    }
}


