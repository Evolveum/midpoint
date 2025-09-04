/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.input.ContainersDropDownPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.*;

/**
 * @author lskublik
 */
public class CorrelationItemRefsTable extends AbstractWizardTable<CorrelationItemType, ItemsSubCorrelatorType> {

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
        menu.add(createViewEditMappingItemMenu());
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

    protected InlineMenuItem createViewEditMappingItemMenu() {
        StringResourceModel title = createStringResource("CorrelationItemRefsTable.button.edit");
        if (isReadOnlyTable()) {
            title = createStringResource("CorrelationItemRefsTable.button.view");
        }
        return new ButtonInlineMenuItem(title) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return isReadOnlyTable()
                        ? getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_PREVIEW)
                        : getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createEditMappingColumnAction();
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

    public InlineMenuItemAction createEditMappingColumnAction() {
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

                PrismContainerValueWrapper<InboundMappingType> relatedInboundMapping = findRelatedInboundMapping(row);
                if (relatedInboundMapping == null) {
                    LOGGER.warn("Cannot find related inbound mapping for correlation item: {}", row.getRealValue());
                    getPageBase().warn(getString("CorrelationItem.relatedMappingNotFound"));
                    target.add(getPageBase().getFeedbackPanel().getParent());
                    return;
                }

                var panel = new CorrelationMappingFormPanel<InboundMappingType>(
                        getPageBase().getMainPopupBodyId(),
                        () -> relatedInboundMapping) {

                    @Override
                    protected boolean isCancelButtonVisible() {
                        return isReadOnlyTable();
                    }

                    @Override
                    protected boolean isReadOnlyMapping() {
                        return isReadOnlyTable();
                    }

                    @Override
                    public IModel<String> getTitle() {
                        return createStringResource("CorrelationMappingFormPanel.title.configuration");
                    }

                    @Override
                    protected boolean isShowEmptyButtonVisible() {
                        return !isReadOnlyTable();
                    }

                    @Contract(" -> new")
                    @Override
                    protected @NotNull IModel<String> getConfirmButtonIcon() {
                        return Model.of("fa fa-check");
                    }

                    @Override
                    protected IModel<String> getConfirmButtonLabel() {
                        return createStringResource("CorrelationMappingFormPanel.confirm");
                    }

                    @Override
                    protected void performCreateMapping(AjaxRequestTarget target) {
                        PrismContainerValueWrapper<CorrelationItemType> object = getRowModel().getObject();
                        CorrelationItemType itemRealValue = object.getRealValue();
                        InboundMappingType inboundRealValue = relatedInboundMapping.getRealValue();
                        ItemPathType itemRef = itemRealValue.getRef();
                        ItemPathType mappingPath = inboundRealValue.getTarget().getPath();
                        if(itemRef != null && mappingPath != null && !itemRef.equivalent(mappingPath)) {
                            itemRealValue.setRef(mappingPath);
                            itemRealValue.setName(inboundRealValue.getName());
                        }
                        refreshTablePanel(target);
                    }
                };

                getPageBase().showMainPopup(panel, target);
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

        if (isCheckboxSelectionEnabled()) {
            columns.add(new CheckBoxHeaderColumn<>());
        }

        IModel<PrismContainerDefinition<CorrelationItemType>> correlationDef = getCorrelationItemDefinition();
        columns.add(new PrismPropertyWrapperColumn<CorrelationItemType, String>(
                correlationDef,
                CorrelationItemType.F_REF,
                getDefaultColumnType(),
                getPageBase()) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CorrelationItemType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<CorrelationItemType>> rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
            }

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
                    return "col-3";
                }
            });

            columns.add(createColumnForPropertyOfFuzzyContainer(
                    LevenshteinDistanceSearchDefinitionType.F_THRESHOLD,
                    "CorrelationItemRefsTable.column.threshold.label",
                    "CorrelationItemRefsTable.column.threshold.help",
                    "col-3"));
            columns.add(createColumnForPropertyOfFuzzyContainer(
                    LevenshteinDistanceSearchDefinitionType.F_INCLUSIVE,
                    "CorrelationItemRefsTable.column.inclusive.label",
                    "CorrelationItemRefsTable.column.inclusive.help",
                    "col-2"));
        }

        return columns;
    }

    private boolean isCorrelationForAssociation() {
        var associationParent = getValueModel().getObject().getParentContainerValue(ShadowAssociationDefinitionType.class);
        return associationParent != null;
    }

    @Contract("_, _, _, _ -> new")
    private @NotNull IColumn<PrismContainerValueWrapper<CorrelationItemType>, String> createColumnForPropertyOfFuzzyContainer(
            ItemName propertyName, String labelKey, String helpKey, String cssClass) {
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
                            (component, objectIVisit) -> container.set(((ContainersDropDownPanel<?>) component).getDropDownModel().getObject()));

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
                            LOGGER.error("Couldn't find property of fuzzy container, path:" + path, e);
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
                return cssClass;
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
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttons = new ArrayList<>();
        initAddExistingButton(idButton, buttons);
        initNewObjectButton(idButton, buttons);
        iniCreateMappingButton(idButton, buttons);
        return buttons;
    }

    protected void iniCreateMappingButton(String idButton, @NotNull List<Component> buttons) {
        AjaxIconButton newObjectButton = new AjaxIconButton(
                idButton,
                new Model<>("fa fa-circle-plus"),
                createStringResource("CorrelationItemRefsTable.createMapping")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createMappingPerformed(target);
            }
        };
        newObjectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm ml-2"));
        newObjectButton.showTitleAsLabel(true);
        newObjectButton.add(new VisibleBehaviour(this::isCreateNewObjectVisible));
        buttons.add(newObjectButton);
    }

    protected void createMappingPerformed(AjaxRequestTarget target) {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentContainerValue = getValueModel().getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        PrismContainerValueWrapper<MappingType> newMappingValue = createNewMappingValue(
                getPageBase(), null, parentContainerValue, target);
        CorrelationMappingFormPanel<MappingType> formCorrelationMappingPanel = new CorrelationMappingFormPanel<>(
                getPageBase().getMainPopupBodyId(),
                () -> newMappingValue) {
            @Override
            protected void onCancel(AjaxRequestTarget target) {
                discardDraftMapping(getPageBase(), newMappingValue);
                refreshTablePanel(target);
                super.onCancel(target);
            }

            @Override
            protected void performCreateMapping(AjaxRequestTarget target) {
                transformAndAddMappingIntoCorrelationItemContainer(getPageBase(), getValueModel(), newMappingValue, target);
                refreshTablePanel(target);
            }
        };

        getPageBase().showMainPopup(formCorrelationMappingPanel, target);
    }

    private void refreshTablePanel(AjaxRequestTarget target) {
        refreshTable(target);
        target.add(getNoValuePanel());
    }

    protected void initAddExistingButton(String idButton, @NotNull List<Component> buttons) {
        AjaxIconButton addExistingButton = new AjaxIconButton(
                idButton,
                new Model<>("fa fas fa-link"),
                createStringResource("CorrelationItemRefsTable.addExisting")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addExistingMappingPerformed(target);
            }
        };
        addExistingButton.add(AttributeAppender.append("class", "btn btn-default btn-sm mr-2"));
        addExistingButton.showTitleAsLabel(true);
        buttons.add(addExistingButton);
    }

    protected void addExistingMappingPerformed(AjaxRequestTarget target) {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentContainerValue = getValueModel().getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        CorrelationExistingMappingTable<?> correlationExistingMappingTable = new CorrelationExistingMappingTable<>(
                getPageBase().getMainPopupBodyId(),
                () -> parentContainerValue) {
            @Override
            protected void onAddSelectedMappings(AjaxRequestTarget target, List<PrismContainerValueWrapper<MappingType>> selectedObjects) {
                if (selectedObjects == null || selectedObjects.isEmpty()) {
                    return;
                }
                selectedObjects.forEach(mappingWrapper -> transformAndAddMappingIntoCorrelationItemContainer(
                        getPageBase(), getValueModel(), mappingWrapper, target));
                refreshTablePanel(target);
            }
        };
        getPageBase().showMainPopup(correlationExistingMappingTable, target);
    }
}
