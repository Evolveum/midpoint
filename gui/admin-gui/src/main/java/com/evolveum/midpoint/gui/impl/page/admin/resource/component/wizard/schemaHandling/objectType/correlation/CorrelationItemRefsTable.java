/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.input.ContainersDropDownPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lskublik
 */
public class CorrelationItemRefsTable extends AbstractWizardTable<CorrelationItemType, ItemsSubCorrelatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItemRefsTable.class);

    public CorrelationItemRefsTable(
            String id,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config, CorrelationItemType.class);
    }

    @Override
    public void editItemPerformed(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<CorrelationItemType>> rowModel,
            List<PrismContainerValueWrapper<CorrelationItemType>> listItems) {
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<CorrelationItemType>> createProvider() {
        return super.createProvider();
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return isReadOnlyTable() ? Collections.emptyList() : Collections.singletonList(createDeleteItemMenu());
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
                @Override
                protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {

                    if (isReadOnlyTable()) {
                        return new Label(componentId, getString("CorrelationItemRefsTable.column.fuzzy.nullValue"));
                    }

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

    private IColumn<PrismContainerValueWrapper<CorrelationItemType>, String> createColumnForPropertyOfFuzzyContainer(
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
        return buttons;
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
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentContainerValue = getValueModel().getObject().getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        ExistingMappingTable<?> existingMappingTable = new ExistingMappingTable<>(
                getPageBase().getMainPopupBodyId(),
                () -> parentContainerValue) {
            @Override
            protected void onAddSelectedMappings(AjaxRequestTarget target, List<PrismContainerValueWrapper<MappingType>> selectedObjects) {
                if (selectedObjects == null || selectedObjects.isEmpty()) {
                    return;
                }

                selectedObjects.forEach(mappingWrapper -> transformAndAddMappingIntoCorrelationItemContainer(mappingWrapper, target));
                refreshTable(target);
                target.add(getNoValuePanel());
            }
        };
        getPageBase().showMainPopup(existingMappingTable, target);
    }

    //TODO check it
    private void transformAndAddMappingIntoCorrelationItemContainer(@NotNull PrismContainerValueWrapper<MappingType> mappingWrapper, AjaxRequestTarget target) {
        MappingType realValueMapping = mappingWrapper.getRealValue();

        IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel = getValueModel();
        IModel<PrismContainerWrapper<CorrelationItemType>> containerModel = createContainerModel(valueModel, ItemPath.create(
                ItemsSubCorrelatorType.F_ITEM));

        PrismContainerWrapper<CorrelationItemType> container = containerModel.getObject();

        var newValue = container.getItem().createNewValue();
        CorrelationItemType bean = newValue.asContainerable();
        bean.setName(realValueMapping.getName());
        bean.setRef(realValueMapping.getTarget().getPath());
        bean.setDescription(realValueMapping.getDescription());

        WebPrismUtil.createNewValueWrapper(container, newValue, getPageBase(), target);
    }

    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel(
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> model, ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }
}
