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
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.*;
import static com.evolveum.midpoint.prism.PrismConstants.VARIABLE_BINDING_DEF_MATCHING_RULE_NAME;
import static com.evolveum.midpoint.web.session.UserProfileStorage.TableId.TABLE_SMART_MAPPINGS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAttributeMappingsDefinitionType.F_REF;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoiceColumnPanel;
import com.evolveum.midpoint.gui.impl.component.search.panel.SimpleCustomSearchPanel;
import com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable;
import com.evolveum.midpoint.gui.impl.duplication.DuplicationProcessHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUsedFor;

import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemBuilder;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import com.evolveum.midpoint.web.session.UserProfileStorage;
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
        ColumnTileTable<PrismContainerValueWrapper<MappingType>> smartMappingTable = createSmartMappingTable();
        add(smartMappingTable);
    }

    public IModel<Search> getSearchModel() {
        return getTable().getSearchModel();
    }

    private @NotNull ColumnTileTable<PrismContainerValueWrapper<MappingType>> createSmartMappingTable() {

        ColumnTileTable<PrismContainerValueWrapper<MappingType>> columnTileTable =
                new ColumnTileTable<>(
                        ID_TABLE,
                        Model.of(ViewToggle.TILE),
                        getTableId(),
                        this::getColumns) {

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
                        List<InlineMenuItem> inlineMenuItems = super.getInlineMenuItems();
                        inlineMenuItems.add(createSuggestionOperationInlineMenu(getPageBase(), this::getStatusInfo, this::refreshAndDetach));
                        inlineMenuItems.add(createSuggestionDetailsInlineMenu(getPageBase(), this::getStatusInfo));
                        inlineMenuItems.add(createAcceptItemMenu());
                        inlineMenuItems.add(createDiscardItemMenu());
                        inlineMenuItems.add(createDuplicateInlineMenu());
                        inlineMenuItems.add(createChangeMappingNameInlineMenu());
                        inlineMenuItems.add(createChangeLifecycleButtonInlineMenu());
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
                        toolbar.add(createToggleSuggestionVisibilityButton(getPageBase(),
                                toolbar.newChildId(),
                                suggestionToggleModel,
                                SmartMappingTable.this::refreshAndDetach,
                                null));

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
                                return new SearchBuilder<>(MappingType.class)
                                        .modelServiceLocator(getPageBase())
                                        .build();
                            }
                        };
                    }

                    @Override
                    protected StringResourceModel getNewObjectButtonTitle() {
                        if (getMappingType() != MappingDirection.INBOUND && getMappingType() != MappingDirection.OUTBOUND) {
                            return super.getNewObjectButtonTitle();
                        }

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
                        return createDataProvider();
                    }

                };

        columnTileTable.setOutputMarkupId(true);
        columnTileTable.add(AttributeAppender.append("class", "p-0"));
        columnTileTable.setDefaultPagingSize(MAX_TILE_COUNT);

        return columnTileTable;
    }

    @SuppressWarnings("unchecked")
    protected ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> createDataProvider() {
        var dto = StatusAwareDataFactory.createMappingModel(this, resourceOid, suggestionToggleModel,
                () -> getContainerModel().getObject(), findResourceObjectTypeDefinition(), getMappingType(),
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
                if (getMappingType() == MappingDirection.INBOUND
                        && isExcludedMapping(mappingUsedForIModel.getObject(), valueWrapper)) {
                    return false;
                }

                boolean defaultMatch = super.matchItems(valueWrapper, query);
                return defaultMatch || matchRefAttribute(valueWrapper);
            }

            /* Check if the ref attribute contains the search text */
            private boolean matchRefAttribute(
                    @NotNull PrismContainerValueWrapper<MappingType> valueWrapper)
                    throws SchemaException {
                PrismPropertyWrapper<ItemPathType> refProperty = valueWrapper.findProperty(F_REF);
                if (refProperty != null && refProperty.getValue() != null) {
                    ItemPathType refPath = refProperty.getValue().getRealValue();
                    return refPath.toString().contains(getSearchTextModelObject());
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

    protected boolean displayNoValuePanel() {
        return getTable().getProvider().size() == 0 && !suggestionToggleModel.getObject();
    }

    @SuppressWarnings("unchecked")
    protected @NotNull List<IColumn<PrismContainerValueWrapper<MappingType>, String>> getColumns() {
        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        boolean isInbound = getMappingType() == MappingDirection.INBOUND;

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
                        return "col header-border-right";
                    }

                    @Override
                    public String getSortProperty() {
                        return MappingType.F_NAME.getLocalPart();
                    }
                };

        Model<PrismContainerDefinition<ResourceAttributeDefinitionType>> resourceAttributeDef =
                Model.of(PrismContext.get().getSchemaRegistry()
                        .findContainerDefinitionByCompileTimeClass(ResourceAttributeDefinitionType.class));

        @SuppressWarnings({ "unchecked", "rawtypes" })
        IColumn refCol = new PrismPropertyWrapperColumn(
                resourceAttributeDef,
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
                                getMappingType().name() + "." + ResourceAttributeDefinitionType.F_REF));
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
                };

        IColumn<PrismContainerValueWrapper<MappingType>, String> sourceOrTargetCol;
        if (getMappingType() == MappingDirection.OUTBOUND) {
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

        if (getMappingType() == MappingDirection.OUTBOUND) {
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

    protected @NotNull InlineMenuItem createDuplicateInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(createStringResource("DuplicationProcessHelper.menu.duplicate"))
                .action(DuplicationProcessHelper.createDuplicateColumnAction(getPageBase(),
                        this::createDuplicateValuePerform))
                .headerMenuItem(false)
                .visibilityChecker(bySuggestion(false, this::getStatusInfo))
                .buildInlineMenu();
    }

    protected final void createDuplicateValuePerform(PrismContainerValue<MappingType> value, AjaxRequestTarget target) {
        createNewValue(value, target);
        refreshAndDetach(target);
    }

    private @NotNull InlineMenuItem createChangeLifecycleButtonInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(createStringResource("AttributeMappingsTable.button.changeLifecycle"))
                .action(createChangeLifecycleColumnAction(getPageBase(), this::refreshAndDetach,
                        () -> getTable().getSelectedContainerItems()))
                .visibilityChecker((rowModel, isHeader) -> isHeader)
                .buildInlineMenu();
    }

    private @NotNull InlineMenuItem createChangeMappingNameInlineMenu() {
        return InlineMenuItemBuilder.create()
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
                @SuppressWarnings("unchecked") PrismPropertyWrapper<String> property =
                        ((PrismContainerValueWrapper<MappingType>) rowModel.getObject()).findProperty(MappingType.F_NAME);
                return property.isReadOnly();
            } catch (SchemaException e) {
                return false;
            }
        };
    }

    protected PrismContainerValueWrapper<MappingType> createNewValue(
            PrismContainerValue<MappingType> value,
            AjaxRequestTarget target) {
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
        return suggestionToggleModel.getObject().equals(Boolean.TRUE);
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
                        getAcceptedSuggestionsCache().add(newValue);
                    }
                    refreshAndDetach(target);
                }

            }
        };
    }

    protected IModel<Boolean> getSuggestionToggleModel() {
        return suggestionToggleModel;
    }

    protected AjaxIconButton createDiscardAllButton(String id) {
        return createAcceptDiscardBulkActionButton(id, Model.of("fa fa-check"), "SmartMappingTable.dismiss.all",
                "text-danger", false);
    }

    protected AjaxIconButton createAcceptAllButton(String id) {
        return createAcceptDiscardBulkActionButton(id, Model.of("fa fa-times"), "SmartMappingTable.apply.all",
                "btn-outline-primary", true);
    }

    protected AjaxIconButton createAcceptDiscardBulkActionButton(String id,
            IModel<String> iconCss, String labelKey, String cssClass, boolean isAccept) {
        AjaxIconButton button = new AjaxIconButton(id, iconCss, createStringResource(labelKey)) {
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
        button.add(new VisibleBehaviour(() -> getSuggestionToggleModel().getObject().equals(Boolean.TRUE)
                && getProvider().getPageSuggestionCount() > 1));
        return button;
    }

    @SuppressWarnings("rawtypes")
    protected StatusAwareDataProvider<?> getProvider() {
        return (StatusAwareDataProvider) getTable().getProvider();
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
}


