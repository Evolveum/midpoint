/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.input.ContainersDropDownPanel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lskublik
 */
public class CorrelationItemRefsTable extends MultivalueContainerListPanel<CorrelationItemType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItemRefsTable.class);

    private final IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel;

    public CorrelationItemRefsTable(
            String id,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel) {
        super(id, CorrelationItemType.class);
        this.valueModel = valueModel;
    }

    @Override
    protected boolean isHeaderVisible() {
        return false;
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttons = super.createToolbarButtonsList(idButton);
        buttons.forEach(button -> {
            if (button instanceof AjaxIconButton) {
                ((AjaxIconButton) button).showTitleAsLabel(true);
            }
        });
        return buttons;
    }

    @Override
    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec) {
        createNewItems(target);
        refreshTable(target);
    }

    @Override
    protected void editItemPerformed(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<CorrelationItemType>> rowModel,
            List<PrismContainerValueWrapper<CorrelationItemType>> listItems) {
    }

    private PrismContainerValueWrapper createNewItems(AjaxRequestTarget target) {
        PrismContainerWrapper<CorrelationItemType> container = getContainerModel().getObject();
        PrismContainerValue<CorrelationItemType> newReaction = container.getItem().createNewValue();
        PrismContainerValueWrapper<CorrelationItemType> newReactionWrapper =
                createNewItemContainerValueWrapper(newReaction, container, target);
        return newReactionWrapper;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        });
        return items;
    }

    @Override
    protected IModel<PrismContainerWrapper<CorrelationItemType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                valueModel,
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
                return "col-3";
            }
        });

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
                correlationDef,
                LevenshteinDistanceSearchDefinitionType.F_THRESHOLD,
                "col-3"));
        columns.add(createColumnForPropertyOfFuzzyContainer(
                correlationDef,
                LevenshteinDistanceSearchDefinitionType.F_INCLUSIVE,
                "col-2"));

        return columns;
    }

    private IColumn<PrismContainerValueWrapper<CorrelationItemType>, String> createColumnForPropertyOfFuzzyContainer(
            IModel<PrismContainerDefinition<CorrelationItemType>> correlationDef, ItemName propertyName, String cssClass) {
        return new PrismPropertyWrapperColumn<CorrelationItemType, String>(
                correlationDef,
                ItemPath.create(
                        CorrelationItemType.F_SEARCH,
                        ItemSearchDefinitionType.F_FUZZY,
                        FuzzySearchDefinitionType.F_LEVENSHTEIN,
                        propertyName),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {

            @Override
            protected boolean isHelpTextVisible(boolean originalHelpTextVisible) {
                return false;
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<CorrelationItemType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<CorrelationItemType>> rowModel) {
                IModel<PrismPropertyWrapper<String>> model = () -> {
                    AtomicReference<ItemName> container = new AtomicReference<>();
                    cellItem.getParent().visitChildren(ContainersDropDownPanel.class, (component, objectIVisit) -> {
                        container.set((ItemName) ((ContainersDropDownPanel) component).getDropDownModel().getObject());
                    });

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

                Component panel = createColumnPanel(componentId, model);
                panel.add(new VisibleBehaviour(() -> model.getObject() != null));
                cellItem.add(panel);
            }

            @Override
            public String getCssClass() {
                return cssClass;
            }
        };
    }

    public boolean isValidFormComponents() {
        AtomicReference<Boolean> valid = new AtomicReference<>(true);
        getTable().visitChildren(SelectableDataTable.SelectableRowItem.class, (row, object) -> {
            ((SelectableDataTable.SelectableRowItem) row).visitChildren(FormComponent.class, (baseFormComponent, object2) -> {
                if (baseFormComponent.hasErrorMessage()) {
                    valid.set(false);
                }
            });
        });
        return valid.get();
    }

    protected LoadableModel<PrismContainerDefinition<CorrelationItemType>> getCorrelationItemDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<CorrelationItemType> load() {
                return valueModel.getObject().getDefinition().findContainerDefinition(ItemsSubCorrelatorType.F_ITEM);
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CORRELATION_ITEMS_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "CorrelationItemRefTable.newObject";
    }
}
