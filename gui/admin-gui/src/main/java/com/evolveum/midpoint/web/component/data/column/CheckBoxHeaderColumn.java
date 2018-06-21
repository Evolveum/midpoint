/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.TableHeadersToolbar;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.iterator.ComponentHierarchyIterator;

import java.io.Serializable;
import java.util.List;

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
            } else if (object instanceof ContainerValueWrapper){
                ContainerValueWrapper valueWrapper = (ContainerValueWrapper) object;
                valueWrapper.setSelected(selected);
            }
        }

        ComponentHierarchyIterator iterator = table.visitChildren(SelectableDataTable.SelectableRowItem.class);

        while (iterator.hasNext()) {
            SelectableDataTable.SelectableRowItem row = (SelectableDataTable.SelectableRowItem) iterator.next();
            if (!row.getOutputMarkupId()) {
                //we skip rows that doesn't have outputMarkupId set to true (it would fail)
                continue;
            }
            target.add(row);
        }
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
        } else if (object instanceof ContainerValueWrapper){
            ContainerValueWrapper valueWrapper = (ContainerValueWrapper) object;
            return valueWrapper.isSelected();
        }
        return false;
    }

    /**
     * This method is called after checkbox in row is updated
     */
    @Override
    protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<T> rowModel) {
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
        ComponentHierarchyIterator iterator = topToolbars.visitChildren(TableHeadersToolbar.class);
        if (!iterator.hasNext()) {
            return null;
        }

        TableHeadersToolbar toolbar = (TableHeadersToolbar) iterator.next();
        // simple attempt to find checkbox which is header for our column
        // todo: this search will fail if there are more checkbox header columns (which is not supported now,
        // because Selectable.F_SELECTED is hardcoded all over the place...
        iterator = toolbar.visitChildren(IsolatedCheckBoxPanel.class);
        while (iterator.hasNext()) {
            Component c = iterator.next();
            if (!c.getOutputMarkupId()) {
                continue;
            }

            return (IsolatedCheckBoxPanel) c;
        }

        return null;
    }
}
