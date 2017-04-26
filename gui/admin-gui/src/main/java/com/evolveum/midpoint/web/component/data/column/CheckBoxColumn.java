/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class CheckBoxColumn<T extends Serializable> extends AbstractColumn<T, String> {

    private String propertyExpression;
    private IModel<Boolean> enabled = new Model<Boolean>(true);

    public CheckBoxColumn(IModel<String> displayModel) {
        this(displayModel, Selectable.F_SELECTED);
    }

    public CheckBoxColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel);
        this.propertyExpression = propertyExpression;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {
        IModel<Boolean> selected = getCheckBoxValueModel(rowModel);

        CheckBoxPanel check = new CheckBoxPanel(componentId, selected, enabled) {

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                DataTable table = findParent(DataTable.class);
                onUpdateRow(target, table, rowModel);

                //updating table row
//                target.add(cellItem.findParent(SelectableDataTable.SelectableRowItem.class));
            }
        };
        check.setOutputMarkupId(true);

        cellItem.add(check);
    }

    protected IModel<Boolean> getCheckBoxValueModel(IModel<T> rowModel){
        return new PropertyModel<Boolean>(rowModel, propertyExpression);
    }

    @Override
    public String getCssClass() {
        return "icon";
    }

    protected IModel<Boolean> getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled.setObject(enabled);
    }

    protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<T> rowModel) {
    }

    protected String getPropertyExpression() {
        return propertyExpression;
    }
}
