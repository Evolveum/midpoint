/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.input.ContainersDropDownPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

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
    protected List<InlineMenuItem> createInlineMenu() {
        return Collections.singletonList(createDeleteItemMenu());
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

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<CorrelationItemType>> correlationDef = getCorrelationItemDefinition();
        columns.add(new PrismPropertyWrapperColumn<CorrelationItemType, String>(
                correlationDef,
                CorrelationItemType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
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
}
