/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.*;
import static com.evolveum.midpoint.web.session.UserProfileStorage.TableId.TABLE_SMART_INBOUND_MAPPINGS;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUsedFor;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemBuilder;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
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

/**
 * Multi-select tile table for mappings items.
 */
public abstract class SmartMappingTable<P extends Containerable> extends BasePanel<String> {

    private static final String CLASS_DOT = SmartMappingTable.class.getName() + ".";
    private static final String OP_DELETE_MAPPING = CLASS_DOT + "deleteMapping";

    private static final String ID_TABLE = "table";
    private static final int MAX_TILE_COUNT = 10;

    private final String resourceOid;

    IModel<PrismContainerValueWrapper<P>> refAttributeDefValue;
    IModel<MappingDirection> mappingDirectionIModel;
    IModel<MappingUsedFor> mappingUsedForIModel = new Model<>(MappingUsedFor.ALL);
    IModel<Boolean> switchToggleModel;

    // Cache of accepted suggestions to keep them shown on the same place after refresh
    private final Set<PrismContainerValueWrapper<MappingType>> acceptedSuggestionsCache = new HashSet<>();

    public SmartMappingTable(
            @NotNull String id,
            @NotNull IModel<MappingDirection> mappingDirection,
            @NotNull IModel<Boolean> switchToggleModel,
            IModel<PrismContainerValueWrapper<P>> refAttributeDefValue,
            @NotNull String resourceOid) {
        super(id);
        this.switchToggleModel = switchToggleModel;
        this.resourceOid = resourceOid;
        this.refAttributeDefValue = refAttributeDefValue;
        this.mappingDirectionIModel = mappingDirection;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        ColumnTileTable<PrismContainerValueWrapper<MappingType>> smartMappingTable = createSmartMappingTable();
        add(smartMappingTable);
    }

    private @NotNull ColumnTileTable<PrismContainerValueWrapper<MappingType>> createSmartMappingTable() {

        ColumnTileTable<PrismContainerValueWrapper<MappingType>> columnTileTable =
                new ColumnTileTable<>(
                        ID_TABLE,
                        Model.of(ViewToggle.TILE),
                        TABLE_SMART_INBOUND_MAPPINGS,
                        this::getInboundMappingColumns) {

                    @Override
                    public @NotNull List<InlineMenuItem> getInlineMenuItems() {
                        List<InlineMenuItem> inlineMenuItems = super.getInlineMenuItems();
                        inlineMenuItems.add(createSuggestionOperationInlineMenu(getPageBase(), this::getStatusInfo, this::refreshAndDetach));
                        inlineMenuItems.add(createSuggestionDetailsInlineMenu(getPageBase(), this::getStatusInfo));
                        inlineMenuItems.add(createDiscardItemMenu());
                        inlineMenuItems.add(createAcceptItemMenu());
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
                    protected void initPanelToolbarButtons(@NotNull RepeatingView toolbar) {
                        toolbar.add(createToggleSuggestionVisibilityButton(toolbar.newChildId(), switchToggleModel
                        ));
                        toolbar.add(createAcceptAllButton(toolbar.newChildId()));
                        toolbar.add(createDiscardAllButton(toolbar.newChildId()));
                        super.initPanelToolbarButtons(toolbar);
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
                                SearchBuilder<?> searchBuilder = new SearchBuilder<>(MappingType.class)
                                        .setFullTextSearchEnabled(true)
                                        .modelServiceLocator(getPageBase());
                                return searchBuilder.build();
                            }
                        };
                    }

                    @Override
                    protected StringResourceModel getNewObjectButtonTitle() {
                        return getMappingType() == MappingDirection.INBOUND
                                ? createStringResource("SmartMappingTable.addInboundMapping")
                                : createStringResource("SmartMappingTable.addOutboundMapping");
                    }

                    @Override
                    public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        performOnEditMapping(target, rowModel);
                    }

                    @Override
                    protected void deleteItemPerformed(@NotNull PrismContainerValueWrapper<MappingType> value) {
                        SmartMappingTable.this.deleteItemPerform(value);
                    }

                    @Override
                    protected @NotNull ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> createProvider() {
                        var dto = StatusAwareDataFactory.createMappingModel(this, resourceOid, switchToggleModel,
                                () -> getContainerModel().getObject(), findResourceObjectTypeDefinition(), getMappingType(),
                                acceptedSuggestionsCache);

                        return new StatusAwareDataProvider<>(this, Model.of(), dto, true) {
                            @Override
                            protected List<PrismContainerValueWrapper<MappingType>> searchThroughList() {
                                var list = super.searchThroughList();
                                if (getMappingType() == MappingDirection.INBOUND) {
                                    MappingUsedFor usedFor = MappingUsedFor.ALL;
                                    excludeMappings(list, usedFor);
                                }
                                return list;
                            }
                        };
                    }
                };

        columnTileTable.setOutputMarkupId(true);
        columnTileTable.add(AttributeAppender.append("class", "p-0"));
        columnTileTable.setDefaultPagingSize(MAX_TILE_COUNT);

        return columnTileTable;
    }

    private @NotNull List<IColumn<PrismContainerValueWrapper<MappingType>, String>> getInboundMappingColumns() {
        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(Model.of()) {

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                PrismContainerValueWrapper<MappingType> mapping = rowModel.getObject();
                MappingType mappingBean = mapping.getRealValue();

                InboundMappingUseType mappingUsed = ((InboundMappingType) mappingBean).getUse();
                if (mappingUsed == null) {
                    mappingUsed = InboundMappingUseType.ALL;
                }
                for (MappingUsedFor usedFor : Arrays.stream(MappingUsedFor.values()).toList()) {
                    if (usedFor.getType().equals(mappingUsed)) {
                        return new DisplayType()
                                .tooltip(usedFor.getTooltip())
                                .beginIcon()
                                .cssClass(usedFor.getIcon())
                                .end();
                    }
                }
                return new DisplayType();
            }

            @Override
            public String getCssClass() {
                return "px-0 tile-column-icon";
            }
        });

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

        columns.add(new PrismPropertyWrapperColumn<>(
                getMappingTypeDefinition(),
                MappingType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-3 header-border-right";
            }

            @Override
            public String getSortProperty() {
                return MappingType.F_NAME.getLocalPart();
            }
        });

        Model<PrismContainerDefinition<ResourceAttributeDefinitionType>> resourceAttributeDef =
                Model.of(PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(
                        ResourceAttributeDefinitionType.class));

        //noinspection unchecked,rawtypes
        columns.add(new PrismPropertyWrapperColumn(
                resourceAttributeDef,
                ResourceAttributeDefinitionType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public Component getHeader(String componentId) {
                return super.getHeader(componentId);
            }

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
                                getMappingType().name() + "." + ResourceAttributeDefinitionType.F_REF));
                    }
                };
            }

            @Override
            public String getCssClass() {
                return "col-2 header-border-right";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                getMappingTypeDefinition(),
                MappingType.F_EXPRESSION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-2 header-border-right";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                getMappingTypeDefinition(),
                MappingType.F_TARGET,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-2 header-border-right";
            }

            @Override
            protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<MappingType>> mainModel) {
                return super.createHeader(componentId, mainModel);
            }
        });

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

    protected boolean isTogglePanelVisible() {
        return false;
    }

    public boolean isValidFormComponents() {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<P>> getValueModel() {
        return refAttributeDefValue;
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
            removeMappingTypeSuggestionNew(getPageBase(), status, parentContainerValue.getRealValue(), task, task.getResult());
        } else {
            resolveDeletedItem(value);
        }
    }

    protected void excludeMappings(@Nullable List<PrismContainerValueWrapper<MappingType>> list, MappingUsedFor usedFor) {
        if (list != null && usedFor != MappingUsedFor.ALL) {
            excludeUnwantedMappings(list, usedFor);
        }
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

    protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
        return createVirtualMappingContainerModel(
                getPageBase(),
                getValueModel(),
                getItemNameOfContainerWithMappings(),
                getItemNameOfRefAttribute(),
                getMappingType());
    }

    protected void resolveDeletedItem(@NotNull PrismContainerValueWrapper<MappingType> value) {
        try {
            if (value.getStatus() == ValueStatus.ADDED) {
                PrismContainerWrapper<ResourceAttributeDefinitionType> container = getValueModel().getObject()
                        .findContainer(getItemNameOfContainerWithMappings());

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

//    protected @NotNull InlineMenuItem createDuplicateInlineMenu(PrismContainerValueWrapper<MappingType> tileModel) {
//        InlineMenuItem duplicateInlineMenu = super.createDuplicateInlineMenu(tileModel);
//        setVisibilityBySuggestion(duplicateInlineMenu, false, this::getStatusInfo);
//        return duplicateInlineMenu;
//    }

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
        dropdown.add(new VisibleBehaviour(() -> MappingDirection.INBOUND == getMappingType() && !getTable().displayNoValuePanel()));
        return dropdown;
    }

    @SuppressWarnings("unchecked")
    protected ColumnTileTable<PrismContainerValueWrapper<MappingType>> getTable() {
        return (ColumnTileTable<PrismContainerValueWrapper<MappingType>>) get(ID_TABLE);
    }

    public PrismContainerValueWrapper<MappingType> acceptSuggestionItemPerformed(
            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            StatusInfo<?> statusInfo,
            @NotNull AjaxRequestTarget target) {
        PrismContainerValueWrapper<MappingType> newValue = createNewValue(rowModel.getObject().getNewValue(), target);
        deleteItemPerform(rowModel.getObject());
        return newValue;
    }

    protected InlineMenuItem createAcceptItemMenu() {
        return createSuggestActionMenuBuilder()
                .label(createStringResource("SmartMappingTable.apply"))
                .icon(GuiStyleConstants.CLASS_MAGIC_WAND)
                .action(createAcceptSuggestionColumnAction())
                .additionalCssClass("btn-default text-purple rounded border-purple flex-row-reverse gap-2")
                .buildButtonMenu();
    }

    protected InlineMenuItem createDiscardItemMenu() {
        return createSuggestActionMenuBuilder()
                .label(createStringResource("SmartMappingTable.discard"))
                .icon("not-fa")
                .action(createDiscardColumnAction())
                .additionalCssClass("btn-link text-purple mr-2")
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
            @SuppressWarnings("unchecked")
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() != null) {
                    deleteItemPerform((PrismContainerValueWrapper<MappingType>) getRowModel().getObject());
                    refreshAndDetach(target);
                }
            }
        };
    }

    protected boolean isSuggestionInlineMenuVisible() {
        return switchToggleModel.getObject().equals(Boolean.TRUE);
    }

    public ColumnMenuAction<PrismContainerValueWrapper<MappingType>> createAcceptSuggestionColumnAction() {
        return new ColumnMenuAction<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() != null) {
                    PrismContainerValueWrapper<MappingType> value = getRowModel().getObject();
                    StatusInfo<?> status = getStatusInfo(value);
                    if (status != null) {
                        PrismContainerValueWrapper<MappingType> newValue = acceptSuggestionItemPerformed(
                                () -> value, status, target);
                        acceptedSuggestionsCache.add(newValue);
                    }
                    refreshAndDetach(target);
                }

            }
        };
    }

    protected IModel<Boolean> getSwitchToggleModel() {
        return switchToggleModel;
    }

    private @NotNull ToggleCheckBoxPanel createToggleSuggestionVisibilityButton(
            @NotNull String idButton,
            @NotNull IModel<Boolean> switchSuggestionModel) {
        ToggleCheckBoxPanel togglePanel = new ToggleCheckBoxPanel(idButton, switchSuggestionModel) {

            @Override
            public @NotNull Component getTitleComponent(@NotNull String id) {
                return new IconWithLabel(id, () -> getSwitchToggleModel().getObject()
                        ? getString("SmartMappingTable.suggestion.enabled")
                        : getString("SmartMappingTable.suggestion.disabled")) {
                    @Override
                    protected String getIconCssClass() {
                        return GuiStyleConstants.CLASS_MAGIC_WAND + " text-purple ml-2";
                    }

                    @Override
                    protected String getComponentCssClass() {
                        return "d-flex m-0 font-weight-normal text-body";
                    }
                };
            }

            @Override
            protected @NotNull Map<String, Object> createSwitchOptions() {
                Map<String, Object> options = new LinkedHashMap<>();
                options.put("onColor", "purple");
                options.put("size", "mini");
                return options;
            }

            @Override
            protected void onToggle(@NotNull AjaxRequestTarget target) {
                refreshAndDetach(target);
            }

            @Override
            public @NotNull String getContainerCssClass() {
                return "d-flex flex-row-reverse align-items-center gap-1";
            }
        };

        togglePanel.setOutputMarkupId(true);
        return togglePanel;
    }

    protected AjaxIconButton createDiscardAllButton(String id) {
        return createAcceptDiscardBulkActionButton(id, "SmartMappingTable.discard.all",
                "btn-default text-purple", false);
    }

    protected AjaxIconButton createAcceptAllButton(String id) {
        return createAcceptDiscardBulkActionButton(id, "SmartMappingTable.apply.all",
                "btn-success bg-purple", true);
    }

    protected AjaxIconButton createAcceptDiscardBulkActionButton(String id, String labelKey, String cssClass, boolean isAccept) {
        AjaxIconButton button = new AjaxIconButton(id, Model.of(""), createStringResource(labelKey)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                List<PrismContainerValueWrapper<MappingType>> allItems = getTable().getAllItems();
                if (!allItems.isEmpty()) {
                    ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
                            acceptConfirmationTitle(getPageBase(), allItems.size(), getTable().getCurrentPageItems().isEmpty())) {

                        @Override
                        public void yesPerformed(AjaxRequestTarget target) {
                            allItems.stream()
                                    .filter(v -> getStatusInfo(v) != null)
                                    .forEach(v -> {
                                        if (isAccept) {
                                            acceptSuggestionItemPerformed(() -> v, getStatusInfo(v), target);
                                        } else {
                                            deleteItemPerform(v);
                                        }
                                    });
                            refreshAndDetach(target);
                        }
                    };

                    getPageBase().showMainPopup(dialog, target);
                }
            }
        };

        button.setOutputMarkupId(true);
        button.showTitleAsLabel(true);
        button.add(AttributeModifier.replace("class", "px-2 btn " + cssClass));
        button.add(new VisibleBehaviour(() -> getSwitchToggleModel().getObject().equals(Boolean.TRUE)));
        return button;
    }
}


