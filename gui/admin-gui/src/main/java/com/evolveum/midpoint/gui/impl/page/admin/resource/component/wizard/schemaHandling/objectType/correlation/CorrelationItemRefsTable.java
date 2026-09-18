/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.input.ContainersDropDownPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.collectRequiredResourceAttributeDefs;

/**
 * @author lskublik
 */
public abstract class CorrelationItemRefsTable<P extends Containerable> extends AbstractWizardTable<CorrelationItemType, ItemsSubCorrelatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItemRefsTable.class);

    public CorrelationItemRefsTable(
            String id,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
            ContainerPanelConfigurationType config) {
        super(id, valueModel, config, CorrelationItemType.class);
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<CorrelationItemType>> createProvider() {
        return super.createProvider();
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> menu = new ArrayList<>();
        menu.add(createViewMappingsItemMenu());
        if (!isReadOnlyTable()) {
            menu.add(createDeleteItemMenu());
        }
        return menu;
    }

    protected InlineMenuItem createDeleteItemMenu() {
        return new InlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        };
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<CorrelationItemType>, String> createActionsColumn() {
        List<InlineMenuItem> allItems = getInlineMenuItems();
        return !allItems.isEmpty() ? new InlineMenuButtonColumn<>(allItems, getPageBase()) {
            @Override
            public String getCssClass() {
                return "inline-menu-column";
            }

            @Override
            protected String getDropDownButtonIcon() {
                return "fa fa-ellipsis-h";
            }

            @Override
            protected String getSpecialButtonClass() {
                return "btn btn-link btn-sm";
            }

            @Override
            protected String getInlineMenuItemCssClass(IModel<PrismContainerValueWrapper<CorrelationItemType>> rowModel) {
                return "btn btn-link btn-sm text-nowrap";
            }

            @Override
            protected String getAdditionalMultiButtonPanelCssClass() {
                return "justify-content-end";
            }
        } : null;
    }

    protected InlineMenuItem createViewMappingsItemMenu() {
        return new ButtonInlineMenuItem(createStringResource("CorrelationItemRefsTable.button.view")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_PREVIEW);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createViewMappingsColumnAction();
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            @Override
            public boolean isMenuHeader() {
                return false;
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
    }

    /**
     * Collects missing inbound mappings required by the suggested correlation rule.
     *
     * A correlation suggestion may include mappings that are required for the
     * suggested correlation rule to work correctly.
     */
    protected List<PrismContainerValueWrapper<MappingType>> collectRequiredMappingSuggestion(
            PageBase pageBase,
            AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel)
            throws SchemaException {

        PrismContainerValueWrapper<CorrelationSuggestionType> parentSuggestionW =
                rowModel.getObject()
                        .getParentContainerValue(CorrelationSuggestionType.class);

        if (parentSuggestionW == null || parentSuggestionW.getRealValue() == null) {
            return List.of();
        }

        List<ResourceAttributeDefinitionType> requiredAttributes =
                collectRequiredResourceAttributeDefs(pageBase, target, parentSuggestionW);

        if (requiredAttributes.isEmpty()) {
            return List.of();
        }

        PrismContainerWrapper<ResourceAttributeDefinitionType> attributesContainer = parentSuggestionW.findContainer(
                CorrelationSuggestionType.F_ATTRIBUTES);

        if (attributesContainer == null) {
            return List.of();
        }

        List<PrismContainerValueWrapper<MappingType>> result = new ArrayList<>();

        for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> attributeWrapper : attributesContainer.getValues()) {

            ResourceAttributeDefinitionType attribute = attributeWrapper.getRealValue();

            if (attribute == null || !requiredAttributes.contains(attribute)) {
                continue;
            }

            PrismContainerWrapper<InboundMappingType> inboundContainer = attributeWrapper.findContainer(
                    ResourceAttributeDefinitionType.F_INBOUND);

            if (inboundContainer == null) {
                continue;
            }

            for (PrismContainerValueWrapper<InboundMappingType> inboundWrapper
                    : inboundContainer.getValues()) {

                inboundWrapper.setStatus(ValueStatus.ADDED);

                @SuppressWarnings({ "unchecked", "rawtypes" })
                PrismContainerValueWrapper<MappingType> mappingWrapper =
                        (PrismContainerValueWrapper) inboundWrapper;

                WebPrismUtil.setReadOnlyRecursively(mappingWrapper);
                result.add(mappingWrapper);
            }
        }

        return result;
    }

    public InlineMenuItemAction createViewMappingsColumnAction() {
        return new ColumnMenuAction<PrismContainerValueWrapper<CorrelationItemType>>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                var row = getRowModel() != null ? getRowModel().getObject() : null;
                if (row == null || row.getRealValue() == null || row.getRealValue().getRef() == null) {
                    LOGGER.warn("No reference in correlation item, cannot edit mapping.");
                    getPageBase().warn(getString("CorrelationItem.noRef"));
                    target.add(getPageBase().getFeedbackPanel().getParent());
                    return;
                }

                PrismContainerValueWrapper<P> parentContainerValue = getMappingContainerParent().getObject();

                WebPrismUtil.setReadOnlyRecursively(parentContainerValue);
                CorrelationExistingMappingTable<?> correlationExistingMappingTable = new CorrelationExistingMappingTable<>(
                        getPageBase().getMainPopupBodyId(),
                        () -> parentContainerValue) {
                    @Override
                    protected boolean isSelectableTable() {
                        return false;
                    }

                    @Override
                    protected @NotNull IModel<List<PrismContainerValueWrapper<MappingType>>>
                    createContainerValueModel() {

                        IModel<List<PrismContainerValueWrapper<MappingType>>> baseModel =
                                super.createContainerValueModel();

                        return new LoadableModel<>() {

                            @Override
                            protected List<PrismContainerValueWrapper<MappingType>> load() {
                                List<PrismContainerValueWrapper<MappingType>> result = new ArrayList<>(baseModel.getObject());

                                try {
                                    result.addAll(collectRequiredMappingSuggestion(getPageBase(), target, getValueModel()));
                                } catch (SchemaException e) {
                                    LOGGER.error("Couldn't collect required suggested mappings", e);
                                }

                                return result;
                            }
                        };
                    }

                    @Override
                    public boolean isExcludeMapping(PrismContainerValueWrapper<MappingType> valueWrapper) {
                        boolean excludeMapping = super.isExcludeMapping(valueWrapper);
                        if (!excludeMapping) {
                            VariableBindingDefinitionType target1 = valueWrapper.getRealValue().getTarget();
                            ItemPathType path = target1.getPath();
                            ItemPathType ref = row.getRealValue().getRef();
                            if (path != null && ref != null) {
                                if (!path.equivalent(ref)) {
                                    excludeMapping = true;
                                }
                            }
                        }

                        return excludeMapping;
                    }
                };
                getPageBase().showMainPopup(correlationExistingMappingTable, target);

            }
        };
    }

    @Override
    protected IModel<PrismContainerWrapper<CorrelationItemType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                getValueModel(),
                ItemsSubCorrelatorType.F_ITEM);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<CorrelationItemType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<CorrelationItemType>, String>> columns = new ArrayList<>();

        if (isCheckboxSelectionEnabled() && !isReadOnlyTable()) {
            columns.add(new CheckBoxHeaderColumn<>());
        }

        if (isReadOnlyTable()) {
            columns.add(new IconColumn<>(Model.of()) {
                @Override
                protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<CorrelationItemType>> rowModel) {
                    String iconCss = null;
                    if (rowModel.getObject().getStatus() == ValueStatus.ADDED) {
                        iconCss = GuiStyleConstants.CLASS_PLUS_CIRCLE + " text-success";
                    } else if (rowModel.getObject().getStatus() == ValueStatus.DELETED) {
                        iconCss = GuiStyleConstants.CLASS_MINUS_CIRCLE + " text-danger";
                    } else if (rowModel.getObject().getStatus() == ValueStatus.MODIFIED) {
                        iconCss = GuiStyleConstants.CLASS_EDIT_MENU_ITEM + " text-warning";
                    }
                    return new DisplayType().beginIcon().cssClass(iconCss).end();
                }
            });
        }

        IModel<PrismContainerDefinition<CorrelationItemType>> correlationDef = getCorrelationItemDefinition();

        columns.add(new PrismPropertyWrapperColumn<CorrelationItemType, String>(
                correlationDef,
                CorrelationItemType.F_REF,
                getDefaultColumnType(),
                getPageBase()) {

            @Override
            public String getCssClass() {
                return isCorrelationForAssociation() ? null : "col-3";
            }
        });

        if (isCorrelationForAssociation()) {
            columns.add(new PrismContainerWrapperColumn<>(
                    correlationDef,
                    ItemPath.create(
                            CorrelationItemType.F_SEARCH,
                            ItemSearchDefinitionType.F_FUZZY),
                    getPageBase()) {
                @SuppressWarnings("rawtypes")
                @Override
                protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                    return new Label(componentId, getString("CorrelationItemRefsTable.column.fuzzy.nullValue"));
                }
            });
        } else {
            columns.add(new PrismContainerWrapperColumn<>(
                    correlationDef,
                    ItemPath.create(
                            CorrelationItemType.F_SEARCH,
                            ItemSearchDefinitionType.F_FUZZY),
                    getPageBase()) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                @Override
                protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {

                    if (!isReadOnlyTable()) {
                        ContainersDropDownPanel<SynchronizationActionsType> panel = new ContainersDropDownPanel(
                                componentId,
                                rowModel) {
                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                target.add(findParent(SelectableDataTable.SelectableRowItem.class));
                            }

                            @Override
                            protected String getNullValidDisplayValue() {
                                return getString("CorrelationItemRefsTable.column.fuzzy.nullValue");
                            }
                        };
                        panel.setOutputMarkupId(true);
                        return panel;
                    }
                    return super.createColumnPanel(componentId, rowModel);
                }

                @Override
                public String getCssClass() {
                    return "col-2";
                }
            });

            columns.add(createColumnForPropertyOfFuzzyContainer(
                    LevenshteinDistanceSearchDefinitionType.F_THRESHOLD,
                    "CorrelationItemRefsTable.column.threshold.label",
                    "CorrelationItemRefsTable.column.threshold.help"
            ));
            columns.add(createColumnForPropertyOfFuzzyContainer(
                    LevenshteinDistanceSearchDefinitionType.F_INCLUSIVE,
                    "CorrelationItemRefsTable.column.inclusive.label",
                    "CorrelationItemRefsTable.column.inclusive.help"
            ));
        }

        return columns;
    }

    private boolean isCorrelationForAssociation() {
        var value = getValueModel().getObject();
        return value != null
                && (value.getParentContainerValue(ShadowAssociationDefinitionType.class) != null
                || value.getParentContainerValue(AssociationSynchronizationExpressionEvaluatorType.class) != null);
    }

    @Contract("_, _, _ -> new")
    private @NotNull IColumn<PrismContainerValueWrapper<CorrelationItemType>, String> createColumnForPropertyOfFuzzyContainer(
            ItemName propertyName, String labelKey, String helpKey) {
        return new AbstractColumn<>(
                getPageBase().createStringResource(labelKey)) {

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId, getDisplayModel()) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return getPageBase().createStringResource(helpKey);
                    }
                };
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<CorrelationItemType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<CorrelationItemType>> rowModel) {
                IModel<PrismPropertyWrapper<String>> model = () -> {
                    AtomicReference<ItemName> container = new AtomicReference<>();
                    cellItem.getParent().visitChildren(
                            ContainersDropDownPanel.class,
                            (component, objectIVisit) -> container.set((
                                    (ContainersDropDownPanel<?>) component).getDropDownModel().getObject()));

                    if (container.get() != null) {
                        ItemPath path = ItemPath.create(
                                CorrelationItemType.F_SEARCH,
                                ItemSearchDefinitionType.F_FUZZY,
                                container.get(),
                                propertyName
                        );
                        try {
                            return rowModel.getObject().findProperty(path);
                        } catch (SchemaException e) {
                            LOGGER.error("Couldn't find property of fuzzy container, path:{}", path, e);
                        }
                    }

                    return null;
                };

                Component panel = new PrismPropertyWrapperColumnPanel<>(
                        componentId, model, AbstractItemWrapperColumn.ColumnType.VALUE) {
                    @Override
                    protected IModel<String> getCustomHeaderModel() {
                        return getDisplayModel();
                    }

                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();

                        if (getModelObject() != null) {
                            getValuesPanel().addOrReplace(createValuePanel(ID_VALUE, getModel()));
                        }
                    }
                };
                panel.add(new VisibleBehaviour(() -> model.getObject() != null));
                panel.setOutputMarkupId(true);
                cellItem.add(panel);
            }

            @Override
            public String getCssClass() {
                return "col-2";
            }
        };
    }

    protected LoadableModel<PrismContainerDefinition<CorrelationItemType>> getCorrelationItemDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<CorrelationItemType> load() {
                return getValueModel().getObject().getDefinition().findContainerDefinition(ItemsSubCorrelatorType.F_ITEM);
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CORRELATION_ITEMS_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "CorrelationItemRefsTable.newObject.simple";
    }

    protected AbstractItemWrapperColumn.ColumnType getDefaultColumnType() {
        return isReadOnlyTable() ? AbstractItemWrapperColumn.ColumnType.STRING : AbstractItemWrapperColumn.ColumnType.VALUE;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return !isReadOnlyTable() && super.isCreateNewObjectVisible();
    }

    boolean isCheckboxSelectionEnabled() {
        return !isReadOnlyTable();
    }

    boolean isReadOnlyTable() {
        return false;
    }

    @Override
    public boolean displayNoValuePanel() {
        return getDataProvider().size() == 0;
    }

    @Contract(pure = true)
    @Override
    protected @NotNull String getAdditionalFooterCssClasses() {
        return "bg-white border-top";
    }

    @Override
    protected String getNoValuePanelCssClass() {
        return "";
    }

    @Override
    protected void initNewObjectButton(String idButton, @NotNull List<Component> buttons) {
        super.initNewObjectButton(idButton, buttons);
    }

    public abstract @NotNull IModel<PrismContainerValueWrapper<P>> getMappingContainerParent();
}
