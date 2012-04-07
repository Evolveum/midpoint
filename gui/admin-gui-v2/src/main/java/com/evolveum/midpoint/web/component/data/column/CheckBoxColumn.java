/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.wicket.Component;
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
public class CheckBoxColumn<T extends Serializable> extends AbstractColumn<Selectable<T>> {

    public CheckBoxColumn() {
        super(null);
    }

    @Override
    public void populateItem(Item<ICellPopulator<Selectable<T>>> cellItem, String componentId,
            final IModel<Selectable<T>> rowModel) {
        cellItem.add(new CheckBoxPanel(componentId, new PropertyModel<Boolean>(rowModel, "selected")) {

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                DataTable table = findParent(DataTable.class);
                onUpdateRow(target, table, rowModel);
            }
        });
    }

    @Override
    public String getCssClass() {
        return "tableCheckbox";
    }

    @Override
    public Component getHeader(String componentId) {
        return new CheckBoxPanel(componentId, new Model<Boolean>()) {

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                DataTable table = findParent(DataTable.class);
                onUpdateHeader(target, table);
            }
        };
    }

    public void onUpdateHeader(AjaxRequestTarget target, DataTable table) {
        //todo implement
    }

    public void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<Selectable<T>> rowModel) {
        //todo implement
    }
}
