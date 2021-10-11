/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.TableHeadersToolbar;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lazyman
 */
public class CheckBoxHeaderColumn<T extends Serializable> extends CheckBoxColumn<T> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CheckBoxHeaderColumn.class);

    public CheckBoxHeaderColumn() {
        super(null);
    }

    private boolean visible = true;

    @Override
    public Component getHeader(final String componentId) {
        final IModel<Boolean> model = new Model<>(false);
        IsolatedCheckBoxPanel panel = new IsolatedCheckBoxPanel(componentId, model, getEnabled(null)) {

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                DataTable table = findParent(DataTable.class);
                boolean selected = model.getObject() != null ? model.getObject() : false;

                onUpdateHeader(target, selected, table);
            }
        };
        panel.setOutputMarkupId(true);
        panel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return CheckBoxHeaderColumn.this.isCheckboxVisible();
            }

        });

        return panel;
    }


    @Override
    public String getCssClass() {
        return "check";
    }

    protected boolean isCheckboxVisible(){
        return visible;
    }

    public void setCheckboxVisible(boolean visible){
        this.visible = visible;
    }

    /**
     * This method is called after select all checkbox is clicked
     * @param target
     * @param selected
     * @param table
     */
    protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
        IDataProvider provider = table.getDataProvider();
        if (!(provider instanceof BaseSortableDataProvider)) {
            LOGGER.debug("Select all checkbox work only with {} provider type. Current provider is type of {}.",
                    new Object[]{BaseSortableDataProvider.class.getName(), provider.getClass().getName()});
        }

        //update selected flag in model dto objects based on select all header state
        BaseSortableDataProvider baseProvider = (BaseSortableDataProvider) provider;
        List<T> objects = baseProvider.getAvailableData();
        for (T object : objects) {
            if (object instanceof Selectable) {
                Selectable selectable = (Selectable) object;
                selectable.setSelected(selected);
            } else if (object instanceof PrismContainerValueWrapper){
                PrismContainerValueWrapper valueWrapper = (PrismContainerValueWrapper) object;
                valueWrapper.setSelected(selected);
            }
        }

        table.visitChildren(SelectableDataTable.SelectableRowItem.class, new IVisitor<SelectableDataTable.SelectableRowItem, Void>() {

            @Override
            public void component(SelectableDataTable.SelectableRowItem row, IVisit<Void> visit) {
                if (row.getOutputMarkupId()) {
                    //we skip rows that doesn't have outputMarkupId set to true (it would fail)
                    target.add(row);
                }
            }
        });

    }

    public boolean shouldBeHeaderSelected(DataTable table) {
        boolean selectedAll = true;

        BaseSortableDataProvider baseProvider = (BaseSortableDataProvider) table.getDataProvider();
        List<T> objects = baseProvider.getAvailableData();
        if (objects == null || objects.isEmpty()) {
            return false;
        }

        for (T object : objects) {
            selectedAll &= isTableRowSelected(object);
        }

        return selectedAll;
    }

    protected boolean isTableRowSelected(T object){
        if (object instanceof Selectable) {
            Selectable selectable = (Selectable) object;
            return selectable.isSelected();
        } else if (object instanceof PrismContainerValueWrapper){
            PrismContainerValueWrapper valueWrapper = (PrismContainerValueWrapper) object;
            return valueWrapper.isSelected();
        }
        return false;
    }

    /**
     * This method is called after checkbox in row is updated
     */
    @Override
    protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<T> rowModel, IModel<Boolean> selected) {
        //update header checkbox
        IsolatedCheckBoxPanel header = findCheckBoxColumnHeader(table);
        if (header == null) {
            return;
        }

        header.getPanelComponent().setModelObject(shouldBeHeaderSelected(table));
        target.add(header);
    }

    public IsolatedCheckBoxPanel findCheckBoxColumnHeader(DataTable table) {
        WebMarkupContainer topToolbars = table.getTopToolbars();
        TableHeadersToolbar toolbar = topToolbars.visitChildren(TableHeadersToolbar.class, new IVisitor<TableHeadersToolbar, TableHeadersToolbar>() {

            @Override
            public void component(TableHeadersToolbar object, IVisit<TableHeadersToolbar> visit) {
                visit.stop(object);
            }
        });

        if (toolbar == null) {
            return null;
        }

        // simple attempt to find checkbox which is header for our column
        // todo: this search will fail if there are more checkbox header columns (which is not supported now,
        // because Selectable.F_SELECTED is hardcoded all over the place...
        IsolatedCheckBoxPanel ret = toolbar.visitChildren(IsolatedCheckBoxPanel.class, new IVisitor<IsolatedCheckBoxPanel, IsolatedCheckBoxPanel>() {

            @Override
            public void component(IsolatedCheckBoxPanel object, IVisit<IsolatedCheckBoxPanel> visit) {
                if (object.getOutputMarkupId()) {
                    visit.stop(object);
                }
            }
        });

        return ret;
    }
}
